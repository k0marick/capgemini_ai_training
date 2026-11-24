package com.todomanager.auth.oauth;

public record OAuthUserProfile(
		AuthProvider provider,
		String providerUserId,
		String email,
		String displayName,
		String avatarUrl) {
}
