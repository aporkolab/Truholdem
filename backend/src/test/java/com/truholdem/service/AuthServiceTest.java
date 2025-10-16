package com.truholdem.service;

import com.truholdem.dto.*;
import com.truholdem.exception.InvalidCredentialsException;
import com.truholdem.exception.TokenRefreshException;
import com.truholdem.model.RefreshToken;
import com.truholdem.model.Role;
import com.truholdem.model.User;
import com.truholdem.repository.RefreshTokenRepository;
import com.truholdem.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setName("USER");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRoles(Set.of(userRole));

        testRefreshToken = new RefreshToken();
        testRefreshToken.setToken("refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
    }

    @Test
    void login_ValidCredentials_ReturnsJwtResponse() {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(testUser)).thenReturn("jwt-token-123");
        when(jwtUtil.getTokenExpiration()).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(testRefreshToken);

        // When
        JwtResponseDto result = authService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals("jwt-token-123", result.getAccessToken());
        assertEquals("refresh-token-123", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(userService).updateLastLogin(testUser);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser);
    }

    @Test
    void login_InvalidCredentials_ThrowsInvalidCredentialsException() {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void register_ValidRequest_ReturnsSuccessMessage() {
        // Given
        UserRegistrationDto registrationRequest = new UserRegistrationDto();
        registrationRequest.setUsername("newuser");
        registrationRequest.setEmail("new@example.com");
        registrationRequest.setPassword("password");
        registrationRequest.setFirstName("John");
        registrationRequest.setLastName("Doe");

        when(userService.createUser(registrationRequest)).thenReturn(testUser);

        // When
        MessageResponseDto result = authService.register(registrationRequest);

        // Then
        assertNotNull(result);
        assertEquals("User registered successfully!", result.getMessage());
        verify(userService).createUser(registrationRequest);
    }

    @Test
    void refreshToken_ValidToken_ReturnsNewJwtResponse() {
        // Given
        TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto();
        refreshRequest.setRefreshToken("refresh-token-123");

        when(refreshTokenService.findByToken("refresh-token-123"))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.verifyExpiration(testRefreshToken))
                .thenReturn(testRefreshToken);
        when(jwtUtil.generateToken(testUser)).thenReturn("new-jwt-token");
        when(jwtUtil.getTokenExpiration()).thenReturn(Instant.now().plusSeconds(3600));

        // When
        JwtResponseDto result = authService.refreshToken(refreshRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-jwt-token", result.getAccessToken());
        assertEquals("refresh-token-123", result.getRefreshToken());
        assertEquals("testuser", result.getUsername());

        verify(refreshTokenService).findByToken("refresh-token-123");
        verify(refreshTokenService).verifyExpiration(testRefreshToken);
        verify(jwtUtil).generateToken(testUser);
    }

    @Test
    void refreshToken_InvalidToken_ThrowsTokenRefreshException() {
        // Given
        TokenRefreshRequestDto refreshRequest = new TokenRefreshRequestDto();
        refreshRequest.setRefreshToken("invalid-token");

        when(refreshTokenService.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(TokenRefreshException.class, () -> authService.refreshToken(refreshRequest));
        verify(refreshTokenService).findByToken("invalid-token");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void logout_ValidUser_ReturnsSuccessMessage() {
        // Given
        String username = "testuser";
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        MessageResponseDto result = authService.logout(username);

        // Then
        assertNotNull(result);
        assertEquals("User logged out successfully!", result.getMessage());
        verify(userService).findByUsername(username);
        verify(refreshTokenService).deleteByUser(testUser);
    }

    @Test
    void changePassword_ValidCurrentPassword_ReturnsSuccessMessage() {
        // Given
        String username = "testuser";
        ChangePasswordRequestDto changeRequest = new ChangePasswordRequestDto();
        changeRequest.setCurrentPassword("currentPassword");
        changeRequest.setNewPassword("newPassword");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        MessageResponseDto result = authService.changePassword(username, changeRequest);

        // Then
        assertNotNull(result);
        assertEquals("Password changed successfully! Please log in again.", result.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).changePassword(testUser, "newPassword");
        verify(refreshTokenService).deleteByUser(testUser);
    }

    @Test
    void changePassword_InvalidCurrentPassword_ThrowsInvalidCredentialsException() {
        // Given
        String username = "testuser";
        ChangePasswordRequestDto changeRequest = new ChangePasswordRequestDto();
        changeRequest.setCurrentPassword("wrongPassword");
        changeRequest.setNewPassword("newPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThrows(InvalidCredentialsException.class, 
                () -> authService.changePassword(username, changeRequest));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService, never()).changePassword(any(), any());
        verify(refreshTokenService, never()).deleteByUser(any());
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = "valid-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);

        // When
        boolean result = authService.validateToken(token);

        // Then
        assertTrue(result);
        verify(jwtUtil).validateToken(token);
    }

    @Test
    void getUsernameFromToken_ValidToken_ReturnsUsername() {
        // Given
        String token = "valid-token";
        String expectedUsername = "testuser";
        when(jwtUtil.getUsernameFromToken(token)).thenReturn(expectedUsername);

        // When
        String result = authService.getUsernameFromToken(token);

        // Then
        assertEquals(expectedUsername, result);
        verify(jwtUtil).getUsernameFromToken(token);
    }

    @Test
    void cleanupExpiredTokens_CallsRepository() {
        // When
        authService.cleanupExpiredTokens();

        // Then
        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }
}
