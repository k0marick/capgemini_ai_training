package com.todomanager.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreferencesRequest(
		@NotBlank @Size(max = 64) String timezone) {
}
