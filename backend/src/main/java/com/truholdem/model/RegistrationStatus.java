package com.truholdem.model;


public enum RegistrationStatus {
    
    
    REGISTERED,
    
    
    PLAYING,
    
    
    ELIMINATED,
    
    
    FINISHED,
    
    
    WITHDRAWN;
    
    
    public boolean canPlay() {
        return this == REGISTERED || this == PLAYING;
    }
    
    
    public boolean isTerminal() {
        return this == ELIMINATED || this == FINISHED || this == WITHDRAWN;
    }
}
