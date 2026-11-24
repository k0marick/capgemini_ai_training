package com.todomanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationSmokeTest {

	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;

	@Test
	void healthIsPublicAndUp() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void flywayAppliedV1AndCreatedTables() {
		Integer applied = jdbc.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);
		assertThat(applied).isEqualTo(1);
		Integer tables = jdbc.queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"
						+ " AND table_name IN ('users','projects','tasks')", Integer.class);
		assertThat(tables).isEqualTo(3);
	}

	@Test
	void unauthenticatedApiIsRejectedWith401() throws Exception {
		mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
	}

}
