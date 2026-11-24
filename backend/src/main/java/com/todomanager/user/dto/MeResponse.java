package com.todomanager.user.dto;

import java.util.UUID;

public record MeResponse(
		UUID id,
		String provider,
		String email,
		String displayName,
		String avatarUrl,
		String timezone) {
}
