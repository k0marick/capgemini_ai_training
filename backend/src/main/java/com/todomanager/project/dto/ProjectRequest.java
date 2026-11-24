package com.todomanager.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
		@NotBlank @Size(max = 150) String name) {
}
