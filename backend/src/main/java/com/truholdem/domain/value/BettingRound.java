package com.truholdem.domain.value;

import com.truholdem.model.GamePhase;

import java.util.Objects;
import java.util.UUID;


public record BettingRound(
    GamePhase phase,
    Chips currentBet,
    Chips minRaise,
    int actionsThisRound,
    UUID lastAggressor
) {

    
    public BettingRound {
        Objects.requireNonNull(phase, "Game phase cannot be null");
        Objects.requireNonNull(currentBet, "Current bet cannot be null");
        Objects.requireNonNull(minRaise, "Minimum raise cannot be null");
        if (actionsThisRound < 0) {
            throw new IllegalArgumentException("Actions this round cannot be negative: " + actionsThisRound);
        }
    }

    
    public static BettingRound start(GamePhase phase, Chips bigBlind) {
        Objects.requireNonNull(phase, "Game phase cannot be null");
        Objects.requireNonNull(bigBlind, "Big blind cannot be null");
        
        
        Chips initialBet = (phase == GamePhase.PRE_FLOP) ? bigBlind : Chips.zero();
        
        return new BettingRound(phase, initialBet, bigBlind, 0, null);
    }

    
    public static BettingRound startPostFlop(GamePhase phase, Chips bigBlind) {
        Objects.requireNonNull(phase, "Game phase cannot be null");
        Objects.requireNonNull(bigBlind, "Big blind cannot be null");
        
        return new BettingRound(phase, Chips.zero(), bigBlind, 0, null);
    }

    
    public BettingRound withBet(Chips bet, UUID playerId) {
        Objects.requireNonNull(bet, "Bet cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        
        UUID newAggressor = bet.isGreaterThan(currentBet) ? playerId : lastAggressor;
        Chips newCurrentBet = bet.max(currentBet);
        
        return new BettingRound(phase, newCurrentBet, minRaise, actionsThisRound + 1, newAggressor);
    }

    
    public BettingRound withRaise(Chips totalBetAmount, Chips raiseAmount, UUID playerId) {
        Objects.requireNonNull(totalBetAmount, "Total bet amount cannot be null");
        Objects.requireNonNull(raiseAmount, "Raise amount cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        
        Chips newMinRaise = raiseAmount.max(minRaise);
        
        return new BettingRound(phase, totalBetAmount, newMinRaise, actionsThisRound + 1, playerId);
    }

    
    public BettingRound withAction() {
        return new BettingRound(phase, currentBet, minRaise, actionsThisRound + 1, lastAggressor);
    }

    
    public boolean isComplete(int activePlayers, int playersWhoHaveActed, boolean allBetsMatched) {
        
        if (playersWhoHaveActed < activePlayers) {
            return false;
        }
        
        return allBetsMatched;
    }

    
    public boolean isComplete(int activePlayers, int actedPlayers) {
        return actedPlayers >= activePlayers && activePlayers > 0;
    }

    
    public Chips amountToCall(Chips playerCurrentBet) {
        Objects.requireNonNull(playerCurrentBet, "Player current bet cannot be null");
        return currentBet.subtractOrZero(playerCurrentBet);
    }

    
    public Chips minimumRaiseTotal() {
        return currentBet.add(minRaise);
    }

    
    public boolean isValidRaise(Chips raiseAmount) {
        Objects.requireNonNull(raiseAmount, "Raise amount cannot be null");
        return raiseAmount.isGreaterThanOrEqual(minRaise);
    }

    
    public boolean isValidRaiseTotal(Chips totalBet) {
        Objects.requireNonNull(totalBet, "Total bet cannot be null");
        return totalBet.isGreaterThanOrEqual(minimumRaiseTotal());
    }

    
    public boolean hasBettingAction() {
        return currentBet.hasChips();
    }

    
    public boolean isFirstAction() {
        return actionsThisRound == 0;
    }

    
    public boolean hasAggressor() {
        return lastAggressor != null;
    }

    @Override
    public String toString() {
        return String.format("BettingRound[%s: bet=%s, minRaise=%s, actions=%d, aggressor=%s]",
            phase, currentBet, minRaise, actionsThisRound, 
            lastAggressor != null ? lastAggressor.toString().substring(0, 8) : "none");
    }
}
