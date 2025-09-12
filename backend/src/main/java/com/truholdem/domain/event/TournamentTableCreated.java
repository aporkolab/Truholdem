package com.truholdem.domain.event;

import java.util.List;
import java.util.UUID;


public final class TournamentTableCreated extends TournamentEvent {
    
    private final UUID tableId;
    private final int tableNumber;
    private final List<UUID> seatedPlayerIds;
    private final boolean isFinalTable;

    public TournamentTableCreated(UUID tournamentId, UUID tableId, 
                                   int tableNumber, List<UUID> seatedPlayerIds,
                                   boolean isFinalTable) {
        super(tournamentId);
        this.tableId = tableId;
        this.tableNumber = tableNumber;
        this.seatedPlayerIds = List.copyOf(seatedPlayerIds);
        this.isFinalTable = isFinalTable;
    }

    public UUID getTableId() {
        return tableId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public List<UUID> getSeatedPlayerIds() {
        return seatedPlayerIds;
    }

    public boolean isFinalTable() {
        return isFinalTable;
    }
    
    public int getPlayerCount() {
        return seatedPlayerIds.size();
    }
}
