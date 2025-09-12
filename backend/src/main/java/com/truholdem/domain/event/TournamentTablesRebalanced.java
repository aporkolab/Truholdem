package com.truholdem.domain.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public final class TournamentTablesRebalanced extends TournamentEvent {
    
    private final int activeTableCount;
    private final List<PlayerMove> playerMoves;
    private final List<UUID> closedTableIds;
    private final boolean finalTableFormed;

    public TournamentTablesRebalanced(UUID tournamentId, int activeTableCount,
                                       List<PlayerMove> playerMoves,
                                       List<UUID> closedTableIds,
                                       boolean finalTableFormed) {
        super(tournamentId);
        this.activeTableCount = activeTableCount;
        this.playerMoves = List.copyOf(playerMoves);
        this.closedTableIds = List.copyOf(closedTableIds);
        this.finalTableFormed = finalTableFormed;
    }

    public int getActiveTableCount() {
        return activeTableCount;
    }

    public List<PlayerMove> getPlayerMoves() {
        return playerMoves;
    }

    public List<UUID> getClosedTableIds() {
        return closedTableIds;
    }

    public boolean isFinalTableFormed() {
        return finalTableFormed;
    }
    
    
    public record PlayerMove(UUID playerId, UUID fromTableId, UUID toTableId) {}
}
