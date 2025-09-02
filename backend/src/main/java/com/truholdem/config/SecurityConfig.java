package com.truholdem.config;

import com.truholdem.security.CustomOAuth2UserService;
import com.truholdem.security.JwtAuthenticationFilter;
import com.truholdem.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the TruHoldem application.
 *
 * This configuration implements JWT-based stateless authentication with the following security measures:
 * - JWT token validation for protected endpoints
 * - Stateless session management (no server-side session)
 * - CSRF disabled for REST API (stateless authentication mitigates CSRF risks)
 * - Role-based access control via method security
 * - Secured actuator endpoints (only health/info public)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2Config oAuth2Config;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         CustomOAuth2UserService customOAuth2UserService,
                         OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                         OAuth2Config oAuth2Config) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2Config = oAuth2Config;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST API (JWT tokens provide CSRF protection)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Stateless session - no session cookies, each request authenticated via JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                // Allow preflight OPTIONS requests
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // Public authentication endpoints
                .requestMatchers("/auth/**").permitAll()

                // API Documentation - public for development
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                // Actuator - only health and info public, rest require authentication
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // WebSocket handshake - allow, actual messages secured via token
                .requestMatchers("/ws/**").permitAll()

                // Poker game endpoints - allow guest play
                .requestMatchers("/poker/**").permitAll()

                // Lobby for quick play - allow guests
                .requestMatchers("/lobby/**").permitAll()

                // Statistics and leaderboard - public for demo
                .requestMatchers("/stats/**").permitAll()

                // Game endpoints that require auth for user-specific features
                .requestMatchers("/game/**").authenticated()
                .requestMatchers("/v1/**").authenticated()
                .requestMatchers("/v2/**").authenticated()
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/tournaments/**").authenticated()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Only configure OAuth2 login if at least one provider is configured
        if (oAuth2Config.isAnyOAuthConfigured()) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/auth/oauth2/authorize")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/auth/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect("http://localhost:4200/auth/login?error=oauth_failed");
                })
            );
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Increased strength for better security
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:4202",
            "http://127.0.0.1:4200",
            "http://truholdem.porkolab.hu",
            "https://truholdem.porkolab.hu",
            "http://www.truholdem.porkolab.hu",
            "https://www.truholdem.porkolab.hu"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
