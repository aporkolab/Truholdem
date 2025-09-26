package com.truholdem.controller;

import com.truholdem.config.OAuth2Config;
import com.truholdem.config.api.ApiV1Config;
import com.truholdem.dto.*;
import com.truholdem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication, JWT token management, and session control")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OAuth2Config oAuth2Config;

    public AuthController(AuthService authService, OAuth2Config oAuth2Config) {
        this.authService = authService;
        this.oAuth2Config = oAuth2Config;
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = """
            Authenticate user with username/password and receive JWT tokens.
            
            **Returns:**
            - `accessToken`: Short-lived JWT for API authentication
            - `refreshToken`: Long-lived token for obtaining new access tokens
            - `tokenType`: Always "Bearer"
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = JwtResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        JwtResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
        summary = "User registration",
        description = """
            Register a new user account.
            
            **Requirements:**
            - Username: 3-50 characters, unique
            - Email: Valid email format, unique
            - Password: Minimum 8 characters
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "User already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MessageResponseDto> register(@Valid @RequestBody UserRegistrationDto registrationRequest) {
        logger.info("Registration attempt for username: {}", registrationRequest.getUsername());
        MessageResponseDto response = authService.register(registrationRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh JWT token",
        description = "Exchange a valid refresh token for a new access token pair"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = JwtResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<JwtResponseDto> refreshToken(@Valid @RequestBody TokenRefreshRequestDto request) {
        logger.debug("Token refresh request received");
        JwtResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Logout current session and invalidate refresh token"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MessageResponseDto> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new MessageResponseDto("User not authenticated"));
        }
        logger.info("Logout request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all devices",
        description = "Invalidate all refresh tokens for the user, logging out from all devices"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Logout from all devices successful",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MessageResponseDto> logoutAllDevices(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logout all devices request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.logoutAllDevices(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(
        summary = "Change password",
        description = "Change the current user's password. Requires current password for verification."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Password changed successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid current password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MessageResponseDto> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        logger.info("Password change request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Check if the current JWT token is valid and not expired"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token is valid",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token is invalid or expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<MessageResponseDto> validateToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new MessageResponseDto("Token is invalid or expired"));
        }
        logger.debug("Token validation request for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponseDto("Token is valid"));
    }

    @GetMapping("/oauth2/providers")
    @Operation(
        summary = "Get available OAuth2 providers",
        description = "Returns which OAuth2 providers (Google, GitHub) are configured and available"
    )
    @ApiResponse(
        responseCode = "200",
        description = "OAuth2 provider availability"
    )
    public ResponseEntity<Map<String, Object>> getOAuth2Providers() {
        Map<String, Object> response = new HashMap<>();
        response.put("google", oAuth2Config.isGoogleConfigured());
        response.put("github", oAuth2Config.isGithubConfigured());
        return ResponseEntity.ok(response);
    }
}
