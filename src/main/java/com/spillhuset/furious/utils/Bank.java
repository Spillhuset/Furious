package com.spillhuset.furious.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Bank {
    private final UUID id;
    private String name;
    // Multiple claimed chunk anchors
    private final List<Claim> claims = new ArrayList<>();
    private double interest; // interest rate in percent per period (no scheduler here)
    private BankType type = BankType.PLAYER; // default to player bank
    // Optional armor stand representing this bank's marker
    private UUID armorStandUuid; // nullable
    // Operational state: whether the bank is open for business
    private boolean open = true;

    public Bank(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.interest = 0.0d;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Backward compatible getters: return first claim if present
    public UUID getWorldId() { return isClaimed() ? claims.get(0).worldId : null; }
    public Integer getChunkX() { return isClaimed() ? claims.get(0).chunkX : null; }
    public Integer getChunkZ() { return isClaimed() ? claims.get(0).chunkZ : null; }

    public boolean isClaimed() { return !claims.isEmpty(); }

    public void claim(UUID worldId, int chunkX, int chunkZ) {
        addClaim(worldId, chunkX, chunkZ);
    }

    public void unclaim() {
        this.claims.clear();
    }

    public void addClaim(UUID worldId, int chunkX, int chunkZ) {
        if (worldId == null) return;
        Claim c = new Claim(worldId, chunkX, chunkZ);
        if (!claims.contains(c)) claims.add(c);
    }

    public boolean hasClaimAt(UUID worldId, int chunkX, int chunkZ) {
        if (worldId == null) return false;
        for (Claim c : claims) {
            if (c.worldId.equals(worldId) && c.chunkX == chunkX && c.chunkZ == chunkZ) return true;
        }
        return false;
    }

    public List<Claim> getClaims() { return new ArrayList<>(claims); }

    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = Math.max(0d, interest); }

    public BankType getType() { return type; }
    public void setType(BankType type) { if (type != null) this.type = type; }

    public UUID getArmorStandUuid() { return armorStandUuid; }
    public void setArmorStandUuid(UUID armorStandUuid) { this.armorStandUuid = armorStandUuid; }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    public static class Claim {
        public final UUID worldId;
        public final int chunkX;
        public final int chunkZ;
        public Claim(UUID worldId, int chunkX, int chunkZ) {
            this.worldId = worldId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Claim c)) return false;
            return chunkX == c.chunkX && chunkZ == c.chunkZ && Objects.equals(worldId, c.worldId);
        }
        @Override public int hashCode() { return Objects.hash(worldId, chunkX, chunkZ); }
    }
}
