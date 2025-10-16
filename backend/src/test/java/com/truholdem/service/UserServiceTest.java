package com.truholdem.service;

import com.truholdem.dto.UserProfileDto;
import com.truholdem.dto.UserRegistrationDto;
import com.truholdem.dto.UserUpdateDto;
import com.truholdem.exception.ResourceNotFoundException;
import com.truholdem.exception.UserAlreadyExistsException;
import com.truholdem.mapper.UserMapper;
import com.truholdem.model.Role;
import com.truholdem.model.User;
import com.truholdem.repository.RoleRepository;
import com.truholdem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private UserRegistrationDto registrationDto;
    private UserUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setActive(true);
        testUser.setEmailVerified(false);
        Set<Role> mutableRoles = new HashSet<>();
        mutableRoles.add(userRole);
        testUser.setRoles(mutableRoles);
        testUser.setCreatedAt(Instant.now());

        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("new@example.com");
        registrationDto.setPassword("password");
        registrationDto.setFirstName("Jane");
        registrationDto.setLastName("Smith");

        updateDto = new UserUpdateDto();
        updateDto.setFirstName("UpdatedJohn");
        updateDto.setLastName("UpdatedDoe");
        updateDto.setEmail("updated@example.com");
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_UserNotExists_ThrowsUsernameNotFoundException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, 
                () -> userService.loadUserByUsername("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void createUser_ValidRegistration_ReturnsCreatedUser() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(registrationDto);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(passwordEncoder).encode(registrationDto.getPassword());
        verify(roleRepository).findByName("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_UsernameAlreadyExists_ThrowsUserAlreadyExistsException() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, 
                () -> userService.createUser(registrationDto));
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsUserAlreadyExistsException() {
        // Given
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, 
                () -> userService.createUser(registrationDto));
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findById_UserExists_ReturnsOptionalUser() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(userId);
    }

    @Test
    void findByUsername_UserExists_ReturnsOptionalUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void updateUser_ValidUpdate_ReturnsUpdatedUser() {
        // Given
        when(userRepository.existsByEmail(updateDto.getEmail())).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.updateUser(testUser, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("UpdatedJohn", testUser.getFirstName());
        assertEquals("UpdatedDoe", testUser.getLastName());
        assertEquals("updated@example.com", testUser.getEmail());
        assertFalse(testUser.isEmailVerified()); // Should reset email verification
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsUserAlreadyExistsException() {
        // Given
        when(userRepository.existsByEmail(updateDto.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, 
                () -> userService.updateUser(testUser, updateDto));
        verify(userRepository).existsByEmail(updateDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivateUser_UserExists_DeactivatesUser() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.deactivateUser(userId);

        // Then
        assertFalse(testUser.isActive());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void deactivateUser_UserNotExists_ThrowsResourceNotFoundException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                () -> userService.deactivateUser(userId));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void activateUser_UserExists_ActivatesUser() {
        // Given
        testUser.setActive(false);
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.activateUser(userId);

        // Then
        assertTrue(testUser.isActive());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateLastLogin_UpdatesTimestamp() {
        // Given
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.updateLastLogin(testUser);

        // Then
        assertNotNull(testUser.getLastLogin());
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmail_SetsEmailVerifiedToTrue() {
        // Given
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.verifyEmail(testUser);

        // Then
        assertTrue(testUser.isEmailVerified());
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_ValidUserAndRole_AddsRole() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.addRoleToUser(userId, "ADMIN");

        // Then
        assertNotNull(result);
        assertTrue(testUser.getRoles().contains(adminRole));
        verify(userRepository).findById(userId);
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_UserNotExists_ThrowsResourceNotFoundException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                () -> userService.addRoleToUser(userId, "ADMIN"));
        verify(userRepository).findById(userId);
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void addRoleToUser_RoleNotExists_ThrowsResourceNotFoundException() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                () -> userService.addRoleToUser(userId, "NONEXISTENT"));
        verify(userRepository).findById(userId);
        verify(roleRepository).findByName("NONEXISTENT");
    }

    @Test
    void removeRoleFromUser_ValidUserAndRole_RemovesRole() {
        // Given
        testUser.addRole(adminRole);
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.removeRoleFromUser(userId, "ADMIN");

        // Then
        assertNotNull(result);
        assertFalse(testUser.getRoles().contains(adminRole));
        verify(userRepository).findById(userId);
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(testUser);
    }

    @Test
    void getUserProfile_UserExists_ReturnsProfile() {
        // Given
        UUID userId = testUser.getId();
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setUsername("testuser");
        profileDto.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toProfileDto(testUser)).thenReturn(profileDto);

        // When
        UserProfileDto result = userService.getUserProfile(userId);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findById(userId);
        verify(userMapper).toProfileDto(testUser);
    }

    @Test
    void getUserProfile_UserNotExists_ThrowsResourceNotFoundException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                () -> userService.getUserProfile(userId));
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toProfileDto(any());
    }

    @Test
    void changePassword_EncodesAndSavesNewPassword() {
        // Given
        String newPassword = "newPassword";
        String encodedPassword = "encodedNewPassword";
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.changePassword(testUser, newPassword);

        // Then
        assertEquals(encodedPassword, testUser.getPasswordHash());
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void findAllActiveUsers_ReturnsActiveUsers() {
        // Given
        List<User> activeUsers = List.of(testUser);
        when(userRepository.findAllActiveUsers()).thenReturn(activeUsers);

        // When
        List<User> result = userService.findAllActiveUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userRepository).findAllActiveUsers();
    }

    @Test
    void findRecentlyActiveUsers_ReturnsRecentUsers() {
        // Given
        Instant since = Instant.now().minusSeconds(3600);
        List<User> recentUsers = List.of(testUser);
        when(userRepository.findUsersLoginSince(since)).thenReturn(recentUsers);

        // When
        List<User> result = userService.findRecentlyActiveUsers(since);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userRepository).findUsersLoginSince(since);
    }

    @Test
    void countNewUsersInPeriod_ReturnsCount() {
        // Given
        Instant since = Instant.now().minusSeconds(86400);
        Long expectedCount = 5L;
        when(userRepository.countNewUsersInPeriod(since)).thenReturn(expectedCount);

        // When
        Long result = userService.countNewUsersInPeriod(since);

        // Then
        assertEquals(expectedCount, result);
        verify(userRepository).countNewUsersInPeriod(since);
    }
}
