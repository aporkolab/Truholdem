package com.truholdem.domain.event;

import com.truholdem.model.TournamentType;

import java.util.UUID;


public final class TournamentCreated extends TournamentEvent {
    
    private final String tournamentName;
    private final TournamentType tournamentType;
    private final int buyIn;
    private final int startingChips;
    private final int maxPlayers;

    public TournamentCreated(UUID tournamentId, String tournamentName, 
                             TournamentType tournamentType, int buyIn, 
                             int startingChips, int maxPlayers) {
        super(tournamentId);
        this.tournamentName = tournamentName;
        this.tournamentType = tournamentType;
        this.buyIn = buyIn;
        this.startingChips = startingChips;
        this.maxPlayers = maxPlayers;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public TournamentType getTournamentType() {
        return tournamentType;
    }

    public int getBuyIn() {
        return buyIn;
    }

    public int getStartingChips() {
        return startingChips;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
