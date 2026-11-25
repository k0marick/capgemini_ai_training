package com.todomanager.task.event;

import java.util.UUID;

/** Published when a user's tasks are created, changed or deleted. */
public record TasksChangedEvent(UUID userId) {
}
