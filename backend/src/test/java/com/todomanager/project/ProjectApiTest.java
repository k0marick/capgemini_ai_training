package com.todomanager.project;

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

class ProjectApiTest extends AbstractApiTest {

	@Test
	void crudRoundTripWithTaskCount() throws Exception {
		UUID owner = newUser();
		String id = createProject(owner, "Home");

		mvc.perform(get("/api/v1/projects").with(as(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Home"))
				.andExpect(jsonPath("$[0].taskCount").value(0));

		mvc.perform(patch("/api/v1/projects/" + id).with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Work\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Work"));

		mvc.perform(delete("/api/v1/projects/" + id).with(as(owner)).with(csrf()))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/projects/" + id).with(as(owner)))
				.andExpect(status().isNotFound());
	}

	@Test
	void foreignProjectIsNotFoundForEveryOperation() throws Exception {
		UUID owner = newUser();
		UUID intruder = newUser();
		String id = createProject(owner, "Private");

		mvc.perform(get("/api/v1/projects/" + id).with(as(intruder)))
				.andExpect(status().isNotFound());
		mvc.perform(patch("/api/v1/projects/" + id).with(as(intruder)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hacked\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/projects/" + id).with(as(intruder)).with(csrf()))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/projects").with(as(intruder)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void deleteIsBlockedWhileTasksExist() throws Exception {
		UUID owner = newUser();
		String projectId = createProject(owner, "Busy");
		String taskId = createTask(owner, projectId, "Still here");

		mvc.perform(delete("/api/v1/projects/" + projectId).with(as(owner)).with(csrf()))
				.andExpect(status().isConflict());

		mvc.perform(delete("/api/v1/tasks/" + taskId).with(as(owner)).with(csrf()))
				.andExpect(status().isNoContent());
		mvc.perform(delete("/api/v1/projects/" + projectId).with(as(owner)).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void blankNameReturnsFieldErrors() throws Exception {
		UUID owner = newUser();
		mvc.perform(post("/api/v1/projects").with(as(owner)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void mutationWithoutCsrfIsForbidden() throws Exception {
		UUID owner = newUser();
		mvc.perform(post("/api/v1/projects").with(as(owner))
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"NoCsrf\"}"))
				.andExpect(status().isForbidden());
	}

}
