package com.truholdem.dto;

import java.time.Instant;
import java.util.List;

public class JwtResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Instant expiresAt;
    private String username;
    private String email;
    private List<String> roles;

    public JwtResponseDto() {}

    public JwtResponseDto(String accessToken, String refreshToken, String tokenType, 
                         Instant expiresAt, String username, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Backward compatibility method for tests
    public void setExpiresIn(long expiresInSeconds) {
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    public long getExpiresIn() {
        return expiresAt != null ? expiresAt.getEpochSecond() - Instant.now().getEpochSecond() : 0;
    }
}
