package com.truholdem.domain.event;

import java.util.UUID;


public final class TournamentPlayerEliminated extends TournamentEvent {
    
    private final UUID playerId;
    private final String playerName;
    private final int finishPosition;
    private final int prizeWon;
    private final int playersRemaining;
    private final UUID eliminatedBy; 

    public TournamentPlayerEliminated(UUID tournamentId, UUID playerId, 
                                       String playerName, int finishPosition,
                                       int prizeWon, int playersRemaining,
                                       UUID eliminatedBy) {
        super(tournamentId);
        this.playerId = playerId;
        this.playerName = playerName;
        this.finishPosition = finishPosition;
        this.prizeWon = prizeWon;
        this.playersRemaining = playersRemaining;
        this.eliminatedBy = eliminatedBy;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getFinishPosition() {
        return finishPosition;
    }

    public int getPrizeWon() {
        return prizeWon;
    }

    public int getPlayersRemaining() {
        return playersRemaining;
    }

    public UUID getEliminatedBy() {
        return eliminatedBy;
    }
    
    public boolean isInTheMoney() {
        return prizeWon > 0;
    }
}
