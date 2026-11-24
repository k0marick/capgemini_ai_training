package com.todomanager.task.dto;

import com.todomanager.task.entity.TaskPriority;
import java.time.LocalDate;
import java.util.UUID;

/** Read model for notifications: a task due within the window, with its overdue classification. */
public record TaskDueItem(UUID taskId, UUID projectId, String title, LocalDate dueDate, TaskPriority priority,
		boolean overdue) {
}
