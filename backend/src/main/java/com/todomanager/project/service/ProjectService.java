package com.todomanager.project.service;

import com.todomanager.common.exception.ConflictException;
import com.todomanager.common.exception.ResourceNotFoundException;
import com.todomanager.project.dto.ProjectResponse;
import com.todomanager.project.entity.Project;
import com.todomanager.project.repository.ProjectRepository;
import com.todomanager.task.service.TaskService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

	private static final String HAS_TASKS = "Project still has tasks; delete them first";

	private final ProjectRepository projects;
	private final TaskService tasks;
	private final Clock clock;

	public ProjectService(ProjectRepository projects, TaskService tasks, Clock clock) {
		this.projects = projects;
		this.tasks = tasks;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<ProjectResponse> list(UUID userId) {
		return projects.findSummariesByUserId(userId);
	}

	@Transactional(readOnly = true)
	public ProjectResponse get(UUID userId, UUID id) {
		return summary(owned(userId, id));
	}

	@Transactional
	public ProjectResponse create(UUID userId, String name) {
		Project project = projects.save(Project.create(UUID.randomUUID(), userId, name, clock.instant()));
		return summary(project);
	}

	@Transactional
	public ProjectResponse rename(UUID userId, UUID id, String name) {
		Project project = owned(userId, id);
		project.rename(name, clock.instant());
		return summary(project);
	}

	/** Block-until-empty (ADR-004): refuses while tasks exist; the FK RESTRICT is the race backstop. */
	@Transactional
	public void delete(UUID userId, UUID id) {
		Project project = owned(userId, id);
		if (tasks.hasTasks(project.getId())) {
			throw new ConflictException(HAS_TASKS);
		}
		try {
			projects.delete(project);
			projects.flush();
		}
		catch (DataIntegrityViolationException raced) {
			throw new ConflictException(HAS_TASKS);
		}
	}

	private Project owned(UUID userId, UUID id) {
		return projects.findByIdAndUserId(id, userId).orElseThrow(ResourceNotFoundException::new);
	}

	private ProjectResponse summary(Project project) {
		return new ProjectResponse(project.getId(), project.getName(), tasks.countByProject(project.getId()),
				project.getCreatedAt(), project.getUpdatedAt());
	}

}
