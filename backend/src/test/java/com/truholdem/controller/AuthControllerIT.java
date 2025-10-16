package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.dto.*;
import com.truholdem.exception.InvalidCredentialsException;
import com.truholdem.exception.TokenRefreshException;
import com.truholdem.exception.UserAlreadyExistsException;
import com.truholdem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIT {

    private static final String BASE_URL = "/api/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private JwtResponseDto jwtResponse;
    private MessageResponseDto successMessage;
    private LoginRequestDto validLogin;
    private UserRegistrationDto validRegistration;
    private TokenRefreshRequestDto validRefresh;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        
        jwtResponse = new JwtResponseDto();
        jwtResponse.setAccessToken("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.signature");
        jwtResponse.setRefreshToken("refresh-token-abc123");
        jwtResponse.setTokenType("Bearer");
        jwtResponse.setExpiresIn(3600L);
        jwtResponse.setUsername("testuser");
        jwtResponse.setEmail("test@example.com");
        jwtResponse.setRoles(Collections.singletonList("ROLE_USER"));

        successMessage = new MessageResponseDto("Operation successful");

        
        validLogin = new LoginRequestDto();
        validLogin.setUsername("testuser");
        validLogin.setPassword("password123");

        
        validRegistration = new UserRegistrationDto();
        validRegistration.setUsername("newuser");
        validRegistration.setEmail("newuser@example.com");
        validRegistration.setPassword("securePassword123");
        validRegistration.setFirstName("John");
        validRegistration.setLastName("Doe");

        
        validRefresh = new TokenRefreshRequestDto();
        validRefresh.setRefreshToken("valid-refresh-token");

        
        mockUserDetails = User.withUsername("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    
    
    
    @Nested
    @DisplayName("Registration Tests - POST /api/auth/register")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user with valid data - returns 200")
        void register_ValidData_Returns200() throws Exception {
            when(authService.register(any(UserRegistrationDto.class)))
                    .thenReturn(new MessageResponseDto("User registered successfully"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegistration)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(authService).register(any(UserRegistrationDto.class));
        }

        @Test
        @DisplayName("Should reject duplicate username - returns 409")
        void register_ExistingUsername_Returns409() throws Exception {
            when(authService.register(any(UserRegistrationDto.class)))
                    .thenThrow(new UserAlreadyExistsException("Username already exists"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegistration)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should reject duplicate email - returns 409")
        void register_ExistingEmail_Returns409() throws Exception {
            when(authService.register(any(UserRegistrationDto.class)))
                    .thenThrow(new UserAlreadyExistsException("Email already registered"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegistration)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should reject invalid email format - returns 400")
        void register_InvalidEmail_Returns400() throws Exception {
            UserRegistrationDto invalidEmailRequest = new UserRegistrationDto();
            invalidEmailRequest.setUsername("validuser");
            invalidEmailRequest.setEmail("not-an-email");
            invalidEmailRequest.setPassword("securePassword123");

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("Should reject password too short - returns 400")
        void register_PasswordTooShort_Returns400() throws Exception {
            UserRegistrationDto shortPasswordRequest = new UserRegistrationDto();
            shortPasswordRequest.setUsername("validuser");
            shortPasswordRequest.setEmail("valid@email.com");
            shortPasswordRequest.setPassword("short");

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shortPasswordRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
        }

        @Test
        @DisplayName("Should reject missing required fields - returns 400")
        void register_MissingFields_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(3))));
        }

        @Test
        @DisplayName("Should reject blank username - returns 400")
        void register_BlankUsername_Returns400() throws Exception {
            UserRegistrationDto blankUsernameRequest = new UserRegistrationDto();
            blankUsernameRequest.setUsername("");
            blankUsernameRequest.setEmail("valid@email.com");
            blankUsernameRequest.setPassword("securePassword123");

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankUsernameRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'username')]").exists());
        }

        @Test
        @DisplayName("Should reject username too short - returns 400")
        void register_UsernameTooShort_Returns400() throws Exception {
            UserRegistrationDto shortUsernameRequest = new UserRegistrationDto();
            shortUsernameRequest.setUsername("ab");
            shortUsernameRequest.setEmail("valid@email.com");
            shortUsernameRequest.setPassword("securePassword123");

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shortUsernameRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept registration with optional fields empty")
        void register_OptionalFieldsEmpty_Returns200() throws Exception {
            UserRegistrationDto minimalRequest = new UserRegistrationDto();
            minimalRequest.setUsername("minimaluser");
            minimalRequest.setEmail("minimal@email.com");
            minimalRequest.setPassword("securePassword123");
            

            when(authService.register(any(UserRegistrationDto.class)))
                    .thenReturn(new MessageResponseDto("User registered successfully"));

            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isOk());
        }
    }

    
    
    
    @Nested
    @DisplayName("Login Tests - POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login with valid credentials - returns 200 with JWT")
        void login_ValidCredentials_Returns200WithJwt() throws Exception {
            when(authService.login(any(LoginRequestDto.class))).thenReturn(jwtResponse);

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLogin)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-abc123"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.roles").isArray());

            verify(authService).login(any(LoginRequestDto.class));
        }

        @Test
        @DisplayName("Should reject wrong password - returns 401")
        void login_WrongPassword_Returns401() throws Exception {
            LoginRequestDto wrongPassword = new LoginRequestDto();
            wrongPassword.setUsername("testuser");
            wrongPassword.setPassword("wrongpassword");

            when(authService.login(any(LoginRequestDto.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid username or password"));

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPassword)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject non-existing user - returns 401")
        void login_NonExistingUser_Returns401() throws Exception {
            LoginRequestDto unknownUser = new LoginRequestDto();
            unknownUser.setUsername("unknownuser");
            unknownUser.setPassword("somepassword");

            when(authService.login(any(LoginRequestDto.class)))
                    .thenThrow(new InvalidCredentialsException("User not found"));

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unknownUser)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject missing username - returns 400")
        void login_MissingUsername_Returns400() throws Exception {
            LoginRequestDto missingUsername = new LoginRequestDto();
            missingUsername.setPassword("password123");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingUsername)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'username')]").exists());
        }

        @Test
        @DisplayName("Should reject missing password - returns 400")
        void login_MissingPassword_Returns400() throws Exception {
            LoginRequestDto missingPassword = new LoginRequestDto();
            missingPassword.setUsername("testuser");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(missingPassword)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
        }

        @Test
        @DisplayName("Should reject empty credentials - returns 400")
        void login_EmptyCredentials_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject blank username - returns 400")
        void login_BlankUsername_Returns400() throws Exception {
            LoginRequestDto blankUsername = new LoginRequestDto();
            blankUsername.setUsername("   ");
            blankUsername.setPassword("password123");

            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankUsername)))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("Token Refresh Tests - POST /api/auth/refresh")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token with valid refresh token - returns 200 with new tokens")
        void refreshToken_ValidToken_Returns200WithNewTokens() throws Exception {
            JwtResponseDto newJwtResponse = new JwtResponseDto();
            newJwtResponse.setAccessToken("new-access-token");
            newJwtResponse.setRefreshToken("new-refresh-token");
            newJwtResponse.setTokenType("Bearer");
            newJwtResponse.setExpiresIn(3600L);

            when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                    .thenReturn(newJwtResponse);

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRefresh)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

            verify(authService).refreshToken(any(TokenRefreshRequestDto.class));
        }

        @Test
        @DisplayName("Should reject expired refresh token - returns 403")
        void refreshToken_ExpiredToken_Returns403() throws Exception {
            TokenRefreshRequestDto expiredToken = new TokenRefreshRequestDto();
            expiredToken.setRefreshToken("expired-refresh-token");

            when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                    .thenThrow(new TokenRefreshException("expired-refresh-token", "Refresh token has expired"));

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(expiredToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject invalid refresh token - returns 403")
        void refreshToken_InvalidToken_Returns403() throws Exception {
            TokenRefreshRequestDto invalidToken = new TokenRefreshRequestDto();
            invalidToken.setRefreshToken("invalid-token-abc");

            when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                    .thenThrow(new TokenRefreshException("invalid-token-abc", "Refresh token not found"));

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject missing refresh token - returns 400")
        void refreshToken_MissingToken_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'refreshToken')]").exists());
        }

        @Test
        @DisplayName("Should reject blank refresh token - returns 400")
        void refreshToken_BlankToken_Returns400() throws Exception {
            TokenRefreshRequestDto blankToken = new TokenRefreshRequestDto();
            blankToken.setRefreshToken("");

            mockMvc.perform(post(BASE_URL + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankToken)))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("Logout Tests - POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout authenticated user - returns 200")
        void logout_AuthenticatedUser_Returns200() throws Exception {
            when(authService.logout(eq("testuser")))
                    .thenReturn(new MessageResponseDto("Logged out successfully"));

            mockMvc.perform(post(BASE_URL + "/logout")
                            .with(user(mockUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("Should reject unauthenticated logout - returns 401")
        void logout_UnauthenticatedUser_Returns401() throws Exception {
            mockMvc.perform(post(BASE_URL + "/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    
    
    
    @Nested
    @DisplayName("Logout All Devices Tests - POST /api/auth/logout-all")
    class LogoutAllDevicesTests {

        @Test
        @DisplayName("Should logout from all devices - returns 200")
        void logoutAllDevices_AuthenticatedUser_Returns200() throws Exception {
            when(authService.logoutAllDevices(eq("testuser")))
                    .thenReturn(new MessageResponseDto("Logged out from all devices"));

            mockMvc.perform(post(BASE_URL + "/logout-all")
                            .with(user(mockUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out from all devices"));
        }
    }

    
    
    
    @Nested
    @DisplayName("Change Password Tests - POST /api/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password with valid request - returns 200")
        void changePassword_ValidRequest_Returns200() throws Exception {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto();
            request.setCurrentPassword("oldPassword123");
            request.setNewPassword("newSecurePassword456");

            when(authService.changePassword(eq("testuser"), any(ChangePasswordRequestDto.class)))
                    .thenReturn(new MessageResponseDto("Password changed successfully"));

            mockMvc.perform(post(BASE_URL + "/change-password")
                            .with(user(mockUserDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("Should reject wrong current password - returns 401")
        void changePassword_WrongCurrentPassword_Returns401() throws Exception {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto();
            request.setCurrentPassword("wrongPassword");
            request.setNewPassword("newSecurePassword456");

            when(authService.changePassword(eq("testuser"), any(ChangePasswordRequestDto.class)))
                    .thenThrow(new InvalidCredentialsException("Current password is incorrect"));

            mockMvc.perform(post(BASE_URL + "/change-password")
                            .with(user(mockUserDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject new password too short - returns 400")
        void changePassword_NewPasswordTooShort_Returns400() throws Exception {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto();
            request.setCurrentPassword("currentPassword123");
            request.setNewPassword("short");

            mockMvc.perform(post(BASE_URL + "/change-password")
                            .with(user(mockUserDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    
    
    
    @Nested
    @DisplayName("Token Validation Tests - GET /api/auth/validate")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token - returns 200")
        void validateToken_ValidToken_Returns200() throws Exception {
            mockMvc.perform(get(BASE_URL + "/validate")
                            .with(user(mockUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Token is valid"));
        }

        @Test
        @DisplayName("Should reject invalid token - returns 401")
        void validateToken_InvalidToken_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/validate"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
        }
    }

    
    
    
    @Nested
    @DisplayName("Content-Type Validation Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should reject non-JSON content type on login")
        void login_NonJsonContentType_Returns415() throws Exception {
            mockMvc.perform(post(BASE_URL + "/login")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("username=test&password=test"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should reject non-JSON content type on register")
        void register_NonJsonContentType_Returns415() throws Exception {
            mockMvc.perform(post(BASE_URL + "/register")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .content("username=test&email=test@test.com&password=test123"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}
