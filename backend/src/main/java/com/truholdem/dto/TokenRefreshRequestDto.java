package com.truholdem.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenRefreshRequestDto {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public TokenRefreshRequestDto() {}

    public TokenRefreshRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
