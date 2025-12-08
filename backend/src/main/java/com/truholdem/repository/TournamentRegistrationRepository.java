package com.truholdem.repository;

import com.truholdem.model.TournamentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, UUID> {

    List<TournamentRegistration> findByTournamentId(UUID tournamentId);

    Optional<TournamentRegistration> findByTournamentIdAndPlayerName(UUID tournamentId, String playerName);

    Optional<TournamentRegistration> findByTournamentIdAndUserId(UUID tournamentId, UUID userId);

    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId AND r.isEliminated = false")
    List<TournamentRegistration> findActivePlayersByTournament(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId ORDER BY r.finishPosition ASC")
    List<TournamentRegistration> findByTournamentIdOrderByPosition(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT COUNT(r) FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId")
    int countByTournamentId(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT COUNT(r) FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId AND r.isEliminated = false")
    int countActiveByTournamentId(@Param("tournamentId") UUID tournamentId);

    boolean existsByTournamentIdAndPlayerName(UUID tournamentId, String playerName);

    
    List<TournamentRegistration> findByPlayerNameOrderByRegisteredAtDesc(String playerName);

    List<TournamentRegistration> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    
    @Query("SELECT r FROM TournamentRegistration r WHERE r.playerName = :playerName AND r.finishPosition = 1")
    List<TournamentRegistration> findWinsByPlayerName(@Param("playerName") String playerName);
}
