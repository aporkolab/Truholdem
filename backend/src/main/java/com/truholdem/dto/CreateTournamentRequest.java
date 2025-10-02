package com.truholdem.dto;

import com.truholdem.model.TournamentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;


public record CreateTournamentRequest(
    
    @NotBlank(message = "Tournament name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    String name,
    
    @NotNull(message = "Tournament type is required")
    TournamentType type,
    
    @Min(value = 100, message = "Starting chips must be at least 100")
    @Max(value = 100000, message = "Starting chips cannot exceed 100,000")
    int startingChips,
    
    @Min(value = 2, message = "Minimum players must be at least 2")
    int minPlayers,
    
    @Min(value = 2, message = "Maximum players must be at least 2")
    @Max(value = 1000, message = "Maximum players cannot exceed 1,000")
    int maxPlayers,
    
    @Min(value = 0, message = "Buy-in cannot be negative")
    int buyIn,
    
    
    String blindStructureType,
    
    
    Integer levelDurationMinutes,
    
    
    Integer rebuyAmount,
    Integer rebuyDeadlineLevel,
    Integer maxRebuys,
    
    
    Integer addOnAmount,
    
    
    Integer bountyAmount,
    
    
    List<Integer> payoutStructure
) {
    
    
    public static CreateTournamentRequest sitAndGo(String name, int buyIn) {
        return new CreateTournamentRequest(
            name,
            TournamentType.SIT_AND_GO,
            1500,
            6,
            9,
            buyIn,
            "TURBO",
            null, null, null, null, null, null, null
        );
    }
    
    
    public static CreateTournamentRequest freezeout(String name, int buyIn, int maxPlayers) {
        return new CreateTournamentRequest(
            name,
            TournamentType.FREEZEOUT,
            1500,
            Math.min(10, maxPlayers),
            maxPlayers,
            buyIn,
            "STANDARD",
            null, null, null, null, null, null, null
        );
    }
    
    
    public static CreateTournamentRequest rebuy(String name, int buyIn, int maxPlayers) {
        return new CreateTournamentRequest(
            name,
            TournamentType.REBUY,
            1500,
            Math.min(10, maxPlayers),
            maxPlayers,
            buyIn,
            "STANDARD",
            null,
            buyIn,     
            6,         
            3,         
            buyIn * 2, 
            null,
            null
        );
    }
    
    
    public static CreateTournamentRequest bounty(String name, int buyIn, int bountyAmount, int maxPlayers) {
        return new CreateTournamentRequest(
            name,
            TournamentType.BOUNTY,
            1500,
            Math.min(10, maxPlayers),
            maxPlayers,
            buyIn,
            "STANDARD",
            null, null, null, null, null,
            bountyAmount,
            null
        );
    }
}
