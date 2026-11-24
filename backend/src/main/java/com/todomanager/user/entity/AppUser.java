package com.todomanager.user.entity;

import com.todomanager.auth.oauth.AuthProvider;
import com.todomanager.auth.oauth.OAuthUserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private AuthProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(name = "avatar_url", length = 2048)
	private String avatarUrl;

	@Column(name = "timezone", nullable = false, length = 64)
	private String timezone;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_login_at", nullable = false)
	private Instant lastLoginAt;

	protected AppUser() {
	}

	private AppUser(UUID id, OAuthUserProfile profile, Instant now) {
		this.id = id;
		this.provider = profile.provider();
		this.providerUserId = profile.providerUserId();
		this.timezone = "UTC";
		this.createdAt = now;
		applyProfile(profile, now);
	}

	public static AppUser create(UUID id, OAuthUserProfile profile, Instant now) {
		return new AppUser(id, profile, now);
	}

	public void updateFromLogin(OAuthUserProfile profile, Instant now) {
		applyProfile(profile, now);
	}

	private void applyProfile(OAuthUserProfile profile, Instant now) {
		this.email = profile.email();
		this.displayName = profile.displayName();
		this.avatarUrl = profile.avatarUrl();
		this.lastLoginAt = now;
	}

	public UUID getId() {
		return id;
	}

	public AuthProvider getProvider() {
		return provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public String getTimezone() {
		return timezone;
	}

}
