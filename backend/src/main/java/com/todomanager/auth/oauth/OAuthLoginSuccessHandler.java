package com.todomanager.auth.oauth;

import com.todomanager.auth.principal.AppUserPrincipal;
import com.todomanager.user.entity.AppUser;
import com.todomanager.user.service.UserProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * After a provider login: provision the user, replace the principal with the internal user id,
 * and persist it to the HTTP session.
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final Map<AuthProvider, OAuthUserProfileMapper> mappers = new EnumMap<>(AuthProvider.class);
	private final UserProvisioningService provisioning;
	private final String appBaseUrl;
	private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

	OAuthLoginSuccessHandler(List<OAuthUserProfileMapper> profileMappers,
			UserProvisioningService provisioning,
			@Value("${app.base-url}") String appBaseUrl) {
		profileMappers.forEach(m -> mappers.put(m.provider(), m));
		this.provisioning = provisioning;
		this.appBaseUrl = appBaseUrl;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
		AuthProvider provider = AuthProvider.fromRegistrationId(token.getAuthorizedClientRegistrationId());
		OAuthUserProfile profile = mappers.get(provider)
				.map(((OAuth2User) token.getPrincipal()).getAttributes());
		AppUser user = provisioning.upsert(profile);

		AppUserPrincipal principal = new AppUserPrincipal(user.getId(), provider);
		OAuth2AuthenticationToken appAuth = new OAuth2AuthenticationToken(
				principal, principal.getAuthorities(), token.getAuthorizedClientRegistrationId());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(appAuth);
		contextRepository.saveContext(context, request, response);

		response.sendRedirect(appBaseUrl + "/");
	}

}
