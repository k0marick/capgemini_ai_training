package com.todomanager.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.todomanager.auth.oauth.AuthProvider;
import com.todomanager.auth.oauth.OAuthUserProfile;
import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.user.entity.AppUser;
import com.todomanager.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Base for API tests: clean schema per test, users created in the DB, requests signed in as a principal. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected UserRepository users;

	@BeforeEach
	protected void cleanSchema() {
		jdbc.update("DELETE FROM tasks");
		jdbc.update("DELETE FROM projects");
		jdbc.update("DELETE FROM users");
	}

	protected UUID newUser() {
		UUID id = UUID.randomUUID();
		OAuthUserProfile profile = new OAuthUserProfile(AuthProvider.GOOGLE, "sub-" + id,
				"user@example.com", "Test User", null);
		users.saveAndFlush(AppUser.create(id, profile, Instant.now()));
		return id;
	}

	/** Signs the request in as the given user without a browser session. */
	protected static RequestPostProcessor as(UUID userId) {
		AppUserPrincipal principal = new AppUserPrincipal(userId, AuthProvider.GOOGLE);
		return authentication(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google"));
	}

	protected String createProject(UUID userId, String name) throws Exception {
		String body = mvc.perform(post("/api/v1/projects").with(as(userId)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	protected String createTask(UUID userId, String projectId, String title) throws Exception {
		String body = mvc.perform(post("/api/v1/projects/" + projectId + "/tasks").with(as(userId)).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"" + title + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

}
