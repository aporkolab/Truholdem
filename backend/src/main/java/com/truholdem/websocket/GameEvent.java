package com.truholdem.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.truholdem.model.GameUpdateType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class GameEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String sourceInstanceId;
    private UUID gameId;
    private GameUpdateType type;
    private String destination;
    private Object payload;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private long sequenceNumber;


    public GameEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    private GameEvent(Builder builder) {
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID().toString();
        this.sourceInstanceId = builder.sourceInstanceId;
        this.gameId = builder.gameId;
        this.type = builder.type;
        this.destination = builder.destination;
        this.payload = builder.payload;
        this.metadata = builder.metadata;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.sequenceNumber = builder.sequenceNumber;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSourceInstanceId() {
        return sourceInstanceId;
    }

    public void setSourceInstanceId(String sourceInstanceId) {
        this.sourceInstanceId = sourceInstanceId;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

    public GameUpdateType getType() {
        return type;
    }

    public void setType(GameUpdateType type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String toString() {
        return "GameEvent{" +
            "eventId='" + eventId + '\'' +
            ", sourceInstanceId='" + sourceInstanceId + '\'' +
            ", gameId=" + gameId +
            ", type=" + type +
            ", destination='" + destination + '\'' +
            ", timestamp=" + timestamp +
            ", sequenceNumber=" + sequenceNumber +
            '}';
    }

    public static class Builder {
        private String eventId;
        private String sourceInstanceId;
        private UUID gameId;
        private GameUpdateType type;
        private String destination;
        private Object payload;
        private Map<String, Object> metadata;
        private Instant timestamp;
        private long sequenceNumber;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder sourceInstanceId(String sourceInstanceId) {
            this.sourceInstanceId = sourceInstanceId;
            return this;
        }

        public Builder gameId(UUID gameId) {
            this.gameId = gameId;
            return this;
        }

        public Builder type(GameUpdateType type) {
            this.type = type;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public GameEvent build() {
            return new GameEvent(this);
        }
    }
}
