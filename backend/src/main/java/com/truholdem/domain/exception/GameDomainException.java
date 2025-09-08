package com.truholdem.domain.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public abstract sealed class GameDomainException extends RuntimeException
        permits InvalidActionException, GameStateException, PlayerNotFoundException {

    private final String errorCode;
    private final Map<String, Object> context;

    
    protected GameDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    
    protected GameDomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    
    protected GameDomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    
    public String getErrorCode() {
        return errorCode;
    }

    
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    
    protected GameDomainException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    
    public GameDomainException withGameId(UUID gameId) {
        return withContext("gameId", gameId);
    }

    
    public GameDomainException withPlayerId(UUID playerId) {
        return withContext("playerId", playerId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName())
          .append("[code=").append(errorCode)
          .append(", message=").append(getMessage());
        if (!context.isEmpty()) {
            sb.append(", context=").append(context);
        }
        sb.append("]");
        return sb.toString();
    }
}
