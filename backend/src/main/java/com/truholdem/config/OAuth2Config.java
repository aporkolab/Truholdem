package com.truholdem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to check if OAuth2 is properly configured.
 */
@Configuration
public class OAuth2Config {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-id:}")
    private String githubClientId;

    /**
     * Check if Google OAuth is configured
     */
    public boolean isGoogleConfigured() {
        return googleClientId != null && !googleClientId.isEmpty()
            && !googleClientId.contains("your-google-client-id");
    }

    /**
     * Check if GitHub OAuth is configured
     */
    public boolean isGithubConfigured() {
        return githubClientId != null && !githubClientId.isEmpty()
            && !githubClientId.contains("your-github-client-id");
    }

    /**
     * Check if any OAuth provider is configured
     */
    public boolean isAnyOAuthConfigured() {
        return isGoogleConfigured() || isGithubConfigured();
    }
}
