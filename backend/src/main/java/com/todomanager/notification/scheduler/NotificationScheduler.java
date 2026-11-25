package com.todomanager.notification.scheduler;

import com.todomanager.notification.websocket.NotificationBroadcaster;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Single scheduler for all notification sockets (architecture §10.3). */
@Component
public class NotificationScheduler {

	private final NotificationBroadcaster broadcaster;

	public NotificationScheduler(NotificationBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@Scheduled(fixedDelayString = "${notifications.interval-ms:60000}")
	public void refresh() {
		broadcaster.refreshAll();
	}

	@Scheduled(fixedRate = 30_000)
	public void ping() {
		broadcaster.pingAll();
	}

}
