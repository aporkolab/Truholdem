package com.truholdem.domain.event;

import com.truholdem.domain.value.Chips;

import java.util.Objects;
import java.util.UUID;


public final class PlayerEliminated extends DomainEvent {

    private final UUID playerId;
    private final String playerName;
    private final int finishPosition;
    private final Chips totalWinnings;
    private final int handsPlayed;
    private final UUID eliminatedByPlayerId;
    private final String eliminatedByPlayerName;

    
    public PlayerEliminated(UUID gameId, UUID playerId, String playerName,
                            int finishPosition, Chips totalWinnings, int handsPlayed,
                            UUID eliminatedByPlayerId, String eliminatedByPlayerName) {
        super(gameId);
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        this.finishPosition = finishPosition;
        this.totalWinnings = Objects.requireNonNull(totalWinnings, "Total winnings cannot be null");
        this.handsPlayed = handsPlayed;
        this.eliminatedByPlayerId = eliminatedByPlayerId;
        this.eliminatedByPlayerName = eliminatedByPlayerName;
    }

    
    public PlayerEliminated(UUID gameId, UUID playerId, String playerName,
                            int finishPosition, Chips totalWinnings, int handsPlayed) {
        this(gameId, playerId, playerName, finishPosition, totalWinnings, handsPlayed, null, null);
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

    public Chips getTotalWinnings() {
        return totalWinnings;
    }

    public int getHandsPlayed() {
        return handsPlayed;
    }

    public UUID getEliminatedByPlayerId() {
        return eliminatedByPlayerId;
    }

    public String getEliminatedByPlayerName() {
        return eliminatedByPlayerName;
    }

    
    public boolean hasEliminator() {
        return eliminatedByPlayerId != null;
    }

    
    public boolean isInTheMoney(int payingPositions) {
        return finishPosition <= payingPositions;
    }

    
    public boolean wasRunnerUp() {
        return finishPosition == 2;
    }

    
    public String getPositionDisplay() {
        return switch (finishPosition) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> finishPosition + "th";
        };
    }

    @Override
    public String toString() {
        String eliminatorPart = hasEliminator() 
                ? String.format(" by %s", eliminatedByPlayerName)
                : "";
        return String.format("PlayerEliminated[%s finished %s%s, played %d hands, won %s total]",
                playerName,
                getPositionDisplay(),
                eliminatorPart,
                handsPlayed,
                totalWinnings);
    }
}
