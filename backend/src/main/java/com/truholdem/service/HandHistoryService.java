package com.truholdem.service;

import com.truholdem.model.*;
import com.truholdem.repository.HandHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Transactional
public class HandHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(HandHistoryService.class);

    private final HandHistoryRepository handHistoryRepository;


    private final Map<UUID, HandHistory> activeHandHistories = new ConcurrentHashMap<>();

    public HandHistoryService(HandHistoryRepository handHistoryRepository) {
        this.handHistoryRepository = handHistoryRepository;
    }

    

    
    public HandHistory startRecording(Game game) {
        HandHistory history = new HandHistory(game);
        activeHandHistories.put(game.getId(), history);
        logger.debug("Started recording hand #{} for game {}", game.getHandNumber(), game.getId());
        return history;
    }

    
    public void recordAction(UUID gameId, Player player, PlayerAction action, int amount, GamePhase phase) {
        HandHistory history = activeHandHistories.get(gameId);
        if (history != null) {
            history.recordAction(player, action, amount, phase);
            logger.debug("Recorded action: {} {} {} in phase {}", 
                player.getName(), action, amount, phase);
        }
    }

    
    public void recordCommunityCards(UUID gameId, List<Card> cards) {
        HandHistory history = activeHandHistories.get(gameId);
        if (history != null) {
            history.recordCommunityCards(cards);
            logger.debug("Recorded {} community cards", cards.size());
        }
    }

    
    public HandHistory finishRecording(UUID gameId, String winnerName, String handDescription, int finalPot) {
        HandHistory history = activeHandHistories.remove(gameId);
        if (history != null) {
            history.recordResult(winnerName, handDescription, finalPot);
            history = handHistoryRepository.save(history);
            logger.info("Saved hand history #{} for game {}. Winner: {}, Pot: {}", 
                history.getHandNumber(), gameId, winnerName, finalPot);
            return history;
        }
        return null;
    }

    
    public void cancelRecording(UUID gameId) {
        activeHandHistories.remove(gameId);
        logger.debug("Cancelled hand history recording for game {}", gameId);
    }

    

    
    @Transactional(readOnly = true)
    public Optional<HandHistory> getHandHistory(UUID historyId) {
        return handHistoryRepository.findById(historyId);
    }

    
    @Transactional(readOnly = true)
    public List<HandHistory> getGameHistory(UUID gameId) {
        return handHistoryRepository.findByGameIdOrderByHandNumberDesc(gameId);
    }

    
    @Transactional(readOnly = true)
    public Page<HandHistory> getGameHistory(UUID gameId, int page, int size) {
        return handHistoryRepository.findByGameId(gameId, PageRequest.of(page, size));
    }

    
    @Transactional(readOnly = true)
    public List<HandHistory> getPlayerHistory(UUID playerId) {
        return handHistoryRepository.findByPlayerId(playerId);
    }

    
    @Transactional(readOnly = true)
    public List<HandHistory> getPlayerWins(String playerName) {
        return handHistoryRepository.findByWinnerName(playerName);
    }

    
    @Transactional(readOnly = true)
    public List<HandHistory> getRecentHands() {
        return handHistoryRepository.findTop50ByOrderByPlayedAtDesc();
    }

    
    @Transactional(readOnly = true)
    public List<HandHistory> getBiggestPots() {
        return handHistoryRepository.findTop10ByOrderByFinalPotDesc();
    }

    
    @Transactional(readOnly = true)
    public long getHandCount(UUID gameId) {
        return handHistoryRepository.countByGameId(gameId);
    }

    

    
    @Transactional(readOnly = true)
    public ReplayData generateReplayData(UUID historyId) {
        Optional<HandHistory> optHistory = handHistoryRepository.findById(historyId);
        if (optHistory.isEmpty()) {
            return null;
        }

        HandHistory history = optHistory.get();
        return new ReplayData(history);
    }

    
    public record ReplayData(
        UUID id,
        int handNumber,
        int smallBlind,
        int bigBlind,
        int dealerPosition,
        List<PlayerSnapshot> players,
        List<ReplayAction> actions,
        List<String> board,
        String winnerName,
        String winningHand,
        int finalPot
    ) {
        public ReplayData(HandHistory history) {
            this(
                history.getId(),
                history.getHandNumber(),
                history.getSmallBlind(),
                history.getBigBlind(),
                history.getDealerPosition(),
                history.getPlayers().stream()
                    .map(p -> new PlayerSnapshot(
                        p.getPlayerId(),
                        p.getPlayerName(),
                        p.getStartingChips(),
                        p.getSeatPosition(),
                        p.getHoleCard1Value() + " of " + p.getHoleCard1Suit(),
                        p.getHoleCard2Value() + " of " + p.getHoleCard2Suit()
                    ))
                    .toList(),
                history.getActions().stream()
                    .map(a -> new ReplayAction(
                        a.playerName(),
                        a.action(),
                        a.amount(),
                        a.phase()
                    ))
                    .toList(),
                history.getBoard().stream()
                    .map(c -> c.value() + " of " + c.suit())
                    .toList(),
                history.getWinnerName(),
                history.getWinningHandDescription(),
                history.getFinalPot()
            );
        }
    }

    public record PlayerSnapshot(
        UUID id,
        String name,
        int startingChips,
        int seatPosition,
        String holeCard1,
        String holeCard2
    ) {}

    public record ReplayAction(
        String playerName,
        String action,
        int amount,
        String phase
    ) {}

    

    
    public void deleteGameHistory(UUID gameId) {
        handHistoryRepository.deleteByGameId(gameId);
        logger.info("Deleted all hand history for game {}", gameId);
    }
}
