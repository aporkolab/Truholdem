package com.truholdem.controller;

import com.truholdem.config.api.ApiV1Config;
import com.truholdem.domain.value.Position;
import com.truholdem.dto.*;
import com.truholdem.model.*;
import com.truholdem.service.HandAnalysisService;
import com.truholdem.service.HandHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@ApiV1Config
@RequestMapping("/analysis")
@Tag(name = "Hand Analysis", description = "Poker equity calculator, EV computation, and GTO engine")
@SecurityRequirement(name = "bearerAuth")
public class HandAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(HandAnalysisController.class);

    private final HandAnalysisService handAnalysisService;
    private final HandHistoryService handHistoryService;

    public HandAnalysisController(HandAnalysisService handAnalysisService,
                                  HandHistoryService handHistoryService) {
        this.handAnalysisService = handAnalysisService;
        this.handHistoryService = handHistoryService;
    }



    @PostMapping("/equity")
    @Operation(
        summary = "Calculate hand equity",
        description = "Calculate equity against opponent range using Monte Carlo simulation"
    )
    @ApiResponse(responseCode = "200", description = "Equity calculated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<EquityResult> calculateEquity(
            @Valid @RequestBody EquityRequest request) {

        logger.info("Calculating equity for hand: {}", request.heroHand());

        List<Card> heroHand = parseCards(request.heroHand());
        List<Card> communityCards = request.communityCards() != null ?
            parseCards(request.communityCards()) : List.of();

        HandRange villainRange = request.villainRange() != null ?
            HandRange.fromNotation(request.villainRange()) :
            HandRange.forPositionType(PositionType.valueOf(
                request.villainPosition() != null ? request.villainPosition() : "DEALER"));

        EquityResult result = handAnalysisService.calculateEquity(
            heroHand,
            communityCards,
            villainRange,
            request.numOpponents() != null ? request.numOpponents() : 1,
            request.simulations() != null ? request.simulations() : 10000
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/equity/quick")
    @Operation(
        summary = "Quick equity calculation",
        description = "Fast equity calculation with fewer simulations"
    )
    public ResponseEntity<EquityResult> quickEquity(
            @RequestParam @NotEmpty String heroHand,
            @RequestParam(required = false) String communityCards,
            @RequestParam(required = false, defaultValue = "DEALER") String villainPosition) {

        List<Card> hero = parseCards(heroHand);
        List<Card> community = communityCards != null && !communityCards.isEmpty() ?
            parseCards(communityCards) : List.of();
        HandRange range = HandRange.forPositionType(PositionType.valueOf(villainPosition));

        EquityResult result = handAnalysisService.calculateEquityQuick(hero, community, range);
        return ResponseEntity.ok(result);
    }



    @PostMapping("/ev")
    @Operation(
        summary = "Calculate Expected Value",
        description = "Calculate EV for all available actions"
    )
    public ResponseEntity<Map<String, EVResult>> calculateEV(
            @Valid @RequestBody EVRequest request) {

        logger.info("Calculating EV for situation: pot={}, bet={}",
            request.potSize(), request.betToCall());

        List<Card> heroHand = parseCards(request.heroHand());
        List<Card> communityCards = request.communityCards() != null ?
            parseCards(request.communityCards()) : List.of();
        HandRange villainRange = request.villainRange() != null ?
            HandRange.fromNotation(request.villainRange()) :
            HandRange.buttonOpen();

        Map<PlayerAction, EVResult> evs = handAnalysisService.calculateAllEVs(
            heroHand,
            communityCards,
            request.potSize(),
            request.betToCall(),
            villainRange
        );


        Map<String, EVResult> result = new LinkedHashMap<>();
        evs.forEach((action, ev) -> result.put(action.name(), ev));

        return ResponseEntity.ok(result);
    }



    @PostMapping("/recommend")
    @Operation(
        summary = "Get GTO recommendation",
        description = "Get optimal action recommendation based on GTO principles"
    )
    public ResponseEntity<GTORecommendationDto> getRecommendation(
            @Valid @RequestBody RecommendationRequest request) {

        logger.info("Getting GTO recommendation for position: {}", request.position());

        List<Card> heroHand = parseCards(request.heroHand());
        List<Card> communityCards = request.communityCards() != null ?
            parseCards(request.communityCards()) : List.of();
        HandRange villainRange = request.villainRange() != null ?
            HandRange.fromNotation(request.villainRange()) :
            HandRange.forPositionType(parseVillainPosition(request.villainPosition()));

        Position position = Position.calculate(
            request.seatNumber() != null ? request.seatNumber() : 5,
            request.dealerPosition() != null ? request.dealerPosition() : 0,
            request.totalPlayers() != null ? request.totalPlayers() : 6
        );

        GTORecommendationDto recommendation = handAnalysisService.getGTORecommendation(
            heroHand,
            communityCards,
            request.potSize(),
            request.betToCall() != null ? request.betToCall() : 0,
            position,
            villainRange
        );

        return ResponseEntity.ok(recommendation);
    }



    @GetMapping("/hand/{handHistoryId}")
    @Operation(
        summary = "Analyze completed hand",
        description = "Get detailed analysis of a completed hand"
    )
    public ResponseEntity<HandAnalysisDto> analyzeHand(
            @PathVariable UUID handHistoryId,
            @RequestParam String playerName) {

        logger.info("Analyzing hand {} for player {}", handHistoryId, playerName);

        HandHistory history = handHistoryService.getHandHistory(handHistoryId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Hand history not found: " + handHistoryId));

        HandAnalysisDto analysis = handAnalysisService.analyzeHand(history, playerName);
        return ResponseEntity.ok(analysis);
    }



    private List<Card> parseCards(String cardString) {
        if (cardString == null || cardString.isEmpty()) {
            return List.of();
        }

        List<Card> cards = new ArrayList<>();
        String[] parts = cardString.split(",");

        for (String part : parts) {
            String card = part.trim().toUpperCase();
            if (card.length() < 2) continue;

            Value value = parseValue(card.substring(0, card.length() - 1));
            Suit suit = parseSuit(card.charAt(card.length() - 1));

            if (value != null && suit != null) {
                cards.add(new Card(suit, value));
            }
        }

        return cards;
    }

    private Value parseValue(String v) {
        return switch (v) {
            case "A" -> Value.ACE;
            case "K" -> Value.KING;
            case "Q" -> Value.QUEEN;
            case "J" -> Value.JACK;
            case "T", "10" -> Value.TEN;
            case "9" -> Value.NINE;
            case "8" -> Value.EIGHT;
            case "7" -> Value.SEVEN;
            case "6" -> Value.SIX;
            case "5" -> Value.FIVE;
            case "4" -> Value.FOUR;
            case "3" -> Value.THREE;
            case "2" -> Value.TWO;
            default -> null;
        };
    }

    private Suit parseSuit(char s) {
        return switch (s) {
            case 'S' -> Suit.SPADES;
            case 'H' -> Suit.HEARTS;
            case 'D' -> Suit.DIAMONDS;
            case 'C' -> Suit.CLUBS;
            default -> null;
        };
    }

    private PositionType parseVillainPosition(String position) {
        if (position == null) return PositionType.DEALER;
        try {
            return PositionType.valueOf(position.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PositionType.DEALER;
        }
    }



    @GetMapping("/ranges/presets")
    @Operation(
        summary = "Get predefined range presets",
        description = "Returns a list of commonly used hand ranges for different positions and situations"
    )
    @ApiResponse(responseCode = "200", description = "Range presets retrieved successfully")
    public ResponseEntity<List<RangePreset>> getRangePresets() {
        logger.info("Getting range presets");

        List<RangePreset> presets = List.of(
            new RangePreset(
                "premium",
                "Premium",
                "Top premium hands (AA, KK, QQ, AKs)",
                List.of("AA", "KK", "QQ", "AKs"),
                2.6
            ),
            new RangePreset(
                "earlyPosition",
                "Early Position",
                "Typical UTG/UTG+1 opening range (~12%)",
                List.of("AA", "KK", "QQ", "JJ", "TT", "AKs", "AQs", "AJs", "ATs", "KQs", "AKo", "AQo"),
                12.0
            ),
            new RangePreset(
                "middlePosition",
                "Middle Position",
                "Typical MP opening range (~18%)",
                List.of("AA", "KK", "QQ", "JJ", "TT", "99", "88", "AKs", "AQs", "AJs", "ATs", "KQs", "KJs", "QJs", "JTs", "AKo", "AQo", "AJo", "KQo"),
                18.0
            ),
            new RangePreset(
                "buttonOpen",
                "Button Open",
                "Wide button opening range (~45%)",
                List.of("AA", "KK", "QQ", "JJ", "TT", "99", "88", "77", "66", "55", "44", "33", "22",
                       "AKs", "AQs", "AJs", "ATs", "A9s", "A8s", "A7s", "A6s", "A5s", "A4s", "A3s", "A2s",
                       "KQs", "KJs", "KTs", "K9s", "K8s", "K7s", "K6s", "K5s",
                       "QJs", "QTs", "Q9s", "Q8s", "JTs", "J9s", "J8s", "T9s", "T8s", "98s", "97s", "87s", "76s", "65s", "54s",
                       "AKo", "AQo", "AJo", "ATo", "A9o", "KQo", "KJo", "KTo", "K9o", "QJo", "QTo", "JTo"),
                45.0
            ),
            new RangePreset(
                "bigBlindDefend",
                "Big Blind Defend",
                "BB defense range vs button open (~38%)",
                List.of("AA", "KK", "QQ", "JJ", "TT", "99", "88", "77", "66", "55", "44", "33", "22",
                       "AKs", "AQs", "AJs", "ATs", "A9s", "A8s", "A7s", "A6s", "A5s", "A4s", "A3s", "A2s",
                       "KQs", "KJs", "KTs", "K9s", "K8s", "QJs", "QTs", "Q9s", "JTs", "J9s", "T9s", "98s", "87s", "76s", "65s", "54s",
                       "AKo", "AQo", "AJo", "ATo", "A9o", "A8o", "A7o", "A6o", "A5o", "A4o", "KQo", "KJo", "KTo", "K9o", "QJo", "QTo", "JTo"),
                38.0
            ),
            new RangePreset(
                "threeBet",
                "3-Bet Range",
                "Polarized 3-bet range (~7%)",
                List.of("AA", "KK", "QQ", "JJ", "AKs", "AQs", "AKo", "AQo", "A5s", "A4s", "76s", "65s"),
                7.0
            ),
            new RangePreset(
                "allPairs",
                "All Pairs",
                "All pocket pairs (22-AA)",
                List.of("AA", "KK", "QQ", "JJ", "TT", "99", "88", "77", "66", "55", "44", "33", "22"),
                5.9
            ),
            new RangePreset(
                "suitedConnectors",
                "Suited Connectors",
                "Connected suited hands",
                List.of("AKs", "KQs", "QJs", "JTs", "T9s", "98s", "87s", "76s", "65s", "54s", "43s", "32s"),
                3.6
            ),
            new RangePreset(
                "broadway",
                "Broadway",
                "All broadway hands (T+ cards)",
                List.of("AA", "KK", "QQ", "JJ", "TT",
                       "AKs", "AQs", "AJs", "ATs", "KQs", "KJs", "KTs", "QJs", "QTs", "JTs",
                       "AKo", "AQo", "AJo", "ATo", "KQo", "KJo", "KTo", "QJo", "QTo", "JTo"),
                14.3
            )
        );

        return ResponseEntity.ok(presets);
    }



    public record RangePreset(
        String id,
        String name,
        String description,
        List<String> hands,
        double percentage
    ) {}

    public record EquityRequest(
        @NotEmpty(message = "Hero hand is required")
        String heroHand,
        String communityCards,
        String villainRange,
        String villainPosition,
        @Min(1) @Max(9)
        Integer numOpponents,
        @Min(100) @Max(100000)
        Integer simulations
    ) {}

    public record EVRequest(
        @NotEmpty(message = "Hero hand is required")
        String heroHand,
        String communityCards,
        @NotNull(message = "Pot size is required")
        @Min(0)
        Integer potSize,
        @Min(0)
        Integer betToCall,
        String villainRange
    ) {}

    public record RecommendationRequest(
        @NotEmpty(message = "Hero hand is required")
        String heroHand,
        String communityCards,
        @NotNull(message = "Pot size is required")
        @Min(0)
        Integer potSize,
        Integer betToCall,
        String position,
        String villainPosition,
        String villainRange,
        Integer seatNumber,
        Integer dealerPosition,
        Integer totalPlayers
    ) {}
}
