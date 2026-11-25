package com.todomanager.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import com.todomanager.project.entity.Project;
import com.todomanager.project.repository.ProjectRepository;
import com.todomanager.support.LiveSession;
import com.todomanager.support.StubOAuthProviders;
import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import com.todomanager.task.repository.TaskRepository;
import com.todomanager.notification.scheduler.NotificationScheduler;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

/** WebSocket notifications on a real server: handshake gate, protocol, isolation, scheduler and logout closure. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "notifications.interval-ms=3600000")
@ActiveProfiles("test")
@Import(NotificationWebSocketTest.FixedClock.class)
class NotificationWebSocketTest {

	private static final String APP_ORIGIN = "http://localhost:5173";
	private static final String EVIL_ORIGIN = "https://evil.example";

	@DynamicPropertySource
	static void providers(DynamicPropertyRegistry registry) {
		StubOAuthProviders.registerProperties(registry);
	}

	@LocalServerPort
	int port;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	ProjectRepository projects;

	@Autowired
	TaskRepository tasks;

	@Autowired
	NotificationScheduler scheduler;

	@BeforeEach
	void cleanSchema() {
		jdbc.update("DELETE FROM tasks");
		jdbc.update("DELETE FROM projects");
		jdbc.update("DELETE FROM users");
	}

	@Test
	void badOriginIsRejectedWith403() throws Exception {
		LiveSession session = LiveSession.github(port);
		assertThatThrownBy(() -> connect(EVIL_ORIGIN, session.cookie()))
				.hasMessageContaining("403");
	}

	@Test
	void missingSessionIsRejectedWith401() {
		assertThatThrownBy(() -> connect(APP_ORIGIN, null))
				.hasMessageContaining("401");
	}

	@Test
	void initialBatchContainsOverdueAndDueSoonWithinWindowOnly() throws Exception {
		LiveSession session = LiveSession.github(port);
		UUID userId = session.userId();
		UUID project = project(userId);
		task(project, "Overdue", "2026-11-23", TaskStatus.TODO);
		task(project, "Today", "2026-11-24", TaskStatus.IN_PROGRESS);
		task(project, "Edge", "2026-11-27", TaskStatus.TODO);
		task(project, "Too far", "2026-11-28", TaskStatus.TODO);
		task(project, "Done", "2026-11-23", TaskStatus.DONE);
		task(project, "No date", null, TaskStatus.TODO);

		Socket socket = connect(APP_ORIGIN, session.cookie());

		String batch = socket.next();
		assertThat(JsonPath.<String>read(batch, "$.type")).isEqualTo("NOTIFICATION_BATCH");
		assertThat(JsonPath.<java.util.List<String>>read(batch, "$.payload.notifications[*].title"))
				.containsExactly("Overdue", "Today", "Edge");
		assertThat(JsonPath.<java.util.List<String>>read(batch, "$.payload.notifications[*].type"))
				.containsExactly("OVERDUE", "DUE_SOON", "DUE_SOON");
	}

	@Test
	void otherAccountsTasksAreNotDelivered() throws Exception {
		LiveSession owner = LiveSession.google(port, "owner-sub", "owner@example.com");
		task(project(owner.userId()), "Owner overdue", "2026-11-20", TaskStatus.TODO);

		LiveSession other = LiveSession.github(port);
		Socket socket = connect(APP_ORIGIN, other.cookie());

		String batch = socket.next();
		assertThat(JsonPath.<java.util.List<String>>read(batch, "$.payload.notifications[*].title")).isEmpty();
	}

	@Test
	void subscribeToOverdueOnlyFiltersTheBatch() throws Exception {
		LiveSession session = LiveSession.github(port);
		UUID project = project(session.userId());
		task(project, "Overdue", "2026-11-23", TaskStatus.TODO);
		task(project, "Today", "2026-11-24", TaskStatus.TODO);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		socket.next();

		socket.send("{\"type\":\"SUBSCRIBE\",\"payload\":{\"enabledTypes\":[\"OVERDUE\"]}}");

		String updated = socket.next();
		assertThat(JsonPath.<String>read(updated, "$.type")).isEqualTo("SUBSCRIPTION_UPDATED");
		assertThat(JsonPath.<java.util.List<String>>read(updated, "$.payload.enabledTypes")).containsExactly("OVERDUE");
		String batch = socket.next();
		assertThat(JsonPath.<java.util.List<String>>read(batch, "$.payload.notifications[*].title"))
				.containsExactly("Overdue");
	}

	@Test
	void invalidMessagesReturnTypedErrors() throws Exception {
		LiveSession session = LiveSession.github(port);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		socket.next();

		socket.send("{not json");
		assertThat(socket.error()).isEqualTo("INVALID_JSON");

		socket.send("{\"type\":\"HELLO\"}");
		assertThat(socket.error()).isEqualTo("UNKNOWN_TYPE");

		socket.send("{\"type\":\"SUBSCRIBE\",\"payload\":{\"enabledTypes\":[\"BOGUS\"]}}");
		assertThat(socket.error()).isEqualTo("INVALID_PAYLOAD");

		socket.send("{\"type\":\"SUBSCRIBE\",\"payload\":{\"enabledTypes\":[\"OVERDUE\",\"OVERDUE\"]}}");
		assertThat(socket.error()).isEqualTo("INVALID_PAYLOAD");

		socket.send("{\"type\":\"SUBSCRIBE\",\"payload\":{\"enabledTypes\":[],\"userId\":\"someone-else\"}}");
		assertThat(socket.error()).isEqualTo("INVALID_PAYLOAD");
	}

	@Test
	void oversizedFrameClosesWith1009() throws Exception {
		LiveSession session = LiveSession.github(port);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		socket.next();

		socket.send("{\"type\":\"" + "x".repeat(5000) + "\"}");

		assertThat(socket.closeCode()).isEqualTo(1009);
	}

	@Test
	void floodingClosesWith1008AfterRateLimitError() throws Exception {
		LiveSession session = LiveSession.github(port);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		socket.next();

		for (int i = 0; i < 25; i++) {
			socket.send("{\"type\":\"PING\"}");
		}

		assertThat(socket.drainUntilError()).isEqualTo("RATE_LIMITED");
		assertThat(socket.closeCode()).isEqualTo(1008);
	}

	@Test
	void schedulerPushesOnlyChangedBatches() throws Exception {
		LiveSession session = LiveSession.github(port);
		UUID project = project(session.userId());
		Task overdue = task(project, "Overdue", "2026-11-23", TaskStatus.TODO);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		assertThat(JsonPath.<java.util.List<String>>read(socket.next(), "$.payload.notifications[*].title"))
				.containsExactly("Overdue");

		scheduler.refresh();
		assertThat(socket.poll(500)).as("unchanged batch must not be pushed").isNull();

		Task reloaded = tasks.findById(overdue.getId()).orElseThrow();
		reloaded.changeStatus(TaskStatus.DONE, Instant.now());
		tasks.saveAndFlush(reloaded);
		scheduler.refresh();
		assertThat(JsonPath.<java.util.List<String>>read(socket.next(), "$.payload.notifications[*].title")).isEmpty();
	}

	@Test
	void logoutClosesSocketWith4401() throws Exception {
		LiveSession session = LiveSession.github(port);
		Socket socket = connect(APP_ORIGIN, session.cookie());
		socket.next();

		assertThat(session.logout()).isEqualTo(204);

		assertThat(socket.closeCode()).isEqualTo(4401);
	}

	private UUID project(UUID userId) {
		UUID id = UUID.randomUUID();
		projects.saveAndFlush(Project.create(id, userId, "Work", Instant.now()));
		return id;
	}

	private Task task(UUID projectId, String title, String due, TaskStatus status) {
		LocalDate dueDate = due == null ? null : LocalDate.parse(due);
		return tasks.saveAndFlush(Task.create(UUID.randomUUID(), projectId, title, null, status,
				TaskPriority.MEDIUM, dueDate, Instant.now()));
	}

	private Socket connect(String origin, String cookie) throws Exception {
		BlockingQueue<Object> events = new LinkedBlockingQueue<>();
		TextWebSocketHandler handler = new TextWebSocketHandler() {
			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) {
				events.add(message.getPayload());
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
				events.add(status);
			}
		};
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.setOrigin(origin);
		if (cookie != null) {
			headers.add("Cookie", cookie);
		}
		WebSocketSession session = new StandardWebSocketClient()
				.execute(handler, headers, URI.create("ws://localhost:" + port + "/ws/notifications"))
				.get(5, TimeUnit.SECONDS);
		return new Socket(session, events);
	}

	/** Test-side view of one socket: frames arrive in order on a queue, a close status ends the stream. */
	private static final class Socket {

		private final WebSocketSession session;
		private final BlockingQueue<Object> events;

		Socket(WebSocketSession session, BlockingQueue<Object> events) {
			this.session = session;
			this.events = events;
		}

		void send(String text) throws Exception {
			session.sendMessage(new TextMessage(text));
		}

		String next() throws InterruptedException {
			Object event = events.poll(5, TimeUnit.SECONDS);
			assertThat(event).as("expected a text frame").isInstanceOf(String.class);
			return (String) event;
		}

		Object poll(long millis) throws InterruptedException {
			return events.poll(millis, TimeUnit.MILLISECONDS);
		}

		String error() throws InterruptedException {
			String frame = next();
			assertThat(JsonPath.<String>read(frame, "$.type")).isEqualTo("ERROR");
			return JsonPath.read(frame, "$.payload.code");
		}

		/** Skips frames until an ERROR arrives and returns its code. */
		String drainUntilError() throws InterruptedException {
			while (true) {
				String frame = next();
				if ("ERROR".equals(JsonPath.<String>read(frame, "$.type"))) {
					String code = JsonPath.read(frame, "$.payload.code");
					if (!"UNKNOWN_TYPE".equals(code)) {
						return code;
					}
				}
			}
		}

		int closeCode() throws InterruptedException {
			while (true) {
				Object event = events.poll(5, TimeUnit.SECONDS);
				assertThat(event).as("expected the socket to close").isNotNull();
				if (event instanceof CloseStatus status) {
					return status.getCode();
				}
			}
		}

	}

	@TestConfiguration
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
		}

	}

}
