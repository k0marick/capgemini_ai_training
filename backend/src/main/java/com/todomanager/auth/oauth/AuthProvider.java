package com.todomanager.auth.oauth;

import java.util.Locale;

public enum AuthProvider {
	GOOGLE,
	GITHUB;

	public static AuthProvider fromRegistrationId(String registrationId) {
		return valueOf(registrationId.toUpperCase(Locale.ROOT));
	}
}
