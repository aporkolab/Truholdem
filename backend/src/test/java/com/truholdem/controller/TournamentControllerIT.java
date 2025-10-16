package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.config.TestSecurityConfig;
import com.truholdem.dto.*;
import com.truholdem.model.*;
import com.truholdem.service.TournamentService;
import com.truholdem.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TournamentController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("TournamentController Integration Tests")
class TournamentControllerIT {

    private static final String BASE_URL = "/api/tournaments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TournamentService tournamentService;

    private Tournament testTournament;
    private UUID tournamentId;
    private UUID playerId;
    private UUID tableId;

    @BeforeEach
    void setUp() {
        tournamentId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        tableId = UUID.randomUUID();

        testTournament = createTestTournament();
    }

    private Tournament createTestTournament() {
        Tournament tournament = Tournament.builder("Sunday Million")
            .type(TournamentType.FREEZEOUT)
            .startingChips(1500)
            .players(2, 9)
            .buyIn(100)
            .build();
        
        
        try {
            var idField = Tournament.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tournament, tournamentId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return tournament;
    }

    

    @Nested
    @DisplayName("Create Tournament - POST /api/tournaments")
    class CreateTournamentTests {

        @Test
        @DisplayName("Should create tournament with valid request - returns 201")
        void createTournament_ValidRequest_Returns201() throws Exception {
            CreateTournamentRequest request = CreateTournamentRequest.freezeout(
                "Test Tournament", 100, 100);
            
            when(tournamentService.createTournament(any())).thenReturn(testTournament);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tournamentId.toString()))
                .andExpect(jsonPath("$.name").value("Sunday Million"))
                .andExpect(jsonPath("$.type").value("FREEZEOUT"))
                .andExpect(jsonPath("$.status").value("REGISTERING"));

            verify(tournamentService).createTournament(any());
        }

