package com.truholdem.service;

import com.truholdem.dto.*;
import com.truholdem.exception.InvalidCredentialsException;
import com.truholdem.exception.TokenRefreshException;
import com.truholdem.model.RefreshToken;
import com.truholdem.model.User;
import com.truholdem.repository.RefreshTokenRepository;
import com.truholdem.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(AuthenticationManager authenticationManager,
                      UserService userService,
                      RefreshTokenService refreshTokenService,
                      JwtUtil jwtUtil,
                      RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public JwtResponseDto login(LoginRequestDto loginRequest) {
        logger.info("Attempting login for username: {}", loginRequest.getUsername());
        logger.debug("Login password length: {}, first char code: {}, last char code: {}",
            loginRequest.getPassword().length(),
            (int) loginRequest.getPassword().charAt(0),
            (int) loginRequest.getPassword().charAt(loginRequest.getPassword().length() - 1));

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            User user = (User) authentication.getPrincipal();
            
            // Update last login time
            userService.updateLastLogin(user);

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            logger.info("Login successful for user: {}", user.getUsername());

            return new JwtResponseDto(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtUtil.getTokenExpiration(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                    .map(role -> role.getName())
                    .toList()
            );

        } catch (AuthenticationException e) {
            logger.warn("Login failed for username: {} - {}", loginRequest.getUsername(), e.getMessage());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Transactional
    public MessageResponseDto register(UserRegistrationDto registrationRequest) {
        logger.info("Attempting to register new user: {}", registrationRequest.getUsername());

        User user = userService.createUser(registrationRequest);
        
        logger.info("User registered successfully: {}", user.getUsername());
        return new MessageResponseDto("User registered successfully!");
    }

    @Transactional
    public JwtResponseDto refreshToken(TokenRefreshRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();
        logger.debug("Attempting to refresh token");

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtUtil.generateToken(user);
                    logger.debug("Token refreshed successfully for user: {}", user.getUsername());
                    
                    return new JwtResponseDto(
                        newAccessToken,
                        requestRefreshToken,
                        "Bearer",
                        jwtUtil.getTokenExpiration(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRoles().stream()
                            .map(role -> role.getName())
                            .toList()
                    );
                })
                .orElseThrow(() -> {
                    logger.warn("Invalid refresh token provided");
                    return new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!");
                });
    }

    @Transactional
    public MessageResponseDto logout(String username) {
        logger.info("Logging out user: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        refreshTokenService.deleteByUser(user);
        
        logger.info("User logged out successfully: {}", username);
        return new MessageResponseDto("User logged out successfully!");
    }

    @Transactional
    public MessageResponseDto logoutAllDevices(String username) {
        logger.info("Logging out user from all devices: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        refreshTokenService.deleteByUser(user);
        
        logger.info("User logged out from all devices: {}", username);
        return new MessageResponseDto("User logged out from all devices!");
    }

    @Transactional
    public MessageResponseDto changePassword(String username, ChangePasswordRequestDto request) {
        logger.info("Changing password for user: {}", username);

        try {
            // Verify current password
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getCurrentPassword())
            );

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            userService.changePassword(user, request.getNewPassword());

            // Invalidate all refresh tokens to force re-login
            refreshTokenService.deleteByUser(user);

            logger.info("Password changed successfully for user: {}", username);
            return new MessageResponseDto("Password changed successfully! Please log in again.");

        } catch (AuthenticationException e) {
            logger.warn("Password change failed for user: {} - invalid current password", username);
            throw new InvalidCredentialsException("Current password is incorrect");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Transactional(readOnly = true)
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        logger.debug("Cleaning up expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
}
