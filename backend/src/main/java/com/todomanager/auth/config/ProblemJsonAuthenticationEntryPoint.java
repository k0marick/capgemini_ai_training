package com.todomanager.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Unauthenticated requests get 401 as RFC 9457 problem JSON, never a redirect. */
class ProblemJsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final String BODY = """
			{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication required"}""";

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write(BODY);
	}

}
