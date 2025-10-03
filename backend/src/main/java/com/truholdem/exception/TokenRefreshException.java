package com.truholdem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {
    
    private final String token;
    
    public TokenRefreshException(String token, String message) {
        super(message);
        this.token = token;
    }
    
    public TokenRefreshException(String token, String message, Throwable cause) {
        super(message, cause);
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
}
