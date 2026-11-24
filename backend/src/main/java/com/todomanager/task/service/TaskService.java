package com.todomanager.task.service;

import com.todomanager.common.exception.BadRequestException;
import com.todomanager.common.exception.ConflictException;
import com.todomanager.common.exception.ResourceNotFoundException;
import com.todomanager.common.web.PageResponse;
import com.todomanager.project.repository.ProjectRepository;
import com.todomanager.task.dto.TaskCreateRequest;
import com.todomanager.task.dto.TaskResponse;
import com.todomanager.task.dto.TaskStatusRequest;
import com.todomanager.task.dto.TaskUpdateRequest;
import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import com.todomanager.task.repository.TaskRepository;
import com.todomanager.task.specification.TaskSpecification;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private static final Set<String> SORTABLE = Set.of("dueDate", "priority", "createdAt", "title", "status");
	private static final String DEFAULT_SORT = "dueDate,asc";
	private static final int MAX_PAGE_SIZE = 100;

	private final TaskRepository tasks;
	private final ProjectRepository projects;
	private final Clock clock;

	public TaskService(TaskRepository tasks, ProjectRepository projects, Clock clock) {
		this.tasks = tasks;
		this.projects = projects;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResponse> list(UUID userId, TaskFilter filter, int page, int size, String sort) {
		Pageable pageable = pageable(page, size, sort);
		Specification<Task> spec = TaskSpecification.belongsToUser(userId);
		if (filter.projectId() != null) {
			spec = spec.and(TaskSpecification.inProject(filter.projectId()));
		}
		if (filter.status() != null) {
			spec = spec.and(TaskSpecification.hasStatus(filter.status()));
		}
		if (filter.priority() != null) {
			spec = spec.and(TaskSpecification.hasPriority(filter.priority()));
		}
		return PageResponse.of(tasks.findAll(spec, pageable).map(TaskResponse::from));
	}

	@Transactional(readOnly = true)
	public TaskResponse get(UUID userId, UUID id) {
		return TaskResponse.from(owned(userId, id));
	}

	@Transactional
	public TaskResponse create(UUID userId, UUID projectId, TaskCreateRequest body) {
		projects.findByIdAndUserId(projectId, userId).orElseThrow(ResourceNotFoundException::new);
		Task task = Task.create(UUID.randomUUID(), projectId, body.title(), body.description(),
				body.status() != null ? body.status() : TaskStatus.TODO,
				body.priority() != null ? body.priority() : TaskPriority.MEDIUM,
				body.dueDate(), clock.instant());
		return TaskResponse.from(tasks.saveAndFlush(task));
	}

	@Transactional
	public TaskResponse update(UUID userId, UUID id, TaskUpdateRequest body) {
		Task task = owned(userId, id);
		requireVersion(task, body.version());
		task.update(body.title(), body.description(), body.status(), body.priority(), body.dueDate(), clock.instant());
		return TaskResponse.from(tasks.saveAndFlush(task));
	}

	@Transactional
	public TaskResponse changeStatus(UUID userId, UUID id, TaskStatusRequest body) {
		Task task = owned(userId, id);
		requireVersion(task, body.version());
		task.changeStatus(body.status(), clock.instant());
		return TaskResponse.from(tasks.saveAndFlush(task));
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		tasks.delete(owned(userId, id));
	}

	@Transactional(readOnly = true)
	public long countByProject(UUID projectId) {
		return tasks.countByProjectId(projectId);
	}

	@Transactional(readOnly = true)
	public boolean hasTasks(UUID projectId) {
		return tasks.existsByProjectId(projectId);
	}

	private Task owned(UUID userId, UUID id) {
		return tasks.findOwnedById(id, userId).orElseThrow(ResourceNotFoundException::new);
	}

	private static void requireVersion(Task task, Long expected) {
		if (!task.getVersion().equals(expected)) {
			throw new ConflictException("Task was modified; reload and retry");
		}
	}

	private static Pageable pageable(int page, int size, String sort) {
		if (page < 0) {
			throw new BadRequestException("page", "must be 0 or greater");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BadRequestException("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		return PageRequest.of(page, size, parseSort(sort == null || sort.isBlank() ? DEFAULT_SORT : sort));
	}

	/** Allowlisted sort; dueDate sorts nulls last. */
	static Sort parseSort(String raw) {
		String[] parts = raw.split(",", 2);
		String property = parts[0].trim();
		String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
		if (!SORTABLE.contains(property)) {
			throw new BadRequestException("sort", "unsupported property; allowed: " + String.join(", ", SORTABLE));
		}
		if (!direction.equals("asc") && !direction.equals("desc")) {
			throw new BadRequestException("sort", "direction must be asc or desc");
		}
		Sort.Order order = new Sort.Order(Sort.Direction.fromString(direction), property);
		return Sort.by(property.equals("dueDate") ? order.nullsLast() : order);
	}

}
