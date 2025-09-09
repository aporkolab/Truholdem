package com.truholdem.domain.aggregate;

import com.truholdem.domain.event.*;
import com.truholdem.domain.exception.GameStateException;
import com.truholdem.domain.exception.InvalidActionException;
import com.truholdem.domain.exception.PlayerNotFoundException;
import com.truholdem.domain.value.BettingRound;
import com.truholdem.domain.value.Chips;
import com.truholdem.domain.value.Pot;
import com.truholdem.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


public class PokerGame {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 10;

    private final UUID id;

    private Long version;

    private final Instant createdAt;

    private Instant updatedAt;

    
    private final int smallBlindAmount;
    private final int bigBlindAmount;

    
    private GamePhase phase;

    private int dealerPosition;
    private int currentPlayerIndex;
    private int handNumber;
    private boolean finished;

    
    private int currentBet;
    private int minRaise;
    private int actionsThisRound;
    private UUID lastAggressorId;

    
    private int potAmount;

    
    private final List<Player> players = new ArrayList<>();

    
    private final List<Card> communityCards = new ArrayList<>();

    
    private final List<Card> deck = new ArrayList<>();

    
    private final List<Integer> sidePotAmounts = new ArrayList<>();

    
    private Instant handStartTime;

    
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    
    
    

    private PokerGame(UUID id, Chips smallBlind, Chips bigBlind) {
        this.id = id;
        this.smallBlindAmount = smallBlind.amount();
        this.bigBlindAmount = bigBlind.amount();
        this.phase = GamePhase.PRE_FLOP;
        this.dealerPosition = 0;
        this.currentPlayerIndex = 0;
        this.handNumber = 0;
        this.finished = false;
        this.currentBet = 0;
        this.minRaise = bigBlind.amount();
        this.actionsThisRound = 0;
        this.potAmount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    
    
    

    
    public static PokerGame create(List<PlayerInfo> playerInfos, Chips smallBlind, Chips bigBlind) {
        Objects.requireNonNull(playerInfos, "Player infos cannot be null");
        Objects.requireNonNull(smallBlind, "Small blind cannot be null");
        Objects.requireNonNull(bigBlind, "Big blind cannot be null");

        if (playerInfos.size() < MIN_PLAYERS || playerInfos.size() > MAX_PLAYERS) {
            throw GameStateException.invalidPlayerCount(playerInfos.size(), MIN_PLAYERS, MAX_PLAYERS);
        }

        if (bigBlind.isLessThan(smallBlind)) {
            throw GameStateException.invalidBlinds(smallBlind.amount(), bigBlind.amount());
        }

        PokerGame game = new PokerGame(UUID.randomUUID(), smallBlind, bigBlind);

        
        List<UUID> playerIds = new ArrayList<>();
        int seatPosition = 0;
        for (PlayerInfo info : playerInfos) {
            Player player = new Player(info.getName(), info.getStartingChips(), info.isBot());
            player.setSeatPosition(seatPosition++);
            game.players.add(player);
            playerIds.add(player.getId());
        }

        
        game.initializeDeck();

        
        game.raiseEvent(new GameCreated(
                game.id,
                playerIds,
                Chips.of(playerInfos.get(0).getStartingChips()),
                smallBlind,
                bigBlind
        ));

        return game;
    }

    
    
    

    
    public void startNewHand() {
        if (finished) {
            throw GameStateException.gameAlreadyFinished(id);
        }

        List<Player> activePlayers = getPlayersWithChips();
        if (activePlayers.size() < MIN_PLAYERS) {
            throw GameStateException.notEnoughPlayers(id, activePlayers.size(), MIN_PLAYERS);
        }

        
        handNumber++;
        handStartTime = Instant.now();
        phase = GamePhase.PRE_FLOP;
        communityCards.clear();
        potAmount = 0;
        sidePotAmounts.clear();
        currentBet = 0;
        minRaise = bigBlindAmount;
        actionsThisRound = 0;
        lastAggressorId = null;

        
        for (Player player : players) {
            player.clearHand();
            player.setFolded(false);
            player.setBetAmount(0);
            player.setHasActed(false);
            player.setAllIn(false);
            player.setTotalBetInRound(0);
        }

        
        if (handNumber > 1) {
            dealerPosition = findNextActivePlayerIndex(dealerPosition);
        }

        
        initializeDeck();
        shuffleDeck();

        
        dealHoleCards();

        
        int sbIndex = findNextActivePlayerIndex(dealerPosition);
        int bbIndex = findNextActivePlayerIndex(sbIndex);
        
        Player sbPlayer = players.get(sbIndex);
        Player bbPlayer = players.get(bbIndex);

        postBlind(sbPlayer, smallBlindAmount, PlayerActed.ActionType.POST_SMALL_BLIND);
        postBlind(bbPlayer, bigBlindAmount, PlayerActed.ActionType.POST_BIG_BLIND);

        currentBet = bigBlindAmount;

        
        currentPlayerIndex = findNextActivePlayerIndex(bbIndex);

        
        raiseEvent(new GameStarted(id, dealerPosition, sbPlayer.getId(), bbPlayer.getId(), handNumber));
        
        updatedAt = Instant.now();
    }

    
    public void executeAction(UUID playerId, PlayerAction action, Chips amount) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(action, "Action cannot be null");

        if (finished) {
            throw GameStateException.gameAlreadyFinished(id);
        }

        Player player = findPlayerById(playerId);
        Player currentPlayer = getCurrentPlayer();

        if (currentPlayer == null) {
            throw PlayerNotFoundException.noCurrentPlayer(id);
        }

        if (!player.getId().equals(currentPlayer.getId())) {
            throw InvalidActionException.notPlayersTurn(playerId, currentPlayer.getId());
        }

        if (player.isFolded()) {
            throw InvalidActionException.playerAlreadyFolded(playerId);
        }

        if (player.isAllIn()) {
            throw InvalidActionException.playerIsAllIn(playerId);
        }

        
        switch (action) {
            case FOLD -> executeFold(player);
            case CHECK -> executeCheck(player);
            case CALL -> executeCall(player);
            case BET -> executeBet(player, amount);
            case RAISE -> executeRaise(player, amount);
            case ALL_IN -> executeAllIn(player);
        }

        player.setHasActed(true);
        actionsThisRound++;

        
        raiseEvent(new PlayerActed(
                id, playerId, player.getName(),
                toEventActionType(action),
                amount != null ? amount : Chips.zero(),
                phase,
                Chips.of(potAmount),
                Chips.of(player.getChips()),
                player.isAllIn()
        ));

        
        if (isBettingRoundComplete()) {
            advancePhase();
        } else {
            advanceToNextPlayer();
        }

        updatedAt = Instant.now();
    }

    
    
    

