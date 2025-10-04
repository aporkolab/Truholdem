package com.truholdem.application.listener;

import com.truholdem.domain.event.HandCompleted;
import com.truholdem.domain.event.PlayerActed;
import com.truholdem.domain.event.PlayerEliminated;
import com.truholdem.domain.event.PotAwarded;
import com.truholdem.domain.value.Chips;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
public class StatisticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(StatisticsEventListener.class);

    
    

    
    @EventListener
    @Async
    public void onPlayerActed(PlayerActed event) {
        log.debug("Processing PlayerActed event for statistics: {} {} in {}",
                event.getPlayerName(), event.getAction(), event.getPhase());

        try {
            
            if (event.getAction() == PlayerActed.ActionType.CALL ||
                event.getAction() == PlayerActed.ActionType.BET ||
                event.getAction() == PlayerActed.ActionType.RAISE) {
                
                log.trace("Recording VPIP for player {}", event.getPlayerId());
                
            }

            
            if (event.getPhase().name().equals("PRE_FLOP") && event.isAggressive()) {
                log.trace("Recording PFR for player {}", event.getPlayerId());
                
            }

            
            if (event.isAggressive()) {
                log.trace("Recording aggressive action for player {}", event.getPlayerId());
                
            }

            
            if (event.getAction() == PlayerActed.ActionType.FOLD) {
                log.trace("Recording fold for player {}", event.getPlayerId());
                
            }

            
            if (event.isAllIn()) {
                log.trace("Recording all-in for player {}", event.getPlayerId());
                
            }

        } catch (Exception e) {
            log.error("Failed to process PlayerActed event for statistics: {}", event, e);
        }
    }

    
    @EventListener
    @Async
    public void onHandCompleted(HandCompleted event) {
        log.debug("Processing HandCompleted event: hand #{}, pot={}, showdown={}",
                event.getHandNumber(), event.getTotalPotSize(), event.wentToShowdown());

        try {
            
            for (var entry : event.getPlayerChipsAfter().entrySet()) {
                log.trace("Recording hand played for player {}", entry.getKey());
                
            }

            
            if (event.wentToShowdown()) {
                for (var potResult : event.getPotResults()) {
                    log.trace("Recording showdown win for player {}", potResult.winnerId());
                    
                }
            }

            
            Chips potSize = event.getTotalPotSize();
            if (potSize.amount() > 0) {
                for (var potResult : event.getPotResults()) {
                    log.trace("Checking biggest pot for player {}: {}", 
                            potResult.winnerId(), potResult.amount());
                    
                }
            }

        } catch (Exception e) {
            log.error("Failed to process HandCompleted event for statistics: {}", event, e);
        }
    }

    
    @EventListener
    @Async
    public void onPotAwarded(PotAwarded event) {
        log.debug("Processing PotAwarded event: {} wins {} {}",
                event.getWinnerName(), event.getPotType(), event.getAmount());

        try {
            
            log.trace("Recording pot win for player {}: {}", 
                    event.getWinnerId(), event.getAmount());
            

            
            if (event.wasWonWithoutShowdown()) {
                log.trace("Recording win without showdown for player {}", event.getWinnerId());
                
            }

            
            log.trace("Checking biggest win for player {}: {}", 
                    event.getWinnerId(), event.getAmount());
            

        } catch (Exception e) {
            log.error("Failed to process PotAwarded event for statistics: {}", event, e);
        }
    }

    
    @EventListener
    @Async
    public void onPlayerEliminated(PlayerEliminated event) {
        log.debug("Processing PlayerEliminated event: {} finished {}",
                event.getPlayerName(), event.getPositionDisplay());

        try {
            
            log.trace("Recording tournament finish for player {}: position {}",
                    event.getPlayerId(), event.getFinishPosition());
            
            
            
            
            

            
            if (event.hasEliminator()) {
                log.trace("Recording elimination: {} eliminated by {}",
                        event.getPlayerId(), event.getEliminatedByPlayerId());
                
                
                
                
            }

        } catch (Exception e) {
            log.error("Failed to process PlayerEliminated event for statistics: {}", event, e);
        }
    }
}
