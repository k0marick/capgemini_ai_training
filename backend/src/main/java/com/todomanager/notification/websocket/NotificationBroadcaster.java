package com.todomanager.notification.websocket;

import com.todomanager.notification.NotificationType;
import com.todomanager.notification.dto.NotificationItem;
import com.todomanager.notification.service.NotificationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends protocol frames (architecture §10.2–10.3). Every send goes through the connection's decorated session,
 * which serializes writes per connection.
 */
@Component
public class NotificationBroadcaster {

	static final int CLOSE_SESSION_ENDED = 4401;

	private static final Logger log = LoggerFactory.getLogger(NotificationBroadcaster.class);

	private final NotificationRegistry registry;
	private final NotificationService notifications;
	private final ObjectMapper json;

	NotificationBroadcaster(NotificationRegistry registry, NotificationService notifications, ObjectMapper json) {
		this.registry = registry;
		this.notifications = notifications;
		this.json = json;
	}

	/**
	 * Sends the batch filtered by the connection's subscription. Skipped when unchanged, unless forced
	 * (initial batch and SUBSCRIBE). An empty batch is sent once so stale items are cleared.
	 */
	void push(NotificationConnection connection, List<NotificationItem> batch, boolean force) {
		List<NotificationItem> filtered = batch.stream()
				.filter(item -> connection.enabledTypes().contains(item.type()))
				.toList();
		String payload = write(Map.of("notifications", filtered));
		synchronized (connection) {
			if (!force && payload.equals(connection.lastPayload())) {
				return;
			}
			connection.lastPayload(payload);
		}
		send(connection, frame("NOTIFICATION_BATCH", payload));
	}

	void sendSubscriptionUpdated(NotificationConnection connection, Set<NotificationType> enabled) {
		send(connection, frame("SUBSCRIPTION_UPDATED", write(Map.of("enabledTypes", enabled))));
	}

	void sendError(NotificationConnection connection, String code, String message) {
		send(connection, frame("ERROR", write(Map.of("code", code, "message", message))));
	}

	/** Recomputes each connected user's batch once and pushes changes to that user's connections. */
	public void refreshAll() {
		for (UUID userId : registry.userIds()) {
			refreshUser(userId);
		}
		registry.all().removeIf(connection -> {
			boolean closed = !connection.session().isOpen();
			if (closed) {
				log.debug("Dropping closed notification connection {}", connection.id());
			}
			return closed;
		});
	}

	/** Recomputes one user's batch and pushes changes to that user's connections. */
	public void refreshUser(UUID userId) {
		List<NotificationItem> batch = notifications.batchFor(userId);
		for (NotificationConnection connection : registry.forUser(userId)) {
			push(connection, batch, false);
		}
	}

	/** Protocol-level ping keeps idle connections alive through proxies (architecture §10.1). */
	public void pingAll() {
		for (NotificationConnection connection : registry.all()) {
			try {
				connection.session().sendMessage(new PingMessage());
			}
			catch (IOException | RuntimeException e) {
				log.debug("Ping failed for connection {}", connection.id(), e);
			}
		}
	}

	/** Closes every socket opened under the given HTTP session (logout or expiry). */
	void closeHttpSession(String httpSessionId) {
		for (NotificationConnection connection : registry.forHttpSession(httpSessionId)) {
			registry.remove(connection.id());
			try {
				connection.session().close(new CloseStatus(CLOSE_SESSION_ENDED, "session-ended"));
			}
			catch (IOException | RuntimeException e) {
				log.debug("Close failed for connection {}", connection.id(), e);
			}
		}
	}

	private void send(NotificationConnection connection, String text) {
		try {
			connection.session().sendMessage(new TextMessage(text));
		}
		catch (IOException | RuntimeException e) {
			log.debug("Send failed for connection {}", connection.id(), e);
		}
	}

	private String frame(String type, String payloadJson) {
		return "{\"type\":\"" + type + "\",\"payload\":" + payloadJson + "}";
	}

	private String write(Object value) {
		return json.writeValueAsString(value);
	}

}
