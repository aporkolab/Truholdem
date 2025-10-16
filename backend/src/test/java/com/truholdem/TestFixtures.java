package com.truholdem;

import com.truholdem.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;


public final class TestFixtures {

    
    
    
    
    private TestFixtures() {
        throw new UnsupportedOperationException(
            "TestFixtures is a utility class and cannot be instantiated"
        );
    }

    
    
    
    
    
    public static final List<Card> POCKET_ACES = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE)
    );
    
    
    public static final List<Card> POCKET_KINGS = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING)
    );
    
    
    public static final List<Card> POCKET_QUEENS = List.of(
        new Card(Suit.SPADES, Value.QUEEN),
        new Card(Suit.HEARTS, Value.QUEEN)
    );
    
    
    public static final List<Card> ACE_KING_SUITED = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.SPADES, Value.KING)
    );
    
    
    public static final List<Card> SEVEN_TWO_OFFSUIT = List.of(
        new Card(Suit.SPADES, Value.SEVEN),
        new Card(Suit.HEARTS, Value.TWO)
    );

    
    
    
    
    
    public static final List<Card> ROYAL_FLUSH = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.SPADES, Value.QUEEN),
        new Card(Suit.SPADES, Value.JACK),
        new Card(Suit.SPADES, Value.TEN)
    );
    
    
    public static final List<Card> STRAIGHT_FLUSH = List.of(
        new Card(Suit.HEARTS, Value.NINE),
        new Card(Suit.HEARTS, Value.EIGHT),
        new Card(Suit.HEARTS, Value.SEVEN),
        new Card(Suit.HEARTS, Value.SIX),
        new Card(Suit.HEARTS, Value.FIVE)
    );
    
    
    public static final List<Card> STEEL_WHEEL = List.of(
        new Card(Suit.CLUBS, Value.FIVE),
        new Card(Suit.CLUBS, Value.FOUR),
        new Card(Suit.CLUBS, Value.THREE),
        new Card(Suit.CLUBS, Value.TWO),
        new Card(Suit.CLUBS, Value.ACE)
    );
    
    
    public static final List<Card> FOUR_OF_A_KIND_ACES = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.DIAMONDS, Value.ACE),
        new Card(Suit.CLUBS, Value.ACE),
        new Card(Suit.SPADES, Value.KING)
    );
    
    
    public static final List<Card> FOUR_OF_A_KIND_KINGS = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.KING),
        new Card(Suit.CLUBS, Value.KING),
        new Card(Suit.SPADES, Value.ACE)
    );
    
    
    public static final List<Card> FULL_HOUSE_ACES_KINGS = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.DIAMONDS, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING)
    );
    
    
    public static final List<Card> FULL_HOUSE_KINGS_ACES = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.KING),
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE)
    );
    
    
    public static final List<Card> FLUSH_DIAMONDS = List.of(
        new Card(Suit.DIAMONDS, Value.ACE),
        new Card(Suit.DIAMONDS, Value.JACK),
        new Card(Suit.DIAMONDS, Value.EIGHT),
        new Card(Suit.DIAMONDS, Value.SIX),
        new Card(Suit.DIAMONDS, Value.THREE)
    );
    
    
    public static final List<Card> FLUSH_CLUBS = List.of(
        new Card(Suit.CLUBS, Value.KING),
        new Card(Suit.CLUBS, Value.QUEEN),
        new Card(Suit.CLUBS, Value.NINE),
        new Card(Suit.CLUBS, Value.SEVEN),
        new Card(Suit.CLUBS, Value.FOUR)
    );
    
    
    public static final List<Card> STRAIGHT_BROADWAY = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.QUEEN),
        new Card(Suit.CLUBS, Value.JACK),
        new Card(Suit.SPADES, Value.TEN)
    );
    
    
    public static final List<Card> STRAIGHT_WHEEL = List.of(
        new Card(Suit.SPADES, Value.FIVE),
        new Card(Suit.HEARTS, Value.FOUR),
        new Card(Suit.DIAMONDS, Value.THREE),
        new Card(Suit.CLUBS, Value.TWO),
        new Card(Suit.SPADES, Value.ACE)
    );
    
    
    public static final List<Card> STRAIGHT_MIDDLE = List.of(
        new Card(Suit.SPADES, Value.EIGHT),
        new Card(Suit.HEARTS, Value.SEVEN),
        new Card(Suit.DIAMONDS, Value.SIX),
        new Card(Suit.CLUBS, Value.FIVE),
        new Card(Suit.SPADES, Value.FOUR)
    );
    
    
    public static final List<Card> THREE_OF_A_KIND_ACES = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.DIAMONDS, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.QUEEN)
    );
    
    
    public static final List<Card> THREE_OF_A_KIND_KINGS = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.KING),
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.QUEEN)
    );
    
    
    public static final List<Card> TWO_PAIR_ACES_KINGS = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.QUEEN)
    );
    
    
    public static final List<Card> TWO_PAIR_KINGS_QUEENS = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.SPADES, Value.QUEEN),
        new Card(Suit.HEARTS, Value.QUEEN),
        new Card(Suit.DIAMONDS, Value.ACE)
    );
    
    
    public static final List<Card> ONE_PAIR_ACES = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.QUEEN),
        new Card(Suit.DIAMONDS, Value.JACK)
    );
    
    
    public static final List<Card> ONE_PAIR_KINGS = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.QUEEN),
        new Card(Suit.DIAMONDS, Value.JACK)
    );
    
    
    public static final List<Card> HIGH_CARD_ACE = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.QUEEN),
        new Card(Suit.CLUBS, Value.JACK),
        new Card(Suit.SPADES, Value.NINE)
    );
    
    
    public static final List<Card> HIGH_CARD_KING = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.QUEEN),
        new Card(Suit.DIAMONDS, Value.JACK),
        new Card(Suit.CLUBS, Value.NINE),
        new Card(Suit.SPADES, Value.SEVEN)
    );

    
    
    
    
    
    public static final List<Card> DRY_FLOP_ACE_HIGH = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.DIAMONDS, Value.SEVEN),
        new Card(Suit.CLUBS, Value.TWO)
    );
    
    
    public static final List<Card> WET_FLOP = List.of(
        new Card(Suit.HEARTS, Value.JACK),
        new Card(Suit.HEARTS, Value.TEN),
        new Card(Suit.SPADES, Value.NINE)
    );
    
    
    public static final List<Card> PAIRED_FLOP = List.of(
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.FIVE)
    );
    
    
    public static final List<Card> MONOTONE_FLOP = List.of(
        new Card(Suit.CLUBS, Value.QUEEN),
        new Card(Suit.CLUBS, Value.EIGHT),
        new Card(Suit.CLUBS, Value.THREE)
    );
    
    
    public static final List<Card> CONNECTED_FLOP = List.of(
        new Card(Suit.SPADES, Value.EIGHT),
        new Card(Suit.HEARTS, Value.SEVEN),
        new Card(Suit.DIAMONDS, Value.SIX)
    );
    
    
    public static final List<Card> LOW_FLOP = List.of(
        new Card(Suit.SPADES, Value.FIVE),
        new Card(Suit.HEARTS, Value.FOUR),
        new Card(Suit.DIAMONDS, Value.TWO)
    );
    
    
    public static final List<Card> BROADWAY_BOARD = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.HEARTS, Value.KING),
        new Card(Suit.DIAMONDS, Value.QUEEN),
        new Card(Suit.CLUBS, Value.JACK),
        new Card(Suit.SPADES, Value.TEN)
    );
    
    
    public static final List<Card> FOUR_FLUSH_BOARD = List.of(
        new Card(Suit.SPADES, Value.ACE),
        new Card(Suit.SPADES, Value.KING),
        new Card(Suit.SPADES, Value.QUEEN),
        new Card(Suit.SPADES, Value.JACK),
        new Card(Suit.HEARTS, Value.TWO)
    );

    
    
    
    
    
    public static Player createPlayer(String name, int chips) {
        return new Player(name, chips, false);
    }
    
    
    public static Player createPlayer(String name) {
        return new Player(name, TestConstants.DEFAULT_STARTING_CHIPS, false);
    }
    
    
    public static Player createBot(String name, int chips) {
        return new Player(name, chips, true);
    }
    
    
    public static Player createBot(String name) {
        return new Player(name, TestConstants.DEFAULT_STARTING_CHIPS, true);
    }
    
    
    public static Player createNumberedBot(int index, int chips) {
        return new Player(TestConstants.BOT_NAME_PREFIX + index, chips, true);
    }
    
    
    public static Player createPlayerWithCards(String name, int chips, List<Card> holeCards) {
        Player player = new Player(name, chips, false);
        holeCards.forEach(player::addCardToHand);
        return player;
    }
    
    
    public static Player createFoldedPlayer(String name, int chips) {
        Player player = new Player(name, chips, false);
        player.fold();
        return player;
    }
    
    
    public static Player createAllInPlayer(String name, int potContribution) {
        Player player = new Player(name, 0, false);
        player.setBetAmount(potContribution);
        player.setTotalBetInRound(potContribution);
        player.setAllIn(true);
        return player;
    }
    
    
    public static List<Player> createPlayers(int count, int chips) {
        if (count > TestConstants.MAX_PLAYERS) {
            throw new IllegalArgumentException(
                "Cannot create more than " + TestConstants.MAX_PLAYERS + " players"
            );
        }
        
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count && i < TestConstants.STANDARD_PLAYER_NAMES.length; i++) {
            players.add(createPlayer(TestConstants.STANDARD_PLAYER_NAMES[i], chips));
        }
        
        for (int i = TestConstants.STANDARD_PLAYER_NAMES.length; i < count; i++) {
            players.add(createNumberedBot(i + 1, chips));
        }
        return players;
    }

    
    
    
    
    
    public static GameTestBuilder gameBuilder() {
        return new GameTestBuilder();
    }
    
    
    public static class GameTestBuilder {
        
        private final Game game;
        private final List<Player> players;
        private final List<Card> communityCards;
        
        
        public GameTestBuilder() {
            this.game = new Game();
            this.players = new ArrayList<>();
            this.communityCards = new ArrayList<>();
            
            
            game.setSmallBlind(TestConstants.DEFAULT_SMALL_BLIND);
            game.setBigBlind(TestConstants.DEFAULT_BIG_BLIND);
            game.setPhase(GamePhase.PRE_FLOP);
        }
        
        
        public GameTestBuilder withSmallBlind(int smallBlind) {
            game.setSmallBlind(smallBlind);
            return this;
        }
        
        
        public GameTestBuilder withBigBlind(int bigBlind) {
            game.setBigBlind(bigBlind);
            game.setMinRaiseAmount(bigBlind);
            return this;
        }
        
        
        public GameTestBuilder withPlayer(String name, int chips) {
            Player player = createPlayer(name, chips);
            player.setSeatPosition(players.size());
            players.add(player);
            return this;
        }
        
        
        public GameTestBuilder withPlayer(String name, int chips, List<Card> holeCards) {
            Player player = createPlayerWithCards(name, chips, holeCards);
            player.setSeatPosition(players.size());
            players.add(player);
            return this;
        }
        
        
        public GameTestBuilder withBot(String name, int chips) {
            Player bot = createBot(name, chips);
            bot.setSeatPosition(players.size());
            players.add(bot);
            return this;
        }
        
        
        public GameTestBuilder withBot(String name, int chips, List<Card> holeCards) {
            Player bot = createBot(name, chips);
            holeCards.forEach(bot::addCardToHand);
            bot.setSeatPosition(players.size());
            players.add(bot);
            return this;
        }
        
        
        public GameTestBuilder withPlayer(Player player) {
            player.setSeatPosition(players.size());
            players.add(player);
            return this;
        }
        
        
        public GameTestBuilder withCommunityCards(List<Card> cards) {
            communityCards.clear();
            communityCards.addAll(cards);
            return this;
        }
        
        
        public GameTestBuilder withCommunityCard(Card card) {
            communityCards.add(card);
            return this;
        }
        
        
        public GameTestBuilder withPhase(GamePhase phase) {
            game.setPhase(phase);
            return this;
        }
        
        
        public GameTestBuilder withPot(int pot) {
            game.setCurrentPot(pot);
            return this;
        }
        
        
        public GameTestBuilder withCurrentBet(int bet) {
            game.setCurrentBet(bet);
            return this;
        }
        
        
        public GameTestBuilder withCurrentPlayerIndex(int index) {
            game.setCurrentPlayerIndex(index);
            return this;
        }
        
        
        public GameTestBuilder withDealerPosition(int position) {
            game.setDealerPosition(position);
            return this;
        }
        
        
        public GameTestBuilder withHandNumber(int handNumber) {
            game.setHandNumber(handNumber);
            return this;
        }
        
        
        public GameTestBuilder finished() {
            game.setFinished(true);
            game.setPhase(GamePhase.FINISHED);
            return this;
        }
        
        
        public GameTestBuilder withWinner(String winnerName, String handDescription) {
            game.setWinnerName(winnerName);
            game.setWinningHandDescription(handDescription);
            return this;
        }
        
        
        public GameTestBuilder withId(UUID id) {
            game.setId(id);
            return this;
        }
        
        
        public GameTestBuilder withMinRaise(int amount) {
            game.setMinRaiseAmount(amount);
            return this;
        }
        
        
        public GameTestBuilder withLastRaise(int amount) {
            game.setLastRaiseAmount(amount);
            return this;
        }
        
        
        public GameTestBuilder headsUp() {
            return withPlayer(TestConstants.PLAYER_HERO, TestConstants.DEFAULT_STARTING_CHIPS)
                   .withPlayer(TestConstants.PLAYER_VILLAIN, TestConstants.DEFAULT_STARTING_CHIPS);
        }
        
        
        public GameTestBuilder sixMax() {
            withPlayer(TestConstants.PLAYER_HERO, TestConstants.DEFAULT_STARTING_CHIPS);
            IntStream.rangeClosed(1, 5)
                .forEach(i -> withBot(TestConstants.BOT_NAME_PREFIX + i, 
                                     TestConstants.DEFAULT_STARTING_CHIPS));
            return this;
        }
        
        
        public Game build() {
            if (players.size() < TestConstants.MIN_PLAYERS) {
                throw new IllegalStateException(
                    "Game requires at least " + TestConstants.MIN_PLAYERS + " players"
                );
            }
            
            
            players.forEach(game::addPlayer);
            
            
            communityCards.forEach(game::addCommunityCard);
            
            return game;
        }
        
        
        public Game buildUnsafe() {
            players.forEach(game::addPlayer);
            communityCards.forEach(game::addCommunityCard);
            return game;
        }
    }

    
    
    
    
    
    public static Card card(Suit suit, Value value) {
        return new Card(suit, value);
    }
    
    
    public static List<Card> cards(Card... cards) {
        return Arrays.asList(cards);
    }
    
    
    public static List<Card> createFullDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                deck.add(new Card(suit, value));
            }
        }
        return deck;
    }
    
    
    public static List<Card> extractHoleCards(List<Card> sevenCardHand) {
        if (sevenCardHand.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 cards to extract hole cards");
        }
        return sevenCardHand.subList(0, 2);
    }
}
