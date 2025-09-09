package com.truholdem.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher springPublisher;

    public DomainEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = Objects.requireNonNull(springPublisher, 
                "Spring publisher cannot be null");
    }

    
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        
        log.debug("Publishing domain event: {} for game {}", 
                event.getEventType(), 
                event.getGameId());
        
        springPublisher.publishEvent(event);
        
        log.trace("Published event: {}", event);
    }

    
    public void publishAll(List<? extends DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        log.debug("Publishing {} domain events", events.size());
        
        for (DomainEvent event : events) {
            publish(event);
        }
    }

    
    public void publishAll(DomainEvent... events) {
        if (events == null || events.length == 0) {
            return;
        }
        
        publishAll(List.of(events));
    }

    
    public GameCreated publish(GameCreated event) {
        publish((DomainEvent) event);
        return event;
    }

    
    public PlayerActed publish(PlayerActed event) {
        publish((DomainEvent) event);
        return event;
    }

    
    public PhaseChanged publish(PhaseChanged event) {
        publish((DomainEvent) event);
        return event;
    }

    
    public PotAwarded publish(PotAwarded event) {
        publish((DomainEvent) event);
        return event;
    }

    
    public HandCompleted publish(HandCompleted event) {
        publish((DomainEvent) event);
        return event;
    }

    
    public PlayerEliminated publish(PlayerEliminated event) {
        publish((DomainEvent) event);
        return event;
    }
}
