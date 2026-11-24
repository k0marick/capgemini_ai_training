package com.todomanager.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.todomanager.auth.oauth.AuthProvider;
import com.todomanager.auth.oauth.OAuthUserProfile;
import com.todomanager.user.entity.AppUser;
import com.todomanager.user.service.UserProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserProvisioningTest {

	@Autowired UserProvisioningService provisioning;
	@Autowired JdbcTemplate jdbc;

	@BeforeEach
	void clean() {
		jdbc.update("DELETE FROM tasks");
		jdbc.update("DELETE FROM projects");
		jdbc.update("DELETE FROM users");
	}

	@Test
	void sameIdentityUpsertsToOneRowAndRefreshesProfile() {
		AppUser first = provisioning.upsert(new OAuthUserProfile(
				AuthProvider.GOOGLE, "sub-1", "a@example.com", "Old Name", null));
		AppUser second = provisioning.upsert(new OAuthUserProfile(
				AuthProvider.GOOGLE, "sub-1", "new@example.com", "New Name", "https://img/a.png"));

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(count()).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT display_name FROM users", String.class)).isEqualTo("New Name");
		assertThat(jdbc.queryForObject("SELECT email FROM users", String.class)).isEqualTo("new@example.com");
	}

	@Test
	void sameEmailAcrossProvidersIsNotMerged() {
		provisioning.upsert(new OAuthUserProfile(AuthProvider.GOOGLE, "g-1", "same@example.com", "G", null));
		provisioning.upsert(new OAuthUserProfile(AuthProvider.GITHUB, "1", "same@example.com", "H", null));

		assertThat(count()).isEqualTo(2);
	}

	@Test
	void providerUserIdIsUniquePerProvider() {
		provisioning.upsert(new OAuthUserProfile(AuthProvider.GITHUB, "42", null, "A", null));
		provisioning.upsert(new OAuthUserProfile(AuthProvider.GOOGLE, "42", null, "B", null));

		assertThat(count()).isEqualTo(2);
	}

	private long count() {
		Long n = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
		return n == null ? 0 : n;
	}

}
