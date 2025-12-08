package com.truholdem.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Embeddable
public class SidePot {

    private int amount;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<UUID> eligiblePlayerIds = new ArrayList<>();

    private int contributionPerPlayer;

    public SidePot() {
    }

    public SidePot(int amount, List<UUID> eligiblePlayerIds, int contributionPerPlayer) {
        this.amount = amount;
        this.eligiblePlayerIds = new ArrayList<>(eligiblePlayerIds);
        this.contributionPerPlayer = contributionPerPlayer;
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

    @Override
    public String toString() {
        return "SidePot{" +
                "amount=" + amount +
                ", eligiblePlayers=" + eligiblePlayerIds.size() +
                ", contributionPerPlayer=" + contributionPerPlayer +
                '}';
    }
}
