package com.todomanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todomanager.support.AbstractApiTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

/** Date categories and search, evaluated against a fixed clock: today = 2026-11-24 (UTC). */
@Import(TaskSearchApiTest.FixedClock.class)
class TaskSearchApiTest extends AbstractApiTest {

	@Test
	void dueCategoriesUseFixedToday() throws Exception {
		UUID user = newUser();
		String project = createProject(user, "Dates");
		createTask(user, project, "Overdue open", "2026-11-20", "TODO");
		createTask(user, project, "Overdue done", "2026-11-20", "DONE");
		createTask(user, project, "Due today", "2026-11-24", "TODO");
		createTask(user, project, "Due in 3 days", "2026-11-27", "IN_PROGRESS");
		createTask(user, project, "Due in 4 days", "2026-11-28", "TODO");
		createTask(user, project, "Due in 6 days", "2026-11-30", "TODO");
		createTask(user, project, "Due in 7 days", "2026-12-01", "TODO");
		createTask(user, project, "No date", null, "TODO");

		assertTitles(user, "due=OVERDUE", "Overdue open");
		assertTitles(user, "due=TODAY", "Due today");
		assertTitles(user, "due=NEXT_7_DAYS", "Due today", "Due in 3 days", "Due in 4 days", "Due in 6 days");
		assertTitles(user, "due=NEXT_7_DAYS&status=TODO", "Due today", "Due in 4 days", "Due in 6 days");
		assertTitles(user, "due=ALL&size=20&sort=title,asc", "Due in 3 days", "Due in 4 days", "Due in 6 days",
				"Due in 7 days", "Due today", "No date", "Overdue done", "Overdue open");
	}

	@Test
	void searchMatchesTitleOrDescriptionCaseInsensitively() throws Exception {
		UUID user = newUser();
		String project = createProject(user, "Search");
		createTask(user, project, "Buy MILK", null, "TODO");
		createTaskWithDescription(user, project, "Groceries", "milk and eggs");
		createTask(user, project, "Unrelated", null, "TODO");

		assertTitles(user, "q=milk&sort=title,asc", "Buy MILK", "Groceries");
	}

	@Test
	void likeWildcardsAreLiteral() throws Exception {
		UUID user = newUser();
		String project = createProject(user, "Wildcards");
		createTask(user, project, "100% done", null, "TODO");
		createTask(user, project, "anything", null, "TODO");
		createTask(user, project, "snake_case", null, "TODO");

		// Passed via param() so the wildcard is not re-encoded by the URL template.
		mvc.perform(get("/api/v1/tasks").with(as(user)).param("q", "%"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("100% done"));
		mvc.perform(get("/api/v1/tasks").with(as(user)).param("q", "_"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("snake_case"));
	}

	@Test
	void searchCombinesWithDueFilterAndStaysOwnerScoped() throws Exception {
		UUID user = newUser();
		UUID other = newUser();
		String mine = createProject(user, "Mine");
		String theirs = createProject(other, "Theirs");
		createTask(user, mine, "report today", "2026-11-24", "TODO");
		createTask(user, mine, "report later", "2026-12-20", "TODO");
		createTask(other, theirs, "report today", "2026-11-24", "TODO");

		assertTitles(user, "q=report&due=TODAY", "report today");
	}

	@Test
	void queryLengthAndUnknownDueAreRejected() throws Exception {
		UUID user = newUser();
		mvc.perform(get("/api/v1/tasks").with(as(user)).param("q", "x".repeat(101)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.q").exists());
		mvc.perform(get("/api/v1/tasks").with(as(user)).param("due", "LATER"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void userTimezoneDefinesToday() throws Exception {
		UUID user = newUser();
		mvc.perform(patch("/api/v1/me/preferences").with(as(user)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"Pacific/Kiritimati\"}"))
				.andExpect(status().isOk());
		String project = createProject(user, "Zone");
		// At 2026-11-24T10:00Z it is already 2026-11-25 in UTC+14.
		createTask(user, project, "Tomorrow for them", "2026-11-25", "TODO");
		assertTitles(user, "due=TODAY", "Tomorrow for them");
	}

	private void assertTitles(UUID user, String query, String... expectedTitles) throws Exception {
		var result = mvc.perform(get("/api/v1/tasks?" + query).with(as(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(expectedTitles.length))
				.andReturn().getResponse().getContentAsString();
		for (String title : expectedTitles) {
			assertThat(result).contains("\"title\":\"" + title + "\"");
		}
	}

	private void createTask(UUID user, String projectId, String title, String dueDate, String status)
			throws Exception {
		String due = dueDate == null ? "null" : "\"" + dueDate + "\"";
		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(user)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"" + title + "\",\"status\":\"" + status + "\",\"dueDate\":" + due + "}"))
				.andExpect(status().isCreated());
	}

	private void createTaskWithDescription(UUID user, String projectId, String title, String description)
			throws Exception {
		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(user)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"" + title + "\",\"description\":\"" + description + "\"}"))
				.andExpect(status().isCreated());
	}

	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
		}

	}

}
