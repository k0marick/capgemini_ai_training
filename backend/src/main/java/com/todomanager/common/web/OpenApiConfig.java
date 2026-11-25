package com.todomanager.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. The spec is served only when springdoc is enabled (dev profile). */
@Configuration
class OpenApiConfig {

	static final String SESSION_SCHEME = "sessionCookie";

	@Bean
	OpenAPI todoManagerOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Personal To-Do Manager API")
						.version("v1")
						.description("REST API under /api/v1. Authentication is a server-side session "
								+ "(JSESSIONID) created by Google or GitHub sign-in. Unsafe methods need the "
								+ "XSRF-TOKEN cookie value sent in the X-XSRF-TOKEN header."))
				.components(new Components().addSecuritySchemes(SESSION_SCHEME,
						new SecurityScheme().type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
				.addSecurityItem(new SecurityRequirement().addList(SESSION_SCHEME));
	}

}
