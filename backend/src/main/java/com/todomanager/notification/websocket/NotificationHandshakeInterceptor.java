package com.todomanager.notification.websocket;

import com.todomanager.auth.principal.AppUserPrincipal;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Handshake gate (architecture §10.1): Origin must be in the allowlist (else 403), then a session principal
 * must be present (else 401). Only then is the upgrade performed.
 */
@Component
class NotificationHandshakeInterceptor implements HandshakeInterceptor {

	static final String ATTR_USER_ID = "userId";
	static final String ATTR_HTTP_SESSION_ID = "httpSessionId";

	private final Set<String> allowedOrigins;

	NotificationHandshakeInterceptor(@Value("${app.base-url}") String baseUrl) {
		this.allowedOrigins = Set.of(baseUrl);
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, Map<String, Object> attributes) {
		String origin = request.getHeaders().getOrigin();
		if (origin == null || !allowedOrigins.contains(origin)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		HttpSession session = ((ServletServerHttpRequest) request).getServletRequest().getSession(false);
		if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal) || session == null) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		UUID userId = principal.userId();
		attributes.put(ATTR_USER_ID, userId);
		attributes.put(ATTR_HTTP_SESSION_ID, session.getId());
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, Exception exception) {
		// no-op
	}

}
