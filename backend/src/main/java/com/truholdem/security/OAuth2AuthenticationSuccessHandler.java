package com.truholdem.security;

import com.truholdem.model.RefreshToken;
import com.truholdem.model.User;
import com.truholdem.service.RefreshTokenService;
import com.truholdem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens
 * and redirecting to the frontend with the tokens.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @Value("${app.oauth2.redirect-uri:http://localhost:4200/auth/oauth-callback}")
    private String redirectUri;

    public OAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil,
                                              RefreshTokenService refreshTokenService,
                                              @Lazy UserService userService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        logger.info("OAuth2 authentication successful");

        User user = extractUser(authentication);
        if (user == null) {
            logger.error("Failed to extract user from OAuth2 authentication");
            response.sendRedirect(redirectUri + "?error=authentication_failed");
            return;
        }

        // Update last login
        userService.updateLastLogin(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        logger.info("Generated tokens for OAuth2 user: {}", user.getUsername());

        // Redirect to frontend with tokens
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();

        logger.debug("Redirecting to: {}", redirectUri);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User extractUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2UserPrincipal oauth2Principal) {
            return oauth2Principal.getUser();
        }

        logger.warn("Unknown principal type: {}", principal.getClass().getName());
        return null;
    }
}
