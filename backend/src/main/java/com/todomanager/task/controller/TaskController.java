package com.todomanager.task.controller;

import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.common.web.PageResponse;
import com.todomanager.task.dto.TaskCreateRequest;
import com.todomanager.task.dto.TaskResponse;
import com.todomanager.task.dto.TaskStatusRequest;
import com.todomanager.task.dto.TaskUpdateRequest;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import com.todomanager.task.service.TaskFilter;
import com.todomanager.task.service.TaskService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class TaskController {

	private final TaskService tasks;

	TaskController(TaskService tasks) {
		this.tasks = tasks;
	}

	@GetMapping("/tasks")
	PageResponse<TaskResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
			@RequestParam(required = false) UUID projectId,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String sort) {
		return tasks.list(principal.userId(), new TaskFilter(projectId, status, priority), page, size, sort);
	}

	@PostMapping("/projects/{projectId}/tasks")
	@ResponseStatus(HttpStatus.CREATED)
	TaskResponse create(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID projectId,
			@Valid @RequestBody TaskCreateRequest body) {
		return tasks.create(principal.userId(), projectId, body);
	}

	@GetMapping("/tasks/{id}")
	TaskResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		return tasks.get(principal.userId(), id);
	}

	@PatchMapping("/tasks/{id}")
	TaskResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody TaskUpdateRequest body) {
		return tasks.update(principal.userId(), id, body);
	}

	@PatchMapping("/tasks/{id}/status")
	TaskResponse changeStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody TaskStatusRequest body) {
		return tasks.changeStatus(principal.userId(), id, body);
	}

	@DeleteMapping("/tasks/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		tasks.delete(principal.userId(), id);
	}

}
