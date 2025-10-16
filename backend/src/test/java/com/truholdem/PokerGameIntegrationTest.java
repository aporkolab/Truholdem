package com.truholdem;

import com.truholdem.dto.ErrorResponse;
import com.truholdem.dto.PlayerActionRequest;
import com.truholdem.model.Game;
import com.truholdem.model.PlayerAction;
import com.truholdem.model.PlayerInfo;
import com.truholdem.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import com.truholdem.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class PokerGameIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GameRepository gameRepository;

    private List<PlayerInfo> playersInfo;

    @BeforeEach
    void setUp() {
        playersInfo = Arrays.asList(
                new PlayerInfo("Player 1", 1000, false),
                new PlayerInfo("Player 2", 1000, true)
        );
    }

    @Test
    void testStartGame_ShouldReturnOkAndGameDetails() {
        // Act
        ResponseEntity<Game> response = restTemplate.postForEntity("/api/poker/game/start", playersInfo, Game.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(2, response.getBody().getPlayers().size());
    }

    @Test
    void testPlayerAction_Raise_ShouldUpdateGameState() {
        // Arrange: Create a game first
        ResponseEntity<Game> createGameResponse = restTemplate.postForEntity("/api/poker/game/start", playersInfo, Game.class);
        Game createdGame = createGameResponse.getBody();
        assertNotNull(createdGame);
        UUID gameId = createdGame.getId();
        UUID playerId = createdGame.getPlayers().get(0).getId();

        PlayerActionRequest raiseAction = new PlayerActionRequest();
        raiseAction.setAction(PlayerAction.RAISE);
        raiseAction.setAmount(100);

        // Act
        String url = String.format("/api/poker/game/%s/player/%s/action", gameId, playerId);
        ResponseEntity<Game> response = restTemplate.postForEntity(url, raiseAction, Game.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100, response.getBody().getCurrentBet());
        assertEquals("Player 1", response.getBody().getPlayers().get(0).getName());
        assertEquals(100, response.getBody().getPlayers().get(0).getBetAmount());
    }

    @Test
    void testPlayerAction_WhenGameNotFound_ShouldReturn404() {
        // Arrange
        UUID nonExistentGameId = UUID.randomUUID();
        UUID dummyPlayerId = UUID.randomUUID();
        PlayerActionRequest action = new PlayerActionRequest();
        action.setAction(PlayerAction.CHECK);

        // Act
        String url = String.format("/api/poker/game/%s/player/%s/action", nonExistentGameId, dummyPlayerId);
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(url, action, ErrorResponse.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Game not found"));
    }

    @Test
    void testPlayerAction_Check_WhenAllowed_ShouldSucceed() {
        // This test models a full pre-flop betting round for 2 players
        // Arrange: Create a game
        ResponseEntity<Game> createGameResponse = restTemplate.postForEntity("/api/poker/game/start", playersInfo, Game.class);
        Game createdGame = createGameResponse.getBody();
        assertNotNull(createdGame);
        UUID gameId = createdGame.getId();

        // Player 1 (Small Blind) is first to act
        UUID player1Id = createdGame.getPlayers().get(0).getId();
        // Player 2 (Big Blind)
        UUID player2Id = createdGame.getPlayers().get(1).getId();

        // Act 1: Player 1 calls the big blind
        PlayerActionRequest callAction = new PlayerActionRequest(player1Id.toString(), PlayerAction.CALL, 0);
        String callUrl = String.format("/api/poker/game/%s/player/%s/action", gameId, player1Id);
        ResponseEntity<Game> callResponse = restTemplate.postForEntity(callUrl, callAction, Game.class);
        assertEquals(HttpStatus.OK, callResponse.getStatusCode());

        // Now it's Player 2's turn. They can check because their bet matches the current bet.
        PlayerActionRequest checkAction = new PlayerActionRequest(player2Id.toString(), PlayerAction.CHECK, 0);

        // Act 2: Player 2 checks
        String checkUrl = String.format("/api/poker/game/%s/player/%s/action", gameId, player2Id);
        ResponseEntity<Game> checkResponse = restTemplate.postForEntity(checkUrl, checkAction, Game.class);

        // Assert
        assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
        assertNotNull(checkResponse.getBody());
        // The check completes the betting round, so the game should advance to the FLOP
        assertEquals("FLOP", checkResponse.getBody().getPhase().toString());
        assertEquals(3, checkResponse.getBody().getCommunityCards().size());
        // Bets should be reset for the new round
        assertEquals(0, checkResponse.getBody().getPlayers().get(1).getBetAmount());
    }

    @Test
    void testPlayerAction_Fold_ShouldUpdatePlayerState() {
        // Arrange
        ResponseEntity<Game> createGameResponse = restTemplate.postForEntity("/api/poker/game/start", playersInfo, Game.class);
        Game createdGame = createGameResponse.getBody();
        assertNotNull(createdGame);
        UUID gameId = createdGame.getId();
        UUID playerId = createdGame.getPlayers().get(0).getId();

        PlayerActionRequest foldAction = new PlayerActionRequest();
        foldAction.setAction(PlayerAction.FOLD);

        // Act
        String url = String.format("/api/poker/game/%s/player/%s/action", gameId, playerId);
        ResponseEntity<Game> response = restTemplate.postForEntity(url, foldAction, Game.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getPlayers().get(0).isFolded());
    }

    @Test
    void testPlayerAction_Call_ShouldUpdatePlayerState() {
        // Arrange
        List<PlayerInfo> threePlayersInfo = Arrays.asList(
                new PlayerInfo("Player 1", 1000, false),
                new PlayerInfo("Player 2", 1000, true),
                new PlayerInfo("Player 3", 1000, false)
        );
        ResponseEntity<Game> createGameResponse = restTemplate.postForEntity("/api/poker/game/start", threePlayersInfo, Game.class);
        Game createdGame = createGameResponse.getBody();
        assertNotNull(createdGame);
        UUID gameId = createdGame.getId();
        // In a 3-player game, player 3 (index 2) is the first to act after blinds.
        UUID player3Id = createdGame.getPlayers().get(2).getId();

        PlayerActionRequest callAction = new PlayerActionRequest(player3Id.toString(), PlayerAction.CALL, 0);

        // Act
        String url = String.format("/api/poker/game/%s/player/%s/action", gameId, player3Id);
        ResponseEntity<Game> response = restTemplate.postForEntity(url, callAction, Game.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Game responseGame = response.getBody();
        assertNotNull(responseGame);
        // The betting round is not over, so the bet amount should be updated and visible.
        assertEquals(20, responseGame.getPlayers().stream().filter(p -> p.getId().equals(player3Id)).findFirst().get().getBetAmount());
        // The game should still be in the PRE_FLOP phase.
        assertEquals("PRE_FLOP", responseGame.getPhase().toString());
    }
}
