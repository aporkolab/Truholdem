package com.truholdem.repository;

import com.truholdem.model.HandHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface HandHistoryRepository extends JpaRepository<HandHistory, UUID> {

    
    List<HandHistory> findByGameIdOrderByHandNumberDesc(UUID gameId);

    
    Page<HandHistory> findByGameId(UUID gameId, Pageable pageable);

    
    @Query("SELECT h FROM HandHistory h JOIN h.players p WHERE p.playerId = :playerId ORDER BY h.playedAt DESC")
    List<HandHistory> findByPlayerId(@Param("playerId") UUID playerId);

    
    @Query("SELECT h FROM HandHistory h WHERE h.winnerName = :playerName ORDER BY h.playedAt DESC")
    List<HandHistory> findByWinnerName(@Param("playerName") String playerName);

    
    List<HandHistory> findTop50ByOrderByPlayedAtDesc();

    
    List<HandHistory> findByPlayedAtBetween(LocalDateTime start, LocalDateTime end);

    
    long countByGameId(UUID gameId);

    
    List<HandHistory> findTop10ByOrderByFinalPotDesc();

    
    void deleteByGameId(UUID gameId);
}
