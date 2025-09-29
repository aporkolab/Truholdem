package com.truholdem.controller;

import com.truholdem.domain.event.*;
import com.truholdem.dto.TournamentMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Controller
public class TournamentWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(TournamentWebSocketController.class);
    private static final String TOURNAMENT_TOPIC = "/topic/tournament/";

    private final SimpMessagingTemplate messagingTemplate;

    public TournamentWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    

    @EventListener
    public void handleTournamentStarted(TournamentStarted event) {
        log.info("Broadcasting TournamentStarted event for tournament {}", event.getTournamentId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("playerCount", event.getPlayerCount());
        data.put("tableCount", event.getTableCount());
        data.put("prizePool", event.getPrizePool());
        
        broadcast(event.getTournamentId(), "TOURNAMENT_STARTED", data);
    }

    @EventListener
    public void handleBlindLevelIncreased(TournamentLevelAdvanced event) {
        log.info("Broadcasting BlindLevelIncreased event for tournament {} - Level {}", 
                 event.getTournamentId(), event.getNewLevel());
        
        Map<String, Object> data = new HashMap<>();
        data.put("newLevel", event.getNewLevel());
        data.put("smallBlind", event.getSmallBlind());
        data.put("bigBlind", event.getBigBlind());
        data.put("ante", event.getAnte());
        data.put("playersRemaining", event.getPlayersRemaining());
        
        broadcast(event.getTournamentId(), "BLIND_LEVEL_INCREASED", data);
    }

    @EventListener
    public void handlePlayerEliminated(TournamentPlayerEliminated event) {
        log.info("Broadcasting PlayerEliminated event for tournament {} - Player {} finished {}", 
                 event.getTournamentId(), event.getPlayerName(), event.getFinishPosition());
        
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", event.getPlayerId());
        data.put("playerName", event.getPlayerName());
        data.put("finishPosition", event.getFinishPosition());
        data.put("prize", event.getPrizeWon());
        data.put("playersRemaining", event.getPlayersRemaining());
        
        if (event.getEliminatedBy() != null) {
            data.put("eliminatedBy", event.getEliminatedBy());
        }
        
        broadcast(event.getTournamentId(), "PLAYER_ELIMINATED", data);
    }

    @EventListener
    public void handleTableRebalance(TournamentTablesRebalanced event) {
        log.info("Broadcasting TableRebalanced event for tournament {} - {} active tables", 
                 event.getTournamentId(), event.getActiveTableCount());
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeTableCount", event.getActiveTableCount());
        data.put("playerMoves", event.getPlayerMoves().stream()
            .map(move -> Map.of(
                "playerId", move.playerId(),
                "fromTableId", move.fromTableId(),
                "toTableId", move.toTableId()
            ))
            .toList());
        data.put("closedTableIds", event.getClosedTableIds());
        data.put("finalTableFormed", event.isFinalTableFormed());
        
        
        if (event.isFinalTableFormed()) {
            broadcast(event.getTournamentId(), "FINAL_TABLE_REACHED", 
                Map.of("activeTableCount", event.getActiveTableCount()));
        }
        
        broadcast(event.getTournamentId(), "TABLE_REBALANCED", data);
    }

    @EventListener
    public void handleTournamentCompleted(TournamentCompleted event) {
        log.info("Broadcasting TournamentCompleted event for tournament {} - Winner: {}", 
                 event.getTournamentId(), event.getWinnerName());
        
        Map<String, Object> data = new HashMap<>();
        data.put("winnerId", event.getWinnerId());
        data.put("winnerName", event.getWinnerName());
        data.put("totalPrizePool", event.getTotalPrizePool());
        data.put("totalPlayers", event.getTotalPlayers());
        data.put("finalLevel", event.getFinalLevel());
        data.put("durationMinutes", event.getDuration().toMinutes());
        data.put("topFinishers", event.getTopFinishers().stream()
            .map(f -> Map.of(
                "position", f.position(),
                "playerId", f.playerId(),
                "playerName", f.playerName(),
                "prizeWon", f.prizeWon()
            ))
            .toList());
        
        broadcast(event.getTournamentId(), "TOURNAMENT_COMPLETED", data);
    }

    @EventListener
    public void handlePlayerRegistered(TournamentPlayerRegistered event) {
        log.info("Broadcasting PlayerRegistered event for tournament {} - Player: {}", 
                 event.getTournamentId(), event.getPlayerName());
        
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", event.getPlayerId());
        data.put("playerName", event.getPlayerName());
        data.put("currentCount", event.getCurrentRegistrations());
        data.put("maxPlayers", event.getMaxPlayers());
        
        broadcast(event.getTournamentId(), "PLAYER_REGISTERED", data);
    }

    @EventListener
    public void handleTableCreated(TournamentTableCreated event) {
        log.info("Broadcasting TableCreated event for tournament {} - Table #{}", 
                 event.getTournamentId(), event.getTableNumber());
        
        Map<String, Object> data = new HashMap<>();
        data.put("tableId", event.getTableId());
        data.put("tableNumber", event.getTableNumber());
        data.put("playerIds", event.getSeatedPlayerIds());
        data.put("isFinalTable", event.isFinalTable());
        
        broadcast(event.getTournamentId(), "TABLE_CREATED", data);
    }

    @EventListener
    public void handleTournamentCreated(TournamentCreated event) {
        log.info("Broadcasting TournamentCreated event for tournament {} - {}", 
                 event.getTournamentId(), event.getTournamentName());
        
        Map<String, Object> data = new HashMap<>();
        data.put("name", event.getTournamentName());
        data.put("type", event.getTournamentType().name());
        data.put("buyIn", event.getBuyIn());
        data.put("startingChips", event.getStartingChips());
        data.put("maxPlayers", event.getMaxPlayers());
        
        
        messagingTemplate.convertAndSend("/topic/tournaments/lobby", 
            TournamentMessage.of("TOURNAMENT_CREATED", event.getTournamentId(), data));
        
        
        broadcast(event.getTournamentId(), "TOURNAMENT_CREATED", data);
    }

    

    
    private void broadcast(UUID tournamentId, String type, Map<String, Object> data) {
        TournamentMessage message = TournamentMessage.of(type, tournamentId, data);
        String destination = TOURNAMENT_TOPIC + tournamentId;
        
        log.debug("Broadcasting {} to {}", type, destination);
        messagingTemplate.convertAndSend(destination, message);
    }

    
    public void sendToUser(String username, UUID tournamentId, String type, Map<String, Object> data) {
        TournamentMessage message = TournamentMessage.of(type, tournamentId, data);
        messagingTemplate.convertAndSendToUser(username, "/queue/tournament", message);
        log.debug("Sent {} to user {} for tournament {}", type, username, tournamentId);
    }

    
    public void broadcastError(UUID tournamentId, String errorMessage) {
        Map<String, Object> data = Map.of("message", errorMessage);
        broadcast(tournamentId, "ERROR", data);
    }
}
