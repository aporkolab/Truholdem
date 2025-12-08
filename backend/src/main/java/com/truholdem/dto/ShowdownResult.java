package com.truholdem.dto;

import com.truholdem.model.Card;
import java.util.List;
import java.util.UUID;


public class ShowdownResult {

    private List<WinnerInfo> winners;
    private int totalPot;
    private String message;

    public ShowdownResult() {
    }

    public ShowdownResult(List<WinnerInfo> winners, int totalPot, String message) {
        this.winners = winners;
        this.totalPot = totalPot;
        this.message = message;
    }

    public List<WinnerInfo> getWinners() {
        return winners;
    }

    public void setWinners(List<WinnerInfo> winners) {
        this.winners = winners;
    }

    public int getTotalPot() {
        return totalPot;
    }

    public void setTotalPot(int totalPot) {
        this.totalPot = totalPot;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    
    public static class WinnerInfo {
        private UUID playerId;
        private String playerName;
        private int amountWon;
        private String handDescription;
        private List<Card> holeCards;
        private List<Card> bestFiveCards;

        public WinnerInfo() {
        }

        public WinnerInfo(UUID playerId, String playerName, int amountWon, 
                         String handDescription, List<Card> holeCards) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.amountWon = amountWon;
            this.handDescription = handDescription;
            this.holeCards = holeCards;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public void setPlayerId(UUID playerId) {
            this.playerId = playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public int getAmountWon() {
            return amountWon;
        }

        public void setAmountWon(int amountWon) {
            this.amountWon = amountWon;
        }

        public String getHandDescription() {
            return handDescription;
        }

        public void setHandDescription(String handDescription) {
            this.handDescription = handDescription;
        }

        public List<Card> getHoleCards() {
            return holeCards;
        }

        public void setHoleCards(List<Card> holeCards) {
            this.holeCards = holeCards;
        }

        public List<Card> getBestFiveCards() {
            return bestFiveCards;
        }

        public void setBestFiveCards(List<Card> bestFiveCards) {
            this.bestFiveCards = bestFiveCards;
        }
    }
}
