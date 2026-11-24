package com.todomanager.notification.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
class NotificationWebSocketConfig implements WebSocketConfigurer {

	private final NotificationWebSocketHandler handler;
	private final NotificationHandshakeInterceptor interceptor;

	NotificationWebSocketConfig(NotificationWebSocketHandler handler, NotificationHandshakeInterceptor interceptor) {
		this.handler = handler;
		this.interceptor = interceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// Origin is checked by the interceptor (403) so the check order matches §10.1; Spring's own check is disabled.
		registry.addHandler(handler, "/ws/notifications")
				.addInterceptors(interceptor)
				.setAllowedOrigins("*");
	}

}
