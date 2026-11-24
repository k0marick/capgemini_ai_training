package com.todomanager.task.service;

import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import java.util.UUID;

/** Optional list filters; null means "no restriction". */
public record TaskFilter(UUID projectId, TaskStatus status, TaskPriority priority) {
}
