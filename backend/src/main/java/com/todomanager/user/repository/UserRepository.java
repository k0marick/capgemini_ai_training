package com.todomanager.user.repository;

import com.todomanager.auth.oauth.AuthProvider;
import com.todomanager.user.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

}
