package com.truholdem.service;

import com.truholdem.config.AppProperties;
import com.truholdem.exception.TokenRefreshException;
import com.truholdem.model.RefreshToken;
import com.truholdem.model.User;
import com.truholdem.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              AppProperties appProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        logger.debug("Creating refresh token for user: {}", user.getUsername());

        // Clean up existing tokens for the user (optional - limit concurrent sessions)
        Long existingTokenCount = refreshTokenRepository.countByUser(user);
        if (existingTokenCount > 5) { // Limit to 5 concurrent sessions
            List<RefreshToken> userTokens = refreshTokenRepository.findByUser(user);
            RefreshToken oldestToken = userTokens.stream()
                    .min((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                    .orElse(null);
            if (oldestToken != null) {
                refreshTokenRepository.delete(oldestToken);
                logger.debug("Deleted oldest refresh token for user: {}", user.getUsername());
            }
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(appProperties.getJwt().getRefreshExpiration()));
        refreshToken.setToken(generateUniqueToken());

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        logger.debug("Refresh token created successfully for user: {}", user.getUsername());
        
        return savedToken;
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            logger.warn("Refresh token expired for user: {}", token.getUser().getUsername());
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), 
                "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Transactional
    public void deleteByUser(User user) {
        logger.debug("Deleting all refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void deleteByToken(String token) {
        logger.debug("Deleting refresh token");
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteExpiredTokens() {
        logger.debug("Cleaning up expired refresh tokens");
        List<RefreshToken> expiredTokens = refreshTokenRepository.findExpiredTokens(Instant.now());
        refreshTokenRepository.deleteAll(expiredTokens);
        
        if (!expiredTokens.isEmpty()) {
            logger.info("Deleted {} expired refresh tokens", expiredTokens.size());
        }
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> findTokensByUser(User user) {
        return refreshTokenRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Long countTokensByUser(User user) {
        return refreshTokenRepository.countByUser(user);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (refreshTokenRepository.findByToken(token).isPresent());
        
        return token;
    }

    // Scheduled cleanup of expired tokens (runs every hour)
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void scheduledTokenCleanup() {
        logger.debug("Running scheduled cleanup of expired refresh tokens");
        deleteExpiredTokens();
    }
}
