package com.truholdem.model;

import jakarta.persistence.*;
import java.util.UUID;


@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    private String icon;

    private String category;

    private int points = 0;

    private String requirementType;

    private int requirementValue;

    private boolean isHidden = false;

    
    public Achievement() {}

    public Achievement(String code, String name, String description, String icon, 
                       String category, int points, String requirementType, int requirementValue) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.points = points;
        this.requirementType = requirementType;
        this.requirementValue = requirementValue;
    }

    
    public boolean checkRequirement(PlayerStatistics stats) {
        if (stats == null || requirementType == null) return false;

        int currentValue = switch (requirementType) {
            case "HANDS_WON" -> stats.getHandsWon();
            case "HANDS_PLAYED" -> stats.getHandsPlayed();
            case "BIGGEST_POT" -> stats.getBiggestPotWon();
            case "WIN_STREAK" -> stats.getLongestWinStreak();
            case "ALL_INS_WON" -> stats.getAllInsWon();
            case "SHOWDOWNS_WON" -> stats.getShowdownsWon();
            case "TOTAL_SESSIONS" -> stats.getTotalSessions();
            default -> 0;
        };

        return currentValue >= requirementValue;
    }

    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getRequirementType() { return requirementType; }
    public void setRequirementType(String requirementType) { this.requirementType = requirementType; }

    public int getRequirementValue() { return requirementValue; }
    public void setRequirementValue(int requirementValue) { this.requirementValue = requirementValue; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }
}
