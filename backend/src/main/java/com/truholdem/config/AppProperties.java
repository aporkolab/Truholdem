package com.truholdem.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @Valid
    @NotNull
    private final Jwt jwt = new Jwt();

    @Valid
    @NotNull
    private final Cors cors = new Cors();

    @Valid
    @NotNull
    private final WebSocket websocket = new WebSocket();

    @Valid
    @NotNull
    private final Game game = new Game();

    
    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public WebSocket getWebsocket() {
        return websocket;
    }

    public Game getGame() {
        return game;
    }

    public static class Jwt {
        @NotBlank
        private String secret;

        @Min(1)
        private long expiration;

        @Min(1)
        private long refreshExpiration;

        
        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public long getRefreshExpiration() {
            return refreshExpiration;
        }

        public void setRefreshExpiration(long refreshExpiration) {
            this.refreshExpiration = refreshExpiration;
        }
    }

    public static class Cors {
        @NotNull
        private List<String> allowedOrigins;

        @NotNull
        private List<String> allowedMethods;

        @NotNull
        private List<String> allowedHeaders;

        private boolean allowCredentials = true;

        
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
    }

    public static class WebSocket {
        @NotNull
        private List<String> allowedOrigins;

        
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Game {
        @Min(1)
        private int defaultChips;

        @Min(1)
        private int defaultSmallBlind;

        @Min(1)
        private int defaultBigBlind;

        @Min(2)
        private int maxPlayers;

        @Min(2)
        private int minPlayers;

        @Min(100)
        private long botThinkTime = 100;

        
        public int getDefaultChips() {
            return defaultChips;
        }

        public void setDefaultChips(int defaultChips) {
            this.defaultChips = defaultChips;
        }

        public int getDefaultSmallBlind() {
            return defaultSmallBlind;
        }

        public void setDefaultSmallBlind(int defaultSmallBlind) {
            this.defaultSmallBlind = defaultSmallBlind;
        }

        public int getDefaultBigBlind() {
            return defaultBigBlind;
        }

        public void setDefaultBigBlind(int defaultBigBlind) {
            this.defaultBigBlind = defaultBigBlind;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public int getMinPlayers() {
            return minPlayers;
        }

        public void setMinPlayers(int minPlayers) {
            this.minPlayers = minPlayers;
        }

        public long getBotThinkTime() {
            return botThinkTime;
        }

        public void setBotThinkTime(long botThinkTime) {
            this.botThinkTime = botThinkTime;
        }
    }
}
