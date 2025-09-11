package com.truholdem.domain.event;

import java.util.UUID;


public final class TournamentLevelAdvanced extends TournamentEvent {
    
    private final int newLevel;
    private final int smallBlind;
    private final int bigBlind;
    private final int ante;
    private final int playersRemaining;

    public TournamentLevelAdvanced(UUID tournamentId, int newLevel, 
                                    int smallBlind, int bigBlind, int ante,
                                    int playersRemaining) {
        super(tournamentId);
        this.newLevel = newLevel;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.ante = ante;
        this.playersRemaining = playersRemaining;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    public int getAnte() {
        return ante;
    }

    public int getPlayersRemaining() {
        return playersRemaining;
    }
}
