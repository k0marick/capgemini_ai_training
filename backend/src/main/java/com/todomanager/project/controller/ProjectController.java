package com.todomanager.project.controller;

import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.project.dto.ProjectRequest;
import com.todomanager.project.dto.ProjectResponse;
import com.todomanager.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
class ProjectController {

	private final ProjectService projects;

	ProjectController(ProjectService projects) {
		this.projects = projects;
	}

	@GetMapping
	List<ProjectResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
		return projects.list(principal.userId());
	}

	@PostMapping
	ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ProjectRequest body) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projects.create(principal.userId(), body.name()));
	}

	@GetMapping("/{id}")
	ProjectResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		return projects.get(principal.userId(), id);
	}

	@PatchMapping("/{id}")
	ProjectResponse rename(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
			@Valid @RequestBody ProjectRequest body) {
		return projects.rename(principal.userId(), id, body.name());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
		projects.delete(principal.userId(), id);
	}

}
