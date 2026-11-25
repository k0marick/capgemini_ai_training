package com.todomanager.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Logs in over real HTTP against the stub providers, for tests that need a running server (WebSocket).
 * The resulting JSESSIONID cookie is the same credential a browser would send.
 */
public final class LiveSession {

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();

	private final int port;
	private final String cookie;

	private LiveSession(int port, String cookie) {
		this.port = port;
		this.cookie = cookie;
	}

	public static LiveSession google(int port, String sub, String email) throws Exception {
		Start start = start(port, "google");
		StubOAuthProviders.stubGoogleLogin(sub, email, true, start.param("nonce"));
		return finish(port, "google", start);
	}

	public static LiveSession github(int port) throws Exception {
		Start start = start(port, "github");
		StubOAuthProviders.stubGithubToken();
		return finish(port, "github", start);
	}

	/** The JSESSIONID cookie header value, e.g. {@code JSESSIONID=abc}. */
	public String cookie() {
		return cookie;
	}

	public UUID userId() throws Exception {
		HttpResponse<String> response = HTTP.send(get("/api/v1/me").header("Cookie", cookie).build(),
				BodyHandlers.ofString());
		return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response.body(), "$.id"));
	}

	/** Logs out through the real endpoint (with CSRF). Returns the HTTP status. */
	public int logout() throws Exception {
		HttpResponse<String> csrf = HTTP.send(get("/api/v1/csrf").header("Cookie", cookie).build(),
				BodyHandlers.ofString());
		String token = com.jayway.jsonpath.JsonPath.read(csrf.body(), "$.token");
		String xsrfCookie = csrf.headers().allValues("Set-Cookie").stream()
				.filter(value -> value.startsWith("XSRF-TOKEN="))
				.findFirst()
				.map(value -> value.substring(0, value.indexOf(';')))
				.orElseThrow();
		HttpResponse<Void> response = HTTP.send(HttpRequest.newBuilder(uri("/api/v1/logout"))
				.header("Cookie", cookie + "; " + xsrfCookie)
				.header("X-XSRF-TOKEN", token)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build(), BodyHandlers.discarding());
		return response.statusCode();
	}

	private record Start(String cookie, URI authorizeUri) {

		String param(String name) {
			return UriComponentsBuilder.fromUri(authorizeUri).build().getQueryParams().getFirst(name);
		}

	}

	private static Start start(int port, String provider) throws Exception {
		HttpResponse<Void> response = HTTP.send(HttpRequest.newBuilder(uri(port, "/oauth2/authorization/" + provider))
				.GET().build(), BodyHandlers.discarding());
		URI location = URI.create(response.headers().firstValue("Location").orElseThrow());
		return new Start(sessionCookie(response, null), location);
	}

	private static LiveSession finish(int port, String provider, Start start) throws Exception {
		HttpResponse<Void> response = HTTP.send(HttpRequest.newBuilder(uri(port,
						"/login/oauth2/code/" + provider + "?code=code-1&state=" + start.param("state")))
				.header("Cookie", start.cookie())
				.GET().build(), BodyHandlers.discarding());
		return new LiveSession(port, sessionCookie(response, start.cookie()));
	}

	private static String sessionCookie(HttpResponse<?> response, String fallback) {
		for (String setCookie : response.headers().allValues("Set-Cookie")) {
			if (setCookie.startsWith("JSESSIONID=")) {
				int end = setCookie.indexOf(';');
				return end < 0 ? setCookie : setCookie.substring(0, end);
			}
		}
		return fallback;
	}

	private HttpRequest.Builder get(String path) {
		return HttpRequest.newBuilder(uri(path)).GET();
	}

	private URI uri(String path) {
		return uri(port, path);
	}

	private static URI uri(int port, String path) {
		return URI.create("http://localhost:" + port + path);
	}

}
