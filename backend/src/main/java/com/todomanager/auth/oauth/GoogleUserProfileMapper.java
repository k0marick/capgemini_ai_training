package com.todomanager.auth.oauth;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class GoogleUserProfileMapper implements OAuthUserProfileMapper {

	@Override
	public AuthProvider provider() {
		return AuthProvider.GOOGLE;
	}

	@Override
	public OAuthUserProfile map(Map<String, Object> attributes) {
		String sub = requireString(attributes, "sub");
		String email = Boolean.TRUE.equals(attributes.get("email_verified"))
				? (String) attributes.get("email")
				: null; // unverified emails are never stored
		String name = (String) attributes.get("name");
		String displayName = notBlank(name) ? name
				: (email != null ? email.substring(0, email.indexOf('@')) : sub);
		return new OAuthUserProfile(AuthProvider.GOOGLE, sub, email, displayName,
				(String) attributes.get("picture"));
	}

	private static String requireString(Map<String, Object> attributes, String key) {
		Object value = attributes.get(key);
		if (!(value instanceof String s) || s.isBlank()) {
			throw new IllegalStateException("Google profile is missing claim: " + key);
		}
		return s;
	}

	private static boolean notBlank(String value) {
		return value != null && !value.isBlank();
	}

}
