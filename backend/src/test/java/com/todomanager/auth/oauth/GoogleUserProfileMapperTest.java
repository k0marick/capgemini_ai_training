package com.todomanager.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleUserProfileMapperTest {

	private final GoogleUserProfileMapper mapper = new GoogleUserProfileMapper();

	@Test
	void verifiedEmailIsStored() {
		OAuthUserProfile p = mapper.map(Map.of("sub", "s1", "email", "a@b.c", "email_verified", true, "name", "Ada"));
		assertThat(p.email()).isEqualTo("a@b.c");
		assertThat(p.displayName()).isEqualTo("Ada");
		assertThat(p.providerUserId()).isEqualTo("s1");
	}

	@Test
	void unverifiedEmailIsDropped() {
		OAuthUserProfile p = mapper.map(Map.of("sub", "s2", "email", "a@b.c", "email_verified", false));
		assertThat(p.email()).isNull();
	}

	@Test
	void missingNameFallsBackToEmailLocalPart() {
		OAuthUserProfile p = mapper.map(Map.of("sub", "s3", "email", "grace@b.c", "email_verified", true));
		assertThat(p.displayName()).isEqualTo("grace");
	}

}
