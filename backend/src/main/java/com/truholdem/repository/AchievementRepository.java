package com.truholdem.repository;

import com.truholdem.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    Optional<Achievement> findByCode(String code);

    List<Achievement> findByCategory(String category);

    List<Achievement> findByIsHiddenFalse();

    List<Achievement> findByRequirementType(String requirementType);

    List<Achievement> findAllByOrderByPointsDesc();
}
