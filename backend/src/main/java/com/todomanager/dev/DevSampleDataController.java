package com.todomanager.dev;

import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.project.dto.ProjectResponse;
import com.todomanager.project.service.ProjectService;
import com.todomanager.task.dto.TaskCreateRequest;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import com.todomanager.task.service.TaskService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only: seeds two projects and a spread of tasks (overdue, due soon, done, undated) for the caller.
 * Not registered outside the {@code dev} profile, so the path does not exist elsewhere.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/dev")
class DevSampleDataController {

	private final ProjectService projects;
	private final TaskService tasks;
	private final Clock clock;

	DevSampleDataController(ProjectService projects, TaskService tasks, Clock clock) {
		this.projects = projects;
		this.tasks = tasks;
		this.clock = clock;
	}

	@PostMapping("/sample-data")
	@ResponseStatus(HttpStatus.CREATED)
	SampleDataResponse seed(@AuthenticationPrincipal AppUserPrincipal principal) {
		UUID userId = principal.userId();
		LocalDate today = LocalDate.now(clock);
		ProjectResponse home = projects.create(userId, "Home");
		ProjectResponse work = projects.create(userId, "Work");
		tasks.create(userId, home.id(), new TaskCreateRequest("Pay electricity bill", "Due last week",
				TaskStatus.TODO, TaskPriority.HIGH, today.minusDays(7)));
		tasks.create(userId, home.id(), new TaskCreateRequest("Water the plants", null,
				TaskStatus.TODO, TaskPriority.LOW, today));
		tasks.create(userId, home.id(), new TaskCreateRequest("Book dentist", null,
				TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, today.plusDays(2)));
		tasks.create(userId, work.id(), new TaskCreateRequest("Send weekly report", "Use the team template",
				TaskStatus.TODO, TaskPriority.HIGH, today.minusDays(1)));
		tasks.create(userId, work.id(), new TaskCreateRequest("Review onboarding doc", null,
				TaskStatus.DONE, TaskPriority.MEDIUM, today.minusDays(3)));
		tasks.create(userId, work.id(), new TaskCreateRequest("Plan next sprint", null,
				null, null, null));
		return new SampleDataResponse(2, 6);
	}

	record SampleDataResponse(int projectsCreated, int tasksCreated) {
	}

}
