package com.truholdem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "side_pots")
public class SidePot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int amount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sidepot_eligible_players", joinColumns = @JoinColumn(name = "sidepot_id"))
    @Column(name = "player_id")
    private List<UUID> eligiblePlayerIds = new ArrayList<>();

    private int contributionPerPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    public SidePot() {
    }

    public SidePot(int amount, List<UUID> eligiblePlayerIds, int contributionPerPlayer) {
        this.amount = amount;
        this.eligiblePlayerIds = new ArrayList<>(eligiblePlayerIds);
        this.contributionPerPlayer = contributionPerPlayer;
    }

    public Long getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void addAmount(int additionalAmount) {
        this.amount += additionalAmount;
    }

    public List<UUID> getEligiblePlayerIds() {
        return eligiblePlayerIds;
    }

    public void setEligiblePlayerIds(List<UUID> eligiblePlayerIds) {
        this.eligiblePlayerIds = eligiblePlayerIds;
    }

    public void addEligiblePlayer(UUID playerId) {
        if (!eligiblePlayerIds.contains(playerId)) {
            eligiblePlayerIds.add(playerId);
        }
    }

    public boolean isEligible(UUID playerId) {
        return eligiblePlayerIds.contains(playerId);
    }

    public int getContributionPerPlayer() {
        return contributionPerPlayer;
    }

    public void setContributionPerPlayer(int contributionPerPlayer) {
        this.contributionPerPlayer = contributionPerPlayer;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public String toString() {
        return "SidePot{" +
                "id=" + id +
                ", amount=" + amount +
                ", eligiblePlayers=" + eligiblePlayerIds.size() +
                ", contributionPerPlayer=" + contributionPerPlayer +
                '}';
    }
}
