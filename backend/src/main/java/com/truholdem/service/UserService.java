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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, 
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder,
                      UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        logger.debug("Loading user by username or email: {}", usernameOrEmail);
        // Try to find by username first, then by email
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        logger.debug("Found user: {}, password hash length: {}, starts with $2a$: {}, hash prefix: {}",
            user.getUsername(),
            user.getPassword().length(),
            user.getPassword().startsWith("$2a$"),
            user.getPassword().substring(0, Math.min(29, user.getPassword().length()))); // Log BCrypt prefix (version + cost + salt prefix)
        return user;
    }

    @Transactional
    public User createUser(UserRegistrationDto registrationDto) {
        logger.info("Creating new user with username: {}", registrationDto.getUsername());

        validateUserRegistration(registrationDto);

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());

        String rawPassword = registrationDto.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Verify the encoding works correctly
        boolean verifyMatch = passwordEncoder.matches(rawPassword, encodedPassword);
        logger.debug("Password encoding - raw length: {}, first char code: {}, last char code: {}, encoded length: {}, starts with $2a$: {}, verify match: {}",
            rawPassword.length(),
            (int) rawPassword.charAt(0),
            (int) rawPassword.charAt(rawPassword.length() - 1),
            encodedPassword.length(),
            encodedPassword.startsWith("$2a$"),
            verifyMatch);

        if (!verifyMatch) {
            logger.error("CRITICAL: Password encoding verification failed! The encoder cannot verify its own output.");
        }

        user.setPasswordHash(encodedPassword);

        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());

        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);
        logger.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user, UserUpdateDto updateDto) {
        logger.info("Updating user with ID: {}", user.getId());

        if (updateDto.getFirstName() != null) {
            user.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null) {
            user.setLastName(updateDto.getLastName());
        }
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists: " + updateDto.getEmail());
            }
            user.setEmail(updateDto.getEmail());
            user.setEmailVerified(false); // Reset email verification
        }
        if (updateDto.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDto.getAvatarUrl());
        }

        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
        logger.info("User deactivated: {}", userId);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setActive(true);
        userRepository.save(user);
        logger.info("User activated: {}", userId);
    }

    @Transactional
    public void updateLastLogin(User user) {
        user.setLastLogin(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#user.id")
    public void verifyEmail(User user) {
        user.setEmailVerified(true);
        userRepository.save(user);
        logger.info("Email verified for user: {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public List<User> findAllActiveUsers() {
        return userRepository.findAllActiveUsers();
    }

    @Transactional(readOnly = true)
    public List<User> findRecentlyActiveUsers(Instant since) {
        return userRepository.findUsersLoginSince(since);
    }

    @Transactional(readOnly = true)
    public Long countNewUsersInPeriod(Instant since) {
        return userRepository.countNewUsersInPeriod(since);
    }

    @Transactional
    public User addRoleToUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        
        user.addRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User removeRoleFromUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        
        user.removeRole(role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return userMapper.toProfileDto(user);
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password changed for user: {}", user.getUsername());
    }

    private void validateUserRegistration(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + registrationDto.getUsername());
        }
        
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + registrationDto.getEmail());
        }
    }
}
