package com.todomanager.user.service;

import com.todomanager.auth.oauth.OAuthUserProfile;
import com.todomanager.common.exception.BadRequestException;
import com.todomanager.user.entity.AppUser;
import com.todomanager.user.repository.UserRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Finds or creates a user by (provider, provider_user_id). Never links accounts by email. */
@Service
public class UserProvisioningService {

	private final UserRepository users;
	private final Clock clock;
	private final TransactionTemplate tx;

	public UserProvisioningService(UserRepository users, Clock clock, TransactionTemplate tx) {
		this.users = users;
		this.clock = clock;
		this.tx = tx;
	}

	public AppUser upsert(OAuthUserProfile profile) {
		Instant now = clock.instant();
		return users.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
				.map(existing -> refresh(existing.getId(), profile, now))
				.orElseGet(() -> create(profile, now));
	}

	public Optional<AppUser> findById(UUID id) {
		return users.findById(id);
	}

	/** Validates an IANA zone ID before storing it. */
	@Transactional
	public AppUser updateTimezone(UUID id, String timezone) {
		try {
			ZoneId.of(timezone);
		}
		catch (DateTimeException invalid) {
			throw new BadRequestException("timezone", "must be a valid IANA time zone");
		}
		AppUser user = users.findById(id)
				// Session outlived its user row: treat as unauthenticated.
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		user.changeTimezone(timezone);
		return user;
	}

	/** Explicit transaction: self-invoked @Transactional methods would not start one. */
	AppUser refresh(UUID id, OAuthUserProfile profile, Instant now) {
		return tx.execute(status -> {
			AppUser user = users.findById(id).orElseThrow();
			user.updateFromLogin(profile, now);
			return user;
		});
	}

	private AppUser create(OAuthUserProfile profile, Instant now) {
		try {
			return insert(profile, now);
		}
		catch (DataIntegrityViolationException raceLost) {
			// A concurrent first login won the unique key; reuse its row.
			return users.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
					.orElseThrow(() -> raceLost);
		}
	}

	@Transactional
	AppUser insert(OAuthUserProfile profile, Instant now) {
		return users.saveAndFlush(AppUser.create(UUID.randomUUID(), profile, now));
	}

}
