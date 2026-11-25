package com.todomanager.task.dto;

import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskUpdateRequest(
		@NotBlank @Size(max = 255) String title,
		@Size(max = 5000) String description,
		@NotNull TaskStatus status,
		@NotNull TaskPriority priority,
		LocalDate dueDate,
		@NotNull Long version) {
}
