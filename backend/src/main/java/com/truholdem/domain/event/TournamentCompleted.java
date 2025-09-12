package com.truholdem.domain.event;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


public final class TournamentCompleted extends TournamentEvent {
    
    private final UUID winnerId;
    private final String winnerName;
    private final int totalPrizePool;
    private final int totalPlayers;
    private final int finalLevel;
    private final Duration duration;
    private final List<FinishResult> topFinishers;

    public TournamentCompleted(UUID tournamentId, UUID winnerId, String winnerName,
                                int totalPrizePool, int totalPlayers, int finalLevel,
                                Duration duration, List<FinishResult> topFinishers) {
        super(tournamentId);
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.totalPrizePool = totalPrizePool;
        this.totalPlayers = totalPlayers;
        this.finalLevel = finalLevel;
        this.duration = duration;
        this.topFinishers = List.copyOf(topFinishers);
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public int getTotalPrizePool() {
        return totalPrizePool;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public int getFinalLevel() {
        return finalLevel;
    }

    public Duration getDuration() {
        return duration;
    }

    public List<FinishResult> getTopFinishers() {
        return topFinishers;
    }
    
    
    public record FinishResult(int position, UUID playerId, String playerName, int prizeWon) {}
}
