package com.todomanager.notification.websocket;

import com.todomanager.task.event.TasksChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Pushes fresh alerts to a user's open sockets as soon as their task change is committed. */
@Component
class TasksChangedListener {

	private final NotificationBroadcaster broadcaster;

	TasksChangedListener(NotificationBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onTasksChanged(TasksChangedEvent event) {
		broadcaster.refreshUser(event.userId());
	}

}
