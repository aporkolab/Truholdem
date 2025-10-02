package com.truholdem.dto;

import com.truholdem.model.GamePhase;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public record HandAnalysisDto(
    
    UUID handHistoryId,
    
    
    String playerName,
    
    
    String handSummary,
    
    
    OverallAssessment overallAssessment,
    
    
    double evDifference,
    
    
    Map<GamePhase, StreetAnalysis> streetAnalyses,
    
    
    List<DecisionAnalysis> keyDecisions,
    
    
    List<String> suggestions,
    
    
    List<String> studyTopics
) {
    public enum OverallAssessment {
        OPTIMAL("Optimal Play", "All decisions were GTO-correct or better"),
        GOOD("Good Play", "Minor mistakes, no significant EV loss"),
        MIXED("Mixed Play", "Some good decisions, some mistakes"),
        POOR("Poor Play", "Multiple significant mistakes"),
        COSTLY("Costly Mistakes", "Major errors, significant EV lost");
        
        private final String label;
        private final String description;
        
        OverallAssessment(String label, String description) {
            this.label = label;
            this.description = description;
        }
        
        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }
    
    
    public record StreetAnalysis(
        GamePhase phase,
        double equityAtStart,
        double equityAtEnd,
        int potAtStart,
        int potAtEnd,
        String boardTexture, 
        List<ActionAnalysis> actions,
        String streetSummary
    ) {
        public double equityChange() {
            return equityAtEnd - equityAtStart;
        }
        
        public String getEquityChangeDisplay() {
            double change = equityChange();
            return String.format("%+.1f%%", change * 100);
        }
    }
    
    
    public record ActionAnalysis(
        String action,
        int amount,
        double equityAtTime,
        double actualEV,
        double optimalEV,
        String optimalAction,
        ActionAssessment assessment,
        String reasoning
    ) {
        public double evLost() {
            return optimalEV - actualEV;
        }
        
        public boolean wasOptimal() {
            return assessment == ActionAssessment.OPTIMAL || 
                   assessment == ActionAssessment.GOOD;
        }
    }
    
    public enum ActionAssessment {
        OPTIMAL("✓ Optimal", 0),
        GOOD("○ Good", 1),
        ACCEPTABLE("~ Acceptable", 2),
        QUESTIONABLE("? Questionable", 3),
        MISTAKE("✗ Mistake", 4),
        BLUNDER("✗✗ Blunder", 5);
        
        private final String symbol;
        private final int severity;
        
        ActionAssessment(String symbol, int severity) {
            this.symbol = symbol;
            this.severity = severity;
        }
        
        public String getSymbol() { return symbol; }
        public int getSeverity() { return severity; }
        
        public static ActionAssessment fromEVLoss(double evLoss, int potSize) {
            double evLossPercent = (evLoss / potSize) * 100;
            if (evLossPercent < 1) return OPTIMAL;
            if (evLossPercent < 3) return GOOD;
            if (evLossPercent < 8) return ACCEPTABLE;
            if (evLossPercent < 15) return QUESTIONABLE;
            if (evLossPercent < 30) return MISTAKE;
            return BLUNDER;
        }
    }
    
    
    public record DecisionAnalysis(
        GamePhase phase,
        String situation,
        String actualAction,
        String optimalAction,
        double evDifference,
        EquityResult equityAtDecision,
        List<EVResult> actionEVs,
        GTORecommendationDto gtoRecommendation,
        String detailedExplanation
    ) {}
    
    
    public static class Builder {
        private UUID handHistoryId;
        private String playerName;
        private String handSummary;
        private OverallAssessment overallAssessment;
        private double evDifference;
        private Map<GamePhase, StreetAnalysis> streetAnalyses;
        private List<DecisionAnalysis> keyDecisions;
        private List<String> suggestions;
        private List<String> studyTopics;
        
        public Builder handHistoryId(UUID id) { this.handHistoryId = id; return this; }
        public Builder playerName(String name) { this.playerName = name; return this; }
        public Builder handSummary(String summary) { this.handSummary = summary; return this; }
        public Builder overallAssessment(OverallAssessment assessment) { 
            this.overallAssessment = assessment; return this; 
        }
        public Builder evDifference(double ev) { this.evDifference = ev; return this; }
        public Builder streetAnalyses(Map<GamePhase, StreetAnalysis> analyses) { 
            this.streetAnalyses = analyses; return this; 
        }
        public Builder keyDecisions(List<DecisionAnalysis> decisions) { 
            this.keyDecisions = decisions; return this; 
        }
        public Builder suggestions(List<String> sug) { this.suggestions = sug; return this; }
        public Builder studyTopics(List<String> topics) { this.studyTopics = topics; return this; }
        
        public HandAnalysisDto build() {
            return new HandAnalysisDto(
                handHistoryId, playerName, handSummary, overallAssessment,
                evDifference, streetAnalyses, keyDecisions, suggestions, studyTopics
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    
    public int getMistakeCount() {
        return (int) keyDecisions.stream()
            .filter(d -> d.evDifference() > 5) 
            .count();
    }
    
    
    public int getBlunderCount() {
        return (int) keyDecisions.stream()
            .filter(d -> d.evDifference() > 20) 
            .count();
    }
    
    
    public String getEvDifferenceDisplay() {
        return String.format("%+.1f chips", evDifference);
    }
    
    
    public String getQuickSummary() {
        return String.format(
            "%s: %s. EV: %s. Mistakes: %d. Key lesson: %s",
            playerName,
            overallAssessment.getLabel(),
            getEvDifferenceDisplay(),
            getMistakeCount(),
            suggestions.isEmpty() ? "Keep up the good work!" : suggestions.get(0)
        );
    }
}
