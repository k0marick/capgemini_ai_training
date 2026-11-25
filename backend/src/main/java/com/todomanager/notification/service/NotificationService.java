package com.todomanager.notification.service;

import com.todomanager.notification.NotificationType;
import com.todomanager.notification.dto.NotificationItem;
import com.todomanager.task.service.TaskService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Builds the owner-scoped notification batch for one user. Goes through TaskService, never the task repository. */
@Service
public class NotificationService {

	private final TaskService tasks;

	public NotificationService(TaskService tasks) {
		this.tasks = tasks;
	}

	public List<NotificationItem> batchFor(UUID userId) {
		return tasks.findDueForNotifications(userId).stream()
				.map(item -> new NotificationItem(
						item.overdue() ? NotificationType.OVERDUE : NotificationType.DUE_SOON,
						item.taskId(), item.projectId(), item.title(), item.dueDate(), item.priority()))
				.toList();
	}

}
