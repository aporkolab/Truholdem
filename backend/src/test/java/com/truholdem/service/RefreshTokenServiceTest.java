package com.truholdem.service;

import com.truholdem.config.AppProperties;
import com.truholdem.exception.TokenRefreshException;
import com.truholdem.model.RefreshToken;
import com.truholdem.model.User;
import com.truholdem.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setRefreshExpiration(60_000);
        service = new RefreshTokenService(repository, properties);
        user = new User("owner", "owner@example.test", "unused");
        user.setId(UUID.randomUUID());
    }

    @Test
    void issuesTokenForUserWithConfiguredExpiryAndRetriesCollisions() {
        when(repository.countByUser(user)).thenReturn(2L);
        when(repository.findByToken(anyString()))
                .thenReturn(Optional.of(token("collision", 60)), Optional.empty());
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        RefreshToken created = service.createRefreshToken(user);

        assertSame(user, created.getUser());
        assertDoesNotThrow(() -> UUID.fromString(created.getToken()));
        assertFalse(created.getExpiryDate().isBefore(before.plusSeconds(60)));
        assertFalse(created.getExpiryDate().isAfter(Instant.now().plusSeconds(60)));
        verify(repository, times(2)).findByToken(anyString());
        verify(repository, never()).delete(any());
    }

    @Test
    void excessSessionsEvictOldestTokenWithoutDeletingNewerSessions() {
        RefreshToken oldest = token("oldest", 60);
        oldest.setCreatedAt(Instant.now().minusSeconds(120));
        RefreshToken newer = token("newer", 60);
        newer.setCreatedAt(Instant.now().minusSeconds(30));
        when(repository.countByUser(user)).thenReturn(6L);
        when(repository.findByUser(user)).thenReturn(List.of(newer, oldest));
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createRefreshToken(user);

        verify(repository).delete(oldest);
        verify(repository, never()).delete(newer);
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void rejectsAndDeletesExpiredToken() {
        RefreshToken expired = token("expired", -60);
        assertThrows(TokenRefreshException.class, () -> service.verifyExpiration(expired));
        verify(repository).delete(expired);
    }

    @Test
    void retainsValidToken() {
        RefreshToken valid = token("valid", 60);
        assertSame(valid, service.verifyExpiration(valid));
        verifyNoInteractions(repository);
    }

    @Test
    void revokesOnlyRequestedToken() {
        RefreshToken existing = token("revoke-me", 60);
        when(repository.findByToken("revoke-me")).thenReturn(Optional.of(existing));
        service.deleteByToken("revoke-me");
        verify(repository).delete(existing);
        verify(repository, never()).deleteByUser(any());
    }

    @Test
    void revokingUnknownTokenIsIdempotent() {
        when(repository.findByToken("missing")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.deleteByToken("missing"));
        verify(repository, never()).delete(any());
    }

    @Test
    void scheduledCleanupDeletesOnlyExpiredTokens() {
        RefreshToken expired = token("expired", -60);
        when(repository.findExpiredTokens(any(Instant.class))).thenReturn(List.of(expired));
        service.scheduledTokenCleanup();
        verify(repository).deleteAll(List.of(expired));
        verify(repository, never()).deleteByUser(any());
    }

    @Test
    void cleanupHandlesNoExpiredTokens() {
        when(repository.findExpiredTokens(any(Instant.class))).thenReturn(List.of());
        assertDoesNotThrow(() -> service.deleteExpiredTokens());
        verify(repository).deleteAll(List.of());
    }

    private RefreshToken token(String value, long expirySeconds) {
        RefreshToken token = new RefreshToken(user, value, Instant.now().plusSeconds(expirySeconds));
        token.setId(UUID.randomUUID());
        return token;
    }
}
