package com.truholdem.domain.event;

import java.util.UUID;


public final class TournamentPlayerRegistered extends TournamentEvent {
    
    private final UUID playerId;
    private final String playerName;
    private final int currentRegistrations;
    private final int maxPlayers;

    public TournamentPlayerRegistered(UUID tournamentId, UUID playerId, 
                                       String playerName, int currentRegistrations,
                                       int maxPlayers) {
        super(tournamentId);
        this.playerId = playerId;
        this.playerName = playerName;
        this.currentRegistrations = currentRegistrations;
        this.maxPlayers = maxPlayers;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getCurrentRegistrations() {
        return currentRegistrations;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public boolean isTournamentFull() {
        return currentRegistrations >= maxPlayers;
    }
}
