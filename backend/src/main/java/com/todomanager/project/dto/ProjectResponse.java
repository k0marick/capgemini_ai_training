package com.todomanager.project.dto;

import java.time.Instant;
import java.util.UUID;

/** taskCount is Long so the JPQL constructor expression (count returns Long) binds without conversion. */
public record ProjectResponse(
		UUID id,
		String name,
		Long taskCount,
		Instant createdAt,
		Instant updatedAt) {
}
