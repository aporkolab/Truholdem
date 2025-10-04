package com.truholdem.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class GameEventStore {

    private static final Logger logger = LoggerFactory.getLogger(GameEventStore.class);

    private static final String EVENTS_PREFIX = "ws:events:";
    private static final String SEQUENCE_SUFFIX = ":seq";

    
    private static final Duration EVENT_TTL = Duration.ofMinutes(10);
    
    
    private static final int MAX_EVENTS_PER_GAME = 500;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    
    private final ConcurrentHashMap<UUID, Long> sequenceCounters = new ConcurrentHashMap<>();

    public GameEventStore(
            @Qualifier("webSocketRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        logger.info("GameEventStore initialized");
    }

    
    public long storeEvent(GameEvent event) {
        if (event.getGameId() == null) {
            logger.warn("Cannot store event without game ID");
            return -1;
        }

        try {
            String eventsKey = EVENTS_PREFIX + event.getGameId();
            String seqKey = eventsKey + SEQUENCE_SUFFIX;

            
            Long sequence = redisTemplate.opsForValue().increment(seqKey);
            if (sequence == null) {
                sequence = 1L;
            }

            
            event.setSequenceNumber(sequence);

            
            String eventJson = objectMapper.writeValueAsString(event);

            
            redisTemplate.opsForZSet().add(eventsKey, eventJson, sequence);

            
            redisTemplate.expire(eventsKey, EVENT_TTL);
            redisTemplate.expire(seqKey, EVENT_TTL);

            
            trimEvents(event.getGameId());

            logger.debug("Stored event {} with sequence {} for game {}", 
                    event.getEventId(), sequence, event.getGameId());

            return sequence;

        } catch (Exception e) {
            logger.error("Failed to store event", e);
            return -1;
        }
    }

    
    public List<GameEvent> getEventsSince(UUID gameId, long sinceSequence) {
        if (gameId == null) {
            return Collections.emptyList();
        }

        try {
            String eventsKey = EVENTS_PREFIX + gameId;

            
            Set<Object> eventJsons = redisTemplate.opsForZSet()
                    .rangeByScore(eventsKey, sinceSequence + 1, Double.MAX_VALUE);

            if (eventJsons == null || eventJsons.isEmpty()) {
                return Collections.emptyList();
            }

            List<GameEvent> events = new ArrayList<>();
            for (Object eventJson : eventJsons) {
                try {
                    GameEvent event = objectMapper.readValue(eventJson.toString(), GameEvent.class);
                    events.add(event);
                } catch (Exception e) {
                    logger.warn("Failed to deserialize event: {}", eventJson, e);
                }
            }

            
            events.sort(Comparator.comparingLong(GameEvent::getSequenceNumber));

            logger.debug("Retrieved {} events since sequence {} for game {}", 
                    events.size(), sinceSequence, gameId);

            return events;

        } catch (Exception e) {
            logger.error("Failed to retrieve events for game {}", gameId, e);
            return Collections.emptyList();
        }
    }

    
    public long getLatestSequence(UUID gameId) {
        if (gameId == null) {
            return 0;
        }

        try {
            String seqKey = EVENTS_PREFIX + gameId + SEQUENCE_SUFFIX;
            Object value = redisTemplate.opsForValue().get(seqKey);
            
            if (value != null) {
                return Long.parseLong(value.toString());
            }
            
            return 0;

        } catch (Exception e) {
            logger.error("Failed to get latest sequence for game {}", gameId, e);
            return 0;
        }
    }

    
    public long getEventCount(UUID gameId) {
        if (gameId == null) {
            return 0;
        }

        String eventsKey = EVENTS_PREFIX + gameId;
        Long count = redisTemplate.opsForZSet().size(eventsKey);
        return count != null ? count : 0;
    }

    
    public void clearEvents(UUID gameId) {
        if (gameId == null) {
            return;
        }

        String eventsKey = EVENTS_PREFIX + gameId;
        String seqKey = eventsKey + SEQUENCE_SUFFIX;

        redisTemplate.delete(eventsKey);
        redisTemplate.delete(seqKey);
        sequenceCounters.remove(gameId);

        logger.info("Cleared events for game {}", gameId);
    }

    
    private void trimEvents(UUID gameId) {
        String eventsKey = EVENTS_PREFIX + gameId;
        Long count = redisTemplate.opsForZSet().size(eventsKey);

        if (count != null && count > MAX_EVENTS_PER_GAME) {
            
            long toRemove = count - MAX_EVENTS_PER_GAME;
            redisTemplate.opsForZSet().removeRange(eventsKey, 0, toRemove - 1);
            logger.debug("Trimmed {} old events for game {}", toRemove, gameId);
        }
    }

    
    @Scheduled(fixedRate = 300000) 
    public void cleanupSequenceCounters() {
        
        sequenceCounters.entrySet().removeIf(entry -> {
            String eventsKey = EVENTS_PREFIX + entry.getKey();
            Boolean exists = redisTemplate.hasKey(eventsKey);
            return exists == null || !exists;
        });

        logger.debug("Cleanup complete - {} active sequence counters", sequenceCounters.size());
    }
}
