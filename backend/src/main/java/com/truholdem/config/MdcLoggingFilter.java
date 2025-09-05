package com.truholdem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_PATH_KEY = "requestPath";
    public static final String REQUEST_METHOD_KEY = "requestMethod";
    public static final String GAME_ID_KEY = "gameId";
    public static final String PLAYER_ID_KEY = "playerId";
    public static final String CLIENT_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = generateCorrelationId();
            }
            MDC.put(CORRELATION_ID_KEY, correlationId);

            
            MDC.put(REQUEST_PATH_KEY, request.getRequestURI());
            MDC.put(REQUEST_METHOD_KEY, request.getMethod());
            MDC.put(CLIENT_IP_KEY, getClientIp(request));

            
            extractGameContext(request.getRequestURI());

            
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);

        } finally {
            
            MDC.clear();
        }
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void extractGameContext(String uri) {
        if (uri == null) return;

        
        if (uri.contains("/game/")) {
            String[] parts = uri.split("/game/");
            if (parts.length > 1) {
                String afterGame = parts[1];
                String gameId = afterGame.split("/")[0];
                if (isValidUuid(gameId)) {
                    MDC.put(GAME_ID_KEY, gameId);
                }

                
                if (afterGame.contains("/player/")) {
                    String[] playerParts = afterGame.split("/player/");
                    if (playerParts.length > 1) {
                        String playerId = playerParts[1].split("/")[0];
                        if (isValidUuid(playerId)) {
                            MDC.put(PLAYER_ID_KEY, playerId);
                        }
                    }
                }

                
                if (afterGame.contains("/bot/")) {
                    String[] botParts = afterGame.split("/bot/");
                    if (botParts.length > 1) {
                        String botId = botParts[1].split("/")[0];
                        if (isValidUuid(botId)) {
                            MDC.put(PLAYER_ID_KEY, botId);
                        }
                    }
                }
            }
        }

        
        if (uri.contains("/history/")) {
            String[] parts = uri.split("/history/");
            if (parts.length > 1) {
                String historyId = parts[1].split("/")[0];
                if (isValidUuid(historyId)) {
                    MDC.put("historyId", historyId);
                }
            }
        }
    }

    private boolean isValidUuid(String str) {
        if (str == null || str.length() < 32) return false;
        try {
            
            if (str.length() == 36) {
                UUID.fromString(str);
                return true;
            } else if (str.length() == 32) {
                
                String withDashes = str.substring(0, 8) + "-" + 
                                   str.substring(8, 12) + "-" + 
                                   str.substring(12, 16) + "-" + 
                                   str.substring(16, 20) + "-" + 
                                   str.substring(20);
                UUID.fromString(withDashes);
                return true;
            }
        } catch (IllegalArgumentException e) {
            
        }
        return false;
    }

    
    public static void setGameContext(UUID gameId, UUID playerId) {
        if (gameId != null) {
            MDC.put(GAME_ID_KEY, gameId.toString());
        }
        if (playerId != null) {
            MDC.put(PLAYER_ID_KEY, playerId.toString());
        }
    }

    
    public static void clearGameContext() {
        MDC.remove(GAME_ID_KEY);
        MDC.remove(PLAYER_ID_KEY);
    }
}
