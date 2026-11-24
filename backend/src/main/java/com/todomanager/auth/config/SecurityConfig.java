package com.todomanager.auth.config;

import com.todomanager.auth.oauth.OAuthLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, OAuthLoginSuccessHandler success,
			@Value("${app.base-url}") String appBaseUrl) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/api/v1/csrf",
								"/oauth2/**", "/login/oauth2/**").permitAll()
						.anyRequest().authenticated())
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.oauth2Login(oauth -> oauth
						.successHandler(success)
						.failureHandler(new SimpleUrlAuthenticationFailureHandler(
								appBaseUrl + "/login?error=oauth_failed")))
				.logout(logout -> logout
						.logoutUrl("/api/v1/logout")
						.logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new ProblemJsonAuthenticationEntryPoint()));
		return http.build();
	}

}
