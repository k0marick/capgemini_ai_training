package com.todomanager.notification.websocket;

import com.todomanager.notification.NotificationType;
import com.todomanager.notification.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Handles one notification socket: initial batch, SUBSCRIBE, error replies and rate limiting. */
@Component
class NotificationWebSocketHandler extends TextWebSocketHandler {

	/** Max inbound text frame (architecture §10.1). */
	static final int MAX_TEXT_FRAME_BYTES = 4096;
	private static final int CLOSE_TOO_BIG = 1009;
	private static final String ATTR_CONNECTION = "connection";
	private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
	private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

	private final NotificationRegistry registry;
	private final NotificationBroadcaster broadcaster;
	private final NotificationService notifications;
	private final ObjectMapper json;

	NotificationWebSocketHandler(NotificationRegistry registry, NotificationBroadcaster broadcaster,
			NotificationService notifications, ObjectMapper json) {
		this.registry = registry;
		this.broadcaster = broadcaster;
		this.notifications = notifications;
		this.json = json;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		UUID userId = (UUID) session.getAttributes().get(NotificationHandshakeInterceptor.ATTR_USER_ID);
		String httpSessionId = (String) session.getAttributes().get(NotificationHandshakeInterceptor.ATTR_HTTP_SESSION_ID);
		WebSocketSession guarded = new ConcurrentWebSocketSessionDecorator(session,
				SEND_TIME_LIMIT_MILLIS, SEND_BUFFER_LIMIT_BYTES);
		NotificationConnection connection = new NotificationConnection(userId, httpSessionId, guarded);
		session.getAttributes().put(ATTR_CONNECTION, connection);
		registry.add(connection);
		broadcaster.push(connection, notifications.batchFor(userId), true);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		NotificationConnection connection = connection(session);
		if (message.getPayload().getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_FRAME_BYTES) {
			closeQuietly(connection, new CloseStatus(CLOSE_TOO_BIG, "frame too large"));
			return;
		}
		if (!connection.allowMessage(System.currentTimeMillis())) {
			broadcaster.sendError(connection, "RATE_LIMITED", "too many messages");
			closeQuietly(connection, CloseStatus.POLICY_VIOLATION.withReason("rate limited"));
			return;
		}
		try {
			JsonNode root = json.readTree(message.getPayload());
			String type = root.path("type").isString() ? root.path("type").asString() : "";
			if (!"SUBSCRIBE".equals(type)) {
				broadcaster.sendError(connection, "UNKNOWN_TYPE", "unknown message type");
				return;
			}
			subscribe(connection, root.path("payload"));
		}
		catch (JacksonException e) {
			broadcaster.sendError(connection, "INVALID_JSON", "message is not valid JSON");
		}
		catch (ProtocolException e) {
			broadcaster.sendError(connection, e.code, e.getMessage());
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		registry.remove(connection(session).id());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		registry.remove(connection(session).id());
	}

	private void subscribe(NotificationConnection connection, JsonNode payload) {
		if (!payload.isObject()) {
			throw new ProtocolException("INVALID_PAYLOAD", "payload must be an object");
		}
		for (String field : payload.propertyNames()) {
			if (!"enabledTypes".equals(field)) {
				throw new ProtocolException("INVALID_PAYLOAD", "unexpected field: " + field);
			}
		}
		JsonNode types = payload.get("enabledTypes");
		if (types == null || !types.isArray()) {
			throw new ProtocolException("INVALID_PAYLOAD", "enabledTypes must be an array");
		}
		if (types.size() > NotificationType.values().length) {
			throw new ProtocolException("INVALID_PAYLOAD", "enabledTypes has too many entries");
		}
		Set<NotificationType> enabled = EnumSet.noneOf(NotificationType.class);
		for (JsonNode node : types) {
			NotificationType type = node.isString() ? parseType(node.asString()) : null;
			if (type == null) {
				throw new ProtocolException("INVALID_PAYLOAD", "enabledTypes contains unknown value");
			}
			if (!enabled.add(type)) {
				throw new ProtocolException("INVALID_PAYLOAD", "enabledTypes contains duplicates");
			}
		}
		connection.enabledTypes(enabled);
		broadcaster.sendSubscriptionUpdated(connection, enabled);
		broadcaster.push(connection, notifications.batchFor(connection.userId()), true);
	}

	private static NotificationType parseType(String value) {
		for (NotificationType type : NotificationType.values()) {
			if (type.name().equals(value)) {
				return type;
			}
		}
		return null;
	}

	private void closeQuietly(NotificationConnection connection, CloseStatus status) {
		registry.remove(connection.id());
		try {
			connection.session().close(status);
		}
		catch (Exception e) {
			// the connection is already unusable; nothing else to do
		}
	}

	private static NotificationConnection connection(WebSocketSession session) {
		return (NotificationConnection) session.getAttributes().get(ATTR_CONNECTION);
	}

	/** Protocol violation that maps to an ERROR frame with the given code. */
	private static final class ProtocolException extends RuntimeException {

		private final String code;

		ProtocolException(String code, String message) {
			super(message);
			this.code = code;
		}

	}

}
