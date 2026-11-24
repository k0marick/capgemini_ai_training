package com.todomanager.task;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todomanager.support.AbstractApiTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TaskApiTest extends AbstractApiTest {

	@Test
	void createAppliesDefaultsAndReturnsVersionZero() throws Exception {
		UUID owner = newUser();
		String projectId = createProject(owner, "Inbox");

		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Buy milk\",\"dueDate\":\"2026-12-01\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("TODO"))
				.andExpect(jsonPath("$.priority").value("MEDIUM"))
				.andExpect(jsonPath("$.dueDate").value("2026-12-01"))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void createInForeignProjectIsNotFound() throws Exception {
		UUID owner = newUser();
		UUID intruder = newUser();
		String projectId = createProject(owner, "Owner only");

		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(intruder)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Sneaky\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void foreignTaskIsNotFoundForEveryOperation() throws Exception {
		UUID owner = newUser();
		UUID intruder = newUser();
		String taskId = createTask(owner, createProject(owner, "Mine"), "Secret");

		mvc.perform(get("/api/v1/tasks/" + taskId).with(as(intruder)))
				.andExpect(status().isNotFound());
		mvc.perform(patch("/api/v1/tasks/" + taskId).with(as(intruder)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"x\",\"status\":\"DONE\",\"priority\":\"LOW\",\"version\":0}"))
				.andExpect(status().isNotFound());
		mvc.perform(patch("/api/v1/tasks/" + taskId + "/status").with(as(intruder)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":0}"))
				.andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/tasks/" + taskId).with(as(intruder)).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void staleVersionReturnsConflict() throws Exception {
		UUID owner = newUser();
		String taskId = createTask(owner, createProject(owner, "Versions"), "Draft");

		mvc.perform(patch("/api/v1/tasks/" + taskId).with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"First\",\"status\":\"TODO\",\"priority\":\"MEDIUM\",\"version\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1));

		mvc.perform(patch("/api/v1/tasks/" + taskId).with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Stale\",\"status\":\"TODO\",\"priority\":\"MEDIUM\",\"version\":0}"))
				.andExpect(status().isConflict());
	}

	@Test
	void quickStatusChangeBumpsVersion() throws Exception {
		UUID owner = newUser();
		String taskId = createTask(owner, createProject(owner, "Quick"), "Ship it");

		mvc.perform(patch("/api/v1/tasks/" + taskId + "/status").with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void invalidBodyReturnsFieldErrors() throws Exception {
		UUID owner = newUser();
		String projectId = createProject(owner, "Validation");

		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.title").exists());

		mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"ok\",\"status\":\"NOPE\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listFiltersByStatusAndValidatesPagingAndSort() throws Exception {
		UUID owner = newUser();
		String projectId = createProject(owner, "Listing");
		createTask(owner, projectId, "Open");
		String doneId = createTask(owner, projectId, "Finished");
		mvc.perform(patch("/api/v1/tasks/" + doneId + "/status").with(as(owner)).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":0}"));

		mvc.perform(get("/api/v1/tasks").with(as(owner)).param("status", "DONE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Finished"));

		mvc.perform(get("/api/v1/tasks").with(as(owner)).param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.size").exists());

		mvc.perform(get("/api/v1/tasks").with(as(owner)).param("sort", "password,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.sort").exists());

		mvc.perform(get("/api/v1/tasks").with(as(owner)).param("status", "NOPE"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deleteRemovesOwnTask() throws Exception {
		UUID owner = newUser();
		String taskId = createTask(owner, createProject(owner, "Cleanup"), "Temporary");

		mvc.perform(delete("/api/v1/tasks/" + taskId).with(as(owner)).with(csrf()))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/tasks/" + taskId).with(as(owner)))
				.andExpect(status().isNotFound());
	}

}
