package com.spillhuset.furious.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Shop {
    private final UUID id;
    private String name;

    // Claimed chunk anchor
    private UUID worldId;
    private int chunkX;
    private int chunkZ;
    private boolean claimed;

    // Global buy/sell toggles (can be used as defaults)
    private boolean buyEnabled;
    private boolean sellEnabled;
    private double buyPrice; // 0 means free when enabled
    private double sellPrice; // 0 means free when enabled

    // Optional spawn point for teleporting to this shop
    private UUID spawnWorldId;
    private Double spawnX;
    private Double spawnY;
    private Double spawnZ;
    private Float spawnYaw;
    private Float spawnPitch;

    // Optional ArmorStand marker to represent spawn (like warp)
    private UUID armorStandUuid;

    // Type of shop (player or guild)
    private ShopType type = ShopType.PLAYER;

    // Per-item configuration
    private final Map<String, ItemEntry> items = new HashMap<>();

    public Shop(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.buyEnabled = false;
        this.sellEnabled = false;
        this.buyPrice = 0d;
        this.sellPrice = 0d;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public void claim(UUID worldId, int chunkX, int chunkZ) {
        this.worldId = worldId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimed = true;
    }

    public void unclaim() {
        this.claimed = false;
        this.worldId = null;
    }

    public boolean isBuyEnabled() {
        return buyEnabled;
    }

    public boolean isSellEnabled() {
        return sellEnabled;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setBuyDisabled() {
        this.buyEnabled = false;
    }

    public void setSellDisabled() {
        this.sellEnabled = false;
    }

    public void setBuyPrice(double price) {
        this.buyEnabled = true;
        this.buyPrice = Math.max(0d, price);
    }

    public void setSellPrice(double price) {
        this.sellEnabled = true;
        this.sellPrice = Math.max(0d, price);
    }

    // Spawn getters/setters
    public boolean hasSpawn() {
        return spawnWorldId != null && spawnX != null;
    }

    public UUID getSpawnWorldId() {
        return spawnWorldId;
    }

    public Double getSpawnX() {
        return spawnX;
    }

    public Double getSpawnY() {
        return spawnY;
    }

    public Double getSpawnZ() {
        return spawnZ;
    }

    public Float getSpawnYaw() {
        return spawnYaw;
    }

    public Float getSpawnPitch() {
        return spawnPitch;
    }

    public void setSpawn(UUID worldId, double x, double y, double z, float yaw, float pitch) {
        this.spawnWorldId = worldId;
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.spawnYaw = yaw;
        this.spawnPitch = pitch;
    }

    public void clearSpawn() {
        this.spawnWorldId = null;
        this.spawnX = null;
        this.spawnY = null;
        this.spawnZ = null;
        this.spawnYaw = null;
        this.spawnPitch = null;
    }

    public UUID getArmorStandUuid() {
        return armorStandUuid;
    }

    public void setArmorStandUuid(UUID armorStandUuid) {
        this.armorStandUuid = armorStandUuid;
    }

    public ShopType getType() { return type; }
    public void setType(ShopType type) { if (type != null) this.type = type; }

    // Items map
    public Map<String, ItemEntry> getItems() {
        return items;
    }

    public ItemEntry getOrCreateItem(String key) {
        return items.computeIfAbsent(key.toUpperCase(), k -> new ItemEntry());
    }

    public void removeItem(String key) {
        if (key != null) items.remove(key.toUpperCase());
    }

    public static class ItemEntry {
        private int stock;
        private boolean buyEnabled;
        private boolean sellEnabled;
        private double buyPrice;
        private double sellPrice;

        public int getStock() {
            return stock;
        }

        public void setStock(int stock) {
            this.stock = Math.max(0, stock);
        }

        public boolean isBuyEnabled() {
            return buyEnabled;
        }

        public boolean isSellEnabled() {
            return sellEnabled;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public double getSellPrice() {
            return sellPrice;
        }

        public void setBuyDisabled() {
            this.buyEnabled = false;
        }

        public void setSellDisabled() {
            this.sellEnabled = false;
        }

        public void setBuyPrice(double price) {
            this.buyEnabled = true;
            this.buyPrice = Math.max(0d, price);
        }

        public void setSellPrice(double price) {
            this.sellEnabled = true;
            this.sellPrice = Math.max(0d, price);
        }
    }
}
