package com.todomanager.auth.principal;

import com.todomanager.auth.oauth.AuthProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Session principal. The internal user UUID is the only identity used for authorization. */
public final class AppUserPrincipal implements OAuth2User {

	private final UUID userId;
	private final AuthProvider provider;

	public AppUserPrincipal(UUID userId, AuthProvider provider) {
		this.userId = userId;
		this.provider = provider;
	}

	public UUID userId() {
		return userId;
	}

	public AuthProvider provider() {
		return provider;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Map.of("userId", userId.toString(), "provider", provider.name());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getName() {
		return userId.toString();
	}

}
