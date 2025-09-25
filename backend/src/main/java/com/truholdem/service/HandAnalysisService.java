package com.truholdem.service;

import com.truholdem.domain.value.Position;
import com.truholdem.dto.*;
import com.truholdem.dto.GTORecommendationDto.ActionAlternative;
import com.truholdem.dto.GTORecommendationDto.HandStrengthCategory;
import com.truholdem.dto.HandAnalysisDto.*;
import com.truholdem.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service
public class HandAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(HandAnalysisService.class);


    private static final int DEFAULT_MONTE_CARLO_ITERATIONS = 10000;
    private static final int QUICK_MONTE_CARLO_ITERATIONS = 2000;
    private static final int PRECISE_MONTE_CARLO_ITERATIONS = 50000;


    private static final double SMALL_BET_POT_RATIO = 0.33;
    private static final double MEDIUM_BET_POT_RATIO = 0.66;
    private static final double LARGE_BET_POT_RATIO = 1.0;


    private static final double VALUE_BET_THRESHOLD = 0.65;
    private static final double BLUFF_THRESHOLD = 0.30;
    private static final double CALL_THRESHOLD_MODIFIER = 0.05;

    private final HandEvaluator handEvaluator;
    private final Random random = new Random();
    private final ExecutorService executorService;

    public HandAnalysisService(HandEvaluator handEvaluator) {
        this.handEvaluator = handEvaluator;

        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }




    public EquityResult calculateEquity(List<Card> heroHand, List<Card> communityCards,
                                        HandRange villainRange, int numOpponents) {
        return calculateEquity(heroHand, communityCards, villainRange,
                              numOpponents, DEFAULT_MONTE_CARLO_ITERATIONS);
    }


    public EquityResult calculateEquity(List<Card> heroHand, List<Card> communityCards,
                                        HandRange villainRange, int numOpponents,
                                        int iterations) {
        if (heroHand == null || heroHand.size() != 2) {
            throw new IllegalArgumentException("Hero must have exactly 2 hole cards");
        }


        Set<Card> deadCards = new HashSet<>(heroHand);
        if (communityCards != null) {
            deadCards.addAll(communityCards);
        }


        List<List<Card>> villainHands = villainRange.generateHands(deadCards);
        if (villainHands.isEmpty()) {
            logger.warn("No valid villain hands in range after removing dead cards");
            return EquityResult.simple(1.0, 0.0, 0.0, 0);
        }


        int wins = 0;
        int ties = 0;
        int losses = 0;
        Map<String, Integer> handTypeCounts = new ConcurrentHashMap<>();

        List<Card> communityCardsSafe = communityCards != null ?
            new ArrayList<>(communityCards) : new ArrayList<>();


        List<Card> remainingDeck = createRemainingDeck(deadCards);

        for (int i = 0; i < iterations; i++) {

            Collections.shuffle(remainingDeck, random);


            List<Card> fullBoard = new ArrayList<>(communityCardsSafe);
            int cardsNeeded = 5 - fullBoard.size();
            int deckIndex = 0;

            for (int j = 0; j < cardsNeeded && deckIndex < remainingDeck.size(); j++) {
                fullBoard.add(remainingDeck.get(deckIndex++));
            }


            List<Card> villainHand = villainHands.get(random.nextInt(villainHands.size()));


            Set<Card> usedCards = new HashSet<>(fullBoard);
            usedCards.addAll(heroHand);
            if (villainHand.stream().anyMatch(usedCards::contains)) {

                continue;
            }


            HandRanking heroRanking = handEvaluator.evaluate(heroHand, fullBoard);
            HandRanking villainRanking = handEvaluator.evaluate(villainHand, fullBoard);


            handTypeCounts.merge(heroRanking.getHandType().name(), 1, Integer::sum);


            int comparison = heroRanking.compareTo(villainRanking);
            if (comparison > 0) wins++;
            else if (comparison < 0) losses++;
            else ties++;
        }

        int totalSimulations = wins + ties + losses;
        if (totalSimulations == 0) {
            logger.warn("No valid simulations completed");
            return EquityResult.simple(0.5, 0.0, 0.5, 0);
        }


        double winProb = (double) wins / totalSimulations;
        double tieProb = (double) ties / totalSimulations;
        double loseProb = (double) losses / totalSimulations;


        Map<String, Double> handTypeBreakdown = new HashMap<>();
        for (Map.Entry<String, Integer> entry : handTypeCounts.entrySet()) {
            handTypeBreakdown.put(entry.getKey(),
                (double) entry.getValue() / totalSimulations);
        }

        logger.debug("Equity calculation: {} wins, {} ties, {} losses out of {} sims",
            wins, ties, losses, totalSimulations);

        return EquityResult.full(winProb, tieProb, loseProb, totalSimulations, handTypeBreakdown);
    }


    public EquityResult calculateEquityQuick(List<Card> heroHand, List<Card> communityCards,
                                             HandRange villainRange) {
        return calculateEquity(heroHand, communityCards, villainRange, 1, QUICK_MONTE_CARLO_ITERATIONS);
    }


    public EquityResult calculateEquityPrecise(List<Card> heroHand, List<Card> communityCards,
                                               HandRange villainRange) {
        return calculateEquity(heroHand, communityCards, villainRange, 1, PRECISE_MONTE_CARLO_ITERATIONS);
    }




    public Map<PlayerAction, EVResult> calculateAllEVs(List<Card> heroHand,
                                                        List<Card> communityCards,
                                                        int potSize, int betToCall,
                                                        HandRange villainRange) {

        EquityResult equity = calculateEquityQuick(heroHand, communityCards, villainRange);

        Map<PlayerAction, EVResult> results = new EnumMap<>(PlayerAction.class);


        results.put(PlayerAction.FOLD, EVResult.forFold(potSize, equity.equity()));


        if (betToCall > 0) {
            results.put(PlayerAction.CALL,
                EVResult.forCall(potSize, betToCall, equity.equity()));
        }


        if (betToCall == 0) {
            results.put(PlayerAction.CHECK,
                EVResult.forCall(potSize, 0, equity.equity()));
        }


        double foldEquity = estimateFoldEquity(heroHand, communityCards, potSize, betToCall);

        if (betToCall == 0) {

            int smallBet = (int) (potSize * SMALL_BET_POT_RATIO);
            int mediumBet = (int) (potSize * MEDIUM_BET_POT_RATIO);
            int largeBet = potSize;

            results.put(PlayerAction.BET,
                EVResult.forBetOrRaise(PlayerAction.BET, potSize, mediumBet,
                    equity.equity(), foldEquity));
        } else {

            int minRaise = betToCall * 2;
            int standardRaise = (int) (betToCall * 2.5);

            results.put(PlayerAction.RAISE,
                EVResult.forBetOrRaise(PlayerAction.RAISE, potSize, standardRaise,
                    equity.equity(), foldEquity));
        }

        return results;
    }


    public EVResult calculateEV(PlayerAction action, List<Card> heroHand,
                                List<Card> communityCards, int potSize,
                                int amount, HandRange villainRange) {
        EquityResult equity = calculateEquityQuick(heroHand, communityCards, villainRange);
        double foldEquity = estimateFoldEquity(heroHand, communityCards, potSize, amount);

        return switch (action) {
            case FOLD -> EVResult.forFold(potSize, equity.equity());
            case CALL, CHECK -> EVResult.forCall(potSize, amount, equity.equity());
            case BET -> EVResult.forBetOrRaise(PlayerAction.BET, potSize, amount,
                equity.equity(), foldEquity);
            case RAISE -> EVResult.forBetOrRaise(PlayerAction.RAISE, potSize, amount,
                equity.equity(), foldEquity);
            case ALL_IN -> EVResult.forBetOrRaise(PlayerAction.RAISE, potSize, amount,
                equity.equity(), foldEquity * 0.7);
        };
    }




    public GTORecommendationDto getGTORecommendation(List<Card> heroHand,
                                                   List<Card> communityCards,
                                                   int potSize, int betToCall,
                                                   Position position,
                                                   HandRange villainRange) {

        EquityResult equityResult = calculateEquityQuick(heroHand, communityCards, villainRange);
        double equity = equityResult.equity();


        double potOdds = betToCall > 0 ?
            (double) betToCall / (potSize + betToCall) : 0;


        HandStrengthCategory handCategory = categorizeHand(equity, heroHand, communityCards);


        Map<PlayerAction, EVResult> evs = calculateAllEVs(
            heroHand, communityCards, potSize, betToCall, villainRange);


        boolean inPosition = position.isInPosition();
        double positionBonus = inPosition ? 0.05 : -0.03;


        if (betToCall == 0) {
            return getUnfacingBetRecommendation(
                heroHand, communityCards, potSize, equity,
                handCategory, evs, inPosition
            );
        } else {
            return getFacingBetRecommendation(
                heroHand, communityCards, potSize, betToCall, equity,
                potOdds, handCategory, evs, inPosition
            );
        }
    }


    private GTORecommendationDto getUnfacingBetRecommendation(
            List<Card> heroHand, List<Card> communityCards, int potSize,
            double equity, HandStrengthCategory category,
            Map<PlayerAction, EVResult> evs, boolean inPosition) {

        EVResult betEV = evs.get(PlayerAction.BET);
        EVResult checkEV = evs.get(PlayerAction.CHECK);


        if (equity >= VALUE_BET_THRESHOLD) {
            int betSize = (int) (potSize * MEDIUM_BET_POT_RATIO);
            return GTORecommendationDto.valueBet(betSize, equity,
                betEV != null ? betEV.expectedValue() : 0, potSize);
        }


        if (equity >= 0.45 && equity < VALUE_BET_THRESHOLD) {
            if (inPosition) {

                int betSize = (int) (potSize * SMALL_BET_POT_RATIO);
                return GTORecommendationDto.valueBet(betSize, equity,
                    betEV != null ? betEV.expectedValue() : 0, potSize);
            }
            return GTORecommendationDto.check(equity,
                "Medium strength hand. Check for pot control out of position.");
        }


        if (equity < BLUFF_THRESHOLD && hasBluffingPotential(heroHand, communityCards)) {
            double foldEquity = estimateFoldEquity(heroHand, communityCards, potSize, 0);
            int betSize = (int) (potSize * MEDIUM_BET_POT_RATIO);
            return GTORecommendationDto.bluff(betSize, foldEquity,
                betEV != null ? betEV.expectedValue() : 0, potSize);
        }


        return GTORecommendationDto.check(equity, "Check with marginal holdings.");
    }


    private GTORecommendationDto getFacingBetRecommendation(
            List<Card> heroHand, List<Card> communityCards, int potSize,
            int betToCall, double equity, double potOdds,
            HandStrengthCategory category, Map<PlayerAction, EVResult> evs,
            boolean inPosition) {

        EVResult callEV = evs.get(PlayerAction.CALL);
        EVResult raiseEV = evs.get(PlayerAction.RAISE);
        EVResult foldEV = evs.get(PlayerAction.FOLD);


        if (equity >= 0.70) {
            int raiseSize = (int) (betToCall * 2.5);
            return GTORecommendationDto.valueRaise(raiseSize, equity,
                raiseEV != null ? raiseEV.expectedValue() : 0, betToCall);
        }


        if (equity > potOdds + CALL_THRESHOLD_MODIFIER) {
            return GTORecommendationDto.call(equity,
                callEV != null ? callEV.expectedValue() : 0, potOdds);
        }


        if (equity > potOdds - CALL_THRESHOLD_MODIFIER &&
            hasImpliedOdds(heroHand, communityCards)) {
            return GTORecommendationDto.call(equity,
                callEV != null ? callEV.expectedValue() : 0, potOdds);
        }


        return GTORecommendationDto.fold(equity, potOdds);
    }




    public HandAnalysisDto analyzeHand(HandHistory handHistory, String playerName) {
        logger.info("Analyzing hand {} for player {}",
            handHistory.getId(), playerName);


        HandHistory.HandHistoryPlayer playerData = handHistory.getPlayers().stream()
            .filter(p -> p.getPlayerName().equals(playerName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Player " + playerName + " not found in hand history"));

        List<Card> heroHand = reconstructHoleCards(playerData);


        Map<GamePhase, StreetAnalysis> streetAnalyses = new EnumMap<>(GamePhase.class);
        List<DecisionAnalysis> keyDecisions = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        double totalEvDiff = 0;


        Map<String, List<HandHistory.ActionRecord>> actionsByPhase =
            handHistory.getActions().stream()
                .collect(Collectors.groupingBy(HandHistory.ActionRecord::phase));


        List<Card> runningBoard = new ArrayList<>();
        int runningPot = handHistory.getSmallBlind() + handHistory.getBigBlind();

        for (GamePhase phase : List.of(GamePhase.PRE_FLOP, GamePhase.FLOP,
                                        GamePhase.TURN, GamePhase.RIVER)) {
            List<HandHistory.ActionRecord> streetActions =
                actionsByPhase.getOrDefault(phase.name(), List.of());


            if (phase == GamePhase.FLOP && handHistory.getBoard().size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    runningBoard.add(reconstructCard(handHistory.getBoard().get(i)));
                }
            } else if (phase == GamePhase.TURN && handHistory.getBoard().size() >= 4) {
                runningBoard.add(reconstructCard(handHistory.getBoard().get(3)));
            } else if (phase == GamePhase.RIVER && handHistory.getBoard().size() >= 5) {
                runningBoard.add(reconstructCard(handHistory.getBoard().get(4)));
            }


            HandRange villainRange = HandRange.buttonOpen();
            double startEquity = runningBoard.isEmpty() ?
                calculatePreFlopEquity(heroHand) :
                calculateEquityQuick(heroHand, runningBoard, villainRange).equity();


            List<ActionAnalysis> actionAnalyses = new ArrayList<>();
            for (HandHistory.ActionRecord action : streetActions) {
                if (!action.playerName().equals(playerName)) continue;

                ActionAnalysis analysis = analyzeAction(
                    action, heroHand, runningBoard, runningPot, villainRange);
                actionAnalyses.add(analysis);


                if (analysis.evLost() > 3) {
                    keyDecisions.add(createDecisionAnalysis(
                        phase, action, heroHand, runningBoard, runningPot, villainRange));
                    totalEvDiff += analysis.evLost();
                }


                runningPot += action.amount();
            }

            double endEquity = runningBoard.isEmpty() ? startEquity :
                calculateEquityQuick(heroHand, runningBoard, villainRange).equity();

            streetAnalyses.put(phase, new StreetAnalysis(
                phase, startEquity, endEquity, runningPot - runningPot, runningPot,
                describeBoardTexture(runningBoard), actionAnalyses,
                summarizeStreet(actionAnalyses)
            ));
        }


        suggestions.addAll(generateSuggestions(keyDecisions));


        OverallAssessment assessment = determineAssessment(totalEvDiff, keyDecisions.size());

        return HandAnalysisDto.builder()
            .handHistoryId(handHistory.getId())
            .playerName(playerName)
            .handSummary(generateHandSummary(handHistory, playerName))
            .overallAssessment(assessment)
            .evDifference(totalEvDiff)
            .streetAnalyses(streetAnalyses)
            .keyDecisions(keyDecisions)
            .suggestions(suggestions)
            .studyTopics(generateStudyTopics(keyDecisions))
            .build();
    }



    private List<Card> createRemainingDeck(Set<Card> usedCards) {
        List<Card> deck = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                Card card = new Card(suit, value);
                if (!containsCard(usedCards, card)) {
                    deck.add(card);
                }
            }
        }
        return deck;
    }

    private boolean containsCard(Set<Card> cards, Card card) {
        return cards.stream().anyMatch(c ->
            c.getSuit() == card.getSuit() && c.getValue() == card.getValue());
    }

    private HandStrengthCategory categorizeHand(double equity, List<Card> heroHand,
                                                 List<Card> communityCards) {
        if (equity >= 0.70) return HandStrengthCategory.VALUE_HEAVY;
        if (equity >= 0.50) return HandStrengthCategory.MEDIUM_STRENGTH;
        if (hasDrawingPotential(heroHand, communityCards)) return HandStrengthCategory.DRAWING;
        if (hasBluffingPotential(heroHand, communityCards)) return HandStrengthCategory.BLUFF_CANDIDATE;
        return HandStrengthCategory.TRASH;
    }

    private boolean hasDrawingPotential(List<Card> heroHand, List<Card> communityCards) {
        if (communityCards == null || communityCards.isEmpty()) return false;

        List<Card> allCards = new ArrayList<>(heroHand);
        allCards.addAll(communityCards);


        Map<Suit, Long> suitCounts = allCards.stream()
            .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));
        if (suitCounts.values().stream().anyMatch(c -> c >= 4)) return true;


        List<Integer> values = allCards.stream()
            .map(c -> c.getValue().ordinal())
            .sorted()
            .distinct()
            .toList();

        for (int i = 0; i < values.size() - 3; i++) {
            if (values.get(i + 3) - values.get(i) <= 4) return true;
        }

        return false;
    }

    private boolean hasBluffingPotential(List<Card> heroHand, List<Card> communityCards) {

        if (heroHand == null || heroHand.size() < 2) return false;


        boolean hasHighCard = heroHand.stream()
            .anyMatch(c -> c.getValue().ordinal() >= Value.JACK.ordinal());


        boolean isSuited = heroHand.get(0).getSuit() == heroHand.get(1).getSuit();

        return hasHighCard || isSuited;
    }

    private boolean hasImpliedOdds(List<Card> heroHand, List<Card> communityCards) {

        return hasDrawingPotential(heroHand, communityCards);
    }

    private double estimateFoldEquity(List<Card> heroHand, List<Card> communityCards,
                                      int potSize, int betSize) {

        double baseFoldEquity = 0.30;


        if (betSize > 0 && potSize > 0) {
            double betToPotRatio = (double) betSize / potSize;
            if (betToPotRatio >= 1.0) baseFoldEquity += 0.15;
            else if (betToPotRatio >= 0.66) baseFoldEquity += 0.10;
            else if (betToPotRatio >= 0.33) baseFoldEquity += 0.05;
        }


        if (communityCards != null && !communityCards.isEmpty()) {
            if (hasFlushPossible(communityCards)) baseFoldEquity += 0.05;
            if (hasStraightPossible(communityCards)) baseFoldEquity += 0.05;
        }

        return Math.min(0.70, baseFoldEquity);
    }

    private boolean hasFlushPossible(List<Card> communityCards) {
        Map<Suit, Long> suitCounts = communityCards.stream()
            .collect(Collectors.groupingBy(Card::getSuit, Collectors.counting()));
        return suitCounts.values().stream().anyMatch(c -> c >= 3);
    }

    private boolean hasStraightPossible(List<Card> communityCards) {
        List<Integer> values = communityCards.stream()
            .map(c -> c.getValue().ordinal())
            .sorted()
            .distinct()
            .toList();

        for (int i = 0; i < values.size() - 2; i++) {
            if (values.get(i + 2) - values.get(i) <= 4) return true;
        }
        return false;
    }

    private double calculatePreFlopEquity(List<Card> heroHand) {

        if (heroHand == null || heroHand.size() < 2) return 0.5;

        Value v1 = heroHand.get(0).getValue();
        Value v2 = heroHand.get(1).getValue();
        boolean suited = heroHand.get(0).getSuit() == heroHand.get(1).getSuit();
        boolean isPair = v1 == v2;

        int high = Math.max(v1.ordinal(), v2.ordinal());
        int low = Math.min(v1.ordinal(), v2.ordinal());

        double baseEquity;
        if (isPair) {
            baseEquity = 0.50 + high * 0.025;
        } else if (high >= Value.ACE.ordinal() && low >= Value.TEN.ordinal()) {
            baseEquity = 0.55 + (high + low) * 0.01;
        } else if (high >= Value.KING.ordinal()) {
            baseEquity = 0.45 + high * 0.015;
        } else {
            baseEquity = 0.35 + (high + low) * 0.008;
        }

        if (suited) baseEquity += 0.04;

        return Math.min(0.85, Math.max(0.25, baseEquity));
    }

    private List<Card> reconstructHoleCards(HandHistory.HandHistoryPlayer playerData) {
        List<Card> cards = new ArrayList<>();
        if (playerData.getHoleCard1Suit() != null && playerData.getHoleCard1Value() != null) {
            cards.add(new Card(
                Suit.valueOf(playerData.getHoleCard1Suit()),
                Value.valueOf(playerData.getHoleCard1Value())
            ));
        }
        if (playerData.getHoleCard2Suit() != null && playerData.getHoleCard2Value() != null) {
            cards.add(new Card(
                Suit.valueOf(playerData.getHoleCard2Suit()),
                Value.valueOf(playerData.getHoleCard2Value())
            ));
        }
        return cards;
    }

    private Card reconstructCard(HandHistory.CardRecord record) {
        return new Card(Suit.valueOf(record.suit()), Value.valueOf(record.value()));
    }

    private ActionAnalysis analyzeAction(HandHistory.ActionRecord action,
                                         List<Card> heroHand, List<Card> board,
                                         int potSize, HandRange villainRange) {
        PlayerAction playerAction = PlayerAction.valueOf(action.action());

        EVResult actualEV = calculateEV(playerAction, heroHand, board,
            potSize, action.amount(), villainRange);


        Map<PlayerAction, EVResult> allEVs = calculateAllEVs(
            heroHand, board, potSize, action.amount(), villainRange);

        EVResult optimalEV = allEVs.values().stream()
            .max(Comparator.comparingDouble(EVResult::expectedValue))
            .orElse(actualEV);

        String optimalAction = allEVs.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().expectedValue()))
            .map(e -> e.getKey().name())
            .orElse(playerAction.name());

        double evLost = optimalEV.expectedValue() - actualEV.expectedValue();
        ActionAssessment assessment = ActionAssessment.fromEVLoss(evLost, potSize);

        return new ActionAnalysis(
            action.action(),
            action.amount(),
            actualEV.actualEquity(),
            actualEV.expectedValue(),
            optimalEV.expectedValue(),
            optimalAction,
            assessment,
            generateActionReasoning(playerAction, actualEV, optimalEV)
        );
    }

    private DecisionAnalysis createDecisionAnalysis(GamePhase phase,
                                                     HandHistory.ActionRecord action,
                                                     List<Card> heroHand,
                                                     List<Card> board,
                                                     int potSize,
                                                     HandRange villainRange) {
        EquityResult equity = calculateEquityQuick(heroHand, board, villainRange);
        Map<PlayerAction, EVResult> evs = calculateAllEVs(
            heroHand, board, potSize, action.amount(), villainRange);

        Position position = Position.calculate(0, 0, 6);
        GTORecommendationDto gto = getGTORecommendation(
            heroHand, board, potSize, action.amount(), position, villainRange);

        String optimalAction = evs.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().expectedValue()))
            .map(e -> e.getKey().name())
            .orElse(action.action());

        EVResult actualEV = evs.get(PlayerAction.valueOf(action.action()));
        EVResult optimalEV = evs.values().stream()
            .max(Comparator.comparingDouble(EVResult::expectedValue))
            .orElse(actualEV);

        double evDiff = actualEV != null && optimalEV != null ?
            optimalEV.expectedValue() - actualEV.expectedValue() : 0;

        return new DecisionAnalysis(
            phase,
            describeSituation(phase, board, potSize),
            action.action(),
            optimalAction,
            evDiff,
            equity,
            new ArrayList<>(evs.values()),
            gto,
            generateDetailedExplanation(action.action(), optimalAction, evDiff)
        );
    }

    private String generateActionReasoning(PlayerAction action, EVResult actual, EVResult optimal) {
        if (actual.expectedValue() >= optimal.expectedValue() - 1) {
            return "Correct play";
        }
        return String.format("Optimal: %s would have been %.1f chips better",
            optimal.action(), optimal.expectedValue() - actual.expectedValue());
    }

    private String describeSituation(GamePhase phase, List<Card> board, int potSize) {
        return String.format("%s with %d chip pot%s",
            phase.name(),
            potSize,
            board.isEmpty() ? "" : " on " + describeBoardTexture(board));
    }

    private String describeBoardTexture(List<Card> board) {
        if (board == null || board.isEmpty()) return "pre-flop";

        StringBuilder sb = new StringBuilder();


        sb.append(board.stream()
            .map(c -> c.getValue().name().charAt(0) + c.getSuit().name().substring(0, 1).toLowerCase())
            .collect(Collectors.joining(" ")));


        if (hasFlushPossible(board)) sb.append(" (flush possible)");
        if (hasStraightPossible(board)) sb.append(" (straight possible)");

        return sb.toString();
    }

    private String generateDetailedExplanation(String actual, String optimal, double evDiff) {
        if (evDiff < 2) {
            return "Close decision, marginal difference.";
        }
        return String.format("Playing %s instead of %s cost approximately %.1f chips in EV.",
            actual, optimal, evDiff);
    }

    private List<String> generateSuggestions(List<DecisionAnalysis> keyDecisions) {
        List<String> suggestions = new ArrayList<>();

        for (DecisionAnalysis decision : keyDecisions) {
            if (decision.evDifference() > 10) {
                suggestions.add(String.format(
                    "On %s: Consider %s instead of %s",
                    decision.phase(),
                    decision.optimalAction(),
                    decision.actualAction()
                ));
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Solid play overall. Keep studying position and pot odds.");
        }

        return suggestions;
    }

    private List<String> generateStudyTopics(List<DecisionAnalysis> keyDecisions) {
        Set<String> topics = new HashSet<>();

        for (DecisionAnalysis decision : keyDecisions) {
            if (decision.phase() == GamePhase.PRE_FLOP) {
                topics.add("Pre-flop hand selection and position");
            }
            if (decision.actualAction().equals("CALL") &&
                decision.optimalAction().equals("FOLD")) {
                topics.add("Pot odds and equity calculations");
            }
            if (decision.actualAction().equals("FOLD") &&
                !decision.optimalAction().equals("FOLD")) {
                topics.add("Recognizing value betting opportunities");
            }
        }

        if (topics.isEmpty()) {
            topics.add("Advanced GTO concepts");
            topics.add("Exploitative adjustments");
        }

        return new ArrayList<>(topics);
    }

    private String summarizeStreet(List<ActionAnalysis> actions) {
        if (actions.isEmpty()) return "No actions";

        long optimal = actions.stream().filter(ActionAnalysis::wasOptimal).count();
        return String.format("%d/%d optimal decisions", optimal, actions.size());
    }

    private String generateHandSummary(HandHistory history, String playerName) {
        return String.format(
            "Hand #%d: %s %s. Final pot: %d chips.",
            history.getHandNumber(),
            playerName.equals(history.getWinnerName()) ? "Won" : "Lost",
            history.getWinningHandDescription() != null ?
                "with " + history.getWinningHandDescription() : "",
            history.getFinalPot()
        );
    }

    private OverallAssessment determineAssessment(double evLoss, int mistakeCount) {
        if (evLoss < 5 && mistakeCount == 0) return OverallAssessment.OPTIMAL;
        if (evLoss < 15 && mistakeCount <= 1) return OverallAssessment.GOOD;
        if (evLoss < 30) return OverallAssessment.MIXED;
        if (evLoss < 50) return OverallAssessment.POOR;
        return OverallAssessment.COSTLY;
    }
}
