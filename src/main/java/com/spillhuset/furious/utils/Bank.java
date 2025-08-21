package com.spillhuset.furious.utils;

import java.util.UUID;

public class Bank {
    private final UUID id;
    private String name;
    // Claimed chunk anchor
    private UUID worldId; // nullable if unclaimed
    private Integer chunkX; // nullable
    private Integer chunkZ; // nullable
    private double interest; // interest rate in percent per period (no scheduler here)
    private BankType type = BankType.PLAYER; // default to player bank
    // Optional armor stand representing this bank's marker
    private UUID armorStandUuid; // nullable

    public Bank(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.interest = 0.0d;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getWorldId() { return worldId; }
    public Integer getChunkX() { return chunkX; }
    public Integer getChunkZ() { return chunkZ; }

    public boolean isClaimed() { return worldId != null && chunkX != null && chunkZ != null; }

    public void claim(UUID worldId, int chunkX, int chunkZ) {
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void unclaim() {
        this.worldId = null;
        this.chunkX = null;
        this.chunkZ = null;
    }

    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = Math.max(0d, interest); }

    public BankType getType() { return type; }
    public void setType(BankType type) { if (type != null) this.type = type; }

    public UUID getArmorStandUuid() { return armorStandUuid; }
    public void setArmorStandUuid(UUID armorStandUuid) { this.armorStandUuid = armorStandUuid; }
}
