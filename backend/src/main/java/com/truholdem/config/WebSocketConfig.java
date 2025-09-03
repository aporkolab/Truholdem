package com.truholdem.config;

import com.truholdem.security.WebSocketAuthInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration with security.
 *
 * Security features:
 * - JWT authentication via STOMP headers on CONNECT
 * - Restricted allowed origins (configured via properties)
 * - User destination prefix for private messages
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = "app.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties appProperties;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(AppProperties appProperties, WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.appProperties = appProperties;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Get allowed origins from configuration, with secure defaults
        String[] allowedOrigins = getAllowedOrigins();

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        // Also register without SockJS for native WebSocket clients
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add JWT authentication interceptor for WebSocket messages
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * Gets allowed origins from configuration.
     * Falls back to localhost origins if not configured.
     */
    private String[] getAllowedOrigins() {
        var origins = appProperties.getWebsocket().getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            // Secure defaults - only localhost for development
            return new String[]{"http://localhost:4200", "http://localhost:3000"};
        }

        // Filter out wildcard "*" for security
        return origins.stream()
                .filter(origin -> !"*".equals(origin))
                .toArray(String[]::new);
    }
}
