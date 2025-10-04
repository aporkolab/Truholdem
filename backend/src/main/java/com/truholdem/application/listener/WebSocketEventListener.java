package com.truholdem.application.listener;

import com.truholdem.websocket.ClusterSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    
    private static final Pattern GAME_DESTINATION_PATTERN = 
            Pattern.compile("/topic/game/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");

    private final ClusterSessionRegistry sessionRegistry;

    public WebSocketEventListener(ClusterSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal principal = headerAccessor.getUser();
        String username = principal != null ? principal.getName() : null;

        logger.info("WebSocket connected - session: {}, user: {}", sessionId, username);

        
        if (sessionId != null) {
            sessionRegistry.registerSession(sessionId, username);
        }
    }

    
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        logger.info("WebSocket disconnected - session: {}", sessionId);

        
        if (sessionId != null) {
            sessionRegistry.unregisterSession(sessionId);
        }
    }

    
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (sessionId == null || destination == null) {
            return;
        }

        
        Matcher matcher = GAME_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String gameIdStr = matcher.group(1);
            try {
                UUID gameId = UUID.fromString(gameIdStr);
                sessionRegistry.subscribeToGame(sessionId, gameId);
                logger.info("Session {} subscribed to game {}", sessionId, gameId);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid game ID in subscription destination: {}", destination);
            }
        }
    }

    
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        logger.debug("Session {} unsubscribed from subscription {}", sessionId, subscriptionId);

        
        
        
    }
}
