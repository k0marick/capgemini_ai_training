package com.todomanager.auth.oauth;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class GitHubUserProfileMapper implements OAuthUserProfileMapper {

	@Override
	public AuthProvider provider() {
		return AuthProvider.GITHUB;
	}

	@Override
	public OAuthUserProfile map(Map<String, Object> attributes) {
		Object id = attributes.get("id");
		if (!(id instanceof Number n)) {
			throw new IllegalStateException("GitHub profile is missing numeric id");
		}
		String login = (String) attributes.get("login");
		String name = (String) attributes.get("name");
		String displayName = (name != null && !name.isBlank()) ? name : login;
		return new OAuthUserProfile(AuthProvider.GITHUB, String.valueOf(n.longValue()),
				(String) attributes.get("email"), displayName,
				(String) attributes.get("avatar_url"));
	}

}
