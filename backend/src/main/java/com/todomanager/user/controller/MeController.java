package com.todomanager.user.controller;

import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.user.dto.MeResponse;
import com.todomanager.user.entity.AppUser;
import com.todomanager.user.service.UserProvisioningService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

	private final UserProvisioningService users;

	MeController(UserProvisioningService users) {
		this.users = users;
	}

	@GetMapping
	MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		AppUser user = users.findById(principal.userId())
				// Session outlived its user row: treat as unauthenticated.
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		return new MeResponse(user.getId(), user.getProvider().name(), user.getEmail(),
				user.getDisplayName(), user.getAvatarUrl(), user.getTimezone());
	}

}
