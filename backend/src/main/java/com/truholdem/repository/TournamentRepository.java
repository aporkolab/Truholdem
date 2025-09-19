package com.truholdem.repository;

import com.truholdem.model.Tournament;
import com.truholdem.model.TournamentStatus;
import com.truholdem.model.TournamentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByStatusIn(List<TournamentStatus> statuses);
    
    List<Tournament> findByTournamentType(TournamentType type);

    @Query("SELECT t FROM Tournament t WHERE t.status IN ('REGISTERING', 'LATE_REGISTRATION') ORDER BY t.createdAt DESC")
    List<Tournament> findOpenTournaments();

    @Query("SELECT t FROM Tournament t WHERE t.status IN ('RUNNING', 'FINAL_TABLE', 'HEADS_UP') ORDER BY t.startTime DESC")
    List<Tournament> findRunningTournaments();
    
    @Query("SELECT t FROM Tournament t WHERE t.status = 'PAUSED' ORDER BY t.startTime DESC")
    List<Tournament> findPausedTournaments();

    List<Tournament> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Tournament t WHERE t.status = 'COMPLETED' ORDER BY t.endTime DESC")
    List<Tournament> findRecentlyCompleted();
    
    @Query("SELECT t FROM Tournament t JOIN t.registrations r WHERE r.playerId = :playerId AND t.status IN ('RUNNING', 'FINAL_TABLE', 'HEADS_UP', 'PAUSED')")
    List<Tournament> findActiveByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT t FROM Tournament t JOIN t.registrations r WHERE r.playerId = :playerId ORDER BY t.createdAt DESC")
    List<Tournament> findByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT t FROM Tournament t WHERE t.startTime >= :from AND t.startTime < :to ORDER BY t.startTime ASC")
    List<Tournament> findScheduledBetween(@Param("from") Instant from, @Param("to") Instant to);
    
    @Query("SELECT t FROM Tournament t WHERE t.tournamentType = :type AND t.status IN ('REGISTERING', 'LATE_REGISTRATION') ORDER BY t.createdAt DESC")
    List<Tournament> findOpenByType(@Param("type") TournamentType type);
    
    @Query("SELECT COUNT(t) FROM Tournament t WHERE t.status IN ('RUNNING', 'FINAL_TABLE', 'HEADS_UP')")
    long countActiveTournaments();
    
    Optional<Tournament> findByIdAndStatus(UUID id, TournamentStatus status);
}
