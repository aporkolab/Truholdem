package com.truholdem;

import java.time.Duration;
import java.util.UUID;


public final class TestConstants {

    
    
    
    
    private TestConstants() {
        throw new UnsupportedOperationException(
            "TestConstants is a utility class and cannot be instantiated"
        );
    }

    
    
    
    
    
    public static final int DEFAULT_STARTING_CHIPS = 1000;
    
    
    public static final int SMALL_STACK = 200;
    
    
    public static final int MEDIUM_STACK = 1000;
    
    
    public static final int LARGE_STACK = 4000;
    
    
    public static final int MINIMUM_STACK = 20;
    
    
    public static final int HIGH_ROLLER_STACK = 10000;

    
    
    
    
    
    public static final int DEFAULT_SMALL_BLIND = 10;
    
    
    public static final int DEFAULT_BIG_BLIND = 20;
    
    
    public static final int MICRO_SMALL_BLIND = 1;
    
    
    public static final int MICRO_BIG_BLIND = 2;
    
    
    public static final int HIGH_STAKES_SMALL_BLIND = 50;
    
    
    public static final int HIGH_STAKES_BIG_BLIND = 100;

    
    
    
    
    
    public static final String PLAYER_HERO = "Hero";
    
    
    public static final String PLAYER_VILLAIN = "Villain";
    
    
    public static final String PLAYER_FISH = "Fish";
    
    
    public static final String PLAYER_SHARK = "Shark";
    
    
    public static final String PLAYER_ROCK = "Rock";
    
    
    public static final String BOT_NAME_PREFIX = "Bot_";
    
    
    public static final String ANONYMOUS_PLAYER = "Anonymous";
    
    
    public static final String[] STANDARD_PLAYER_NAMES = {
        PLAYER_HERO, PLAYER_VILLAIN, PLAYER_FISH, PLAYER_SHARK, PLAYER_ROCK
    };

    
    
    
    
    
    public static final int MIN_PLAYERS = 2;
    
    
    public static final int MAX_PLAYERS = 10;
    
    
    public static final int SIX_MAX_PLAYERS = 6;
    
    
    public static final int HEADS_UP_PLAYERS = 2;

    
    
    
    
    
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    
    
    public static final Duration SHORT_TIMEOUT = Duration.ofSeconds(2);
    
    
    public static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);
    
    
    public static final Duration E2E_TIMEOUT = Duration.ofSeconds(60);
    
    
    public static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    
    
    public static final int BOT_THINK_TIME_MS = 100;

    
    
    
    
    
    public static final UUID TEST_GAME_ID = 
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    
    
    public static final UUID TEST_PLAYER_ID_1 = 
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    
    
    public static final UUID TEST_PLAYER_ID_2 = 
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    
    
    public static final UUID TEST_PLAYER_ID_3 = 
        UUID.fromString("44444444-4444-4444-4444-444444444444");

    
    
    
    
    
    public static final int STANDARD_RAISE = DEFAULT_BIG_BLIND * 3;
    
    
    public static final int MIN_RAISE = DEFAULT_BIG_BLIND * 2;
    
    
    public static final int LARGE_RAISE = DEFAULT_BIG_BLIND * 5;
    
    
    public static final int OVERBET = DEFAULT_BIG_BLIND * 10;

    
    
    
    
    
    public static final String TEST_JWT_SECRET = 
        "testSecretKeyForTesting_AtLeast32Chars_LongEnough!!";
    
    
    public static final long TEST_JWT_EXPIRATION = 3600000L;
    
    
    public static final String TEST_USERNAME = "testuser";
    
    
    public static final String TEST_PASSWORD = "TestPassword123!";
    
    
    public static final String TEST_EMAIL = "testuser@truholdem.test";

    
    
    
    
    
    public static final String H2_TEST_URL = 
        "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    
    
    public static final String POSTGRES_IMAGE = "postgres:15-alpine";
    
    
    public static final String REDIS_IMAGE = "redis:7-alpine";
}
