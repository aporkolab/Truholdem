package com.truholdem.security;

import com.truholdem.model.OAuthProvider;
import com.truholdem.model.Role;
import com.truholdem.model.User;
import com.truholdem.repository.RoleRepository;
import com.truholdem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom OAuth2 user service that creates or updates users based on OAuth2 provider data.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());

        logger.info("Processing OAuth2 login from provider: {}", provider);

        String oauthId = extractOAuthId(oauth2User, provider);
        String email = extractEmail(oauth2User, provider);
        String name = extractName(oauth2User, provider);
        String avatarUrl = extractAvatarUrl(oauth2User, provider);

        logger.debug("OAuth2 user info - id: {}, email: {}, name: {}", oauthId, email, name);

        // Find or create user
        User user = findOrCreateUser(provider, oauthId, email, name, avatarUrl);

        // Return a custom OAuth2User that wraps our User entity
        return new OAuth2UserPrincipal(user, oauth2User.getAttributes());
    }

    private String extractOAuthId(OAuth2User oauth2User, OAuthProvider provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("sub");
            case GITHUB -> String.valueOf(attributes.get("id"));
        };
    }

    private String extractEmail(OAuth2User oauth2User, OAuthProvider provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");

        // GitHub may not provide email if it's private
        if (email == null && provider == OAuthProvider.GITHUB) {
            String login = (String) attributes.get("login");
            email = login + "@github.placeholder.com";
            logger.warn("GitHub user {} has private email, using placeholder", login);
        }

        return email;
    }

    private String extractName(OAuth2User oauth2User, OAuthProvider provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("name");
            case GITHUB -> {
                String name = (String) attributes.get("name");
                yield name != null ? name : (String) attributes.get("login");
            }
        };
    }

    private String extractAvatarUrl(OAuth2User oauth2User, OAuthProvider provider) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("picture");
            case GITHUB -> (String) attributes.get("avatar_url");
        };
    }

    private User findOrCreateUser(OAuthProvider provider, String oauthId, String email, String name, String avatarUrl) {
        // First try to find by OAuth provider and ID
        Optional<User> existingOAuthUser = userRepository.findByOauthProviderAndOauthId(provider, oauthId);
        if (existingOAuthUser.isPresent()) {
            User user = existingOAuthUser.get();
            // Update avatar if changed
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
            }
            logger.info("Found existing OAuth user: {}", user.getUsername());
            return user;
        }

        // Check if user exists with same email (link accounts)
        Optional<User> existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser.isPresent()) {
            User user = existingEmailUser.get();
            // Link OAuth provider to existing account
            user.setOauthProvider(provider);
            user.setOauthId(oauthId);
            if (avatarUrl != null && user.getAvatarUrl() == null) {
                user.setAvatarUrl(avatarUrl);
            }
            userRepository.save(user);
            logger.info("Linked OAuth provider {} to existing user: {}", provider, user.getUsername());
            return user;
        }

        // Create new user
        User newUser = new User();
        newUser.setOauthProvider(provider);
        newUser.setOauthId(oauthId);
        newUser.setEmail(email);
        newUser.setUsername(generateUniqueUsername(name, provider));
        newUser.setAvatarUrl(avatarUrl);
        newUser.setEmailVerified(true); // OAuth emails are verified
        newUser.setPasswordHash(""); // No password for OAuth users

        // Parse name into first/last
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            newUser.setFirstName(parts[0]);
            newUser.setLastName(parts[1]);
        } else {
            newUser.setFirstName(name);
        }

        // Assign USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
        newUser.addRole(userRole);

        User savedUser = userRepository.save(newUser);
        logger.info("Created new OAuth user: {} via {}", savedUser.getUsername(), provider);
        return savedUser;
    }

    private String generateUniqueUsername(String name, OAuthProvider provider) {
        // Create base username from name or provider
        String baseUsername = name != null
            ? name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase()
            : provider.name().toLowerCase() + "_user";

        if (baseUsername.length() < 3) {
            baseUsername = provider.name().toLowerCase() + "_" + baseUsername;
        }

        // Truncate if too long
        if (baseUsername.length() > 40) {
            baseUsername = baseUsername.substring(0, 40);
        }

        // Check if username exists and add suffix if needed
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }
}
