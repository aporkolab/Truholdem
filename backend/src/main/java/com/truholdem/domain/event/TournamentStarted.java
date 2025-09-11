package com.truholdem.domain.event;

import java.util.UUID;


public final class TournamentStarted extends TournamentEvent {
    
    private final int playerCount;
    private final int tableCount;
    private final int prizePool;

    public TournamentStarted(UUID tournamentId, int playerCount, 
                             int tableCount, int prizePool) {
        super(tournamentId);
        this.playerCount = playerCount;
        this.tableCount = tableCount;
        this.prizePool = prizePool;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getTableCount() {
        return tableCount;
    }

    public int getPrizePool() {
        return prizePool;
    }
}
