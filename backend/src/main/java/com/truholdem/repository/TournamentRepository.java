package com.truholdem.repository;

import com.truholdem.model.Tournament;
import com.truholdem.model.Tournament.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByStatusIn(List<TournamentStatus> statuses);

    @Query("SELECT t FROM Tournament t WHERE t.status = 'REGISTERING' ORDER BY t.createdAt DESC")
    List<Tournament> findOpenTournaments();

    @Query("SELECT t FROM Tournament t WHERE t.status = 'RUNNING' ORDER BY t.startedAt DESC")
    List<Tournament> findRunningTournaments();

    List<Tournament> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Tournament t WHERE t.status = 'FINISHED' ORDER BY t.endedAt DESC")
    List<Tournament> findRecentlyFinished();
}
