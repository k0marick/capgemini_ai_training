package com.todomanager.task.dto;

import com.todomanager.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
		@NotNull TaskStatus status,
		@NotNull Long version) {
}
