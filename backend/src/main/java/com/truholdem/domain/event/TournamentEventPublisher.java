package com.truholdem.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component
public class TournamentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TournamentEventPublisher.class);

    private final ApplicationEventPublisher springPublisher;

    public TournamentEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = Objects.requireNonNull(springPublisher, 
                "Spring publisher cannot be null");
    }

    
    public void publish(TournamentEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        
        log.debug("Publishing tournament event: {} for tournament {}", 
                event.getEventType(), 
                event.getTournamentId());
        
        springPublisher.publishEvent(event);
        
        log.trace("Published event: {}", event);
    }

    
    public void publishAll(List<? extends TournamentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        log.debug("Publishing {} tournament events", events.size());
        
        for (TournamentEvent event : events) {
            publish(event);
        }
    }

    
    public void publishAll(TournamentEvent... events) {
        if (events == null || events.length == 0) {
            return;
        }
        
        publishAll(List.of(events));
    }

    
    
    public TournamentCreated publish(TournamentCreated event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentStarted publish(TournamentStarted event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentLevelAdvanced publish(TournamentLevelAdvanced event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentPlayerRegistered publish(TournamentPlayerRegistered event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentPlayerEliminated publish(TournamentPlayerEliminated event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentTableCreated publish(TournamentTableCreated event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentTablesRebalanced publish(TournamentTablesRebalanced event) {
        publish((TournamentEvent) event);
        return event;
    }

    public TournamentCompleted publish(TournamentCompleted event) {
        publish((TournamentEvent) event);
        return event;
    }
}
