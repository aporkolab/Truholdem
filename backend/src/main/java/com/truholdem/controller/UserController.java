package com.truholdem.controller;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.dto.ErrorResponse;
import com.truholdem.dto.MessageResponseDto;
import com.truholdem.dto.UserProfileDto;
import com.truholdem.dto.UserUpdateDto;
import com.truholdem.model.User;
import com.truholdem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@RestController
@ApiV1Config
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User profiles, admin controls, and role management")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the authenticated user's profile information"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Profile request for user: {}", userDetails.getUsername());
        
        User user = (User) userDetails;
        UserProfileDto profile = userService.getUserProfile(user.getId());
        
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/{userId}")
    @Operation(
        summary = "Get user profile by ID",
        description = "Retrieve any user's profile information. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> getUserProfile(
            @Parameter(description = "UUID of the user") @PathVariable UUID userId) {
        logger.info("Admin profile request for user ID: {}", userId);
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update user profile",
        description = "Update the authenticated user's profile information"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateDto updateDto) {
        
        logger.info("Profile update request for user: {}", userDetails.getUsername());
        
        User user = (User) userDetails;
        User updatedUser = userService.updateUser(user, updateDto);
        UserProfileDto profile = userService.getUserProfile(updatedUser.getId());
        
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate a user account, preventing login. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User deactivated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> deactivateUser(
            @Parameter(description = "UUID of the user to deactivate") @PathVariable UUID userId) {
        logger.info("Admin deactivate user request for user ID: {}", userId);
        userService.deactivateUser(userId);
        return ResponseEntity.ok(new MessageResponseDto("User deactivated successfully"));
    }

    @PostMapping("/{userId}/activate")
    @Operation(
        summary = "Activate user",
        description = "Activate a previously deactivated user account. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User activated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> activateUser(
            @Parameter(description = "UUID of the user to activate") @PathVariable UUID userId) {
        logger.info("Admin activate user request for user ID: {}", userId);
        userService.activateUser(userId);
        return ResponseEntity.ok(new MessageResponseDto("User activated successfully"));
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @Operation(
        summary = "Add role to user",
        description = "Assign a role to a user for RBAC. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Role added successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User or role not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> addRoleToUser(
            @Parameter(description = "UUID of the user") @PathVariable UUID userId,
            @Parameter(description = "Role name (e.g., ADMIN, USER)") @PathVariable String roleName) {
        
        logger.info("Admin add role '{}' to user ID: {}", roleName, userId);
        userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok(new MessageResponseDto("Role added successfully"));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @Operation(
        summary = "Remove role from user",
        description = "Remove a role from a user. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Role removed successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - admin role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User or role not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> removeRoleFromUser(
            @Parameter(description = "UUID of the user") @PathVariable UUID userId,
            @Parameter(description = "Role name to remove") @PathVariable String roleName) {
        
        logger.info("Admin remove role '{}' from user ID: {}", roleName, userId);
        userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(new MessageResponseDto("Role removed successfully"));
    }

    @GetMapping("/active")
    @Operation(
        summary = "Get active users",
        description = "Retrieve list of all active user accounts. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getActiveUsers() {
        logger.info("Admin request for all active users");
        List<User> activeUsers = userService.findAllActiveUsers();
        List<UserProfileDto> profiles = activeUsers.stream()
                .map(user -> userService.getUserProfile(user.getId()))
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/recent")
    @Operation(
        summary = "Get recently active users",
        description = "Retrieve users who have been active within the specified period. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recently active users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getRecentlyActiveUsers(
            @Parameter(description = "Number of days to look back", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        
        logger.info("Admin request for users active in last {} days", days);
        Instant since = Instant.now().minusSeconds(days * 24 * 60 * 60);
        List<User> recentUsers = userService.findRecentlyActiveUsers(since);
        List<UserProfileDto> profiles = recentUsers.stream()
                .map(user -> userService.getUserProfile(user.getId()))
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/stats/new-users")
    @Operation(
        summary = "Get new user count",
        description = "Get count of users registered within the specified period. **Requires ADMIN role.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New user count retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getNewUserCount(
            @Parameter(description = "Number of days to look back", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        logger.info("Admin request for new user count in last {} days", days);
        Instant since = Instant.now().minusSeconds(days * 24 * 60 * 60);
        Long count = userService.countNewUsersInPeriod(since);
        return ResponseEntity.ok(count);
    }
}
