package com.truholdem.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Embeddable
public class BlindStructure {
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tournament_blind_levels",
        joinColumns = @JoinColumn(name = "tournament_id")
    )
    @OrderColumn(name = "level_order")
    private List<BlindLevel> levels = new ArrayList<>();
    
    @Column(name = "level_duration_minutes")
    private int levelDurationMinutes;
    
    
    protected BlindStructure() {}
    
    private BlindStructure(List<BlindLevel> levels, int levelDurationMinutes) {
        if (levels == null || levels.isEmpty()) {
            throw new IllegalArgumentException("Blind structure must have at least one level");
        }
        if (levelDurationMinutes < 1) {
            throw new IllegalArgumentException("Level duration must be at least 1 minute");
        }
        this.levels = new ArrayList<>(levels);
        this.levelDurationMinutes = levelDurationMinutes;
    }
    
    
    public static BlindStructure turbo() {
        List<BlindLevel> turboLevels = List.of(
            BlindLevel.of(1, 10, 20),
            BlindLevel.of(2, 15, 30),
            BlindLevel.of(3, 25, 50),
            BlindLevel.of(4, 50, 100),
            BlindLevel.of(5, 75, 150),
            BlindLevel.of(6, 100, 200),
            BlindLevel.withAnte(7, 150, 300, 25),
            BlindLevel.withAnte(8, 200, 400, 50),
            BlindLevel.withAnte(9, 300, 600, 75),
            BlindLevel.withAnte(10, 500, 1000, 100),
            BlindLevel.withAnte(11, 750, 1500, 150),
            BlindLevel.withAnte(12, 1000, 2000, 200)
        );
        return new BlindStructure(turboLevels, 5);
    }
    
    
    public static BlindStructure standard() {
        List<BlindLevel> standardLevels = List.of(
            BlindLevel.of(1, 10, 20),
            BlindLevel.of(2, 15, 30),
            BlindLevel.of(3, 20, 40),
            BlindLevel.of(4, 25, 50),
            BlindLevel.of(5, 50, 100),
            BlindLevel.of(6, 75, 150),
            BlindLevel.of(7, 100, 200),
            BlindLevel.withAnte(8, 150, 300, 25),
            BlindLevel.withAnte(9, 200, 400, 50),
            BlindLevel.withAnte(10, 300, 600, 75),
            BlindLevel.withAnte(11, 400, 800, 100),
            BlindLevel.withAnte(12, 500, 1000, 125),
            BlindLevel.withAnte(13, 750, 1500, 175),
            BlindLevel.withAnte(14, 1000, 2000, 250),
            BlindLevel.withAnte(15, 1500, 3000, 400)
        );
        return new BlindStructure(standardLevels, 15);
    }
    
    
    public static BlindStructure deep() {
        List<BlindLevel> deepLevels = List.of(
            BlindLevel.of(1, 10, 20),
            BlindLevel.of(2, 10, 25),
            BlindLevel.of(3, 15, 30),
            BlindLevel.of(4, 20, 40),
            BlindLevel.of(5, 25, 50),
            BlindLevel.withAnte(6, 50, 100, 10),
            BlindLevel.withAnte(7, 75, 150, 15),
            BlindLevel.withAnte(8, 100, 200, 25),
            BlindLevel.withAnte(9, 125, 250, 25),
            BlindLevel.withAnte(10, 150, 300, 50),
            BlindLevel.withAnte(11, 200, 400, 50),
            BlindLevel.withAnte(12, 250, 500, 75),
            BlindLevel.withAnte(13, 300, 600, 75),
            BlindLevel.withAnte(14, 400, 800, 100),
            BlindLevel.withAnte(15, 500, 1000, 125),
            BlindLevel.withAnte(16, 600, 1200, 150),
            BlindLevel.withAnte(17, 800, 1600, 200),
            BlindLevel.withAnte(18, 1000, 2000, 250)
        );
        return new BlindStructure(deepLevels, 20);
    }
    
    
    public static BlindStructure custom(List<BlindLevel> levels, int levelDurationMinutes) {
        return new BlindStructure(levels, levelDurationMinutes);
    }
    
    
    public BlindLevel getLevelAt(int levelNumber) {
        if (levelNumber < 1) {
            throw new IllegalArgumentException("Level number must be positive");
        }
        int index = Math.min(levelNumber - 1, levels.size() - 1);
        return levels.get(index);
    }
    
    
    public List<BlindLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }
    
    public int getLevelDurationMinutes() {
        return levelDurationMinutes;
    }
    
    
    public int getTotalLevels() {
        return levels.size();
    }
    
    
    public boolean hasMoreLevels(int currentLevel) {
        return currentLevel < levels.size();
    }
    
    
    public int getAnteStartLevel() {
        for (BlindLevel level : levels) {
            if (level.hasAnte()) {
                return level.getLevel();
            }
        }
        return -1;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlindStructure that = (BlindStructure) o;
        return levelDurationMinutes == that.levelDurationMinutes && 
               Objects.equals(levels, that.levels);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(levels, levelDurationMinutes);
    }
    
    @Override
    public String toString() {
        return String.format("BlindStructure{levels=%d, duration=%d min}", 
                             levels.size(), levelDurationMinutes);
    }
}
