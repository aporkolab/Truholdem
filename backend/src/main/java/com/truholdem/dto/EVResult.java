package com.truholdem.dto;

import com.truholdem.model.PlayerAction;


public record EVResult(
    
    PlayerAction action,
    
    
    double expectedValue,
    
    
    double evPercentOfPot,
    
    
    double breakEvenEquity,
    
    
    double actualEquity,
    
    
    double equityDelta,
    
    
    int amountRequired,
    
    
    int potSize,
    
    
    boolean isProfitable,
    
    
    String explanation
) {
    
    public static EVResult forFold(int potSize, double actualEquity) {
        return new EVResult(
            PlayerAction.FOLD,
            0,
            0,
            0,
            actualEquity,
            actualEquity, 
            0,
            potSize,
            true, 
            "Folding costs nothing but surrenders pot equity"
        );
    }
    
    
    public static EVResult forCall(int potSize, int callAmount, double equity) {
        int totalPot = potSize + callAmount;
        double ev = (equity * totalPot) - callAmount;
        double evPercent = (ev / potSize) * 100;
        
        
        double breakEven = (double) callAmount / totalPot;
        double delta = equity - breakEven;
        
        String explanation = String.format(
            "Call %d into %d pot. Need %.1f%% equity to break even. We have %.1f%% → %s EV",
            callAmount, potSize, breakEven * 100, equity * 100, ev >= 0 ? "+" : ""
        );
        
        return new EVResult(
            PlayerAction.CALL,
            ev,
            evPercent,
            breakEven,
            equity,
            delta,
            callAmount,
            potSize,
            ev >= 0,
            explanation
        );
    }
    
    
    public static EVResult forBetOrRaise(PlayerAction action, int potSize, int betAmount, 
                                         double equity, double foldEquity) {
        
        double foldEV = foldEquity * potSize;
        
        
        int newPot = potSize + betAmount;
        double callByVillainEV = (equity * (newPot + betAmount)) - betAmount;
        double callWeight = 1 - foldEquity;
        
        double totalEV = foldEV + (callWeight * callByVillainEV);
        double evPercent = (totalEV / potSize) * 100;
        
        
        
        double requiredShowdownEquity = (foldEquity > 0) ? 
            ((double) betAmount / (newPot + betAmount)) * (1 - foldEquity) : 
            (double) betAmount / (newPot + betAmount);
        
        String explanation = String.format(
            "%s %d. Fold equity: %.0f%%. If called, need %.1f%% equity (have %.1f%%). Total EV: %+.1f",
            action == PlayerAction.BET ? "Bet" : "Raise",
            betAmount, foldEquity * 100, requiredShowdownEquity * 100, equity * 100, totalEV
        );
        
        return new EVResult(
            action,
            totalEV,
            evPercent,
            requiredShowdownEquity,
            equity,
            equity - requiredShowdownEquity,
            betAmount,
            potSize,
            totalEV >= 0,
            explanation
        );
    }
    
    
    public int compareToAction(EVResult other) {
        return Double.compare(this.expectedValue, other.expectedValue);
    }
    
    
    public String formattedEV() {
        return String.format("%+.1f chips (%.1f%% of pot)", 
            expectedValue, evPercentOfPot);
    }
    
    
    public String getRecommendationStrength() {
        if (evPercentOfPot > 30) return "STRONGLY RECOMMENDED";
        if (evPercentOfPot > 15) return "RECOMMENDED";
        if (evPercentOfPot > 5) return "SLIGHTLY +EV";
        if (evPercentOfPot > -5) return "NEUTRAL";
        if (evPercentOfPot > -15) return "SLIGHTLY -EV";
        if (evPercentOfPot > -30) return "NOT RECOMMENDED";
        return "STRONGLY NOT RECOMMENDED";
    }
}
