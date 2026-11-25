package com.todomanager.notification.websocket;

import com.todomanager.notification.NotificationType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.socket.WebSocketSession;

/** One open notification socket. Writes go through the decorated session, which serializes them. */
final class NotificationConnection {

	private static final int MAX_MESSAGES_PER_WINDOW = 20;
	private static final long WINDOW_MILLIS = 10_000;

	private final String id = UUID.randomUUID().toString();
	private final UUID userId;
	private final String httpSessionId;
	private final WebSocketSession session;
	private final Deque<Long> recentMessages = new ArrayDeque<>();
	private volatile Set<NotificationType> enabledTypes = EnumSet.allOf(NotificationType.class);
	private String lastPayload;

	NotificationConnection(UUID userId, String httpSessionId, WebSocketSession session) {
		this.userId = userId;
		this.httpSessionId = httpSessionId;
		this.session = session;
	}

	String id() {
		return id;
	}

	UUID userId() {
		return userId;
	}

	String httpSessionId() {
		return httpSessionId;
	}

	WebSocketSession session() {
		return session;
	}

	Set<NotificationType> enabledTypes() {
		return enabledTypes;
	}

	void enabledTypes(Set<NotificationType> types) {
		this.enabledTypes = Set.copyOf(types);
	}

	/** Last payload sent on this connection; used to skip unchanged batches. Guard with synchronized (this). */
	String lastPayload() {
		return lastPayload;
	}

	void lastPayload(String payload) {
		this.lastPayload = payload;
	}

	/** Sliding-window rate limit for inbound messages. Called only from the connection's receive thread. */
	boolean allowMessage(long nowMillis) {
		while (!recentMessages.isEmpty() && nowMillis - recentMessages.peekFirst() > WINDOW_MILLIS) {
			recentMessages.pollFirst();
		}
		if (recentMessages.size() >= MAX_MESSAGES_PER_WINDOW) {
			return false;
		}
		recentMessages.addLast(nowMillis);
		return true;
	}

}
