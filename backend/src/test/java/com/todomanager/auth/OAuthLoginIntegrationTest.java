package com.todomanager.auth;

import static com.todomanager.support.StubOAuthProviders.stubGithubToken;
import static com.todomanager.support.StubOAuthProviders.stubGoogleLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todomanager.support.StubOAuthProviders;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthLoginIntegrationTest {

	private static final String APP_BASE = "http://localhost:5173";

	@DynamicPropertySource
	static void providers(DynamicPropertyRegistry registry) {
		StubOAuthProviders.registerProperties(registry);
	}

	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;

	@BeforeEach
	void cleanUsers() {
		jdbc.update("DELETE FROM tasks");
		jdbc.update("DELETE FROM projects");
		jdbc.update("DELETE FROM users");
	}

	@Test
	void googleLoginProvisionsUserAndSessionReturnsProfile() throws Exception {
		Login login = startLogin("/oauth2/authorization/google");
		stubGoogleLogin("google-sub-1", "ada@example.com", true, login.param("nonce"));

		MvcResult cb = callback("/login/oauth2/code/google", login);
		assertThat(cb.getResponse().getRedirectedUrl()).isEqualTo(APP_BASE + "/");

		mvc.perform(get("/api/v1/me").session(sessionOf(cb)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("GOOGLE"))
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(jsonPath("$.displayName").value("Google User"))
				.andExpect(jsonPath("$.timezone").value("UTC"));
		assertThat(userCount()).isEqualTo(1);
	}

	@Test
	void googleUnverifiedEmailIsNotStored() throws Exception {
		Login login = startLogin("/oauth2/authorization/google");
		stubGoogleLogin("google-sub-2", "unverified@example.com", false, login.param("nonce"));

		MvcResult cb = callback("/login/oauth2/code/google", login);

		mvc.perform(get("/api/v1/me").session(sessionOf(cb)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").isEmpty());
	}

	@Test
	void githubLoginWithPrivateEmailSucceedsWithNullEmail() throws Exception {
		Login login = startLogin("/oauth2/authorization/github");
		stubGithubToken();

		MvcResult cb = callback("/login/oauth2/code/github", login);
		assertThat(cb.getResponse().getRedirectedUrl()).isEqualTo(APP_BASE + "/");

		mvc.perform(get("/api/v1/me").session(sessionOf(cb)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("GITHUB"))
				.andExpect(jsonPath("$.displayName").value("octocat"))
				.andExpect(jsonPath("$.email").isEmpty());
	}

	@Test
	void repeatLoginReusesSameUserRow() throws Exception {
		for (int i = 0; i < 2; i++) {
			Login login = startLogin("/oauth2/authorization/google");
			stubGoogleLogin("google-sub-3", "same@example.com", true, login.param("nonce"));
			callback("/login/oauth2/code/google", login);
		}
		assertThat(userCount()).isEqualTo(1);
	}

	@Test
	void sameEmailAcrossProvidersCreatesSeparateAccounts() throws Exception {
		Login google = startLogin("/oauth2/authorization/google");
		stubGoogleLogin("google-sub-4", "shared@example.com", true, google.param("nonce"));
		callback("/login/oauth2/code/google", google);

		Login github = startLogin("/oauth2/authorization/github");
		stubGithubToken();
		callback("/login/oauth2/code/github", github);

		assertThat(userCount()).isEqualTo(2);
	}

	@Test
	void logoutInvalidatesSession() throws Exception {
		Login login = startLogin("/oauth2/authorization/github");
		stubGithubToken();
		MockHttpSession authed = sessionOf(callback("/login/oauth2/code/github", login));

		mvc.perform(post("/api/v1/logout").with(csrf()).session(authed))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/me").session(authed))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutWithoutCsrfTokenIsForbidden() throws Exception {
		Login login = startLogin("/oauth2/authorization/github");
		stubGithubToken();
		MvcResult cb = callback("/login/oauth2/code/github", login);

		mvc.perform(post("/api/v1/logout").session(sessionOf(cb)))
				.andExpect(status().isForbidden());
	}

	@Test
	void unauthenticatedApiReturns401ProblemJsonNotRedirect() throws Exception {
		mvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Content-Type", startsWith("application/problem+json")))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void csrfEndpointIssuesTokenAndCookie() throws Exception {
		mvc.perform(get("/api/v1/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(header().exists("Set-Cookie"));
	}

	@Test
	void authorizationRequestUsesPkce() throws Exception {
		Login login = startLogin("/oauth2/authorization/google");
		assertThat(login.param("code_challenge_method")).isEqualTo("S256");
		assertThat(login.param("code_challenge")).isNotBlank();
	}

	@Test
	void providerErrorRedirectsToLoginErrorPage() throws Exception {
		Login login = startLogin("/oauth2/authorization/github");
		mvc.perform(get("/login/oauth2/code/github").param("error", "access_denied").session(login.session()))
				.andExpect(header().string("Location", APP_BASE + "/login?error=oauth_failed"));
	}

	/** Starts a provider login and remembers the session plus the provider authorize URL. */
	private record Login(MockHttpSession session, String authorizeUrl) {

		String param(String name) {
			String query = URI.create(authorizeUrl).getRawQuery();
			for (String pair : query.split("&")) {
				String[] kv = pair.split("=", 2);
				if (kv[0].equals(name)) {
					return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
				}
			}
			return null;
		}

	}

	private Login startLogin(String path) throws Exception {
		MvcResult start = mvc.perform(get(path)).andExpect(status().isFound()).andReturn();
		return new Login((MockHttpSession) start.getRequest().getSession(false),
				start.getResponse().getRedirectedUrl());
	}

	private MvcResult callback(String path, Login login) throws Exception {
		return mvc.perform(get(path)
						.param("code", "code-1")
						.param("state", login.param("state"))
						.session(login.session()))
				.andReturn();
	}

	private MockHttpSession sessionOf(MvcResult result) {
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private long userCount() {
		Long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
		return count == null ? 0 : count;
	}

}
