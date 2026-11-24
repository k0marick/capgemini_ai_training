package com.todomanager.notification.websocket;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Open notification connections, keyed by connection ID. Lookups by user and HTTP session scan the map. */
@Component
class NotificationRegistry {

	private final Map<String, NotificationConnection> connections = new ConcurrentHashMap<>();

	void add(NotificationConnection connection) {
		connections.put(connection.id(), connection);
	}

	void remove(String connectionId) {
		connections.remove(connectionId);
	}

	Collection<NotificationConnection> all() {
		return connections.values();
	}

	Set<UUID> userIds() {
		return connections.values().stream().map(NotificationConnection::userId).collect(Collectors.toUnmodifiableSet());
	}

	List<NotificationConnection> forUser(UUID userId) {
		return connections.values().stream().filter(c -> c.userId().equals(userId)).toList();
	}

	List<NotificationConnection> forHttpSession(String httpSessionId) {
		return connections.values().stream().filter(c -> c.httpSessionId().equals(httpSessionId)).toList();
	}

}
