package com.todomanager.task.service;

import com.todomanager.common.exception.BadRequestException;
import com.todomanager.common.exception.ConflictException;
import com.todomanager.common.exception.ResourceNotFoundException;
import com.todomanager.common.web.PageResponse;
import com.todomanager.project.repository.ProjectRepository;
import com.todomanager.task.TaskDateRules;
import com.todomanager.task.TaskDateRules.DueFilter;
import com.todomanager.task.dto.TaskCreateRequest;
import com.todomanager.task.dto.TaskDueItem;
import com.todomanager.task.dto.TaskResponse;
import com.todomanager.task.dto.TaskStatusRequest;
import com.todomanager.task.dto.TaskUpdateRequest;
import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import com.todomanager.task.event.TasksChangedEvent;
import com.todomanager.task.repository.TaskRepository;
import com.todomanager.task.specification.TaskSpecification;
import com.todomanager.user.service.UserProvisioningService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

	private static final Set<String> SORTABLE = Set.of("dueDate", "priority", "createdAt", "title", "status");
	private static final String DEFAULT_SORT = "dueDate,asc";
	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_QUERY_LENGTH = 100;

	private final TaskRepository tasks;
	private final ProjectRepository projects;
	private final UserProvisioningService users;
	private final Clock clock;
	private final ApplicationEventPublisher events;

	public TaskService(TaskRepository tasks, ProjectRepository projects, UserProvisioningService users, Clock clock,
			ApplicationEventPublisher events) {
		this.tasks = tasks;
		this.projects = projects;
		this.users = users;
		this.clock = clock;
		this.events = events;
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
		String query = filter.query() == null ? "" : filter.query().trim();
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new BadRequestException("q", "must be at most " + MAX_QUERY_LENGTH + " characters");
		}
		if (!query.isEmpty()) {
			spec = spec.and(TaskSpecification.matchesText(query));
		}
		DueFilter due = filter.due() == null ? DueFilter.ALL : filter.due();
		if (due != DueFilter.ALL) {
			LocalDate today = TaskDateRules.today(clock, userZone(userId));
			spec = spec.and(TaskDateRules.dueFilter(due, today));
		}
		return PageResponse.of(tasks.findAll(spec, pageable).map(TaskResponse::from));
	}

	/** Open tasks due within the notification window (overdue included); owner-scoped. Used by the notification feature. */
	@Transactional(readOnly = true)
	public List<TaskDueItem> findDueForNotifications(UUID userId) {
		LocalDate today = TaskDateRules.today(clock, userZone(userId));
		Specification<Task> spec = TaskSpecification.belongsToUser(userId)
				.and(TaskDateRules.openDueBy(TaskDateRules.dueSoonHorizon(today)));
		return tasks.findAll(spec, Sort.by("dueDate", "id").ascending()).stream()
				.map(task -> new TaskDueItem(task.getId(), task.getProjectId(), task.getTitle(), task.getDueDate(),
						task.getPriority(), TaskDateRules.isOverdue(task.getDueDate(), today)))
				.toList();
	}

	private ZoneId userZone(UUID userId) {
		return users.findById(userId)
				.map(user -> ZoneId.of(user.getTimezone()))
				.orElse(ZoneOffset.UTC);
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
		return saved(userId, task);
	}

	@Transactional
	public TaskResponse update(UUID userId, UUID id, TaskUpdateRequest body) {
		Task task = owned(userId, id);
		requireVersion(task, body.version());
		task.update(body.title(), body.description(), body.status(), body.priority(), body.dueDate(), clock.instant());
		return saved(userId, task);
	}

	@Transactional
	public TaskResponse changeStatus(UUID userId, UUID id, TaskStatusRequest body) {
		Task task = owned(userId, id);
		requireVersion(task, body.version());
		task.changeStatus(body.status(), clock.instant());
		return saved(userId, task);
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		tasks.delete(owned(userId, id));
		events.publishEvent(new TasksChangedEvent(userId));
	}

	/** Persists the task and announces the change; listeners run after commit. */
	private TaskResponse saved(UUID userId, Task task) {
		TaskResponse response = TaskResponse.from(tasks.saveAndFlush(task));
		events.publishEvent(new TasksChangedEvent(userId));
		return response;
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
