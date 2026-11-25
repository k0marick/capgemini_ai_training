package com.todomanager.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todomanager.support.AbstractApiTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PreferencesApiTest extends AbstractApiTest {

	@Test
	void timezoneUpdateIsReflectedInMe() throws Exception {
		UUID user = newUser();

		mvc.perform(patch("/api/v1/me/preferences").with(as(user)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"Europe/Prague\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Europe/Prague"));

		mvc.perform(get("/api/v1/me").with(as(user)))
				.andExpect(jsonPath("$.timezone").value("Europe/Prague"));
	}

	@Test
	void unknownTimezoneIsRejected() throws Exception {
		UUID user = newUser();

		mvc.perform(patch("/api/v1/me/preferences").with(as(user)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"Mars/Olympus\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.timezone").exists());
	}

}
