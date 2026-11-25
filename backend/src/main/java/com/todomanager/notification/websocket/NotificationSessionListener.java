package com.todomanager.notification.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

/** Logout and session expiry close the sockets opened under that session (close 4401, architecture §10.3). */
@Component
class NotificationSessionListener {

	private final NotificationBroadcaster broadcaster;

	NotificationSessionListener(NotificationBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@EventListener
	void onSessionDestroyed(SessionDestroyedEvent event) {
		broadcaster.closeHttpSession(event.getId());
	}

}