    private void executeFold(Player player) {
        player.fold();
        
        
        List<Player> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            awardPotToLastPlayer(activePlayers.get(0));
        }
    }

    private void executeCheck(Player player) {
        int amountToCall = currentBet - player.getBetAmount();
        if (amountToCall > 0) {
            throw InvalidActionException.cannotCheckFacingBet(
                    player.getId(), Chips.of(currentBet), Chips.of(player.getBetAmount()));
        }
    }

    private void executeCall(Player player) {
        int amountToCall = currentBet - player.getBetAmount();
        if (amountToCall <= 0) {
            return; 
        }

        int actualBet = Math.min(amountToCall, player.getChips());
        addToPot(player, actualBet);
    }

    private void executeBet(Player player, Chips amount) {
        if (amount == null || amount.isZero()) {
            throw InvalidActionException.invalidBetAmount(player.getId(), 0);
        }

        if (currentBet > 0) {
            throw InvalidActionException.cannotBetFacingBet(player.getId(), Chips.of(currentBet));
        }

        if (amount.amount() < bigBlindAmount) {
            throw InvalidActionException.invalidRaiseAmount(
                    player.getId(), amount, Chips.of(bigBlindAmount));
        }

        if (!Chips.of(player.getChips()).canAfford(amount)) {
            throw InvalidActionException.insufficientChips(
                    player.getId(), amount, Chips.of(player.getChips()));
        }

        addToPot(player, amount.amount());
        currentBet = amount.amount();
        minRaise = amount.amount();
        lastAggressorId = player.getId();
        resetActionsForRaise();
    }

    private void executeRaise(Player player, Chips amount) {
        if (amount == null || amount.isZero()) {
            throw InvalidActionException.invalidBetAmount(player.getId(), 0);
        }

        if (currentBet == 0) {
            throw InvalidActionException.cannotRaiseNoBet(player.getId());
        }

        int totalBetRequired = currentBet + amount.amount();
        int amountNeeded = totalBetRequired - player.getBetAmount();

        if (amount.amount() < minRaise) {
            throw InvalidActionException.invalidRaiseAmount(
                    player.getId(), amount, Chips.of(minRaise));
        }

        if (amountNeeded > player.getChips()) {
            throw InvalidActionException.insufficientChips(
                    player.getId(), Chips.of(amountNeeded), Chips.of(player.getChips()));
        }

        addToPot(player, amountNeeded);
        currentBet = totalBetRequired;
        minRaise = amount.amount();
        lastAggressorId = player.getId();
        resetActionsForRaise();
    }

    private void executeAllIn(Player player) {
        int allInAmount = player.getChips();
        addToPot(player, allInAmount);
        
        if (player.getBetAmount() > currentBet) {
            int raiseAmount = player.getBetAmount() - currentBet;
            currentBet = player.getBetAmount();
            if (raiseAmount >= minRaise) {
                minRaise = raiseAmount;
                lastAggressorId = player.getId();
                resetActionsForRaise();
            }
        }
    }

    
    
    

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public int getHandNumber() {
        return handNumber;
    }

    public boolean isFinished() {
        return finished;
    }

    public Chips getSmallBlind() {
        return Chips.of(smallBlindAmount);
    }

    public Chips getBigBlind() {
        return Chips.of(bigBlindAmount);
    }

    public Chips getPotSize() {
        int total = potAmount;
        for (int sidePot : sidePotAmounts) {
            total += sidePot;
        }
        return Chips.of(total);
    }

    public Chips getCurrentBet() {
        return Chips.of(currentBet);
    }

    public Chips getMinRaise() {
        return Chips.of(minRaise);
    }

    public int getDealerPosition() {
        return dealerPosition;
    }

    public List<Card> getCommunityCards() {
        return Collections.unmodifiableList(communityCards);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty() || currentPlayerIndex >= players.size()) {
            return null;
        }
        Player player = players.get(currentPlayerIndex);
        if (player.isFolded() || player.isAllIn()) {
            return null;
        }
        return player;
    }

    public Player findPlayerById(UUID playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> PlayerNotFoundException.byIdInGame(playerId, id));
    }

    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(p -> !p.isFolded() && p.getChips() >= 0)
                .collect(Collectors.toList());
    }

    public List<Player> getPlayersWithChips() {
        return players.stream()
                .filter(p -> p.getChips() > 0)
                .collect(Collectors.toList());
    }

    public BettingRound getCurrentBettingRound() {
        return new BettingRound(
                phase,
                Chips.of(currentBet),
                Chips.of(minRaise),
                actionsThisRound,
                lastAggressorId
        );
    }

    
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    
    
    

    private void raiseEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private void initializeDeck() {
        deck.clear();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                deck.add(new Card(suit, value));
            }
        }
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    private void dealHoleCards() {
        List<Player> activePlayers = getPlayersWithChips();
        for (int round = 0; round < 2; round++) {
            for (Player player : activePlayers) {
                if (!deck.isEmpty()) {
                    player.addCardToHand(deck.remove(0));
                }
            }
        }
    }

    private void postBlind(Player player, int amount, PlayerActed.ActionType actionType) {
        int actualAmount = Math.min(amount, player.getChips());
        addToPot(player, actualAmount);
        
        raiseEvent(new PlayerActed(
                id, player.getId(), player.getName(),
                actionType,
                Chips.of(actualAmount),
                phase,
                Chips.of(potAmount),
                Chips.of(player.getChips()),
                player.isAllIn()
        ));
    }

    private void addToPot(Player player, int amount) {
        int actualBet = player.placeBet(amount);
        potAmount += actualBet;
    }

    private int findNextActivePlayerIndex(int fromIndex) {
        int playersCount = players.size();
        for (int i = 1; i <= playersCount; i++) {
            int index = (fromIndex + i) % playersCount;
            Player player = players.get(index);
            if (!player.isFolded() && player.getChips() > 0) {
                return index;
            }
        }
        return fromIndex;
    }

    private void advanceToNextPlayer() {
        int startIndex = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            Player player = players.get(currentPlayerIndex);
            if (!player.isFolded() && !player.isAllIn() && player.getChips() > 0) {
                return;
            }
        } while (currentPlayerIndex != startIndex);
    }

    private boolean isBettingRoundComplete() {
        List<Player> activePlayers = getActivePlayers();
        
        
        if (activePlayers.size() <= 1) {
            return true;
        }

        
        for (Player player : activePlayers) {
            if (!player.isAllIn()) {
                if (!player.hasActed()) {
                    return false;
                }
                if (player.getBetAmount() < currentBet) {
                    return false;
                }
            }
        }

        return true;
    }

    private void resetActionsForRaise() {
        for (Player player : players) {
            if (!player.isFolded() && !player.isAllIn()) {
                player.setHasActed(false);
            }
        }
        
        if (lastAggressorId != null) {
            findPlayerById(lastAggressorId).setHasActed(true);
        }
        actionsThisRound = 1;
    }

    private void advancePhase() {
        GamePhase previousPhase = phase;
        List<Card> newCards = new ArrayList<>();

        switch (phase) {
            case PRE_FLOP -> {
                dealCommunityCards(3); 
                newCards.addAll(communityCards);
                phase = GamePhase.FLOP;
            }
            case FLOP -> {
                dealCommunityCards(1); 
                newCards.add(communityCards.get(communityCards.size() - 1));
                phase = GamePhase.TURN;
            }
            case TURN -> {
                dealCommunityCards(1); 
                newCards.add(communityCards.get(communityCards.size() - 1));
                phase = GamePhase.RIVER;
            }
            case RIVER -> {
                phase = GamePhase.SHOWDOWN;
                determineWinner();
                return;
            }
            default -> {
                return;
            }
        }

        
        resetForNewBettingRound();

        
        raiseEvent(new PhaseChanged(
                id, previousPhase, phase,
                newCards, new ArrayList<>(communityCards),
                Chips.of(potAmount),
                getActivePlayers().size()
        ));

        
        if (countPlayersWhoCanAct() <= 1) {
            advancePhase();
        }
    }

    private void dealCommunityCards(int count) {
        
        if (!deck.isEmpty()) {
            deck.remove(0);
        }
        
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            communityCards.add(deck.remove(0));
        }
    }

    private void resetForNewBettingRound() {
        currentBet = 0;
        actionsThisRound = 0;
        lastAggressorId = null;

        for (Player player : players) {
            player.resetBetForNewRound();
        }

        
        currentPlayerIndex = findNextActivePlayerIndex(dealerPosition);
    }

    private int countPlayersWhoCanAct() {
        return (int) players.stream()
                .filter(p -> !p.isFolded() && !p.isAllIn() && p.getChips() > 0)
                .count();
    }

    private void determineWinner() {
        List<Player> eligiblePlayers = getActivePlayers();
        
        if (eligiblePlayers.isEmpty()) {
            throw GameStateException.noActivePlayers(id);
        }

        
        
        Player winner = eligiblePlayers.get(0);
        awardPot(winner, "Winner (showdown)");

        completeHand(true);
    }

    private void awardPotToLastPlayer(Player winner) {
        awardPot(winner, null);
        completeHand(false);
    }

    private void awardPot(Player winner, String handDescription) {
        int amount = potAmount;
        winner.addWinnings(amount);

        raiseEvent(new PotAwarded(
                id, winner.getId(), winner.getName(),
                Chips.of(amount),
                handDescription,
                Pot.PotType.MAIN
        ));

        potAmount = 0;
    }

    private void completeHand(boolean wentToShowdown) {
        Duration duration = handStartTime != null 
                ? Duration.between(handStartTime, Instant.now())
                : Duration.ZERO;

        Map<UUID, Chips> playerChipsAfter = new HashMap<>();
        for (Player player : players) {
            playerChipsAfter.put(player.getId(), Chips.of(player.getChips()));
        }

        phase = GamePhase.FINISHED;

        raiseEvent(new HandCompleted(
                id, handNumber,
                List.of(), 
                playerChipsAfter,
                duration,
                actionsThisRound,
                wentToShowdown
        ));

        
        checkForEliminatedPlayers();

        
        if (getPlayersWithChips().size() < MIN_PLAYERS) {
            finished = true;
        }
    }

    private void checkForEliminatedPlayers() {
        int position = getPlayersWithChips().size() + 1;
        for (Player player : players) {
            if (player.getChips() <= 0 && !player.isFolded()) {
                raiseEvent(new PlayerEliminated(
                        id, player.getId(), player.getName(),
                        position,
                        Chips.zero(),
                        handNumber
                ));
            }
        }
    }

    private PlayerActed.ActionType toEventActionType(PlayerAction action) {
        return switch (action) {
            case FOLD -> PlayerActed.ActionType.FOLD;
            case CHECK -> PlayerActed.ActionType.CHECK;
            case CALL -> PlayerActed.ActionType.CALL;
            case BET -> PlayerActed.ActionType.BET;
            case RAISE -> PlayerActed.ActionType.RAISE;
            case ALL_IN -> PlayerActed.ActionType.ALL_IN;
        };
    }
}
