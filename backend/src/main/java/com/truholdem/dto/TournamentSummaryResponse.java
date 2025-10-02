package com.truholdem.dto;

import com.truholdem.model.Tournament;
import com.truholdem.model.TournamentStatus;
import com.truholdem.model.TournamentType;

import java.time.Instant;
import java.util.UUID;


public record TournamentSummaryResponse(
    UUID id,
    String name,
    TournamentType type,
    TournamentStatus status,
    int registeredPlayers,
    int maxPlayers,
    int buyIn,
    int prizePool,
    int currentLevel,
    Instant createdAt,
    Instant startTime
) {
    
    public static TournamentSummaryResponse from(Tournament tournament) {
        return new TournamentSummaryResponse(
            tournament.getId(),
            tournament.getName(),
            tournament.getTournamentType(),
            tournament.getStatus(),
            tournament.getRegistrations().size(),
            tournament.getMaxPlayers(),
            tournament.getBuyIn(),
            tournament.getPrizePool(),
            tournament.getCurrentLevel(),
            tournament.getCreatedAt(),
            tournament.getStartTime()
        );
    }
}
