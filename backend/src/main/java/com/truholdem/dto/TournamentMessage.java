package com.truholdem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentMessage(
    String type,
    UUID tournamentId,
    Map<String, Object> data,
    Instant timestamp
) {
    
    public TournamentMessage(String type, UUID tournamentId, Map<String, Object> data) {
        this(type, tournamentId, data, Instant.now());
    }
    
    
    public static TournamentMessage of(String type, UUID tournamentId) {
        return new TournamentMessage(type, tournamentId, Map.of());
    }
    
    
    public static TournamentMessage of(String type, UUID tournamentId, Map<String, Object> data) {
        return new TournamentMessage(type, tournamentId, data);
    }
}
