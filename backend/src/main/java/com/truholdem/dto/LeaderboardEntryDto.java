package com.truholdem.dto;

import com.truholdem.model.TournamentRegistration;
import com.truholdem.model.RegistrationStatus;

import java.util.UUID;


public record LeaderboardEntryDto(
    int rank,
    UUID playerId,
    String playerName,
    int chips,
    RegistrationStatus status,
    Integer finishPosition,
    Integer prizeWon,
    int rebuysUsed,
    int addOnsUsed,
    int bountiesCollected
) {
    
    public static LeaderboardEntryDto from(TournamentRegistration reg, int rank) {
        return new LeaderboardEntryDto(
            rank,
            reg.getPlayerId(),
            reg.getPlayerName(),
            reg.getCurrentChips(),
            reg.getStatus(),
            reg.getFinishPosition(),
            reg.getPrizeWon(),
            reg.getRebuysUsed(),
            reg.getAddOnsUsed(),
            reg.getBountiesCollected()
        );
    }
}
