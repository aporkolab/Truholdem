package com.truholdem.repository;

import com.truholdem.model.RegistrationStatus;
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

    Optional<TournamentRegistration> findByTournamentIdAndPlayerId(UUID tournamentId, UUID playerId);

    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId AND r.status IN ('REGISTERED', 'PLAYING')")
    List<TournamentRegistration> findActivePlayersByTournament(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId ORDER BY r.finishPosition ASC NULLS LAST")
    List<TournamentRegistration> findByTournamentIdOrderByPosition(@Param("tournamentId") UUID tournamentId);
    
    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId ORDER BY r.currentChips DESC")
    List<TournamentRegistration> findByTournamentIdOrderByChipsDesc(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT COUNT(r) FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId")
    int countByTournamentId(@Param("tournamentId") UUID tournamentId);

    @Query("SELECT COUNT(r) FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId AND r.status IN ('REGISTERED', 'PLAYING')")
    int countActiveByTournamentId(@Param("tournamentId") UUID tournamentId);

    boolean existsByTournamentIdAndPlayerName(UUID tournamentId, String playerName);
    
    boolean existsByTournamentIdAndPlayerId(UUID tournamentId, UUID playerId);

    List<TournamentRegistration> findByPlayerNameOrderByRegisteredAtDesc(String playerName);

    List<TournamentRegistration> findByPlayerIdOrderByRegisteredAtDesc(UUID playerId);
    
    List<TournamentRegistration> findByStatus(RegistrationStatus status);

    @Query("SELECT r FROM TournamentRegistration r WHERE r.playerName = :playerName AND r.finishPosition = 1")
    List<TournamentRegistration> findWinsByPlayerName(@Param("playerName") String playerName);
    
    @Query("SELECT r FROM TournamentRegistration r WHERE r.playerId = :playerId AND r.finishPosition = 1")
    List<TournamentRegistration> findWinsByPlayerId(@Param("playerId") UUID playerId);
    
    
    @Query("SELECT SUM(r.prizeWon) FROM TournamentRegistration r WHERE r.playerId = :playerId")
    Long getTotalPrizesByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT SUM(r.bountiesCollected) FROM TournamentRegistration r WHERE r.playerId = :playerId")
    Long getTotalBountiesByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT AVG(r.finishPosition) FROM TournamentRegistration r WHERE r.playerId = :playerId AND r.finishPosition IS NOT NULL")
    Double getAverageFinishPositionByPlayerId(@Param("playerId") UUID playerId);
    
    @Query("SELECT r FROM TournamentRegistration r WHERE r.tournament.id = :tournamentId AND r.finishPosition <= :topN ORDER BY r.finishPosition ASC")
    List<TournamentRegistration> findTopFinishersByTournament(@Param("tournamentId") UUID tournamentId, @Param("topN") int topN);
}