        @Test
        @DisplayName("Should create sit-and-go tournament - returns 201")
        void createTournament_SitAndGo_Returns201() throws Exception {
            CreateTournamentRequest request = CreateTournamentRequest.sitAndGo(
                "Quick SNG", 50);
            
            when(tournamentService.createTournament(any())).thenReturn(testTournament);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        @DisplayName("Should create rebuy tournament - returns 201")
        void createTournament_Rebuy_Returns201() throws Exception {
            CreateTournamentRequest request = CreateTournamentRequest.rebuy(
                "Rebuy Madness", 50, 200);
            
            when(tournamentService.createTournament(any())).thenReturn(testTournament);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should reject tournament with empty name - returns 400")
        void createTournament_EmptyName_Returns400() throws Exception {
            String invalidRequest = """
                {
                    "name": "",
                    "type": "FREEZEOUT",
                    "startingChips": 1500,
                    "minPlayers": 2,
                    "maxPlayers": 9,
                    "buyIn": 100
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tournament with invalid starting chips - returns 400")
        void createTournament_InvalidStartingChips_Returns400() throws Exception {
            String invalidRequest = """
                {
                    "name": "Test",
                    "type": "FREEZEOUT",
                    "startingChips": 50,
                    "minPlayers": 2,
                    "maxPlayers": 9,
                    "buyIn": 100
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject tournament with null type - returns 400")
        void createTournament_NullType_Returns400() throws Exception {
            String invalidRequest = """
                {
                    "name": "Test Tournament",
                    "startingChips": 1500,
                    "minPlayers": 2,
                    "maxPlayers": 9,
                    "buyIn": 100
                }
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }

    

    @Nested
    @DisplayName("List Tournaments - GET /api/tournaments")
    class ListTournamentsTests {

        @Test
        @DisplayName("Should list all tournaments - returns 200")
        void listTournaments_NoFilter_Returns200() throws Exception {
            when(tournamentService.getTournamentsByStatus("all"))
                .thenReturn(List.of(testTournament));

            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(tournamentId.toString()))
                .andExpect(jsonPath("$[0].name").value("Sunday Million"));
        }

        @Test
        @DisplayName("Should filter by OPEN status - returns 200")
        void listTournaments_OpenFilter_Returns200() throws Exception {
            when(tournamentService.getTournamentsByStatus("OPEN"))
                .thenReturn(List.of(testTournament));

            mockMvc.perform(get(BASE_URL)
                    .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

            verify(tournamentService).getTournamentsByStatus("OPEN");
        }

        @Test
        @DisplayName("Should filter by RUNNING status - returns 200")
        void listTournaments_RunningFilter_Returns200() throws Exception {
            when(tournamentService.getTournamentsByStatus("RUNNING"))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL)
                    .param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return empty list when no tournaments - returns 200")
        void listTournaments_Empty_Returns200() throws Exception {
            when(tournamentService.getTournamentsByStatus(any()))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    

    @Nested
    @DisplayName("Get Tournament - GET /api/tournaments/{id}")
    class GetTournamentTests {

        @Test
        @DisplayName("Should get tournament by ID - returns 200")
        void getTournament_ValidId_Returns200() throws Exception {
            when(tournamentService.getTournament(tournamentId)).thenReturn(testTournament);

            mockMvc.perform(get(BASE_URL + "/{id}", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournamentId.toString()))
                .andExpect(jsonPath("$.name").value("Sunday Million"))
                .andExpect(jsonPath("$.type").value("FREEZEOUT"))
                .andExpect(jsonPath("$.status").value("REGISTERING"))
                .andExpect(jsonPath("$.buyIn").value(100))
                .andExpect(jsonPath("$.startingChips").value(1500));
        }

        @Test
        @DisplayName("Should return 404 for non-existing tournament")
        void getTournament_NotFound_Returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(tournamentService.getTournament(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Tournament not found"));

            mockMvc.perform(get(BASE_URL + "/{id}", nonExistentId))
                .andExpect(status().isNotFound());
        }
    }

    

    @Nested
    @DisplayName("Tournament Registration - POST/DELETE /api/tournaments/{id}/register")
    class RegistrationTests {

        @Test
        @DisplayName("Should register player for tournament - returns 200")
        void registerPlayer_ValidRequest_Returns200() throws Exception {
            RegisterForTournamentRequest request = new RegisterForTournamentRequest(
                playerId, "TestPlayer");
            
            when(tournamentService.getTournament(tournamentId)).thenReturn(testTournament);

            mockMvc.perform(post(BASE_URL + "/{id}/register", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournamentId.toString()));

            verify(tournamentService).registerPlayer(tournamentId, playerId, "TestPlayer");
        }

        @Test
        @DisplayName("Should reject registration with empty name - returns 400")
        void registerPlayer_EmptyName_Returns400() throws Exception {
            String invalidRequest = """
                {
                    "playerId": "%s",
                    "playerName": ""
                }
                """.formatted(playerId);

            mockMvc.perform(post(BASE_URL + "/{id}/register", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject duplicate registration - returns 400")
        void registerPlayer_AlreadyRegistered_Returns400() throws Exception {
            RegisterForTournamentRequest request = new RegisterForTournamentRequest(
                playerId, "TestPlayer");
            
            doThrow(new IllegalStateException("Player already registered"))
                .when(tournamentService).registerPlayer(any(), any(), any());

            mockMvc.perform(post(BASE_URL + "/{id}/register", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should unregister player from tournament - returns 204")
        void unregisterPlayer_ValidRequest_Returns204() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}/register/{playerId}", tournamentId, playerId))
                .andExpect(status().isNoContent());

            verify(tournamentService).unregisterPlayer(tournamentId, playerId);
        }

        @Test
        @DisplayName("Should return 404 when unregistering non-existing player")
        void unregisterPlayer_NotFound_Returns404() throws Exception {
            doThrow(new IllegalStateException("Player not found in tournament"))
                .when(tournamentService).unregisterPlayer(any(), any());

            mockMvc.perform(delete(BASE_URL + "/{id}/register/{playerId}", tournamentId, playerId))
                .andExpect(status().isBadRequest());
        }
    }

    

    @Nested
    @DisplayName("Start Tournament - POST /api/tournaments/{id}/start")
    class StartTournamentTests {

        @Test
        @DisplayName("Should start tournament - returns 200")
        void startTournament_ValidRequest_Returns200() throws Exception {
            when(tournamentService.getTournament(tournamentId)).thenReturn(testTournament);

            mockMvc.perform(post(BASE_URL + "/{id}/start", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournamentId.toString()));

            verify(tournamentService).startTournament(tournamentId);
        }

        @Test
        @DisplayName("Should reject start with insufficient players - returns 400")
        void startTournament_InsufficientPlayers_Returns400() throws Exception {
            doThrow(new IllegalStateException("Not enough players"))
                .when(tournamentService).startTournament(tournamentId);

            mockMvc.perform(post(BASE_URL + "/{id}/start", tournamentId))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject start for already running tournament - returns 400")
        void startTournament_AlreadyRunning_Returns400() throws Exception {
            doThrow(new IllegalStateException("Tournament not in REGISTERING status"))
                .when(tournamentService).startTournament(tournamentId);

            mockMvc.perform(post(BASE_URL + "/{id}/start", tournamentId))
                .andExpect(status().isBadRequest());
        }
    }

    

    @Nested
    @DisplayName("Leaderboard - GET /api/tournaments/{id}/leaderboard")
    class LeaderboardTests {

        @Test
        @DisplayName("Should get leaderboard - returns 200")
        void getLeaderboard_ValidTournament_Returns200() throws Exception {
            TournamentRegistration reg1 = createMockRegistration(playerId, "Player1", 3000);
            TournamentRegistration reg2 = createMockRegistration(UUID.randomUUID(), "Player2", 2000);
            
            when(tournamentService.getLeaderboard(tournamentId)).thenReturn(List.of(reg1, reg2));

            mockMvc.perform(get(BASE_URL + "/{id}/leaderboard", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].playerName").value("Player1"))
                .andExpect(jsonPath("$[0].chips").value(3000))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].playerName").value("Player2"));
        }

        @Test
        @DisplayName("Should return empty leaderboard - returns 200")
        void getLeaderboard_Empty_Returns200() throws Exception {
            when(tournamentService.getLeaderboard(tournamentId)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/{id}/leaderboard", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    

    @Nested
    @DisplayName("Tables - GET /api/tournaments/{id}/tables")
    class TablesTests {

        @Test
        @DisplayName("Should get tournament tables - returns 200")
        void getTables_ValidTournament_Returns200() throws Exception {
            TournamentTable table = createMockTable(tableId, 1, 6, false);
            when(tournamentService.getTournamentTables(tournamentId)).thenReturn(List.of(table));

            mockMvc.perform(get(BASE_URL + "/{id}/tables", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(tableId.toString()))
                .andExpect(jsonPath("$[0].tableNumber").value(1))
                .andExpect(jsonPath("$[0].playerCount").value(6))
                .andExpect(jsonPath("$[0].isFinalTable").value(false));
        }

        @Test
        @DisplayName("Should get specific table - returns 200")
        void getSpecificTable_ValidRequest_Returns200() throws Exception {
            TournamentTable table = createMockTable(tableId, 1, 6, true);
            when(tournamentService.getTournamentTable(tournamentId, tableId)).thenReturn(table);

            mockMvc.perform(get(BASE_URL + "/{id}/tables/{tableId}", tournamentId, tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tableId.toString()))
                .andExpect(jsonPath("$.isFinalTable").value(true));
        }

        @Test
        @DisplayName("Should return 404 for non-existing table")
        void getSpecificTable_NotFound_Returns404() throws Exception {
            UUID nonExistentTableId = UUID.randomUUID();
            when(tournamentService.getTournamentTable(tournamentId, nonExistentTableId))
                .thenThrow(new ResourceNotFoundException("Table not found"));

            mockMvc.perform(get(BASE_URL + "/{id}/tables/{tableId}", tournamentId, nonExistentTableId))
                .andExpect(status().isNotFound());
        }
    }

    

    @Nested
    @DisplayName("Blinds - GET /api/tournaments/{id}/blinds")
    class BlindsTests {

        @Test
        @DisplayName("Should get blind info - returns 200")
        void getBlindInfo_ValidTournament_Returns200() throws Exception {
            when(tournamentService.getTournament(tournamentId)).thenReturn(testTournament);

            mockMvc.perform(get(BASE_URL + "/{id}/blinds", tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLevel").value(1))
                .andExpect(jsonPath("$.currentBlinds.smallBlind").exists())
                .andExpect(jsonPath("$.currentBlinds.bigBlind").exists())
                .andExpect(jsonPath("$.nextBlinds").exists())
                .andExpect(jsonPath("$.levelDurationMinutes").exists());
        }
    }

    

    @Nested
    @DisplayName("Rebuy - POST /api/tournaments/{id}/rebuy")
    class RebuyTests {

        @Test
        @DisplayName("Should process rebuy - returns 200")
        void processRebuy_ValidRequest_Returns200() throws Exception {
            TournamentRegistration reg = createMockRegistration(playerId, "TestPlayer", 1500);
            when(tournamentService.processRebuy(tournamentId, playerId)).thenReturn(reg);

            RebuyRequest request = new RebuyRequest(playerId);

            mockMvc.perform(post(BASE_URL + "/{id}/rebuy", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.newChipCount").value(1500));
        }

        @Test
        @DisplayName("Should reject rebuy past deadline - returns 400")
        void processRebuy_PastDeadline_Returns400() throws Exception {
            doThrow(new IllegalStateException("Player cannot rebuy: past deadline"))
                .when(tournamentService).processRebuy(tournamentId, playerId);

            RebuyRequest request = new RebuyRequest(playerId);

            mockMvc.perform(post(BASE_URL + "/{id}/rebuy", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject rebuy with null player ID - returns 400")
        void processRebuy_NullPlayerId_Returns400() throws Exception {
            String invalidRequest = "{}";

            mockMvc.perform(post(BASE_URL + "/{id}/rebuy", tournamentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }

    

    private TournamentRegistration createMockRegistration(UUID playerId, String name, int chips) {
        TournamentRegistration reg = mock(TournamentRegistration.class);
        when(reg.getPlayerId()).thenReturn(playerId);
        when(reg.getPlayerName()).thenReturn(name);
        when(reg.getCurrentChips()).thenReturn(chips);
        when(reg.getStatus()).thenReturn(RegistrationStatus.PLAYING);
        when(reg.getFinishPosition()).thenReturn(null);
        when(reg.getPrizeWon()).thenReturn(null);
        when(reg.getRebuysUsed()).thenReturn(0);
        when(reg.getAddOnsUsed()).thenReturn(0);
        when(reg.getBountiesCollected()).thenReturn(0);
        when(reg.canRebuy()).thenReturn(true);
        when(reg.getTournament()).thenReturn(testTournament);
        return reg;
    }

    private TournamentTable createMockTable(UUID id, int number, int playerCount, boolean isFinalTable) {
        TournamentTable table = mock(TournamentTable.class);
        when(table.getId()).thenReturn(id);
        when(table.getTableNumber()).thenReturn(number);
        when(table.getPlayerCount()).thenReturn(playerCount);
        when(table.isFinalTable()).thenReturn(isFinalTable);
        when(table.isActive()).thenReturn(true);
        when(table.getPlayerIds()).thenReturn(new ArrayList<>());
        return table;
    }
}
