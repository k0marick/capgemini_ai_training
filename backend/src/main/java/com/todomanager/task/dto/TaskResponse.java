package com.todomanager.task.dto;

import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		UUID projectId,
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
				task.getStatus(), task.getPriority(), task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt(),
				task.getVersion());
	}

}
