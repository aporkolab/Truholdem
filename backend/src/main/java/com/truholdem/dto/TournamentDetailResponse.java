package com.truholdem.dto;

import com.truholdem.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record TournamentDetailResponse(
    UUID id,
    String name,
    TournamentType type,
    TournamentStatus status,
    
    
    int registeredPlayers,
    int playersRemaining,
    int minPlayers,
    int maxPlayers,
    
    
    int currentLevel,
    BlindLevelInfo currentBlinds,
    BlindLevelInfo nextBlinds,
    long secondsToNextLevel,
    
    
    int startingChips,
    int averageStack,
    int chipLeaderStack,
    String chipLeaderName,
    
    
    int buyIn,
    int prizePool,
    List<Integer> payoutStructure,
    int paidPositions,
    
    
    int tableCount,
    List<TableSummary> tables,
    
    
    Instant createdAt,
    Instant startTime,
    String duration
) {
    
    public static TournamentDetailResponse from(Tournament tournament) {
        BlindLevel current = tournament.getCurrentBlindLevel();
        BlindLevel next = tournament.getBlindStructure().getLevelAt(tournament.getCurrentLevel() + 1);
        
        long secondsToNext = 0;
        if (tournament.getLevelStartTime() != null) {
            long elapsed = Duration.between(tournament.getLevelStartTime(), Instant.now()).toSeconds();
            long levelDuration = tournament.getBlindStructure().getLevelDurationMinutes() * 60L;
            secondsToNext = Math.max(0, levelDuration - elapsed);
        }
        
        String chipLeaderName = null;
        int chipLeaderStack = 0;
        var chipLeader = tournament.getChipLeader();
        if (chipLeader.isPresent()) {
            chipLeaderName = chipLeader.get().getPlayerName();
            chipLeaderStack = chipLeader.get().getCurrentChips();
        }
        
        String durationStr = null;
        if (tournament.getStartTime() != null) {
            Duration d = Duration.between(tournament.getStartTime(), Instant.now());
            durationStr = String.format("%d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
        }
        
        List<TableSummary> tableSummaries = tournament.getActiveTables().stream()
            .map(TableSummary::from)
            .toList();
        
        return new TournamentDetailResponse(
            tournament.getId(),
            tournament.getName(),
            tournament.getTournamentType(),
            tournament.getStatus(),
            tournament.getRegistrations().size(),
            tournament.getPlayersRemaining(),
            tournament.getMinPlayers(),
            tournament.getMaxPlayers(),
            tournament.getCurrentLevel(),
            BlindLevelInfo.from(current, tournament.getCurrentLevel()),
            BlindLevelInfo.from(next, tournament.getCurrentLevel() + 1),
            secondsToNext,
            tournament.getStartingChips(),
            tournament.getAverageStack(),
            chipLeaderStack,
            chipLeaderName,
            tournament.getBuyIn(),
            tournament.getPrizePool(),
            tournament.getPayoutStructure(),
            tournament.getPaidPositions(),
            tournament.getActiveTables().size(),
            tableSummaries,
            tournament.getCreatedAt(),
            tournament.getStartTime(),
            durationStr
        );
    }
    
    
    public record BlindLevelInfo(
        int level,
        int smallBlind,
        int bigBlind,
        int ante
    ) {
        public static BlindLevelInfo from(BlindLevel level, int levelNumber) {
            return new BlindLevelInfo(
                levelNumber,
                level.getSmallBlind(),
                level.getBigBlind(),
                level.getAnte()
            );
        }
    }
    
    
    public record TableSummary(
        UUID id,
        int tableNumber,
        int playerCount,
        boolean isFinalTable
    ) {
        public static TableSummary from(TournamentTable table) {
            return new TableSummary(
                table.getId(),
                table.getTableNumber(),
                table.getPlayerCount(),
                table.isFinalTable()
            );
        }
    }
}
