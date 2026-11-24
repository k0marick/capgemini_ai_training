package com.todomanager.notification.dto;

import com.todomanager.notification.NotificationType;
import com.todomanager.task.entity.TaskPriority;
import java.time.LocalDate;
import java.util.UUID;

public record NotificationItem(
		NotificationType type,
		UUID taskId,
		UUID projectId,
		String title,
		LocalDate dueDate,
		TaskPriority priority) {
}
