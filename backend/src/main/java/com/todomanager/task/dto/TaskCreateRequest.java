package com.todomanager.task.dto;

import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** status and priority default to TODO and MEDIUM when omitted. */
public record TaskCreateRequest(
		@NotBlank @Size(max = 255) String title,
		@Size(max = 5000) String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate) {
}
