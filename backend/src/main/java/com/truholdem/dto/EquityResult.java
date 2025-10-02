package com.truholdem.dto;

import java.util.Map;


public record EquityResult(
    
    double winProbability,
    
    
    double tieProbability,
    
    
    double loseProbability,
    
    
    double equity,
    
    
    int simulationCount,
    
    
    Map<String, Double> handTypeBreakdown,
    
    
    double confidenceLow,
    
    
    double confidenceHigh
) {
    
    public static EquityResult simple(double win, double tie, double lose, int simulations) {
        double equity = win + (tie / 2);
        
        double se = Math.sqrt((equity * (1 - equity)) / simulations);
        double margin = 1.96 * se; 
        
        return new EquityResult(
            win,
            tie,
            lose,
            equity,
            simulations,
            Map.of(),
            Math.max(0, equity - margin),
            Math.min(1, equity + margin)
        );
    }
    
    
    public static EquityResult full(double win, double tie, double lose, 
                                    int simulations, Map<String, Double> handTypes) {
        double equity = win + (tie / 2);
        double se = Math.sqrt((equity * (1 - equity)) / simulations);
        double margin = 1.96 * se;
        
        return new EquityResult(
            win,
            tie,
            lose,
            equity,
            simulations,
            handTypes,
            Math.max(0, equity - margin),
            Math.min(1, equity + margin)
        );
    }
    
    
    public String equityPercentage() {
        return String.format("%.1f%%", equity * 100);
    }
    
    
    public boolean isFavorite() {
        return equity > 0.5;
    }
    
    
    public boolean isSignificantFavorite() {
        return equity > 0.65;
    }
    
    
    public boolean isCoinFlip() {
        return equity >= 0.45 && equity <= 0.55;
    }
    
    
    public String oddsFormat() {
        if (equity > 0.5) {
            double odds = equity / (1 - equity);
            return String.format("%.1f:1 favorite", odds);
        } else if (equity < 0.5) {
            double odds = (1 - equity) / equity;
            return String.format("%.1f:1 underdog", odds);
        }
        return "Even odds";
    }
}
