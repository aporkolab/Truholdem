package com.truholdem.service;

import com.truholdem.model.Game;
import com.truholdem.model.Player;
import com.truholdem.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameAuthorizationServiceTest {
    @Mock
    private PokerGameService pokerGameService;

    private GameAuthorizationService authorization;
    private Game game;
    private User owner;
    private Player human;
    private Player bot;

    enum Action { PLAYER, BOT, NEW_HAND, VIEW }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authorization = new GameAuthorizationService(pokerGameService, true);
        owner = new User("owner", "owner@example.test", "unused");
        owner.setId(UUID.randomUUID());
        human = new Player("owner", 1000, false);
        human.setId(UUID.randomUUID());
        human.setUserId(owner.getId());
        bot = new Player("bot", 1000, true);
        bot.setId(UUID.randomUUID());
        game = new Game();
        game.setId(UUID.randomUUID());
        game.setPlayers(List.of(human, bot));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void rejectsMissingAuthentication(Action action) {
        assertThrows(AccessDeniedException.class, () -> invoke(action));
        verifyNoInteractions(pokerGameService);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void rejectsUnauthenticatedUser(Action action) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner, "unused"));
        assertThrows(AccessDeniedException.class, () -> invoke(action));
        verifyNoInteractions(pokerGameService);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void rejectsNonUserPrincipal(Action action) {
        signIn("anonymousUser");
        assertThrows(AccessDeniedException.class, () -> invoke(action));
        verifyNoInteractions(pokerGameService);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void rejectsMissingGame(Action action) {
        signIn(owner);
        when(pokerGameService.getGame(game.getId())).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> invoke(action));
    }

    @Test
    void ownerCanControlTheirHumanPlayer() {
        loadGameAs(owner);
        assertDoesNotThrow(() -> authorization.validatePlayerAction(game.getId(), human.getId()));
    }

    @Test
    void anotherUserCannotControlHumanPlayer() {
        loadGameAs(outsider());
        assertThrows(AccessDeniedException.class,
                () -> authorization.validatePlayerAction(game.getId(), human.getId()));
    }

    @Test
    void rejectsPlayersAbsentFromGame() {
        loadGameAs(owner);
        UUID absentId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class,
                () -> authorization.validatePlayerAction(game.getId(), absentId));
        assertThrows(AccessDeniedException.class,
                () -> authorization.validateBotAction(game.getId(), absentId));
    }

    @Test
    void participantCanTriggerBotThroughEitherEntryPoint() {
        loadGameAs(owner);
        assertDoesNotThrow(() -> authorization.validatePlayerAction(game.getId(), bot.getId()));
        assertDoesNotThrow(() -> authorization.validateBotAction(game.getId(), bot.getId()));
    }

    @Test
    void outsiderCannotTriggerBotsOrStartNewHand() {
        loadGameAs(outsider());
        assertThrows(AccessDeniedException.class,
                () -> authorization.validatePlayerAction(game.getId(), bot.getId()));
        assertThrows(AccessDeniedException.class,
                () -> authorization.validateBotAction(game.getId(), bot.getId()));
        assertThrows(AccessDeniedException.class,
                () -> authorization.validateNewHandAction(game.getId()));
    }

    @Test
    void rejectsHumanPlayerAtBotEndpoint() {
        loadGameAs(owner);
        assertThrows(AccessDeniedException.class,
                () -> authorization.validateBotAction(game.getId(), human.getId()));
    }

    @Test
    void participantCanStartNewHand() {
        loadGameAs(owner);
        assertDoesNotThrow(() -> authorization.validateNewHandAction(game.getId()));
    }

    @Test
    void authenticatedSpectatorCanViewExistingGame() {
        loadGameAs(outsider());
        assertDoesNotThrow(() -> authorization.validateGameView(game.getId()));
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void explicitTestModeBypassesAuthorization(Action action) {
        authorization = new GameAuthorizationService(pokerGameService, false);
        assertDoesNotThrow(() -> invoke(action));
        verifyNoInteractions(pokerGameService);
    }

    private void invoke(Action action) {
        switch (action) {
            case PLAYER -> authorization.validatePlayerAction(game.getId(), human.getId());
            case BOT -> authorization.validateBotAction(game.getId(), bot.getId());
            case NEW_HAND -> authorization.validateNewHandAction(game.getId());
            case VIEW -> authorization.validateGameView(game.getId());
        }
    }

    private void loadGameAs(User user) {
        signIn(user);
        when(pokerGameService.getGame(game.getId())).thenReturn(Optional.of(game));
    }

    private void signIn(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User outsider() {
        User user = new User("outsider", "outsider@example.test", "unused");
        user.setId(UUID.randomUUID());
        return user;
    }
}
