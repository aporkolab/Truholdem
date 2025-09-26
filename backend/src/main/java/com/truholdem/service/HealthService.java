package com.truholdem.service;

import com.truholdem.repository.GameRepository;
import com.truholdem.repository.TournamentRepository;
import com.truholdem.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Service layer for health check related queries.
 * Provides counts and statistics for health endpoints without
 * violating layered architecture by keeping repository access in service layer.
 */
@Service
public class HealthService {

    private final GameRepository gameRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    public HealthService(
            GameRepository gameRepository,
            TournamentRepository tournamentRepository,
            UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get the count of active games.
     * @return number of games in the database
     */
    public long getActiveGamesCount() {
        return gameRepository.count();
    }

    /**
     * Get the count of active tournaments.
     * @return number of tournaments in the database
     */
    public long getActiveTournamentsCount() {
        return tournamentRepository.count();
    }

    /**
     * Get the total number of users.
     * @return number of users in the database
     */
    public long getTotalUsersCount() {
        return userRepository.count();
    }
}
