package com.truholdem.model;


public enum TournamentStatus {
    
    
    REGISTERING,
    
    
    LATE_REGISTRATION,
    
    
    RUNNING,
    
    
    PAUSED,
    
    
    FINAL_TABLE,
    
    
    HEADS_UP,
    
    
    COMPLETED,
    
    
    CANCELLED;
    
    
    public boolean isPlayable() {
        return this == RUNNING || this == LATE_REGISTRATION || 
               this == FINAL_TABLE || this == HEADS_UP;
    }
    
    
    public boolean allowsRegistration() {
        return this == REGISTERING || this == LATE_REGISTRATION;
    }
    
    
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
