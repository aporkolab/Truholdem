package com.truholdem.dto;

import com.truholdem.model.PlayerAction;

import java.util.List;
import java.util.Map;


public record GTORecommendationDto(
    
    PlayerAction primaryAction,
    
    
    int recommendedSize,
    
    
    Map<PlayerAction, Double> mixedStrategy,
    
    
    HandStrengthCategory handCategory,
    
    
    String reasoning,
    
    
    double expectedValue,
    
    
    double confidence,
    
    
    List<ActionAlternative> alternatives,
    
    
    List<String> exploitativeAdjustments
) {
    public enum HandStrengthCategory {
        
        VALUE_HEAVY("Strong Value", "Bet/raise for value. Extract maximum chips."),
        
        
        MEDIUM_STRENGTH("Medium Strength", "Mostly calling/checking. Pot control."),
        
        
        DRAWING("Drawing Hand", "Consider equity, implied odds. Semi-bluff if appropriate."),
        
        
        BLUFF_CANDIDATE("Bluff Candidate", "Good bluffing hand. Bet with appropriate frequency."),
        
        
        TRASH("No Value", "Fold to aggression. Don't bluff without equity.");
        
        private final String displayName;
        private final String defaultStrategy;
        
        HandStrengthCategory(String displayName, String defaultStrategy) {
            this.displayName = displayName;
            this.defaultStrategy = defaultStrategy;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDefaultStrategy() {
            return defaultStrategy;
        }
    }
    
    
    public record ActionAlternative(
        PlayerAction action,
        int size,
        double expectedValue,
        String reasoning
    ) implements Comparable<ActionAlternative> {
        @Override
        public int compareTo(ActionAlternative other) {
            return Double.compare(other.expectedValue, this.expectedValue);
        }
    }
    
    
    public static GTORecommendationDto valueBet(int betSize, double equity, double ev, int potSize) {
        String reasoning = String.format(
            "Strong hand (%.0f%% equity). Bet %.0f%% pot for value.",
            equity * 100, (betSize * 100.0 / potSize)
        );
        
        return new GTORecommendationDto(
            PlayerAction.BET,
            betSize,
            Map.of(PlayerAction.BET, 0.85, PlayerAction.CHECK, 0.15),
            HandStrengthCategory.VALUE_HEAVY,
            reasoning,
            ev,
            0.85,
            List.of(
                new ActionAlternative(PlayerAction.CHECK, 0, ev * 0.7, "Trapping"),
                new ActionAlternative(PlayerAction.FOLD, 0, 0, "Never fold")
            ),
            List.of()
        );
    }
    
    
    public static GTORecommendationDto valueRaise(int raiseSize, double equity, double ev, int currentBet) {
        String reasoning = String.format(
            "Strong hand (%.0f%% equity). Raise to %.0fx for value.",
            equity * 100, (raiseSize / (double) currentBet)
        );
        
        return new GTORecommendationDto(
            PlayerAction.RAISE,
            raiseSize,
            Map.of(PlayerAction.RAISE, 0.80, PlayerAction.CALL, 0.20),
            HandStrengthCategory.VALUE_HEAVY,
            reasoning,
            ev,
            0.80,
            List.of(
                new ActionAlternative(PlayerAction.CALL, 0, ev * 0.8, "Slow play/trap"),
                new ActionAlternative(PlayerAction.FOLD, 0, 0, "Never fold")
            ),
            List.of()
        );
    }
    
    
    public static GTORecommendationDto call(double equity, double ev, double requiredEquity) {
        String reasoning = String.format(
            "Have %.0f%% equity, need %.0f%% to call. %s profitable call.",
            equity * 100, requiredEquity * 100, ev >= 0 ? "Marginally" : "NOT a"
        );
        
        return new GTORecommendationDto(
            PlayerAction.CALL,
            0,
            Map.of(PlayerAction.CALL, ev >= 0 ? 0.75 : 0.40, 
                   PlayerAction.FOLD, ev >= 0 ? 0.25 : 0.60),
            HandStrengthCategory.MEDIUM_STRENGTH,
            reasoning,
            ev,
            ev >= 0 ? 0.70 : 0.50,
            List.of(
                new ActionAlternative(PlayerAction.RAISE, 0, ev * 0.6, "Semi-bluff if applicable"),
                new ActionAlternative(PlayerAction.FOLD, 0, 0, "Give up pot equity")
            ),
            List.of("Tighten against aggressive opponents", 
                    "Loosen against passive opponents")
        );
    }
    
    
    public static GTORecommendationDto bluff(int betSize, double foldEquity, double ev, int potSize) {
        String reasoning = String.format(
            "Low showdown value. Need %.0f%% fold equity, expect ~%.0f%%. Bluff %.0f%% pot.",
            (1 - foldEquity) * 100, foldEquity * 100, (betSize * 100.0 / potSize)
        );
        
        return new GTORecommendationDto(
            PlayerAction.BET,
            betSize,
            Map.of(PlayerAction.BET, 0.35, PlayerAction.CHECK, 0.65),
            HandStrengthCategory.BLUFF_CANDIDATE,
            reasoning,
            ev,
            0.55,
            List.of(
                new ActionAlternative(PlayerAction.CHECK, 0, 0, "Give up, no equity"),
                new ActionAlternative(PlayerAction.FOLD, 0, 0, "N/A - no bet to face")
            ),
            List.of("Increase bluff frequency vs tight players",
                    "Avoid bluffing calling stations")
        );
    }
    
    
    public static GTORecommendationDto fold(double equity, double requiredEquity) {
        String reasoning = String.format(
            "Only %.0f%% equity, need %.0f%% to continue. Clear fold.",
            equity * 100, requiredEquity * 100
        );
        
        return new GTORecommendationDto(
            PlayerAction.FOLD,
            0,
            Map.of(PlayerAction.FOLD, 0.90, PlayerAction.CALL, 0.10),
            HandStrengthCategory.TRASH,
            reasoning,
            0,
            0.85,
            List.of(
                new ActionAlternative(PlayerAction.CALL, 0, -10, "Burning money"),
                new ActionAlternative(PlayerAction.RAISE, 0, -20, "Bluffing into strength")
            ),
            List.of()
        );
    }
    
    
    public static GTORecommendationDto check(double equity, String reason) {
        return new GTORecommendationDto(
            PlayerAction.CHECK,
            0,
            Map.of(PlayerAction.CHECK, 0.70, PlayerAction.BET, 0.30),
            HandStrengthCategory.MEDIUM_STRENGTH,
            reason,
            0,
            0.65,
            List.of(
                new ActionAlternative(PlayerAction.BET, 0, 0, "Thin value/protection")
            ),
            List.of("Bet more vs players who check back weak hands",
                    "Check more vs aggressive players")
        );
    }
    
    
    public String primaryActionDisplay() {
        if (recommendedSize > 0) {
            return String.format("%s %d", primaryAction, recommendedSize);
        }
        return primaryAction.toString();
    }
    
    
    public String mixedStrategyDisplay() {
        StringBuilder sb = new StringBuilder();
        mixedStrategy.entrySet().stream()
            .filter(e -> e.getValue() > 0.05)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                if (sb.length() > 0) sb.append(", ");
                sb.append(String.format("%s %.0f%%", e.getKey(), e.getValue() * 100));
            });
        return sb.toString();
    }
}
