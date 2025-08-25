package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.Shop;
import com.spillhuset.furious.utils.ShopGuildItem;
import com.spillhuset.furious.utils.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopsService {
    private final Furious plugin;

    public ShopsService(Furious plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Shop> shopsById = new HashMap<>();
    private final Map<String, UUID> shopIdByName = new HashMap<>();

    private File shopsFile;
    private FileConfiguration shopsConfig;

    // Persistence
    public void load() {
        shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        try {
            if (!shopsFile.exists()) shopsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating shops.yml: " + e.getMessage());
        }
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
        shopsById.clear();
        shopIdByName.clear();
        ConfigurationSection root = shopsConfig.getConfigurationSection("shops");
        if (root != null) {
            for (String idKey : root.getKeys(false)) {
                ConfigurationSection ss = root.getConfigurationSection(idKey);
                if (ss == null) continue;
                try {
                    UUID id = UUID.fromString(idKey);
                    String name = ss.getString("name", idKey);
                    Shop s = new Shop(id, name);
                    // type
                    String typeStr = ss.getString("type", "PLAYER");
                    try {
                        s.setType(com.spillhuset.furious.utils.ShopType.valueOf(typeStr.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                    // open flag
                    try {
                        s.setOpen(ss.getBoolean("open", true));
                    } catch (Throwable ignored) {
                    }
                    // claims: load multi-claims if present; otherwise load legacy single claim
                    ConfigurationSection claimsSec = ss.getConfigurationSection("claims");
                    if (claimsSec != null) {
                        for (String key : claimsSec.getKeys(false)) {
                            ConfigurationSection c = claimsSec.getConfigurationSection(key);
                            if (c == null) continue;
                            String wStr = c.getString("world", null);
                            if (wStr == null) continue;
                            try {
                                UUID wid = UUID.fromString(wStr);
                                int cx = c.getInt("chunkX");
                                int cz = c.getInt("chunkZ");
                                s.addClaim(wid, cx, cz);
                            } catch (IllegalArgumentException ignored2) {
                            }
                        }
                    } else if (ss.getBoolean("claimed", false)) {
                        String wid = ss.getString("world", null);
                        if (wid != null) {
                            UUID worldId = UUID.fromString(wid);
                            int cx = ss.getInt("chunkX");
                            int cz = ss.getInt("chunkZ");
                            s.claim(worldId, cx, cz);
                        }
                    }
                    // global prices (optional)
                    if (ss.isSet("buy.enabled") && ss.getBoolean("buy.enabled"))
                        s.setBuyPrice(Math.max(0, ss.getDouble("buy.price", 0)));
                    if (ss.isSet("sell.enabled") && ss.getBoolean("sell.enabled"))
                        s.setSellPrice(Math.max(0, ss.getDouble("sell.price", 0)));
                    // spawn
                    ConfigurationSection sp = ss.getConfigurationSection("spawn");
                    if (sp != null) {
                        String w = sp.getString("world", null);
                        if (w != null) {
                            UUID wid = UUID.fromString(w);
                            double x = sp.getDouble("x");
                            double y = sp.getDouble("y");
                            double z = sp.getDouble("z");
                            float yaw = (float) sp.getDouble("yaw");
                            float pitch = (float) sp.getDouble("pitch");
                            s.setSpawn(wid, x, y, z, yaw, pitch);
                        }
                        String armor = sp.getString("armorStand", null);
                        if (armor != null) {
                            try {
                                s.setArmorStandUuid(UUID.fromString(armor));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    // items
                    ConfigurationSection items = ss.getConfigurationSection("items");
                    if (items != null) {
                        for (String key : items.getKeys(false)) {
                            ConfigurationSection is = items.getConfigurationSection(key);
                            if (is == null) continue;
                            Shop.ItemEntry e = s.getOrCreateItem(key);
                            e.setStock(is.getInt("stock", 0));
                            if (is.getBoolean("buy.enabled", false))
                                e.setBuyPrice(Math.max(0, is.getDouble("buy.price", 0)));
                            else e.setBuyDisabled();
                            if (is.getBoolean("sell.enabled", false))
                                e.setSellPrice(Math.max(0, is.getDouble("sell.price", 0)));
                            else e.setSellDisabled();
                        }
                    }
                    shopsById.put(id, s);
                    shopIdByName.put(name.toLowerCase(), id);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load shop " + idKey + ": " + ex.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection root = out.createSection("shops");
        for (Shop s : shopsById.values()) {
            ConfigurationSection ss = root.createSection(s.getId().toString());
            ss.set("name", s.getName());
            ss.set("type", s.getType().name());
            // open and claims
            ss.set("open", s.isOpen());
            ss.set("claimed", s.isClaimed());
            if (s.isClaimed()) {
                // legacy single-claim fields for backward compatibility
                ss.set("world", s.getWorldId().toString());
                ss.set("chunkX", s.getChunkX());
                ss.set("chunkZ", s.getChunkZ());
                // multi-claims list
                ss.set("claims", null);
                int idx = 0;
                for (com.spillhuset.furious.utils.Shop.Claim c : s.getClaims()) {
                    String cPath = "claims." + (idx++);
                    ss.set(cPath + ".world", c.worldId.toString());
                    ss.set(cPath + ".chunkX", c.chunkX);
                    ss.set(cPath + ".chunkZ", c.chunkZ);
                }
            }
            // global prices
            ss.set("buy.enabled", s.isBuyEnabled());
            ss.set("buy.price", Math.max(0, s.getBuyPrice()));
            ss.set("sell.enabled", s.isSellEnabled());
            ss.set("sell.price", Math.max(0, s.getSellPrice()));
            // spawn
            if (s.hasSpawn()) {
                ConfigurationSection sp = ss.createSection("spawn");
                sp.set("world", s.getSpawnWorldId().toString());
                sp.set("x", s.getSpawnX());
                sp.set("y", s.getSpawnY());
                sp.set("z", s.getSpawnZ());
                sp.set("yaw", s.getSpawnYaw());
                sp.set("pitch", s.getSpawnPitch());
                if (s.getArmorStandUuid() != null) sp.set("armorStand", s.getArmorStandUuid().toString());
            }
            // items
            if (!s.getItems().isEmpty()) {
                ConfigurationSection items = ss.createSection("items");
                for (Map.Entry<String, Shop.ItemEntry> e : s.getItems().entrySet()) {
                    ConfigurationSection is = items.createSection(e.getKey());
                    is.set("stock", e.getValue().getStock());
                    is.set("buy.enabled", e.getValue().isBuyEnabled());
                    is.set("buy.price", Math.max(0, e.getValue().getBuyPrice()));
                    is.set("sell.enabled", e.getValue().isSellEnabled());
                    is.set("sell.price", Math.max(0, e.getValue().getSellPrice()));
                }
            }
        }
        shopsConfig = out;
        try {
            out.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving shops.yml: " + e.getMessage());
        }
    }

    private String buildArmorStandName(Shop shop) {
        return "Shop: " + shop.getName();
    }

    private void applyArmorStandVisibility(Shop shop) {
        if (shop.getArmorStandUuid() == null) return;
        Entity ent = plugin.getServer().getEntity(shop.getArmorStandUuid());
        if (!(ent instanceof ArmorStand stand)) return;
        try {
            stand.setCustomNameVisible(true);
        } catch (Throwable ignored) {
        }
        try {
            stand.customName(net.kyori.adventure.text.Component.text(buildArmorStandName(shop)));
        } catch (Throwable ignored) {
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                if (p.isOp()) p.showEntity(plugin, stand);
                else p.hideEntity(plugin, stand);
            } catch (Throwable ignored) {
            }
        }
    }

    public void applyShopArmorStandVisibilityForViewer(org.bukkit.entity.Player viewer) {
        if (viewer == null) return;
        try {
            for (Shop shop : new java.util.ArrayList<>(shopsById.values())) {
                java.util.UUID asId = shop.getArmorStandUuid();
                if (asId == null) continue;
                org.bukkit.entity.Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                    if (viewer.isOp()) viewer.showEntity(plugin, stand);
                    else viewer.hideEntity(plugin, stand);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void spawnArmorStandForShop(Shop shop) {
        if (!shop.hasSpawn()) return;
        var world = Bukkit.getWorld(shop.getSpawnWorldId());
        if (world == null) return;
        Location loc = new Location(world, shop.getSpawnX(), shop.getSpawnY(), shop.getSpawnZ(), shop.getSpawnYaw(), shop.getSpawnPitch());
        try {
            java.util.UUID id = plugin.armorStandManager.create(loc, buildArmorStandName(shop));
            if (id != null) {
                shop.setArmorStandUuid(id);
                try {
                    plugin.armorStandManager.register(id, () -> removeByArmorStand(id));
                } catch (Throwable ignored) {
                }
                applyArmorStandVisibility(shop);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to spawn ArmorStand for shop: " + t.getMessage());
        }
    }

    public void ensureArmorStands() {
        for (Shop shop : new ArrayList<>(shopsById.values())) {
            if (!shop.hasSpawn()) continue;
            UUID asId = shop.getArmorStandUuid();
            Entity ent = asId != null ? plugin.getServer().getEntity(asId) : null;
            if (!(ent instanceof ArmorStand)) {
                spawnArmorStandForShop(shop);
            } else {
                try {
                    plugin.armorStandManager.register(ent.getUniqueId(), () -> removeByArmorStand(ent.getUniqueId()));
                } catch (Throwable ignored) {
                }
                applyArmorStandVisibility(shop);
            }
        }
    }

    // Remove spawn anchor for a shop: delete ArmorStand and clear stored spawn
    public boolean removeSpawn(CommandSender sender, String shopName) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        UUID asId = s.getArmorStandUuid();
        // Unregister and remove the armor stand entity if present
        try {
            if (asId != null) {
                try { plugin.armorStandManager.unregister(asId); } catch (Throwable ignored) {}
                Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof ArmorStand) {
                    try { ent.remove(); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {
        }
        // Clear spawn and armor stand references
        s.clearSpawn();
        s.setArmorStandUuid(null);
        save();
        Components.sendSuccess(sender, Components.t("Spawn anchor removed for shop "), Components.valueComp(s.getName()));
        return true;
    }

    /**
     * Attempt to adopt an unreferenced, managed ArmorStand into a matching Shop by proximity to its spawn.
     */
    public boolean adoptArmorStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null || stand.getWorld() == null) return false;
        org.bukkit.Location sLoc = stand.getLocation();
        try {
            for (com.spillhuset.furious.utils.Shop shop : new java.util.ArrayList<>(shopsById.values())) {
                if (!shop.hasSpawn()) continue;
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(shop.getSpawnWorldId());
                if (w == null) continue;
                org.bukkit.Location sp = new org.bukkit.Location(w, shop.getSpawnX(), shop.getSpawnY(), shop.getSpawnZ(), shop.getSpawnYaw(), shop.getSpawnPitch());
                if (!w.equals(sLoc.getWorld())) continue;
                if (sp.distanceSquared(sLoc) <= 4.0) {
                    if (stand.getUniqueId().equals(shop.getArmorStandUuid())) return true;
                    shop.setArmorStandUuid(stand.getUniqueId());
                    try { plugin.armorStandManager.register(stand.getUniqueId(), () -> removeByArmorStand(stand.getUniqueId())); } catch (Throwable ignored) {}
                    applyArmorStandVisibility(shop);
                    save();
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean hasArmorStand(java.util.UUID armorStandId) {
        if (armorStandId == null) return false;
        for (Shop shop : new java.util.ArrayList<>(shopsById.values())) {
            if (armorStandId.equals(shop.getArmorStandUuid())) return true;
        }
        return false;
    }

    public boolean removeByArmorStand(UUID armorStandId) {
        boolean changed = false;
        for (Shop shop : new ArrayList<>(shopsById.values())) {
            if (armorStandId.equals(shop.getArmorStandUuid())) {
                // remove spawn association
                // clear spawn (treat same as warp removal/destroyed)
                shop.clearSpawn();
                shop.setArmorStandUuid(null);
                changed = true;
            }
        }
        if (changed) save();
        return changed;
    }

    public Shop getShopByName(String name) {
        if (name == null) return null;
        UUID id = shopIdByName.get(name.toLowerCase());
        return id == null ? null : shopsById.get(id);
    }

    public Shop ensureShop(String name) {
        Shop s = getShopByName(name);
        if (s != null) return s;
        UUID id = UUID.randomUUID();
        s = new Shop(id, name);
        shopsById.put(id, s);
        shopIdByName.put(name.toLowerCase(), id);
        save();
        return s;
    }

    // Admin ops
    public boolean createShop(CommandSender sender, String name) {
        if (name == null || name.isBlank()) {
            Components.sendErrorMessage(sender, "Name required.");
            return false;
        }
        if (getShopByName(name) != null) {
            Components.sendErrorMessage(sender, "Shop already exists.");
            return false;
        }
        ensureShop(name);
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(name), Components.t(" created."));
        return true;
    }

    public boolean deleteShop(CommandSender sender, String name) {
        Shop s = getShopByName(name);
        if (s == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        // remove armor stand if present
        try {
            if (s.getArmorStandUuid() != null) {
                try {
                    plugin.armorStandManager.unregister(s.getArmorStandUuid());
                } catch (Throwable ignored) {
                }
                Entity ent = plugin.getServer().getEntity(s.getArmorStandUuid());
                if (ent instanceof ArmorStand) ent.remove();
            }
        } catch (Throwable ignored) {
        }
        shopsById.remove(s.getId());
        shopIdByName.remove(s.getName().toLowerCase());
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(name), Components.t(" deleted."));
        return true;
    }

    public boolean setSpawn(CommandSender sender, String shopName, Location loc) {
        Shop s = ensureShop(shopName);
        if (loc.getWorld() == null) {
            Components.sendErrorMessage(sender, "Invalid world.");
            return false;
        }
        s.setSpawn(loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // spawn or move armor stand
        try {
            if (s.getArmorStandUuid() != null) {
                Entity ent = plugin.getServer().getEntity(s.getArmorStandUuid());
                if (ent instanceof ArmorStand) {
                    ent.teleportAsync(loc);
                    try {
                        applyArmorStandVisibility(s);
                    } catch (Throwable ignored) {
                    }
                } else {
                    spawnArmorStandForShop(s);
                }
            } else {
                spawnArmorStandForShop(s);
            }
        } catch (Throwable ignored) {
        }
        // Verify presence of the ArmorStand after attempting to create/move it; retry once if missing
        boolean hasStand = false;
        try {
            if (s.getArmorStandUuid() != null) {
                Entity ent2 = plugin.getServer().getEntity(s.getArmorStandUuid());
                hasStand = ent2 instanceof ArmorStand;
            }
            if (!hasStand) {
                spawnArmorStandForShop(s);
                if (s.getArmorStandUuid() != null) {
                    Entity ent3 = plugin.getServer().getEntity(s.getArmorStandUuid());
                    hasStand = ent3 instanceof ArmorStand;
                }
            }
        } catch (Throwable ignored) {
        }
        save();
        if (hasStand) {
            Components.sendSuccess(sender, Components.t("Spawn set for shop "), Components.valueComp(s.getName()), Components.t(" (armor stand created)"));
        } else {
            Components.sendSuccess(sender, Components.t("Spawn set for shop "), Components.valueComp(s.getName()), Components.t(" (armor stand not yet visible; it will appear shortly once the area finishes loading)"));
            // Best-effort ensure on next tick so chunk/entity systems have settled
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        if (plugin.armorStandManager != null) plugin.armorStandManager.ensureArmorStands();
                    } catch (Throwable ignored) {
                    }
                });
            } catch (Throwable ignored) {
            }
        }
        return hasStand;
    }

    // Helper: compute center location of the first claimed chunk for a shop (safe Y)
    private Location firstClaimCenterLocation(Shop shop, org.bukkit.World world) {
        if (shop == null || world == null || !shop.isClaimed()) return null;
        Integer cX = shop.getChunkX();
        Integer cZ = shop.getChunkZ();
        if (cX == null || cZ == null) return null;
        int bx = cX * 16 + 7;
        int bz = cZ * 16 + 7;
        int by;
        try {
            by = world.getHighestBlockYAt(bx, bz) + 1;
        } catch (Throwable ignored) {
            by = world.getSpawnLocation().getBlockY();
        }
        return new Location(world, bx + 0.5, by, bz + 0.5);
    }

    public boolean teleport(Player player, String shopName) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(player, "Shop not found.");
            return false;
        }
        // Prefer explicit spawn if set and world is available
        if (s.hasSpawn()) {
            org.bukkit.World w = Bukkit.getWorld(s.getSpawnWorldId());
            if (w != null) {
                Location to = new Location(w, s.getSpawnX(), s.getSpawnY(), s.getSpawnZ(), s.getSpawnYaw(), s.getSpawnPitch());
                // Preserve player's current look direction if spawn has no yaw/pitch
                if (to.getYaw() == 0 && to.getPitch() == 0) {
                    to.setYaw(player.getLocation().getYaw());
                    to.setPitch(player.getLocation().getPitch());
                }
                try {
                    plugin.teleportsService.queueTeleport(player, to, "Shop: " + s.getName());
                } catch (Throwable t) {
                    // Fallback to direct teleport if teleportsService not available
                    player.teleport(to);
                }
                Components.sendSuccess(player, Components.t("Teleported to shop "), Components.valueComp(s.getName()));
                return true;
            }
            // Spawn set but world missing: fall through to claim-based fallback
        }
        // Fallback: center of the first claimed chunk
        if (!s.isClaimed()) {
            Components.sendErrorMessage(player, "Shop has no spawn and no claimed chunks.");
            return false;
        }
        org.bukkit.World world = Bukkit.getWorld(s.getWorldId());
        if (world == null) {
            Components.sendErrorMessage(player, "Shop world is not loaded.");
            return false;
        }
        Location base = firstClaimCenterLocation(s, world);
        if (base == null) {
            Components.sendErrorMessage(player, "Shop has no valid claim to teleport to.");
            return false;
        }
        Location target = new Location(base.getWorld(), base.getX(), base.getY(), base.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
        try {
            plugin.teleportsService.queueTeleport(player, target, "Shop: " + s.getName());
        } catch (Throwable t) {
            player.teleport(target);
        }
        Components.sendInfo(player, Components.t("Shop spawn not set or world missing; teleported to the shop's claimed area."));
        return true;
    }

    public List<String> suggestShopNames(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String n : shopIdByName.keySet()) if (n.startsWith(p)) out.add(n);
        return out;
    }

    public enum ClaimCheck {OK, NOT_IN_CLAIM, WRONG_GUILD_TYPE}

    public ClaimCheck canClaimHere(Location loc) {
        if (loc == null) return ClaimCheck.NOT_IN_CLAIM;
        UUID worldId = loc.getWorld() == null ? null : loc.getWorld().getUID();
        if (worldId == null) return ClaimCheck.NOT_IN_CLAIM;
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        UUID ownerGid = plugin.guildService.getClaimOwner(worldId, cx, cz);
        if (ownerGid == null) return ClaimCheck.NOT_IN_CLAIM;
        Guild g = plugin.guildService.getGuildById(ownerGid);
        if (g == null) return ClaimCheck.NOT_IN_CLAIM;
        GuildType type = g.getType();
        if (type == GuildType.SAFE || type == GuildType.WAR) return ClaimCheck.OK;
        return ClaimCheck.WRONG_GUILD_TYPE;
    }

    public boolean claimShop(CommandSender sender, String shopName, Location loc) {
        Shop shop = ensureShop(shopName);
        ClaimCheck check = canClaimHere(loc);
        if (check != ClaimCheck.OK) {
            switch (check) {
                case NOT_IN_CLAIM -> Components.sendErrorMessage(sender, "This chunk is not claimed by a guild.");
                case WRONG_GUILD_TYPE ->
                        Components.sendErrorMessage(sender, "Shop claims must be inside SAFE or WAR guild.");
            }
            return false;
        }
        Chunk chunk = loc.getChunk();
        UUID wid = loc.getWorld().getUID();
        if (shop.hasClaimAt(wid, chunk.getX(), chunk.getZ())) {
            Components.sendErrorMessage(sender, "This chunk is already claimed by this shop.");
            return false;
        }
        shop.addClaim(wid, chunk.getX(), chunk.getZ());
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(shop.getName()), Components.t(" claimed at this chunk."));
        return true;
    }

    public boolean unclaimShop(CommandSender sender, String shopName) {
        Shop shop = getShopByName(shopName);
        if (shop == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        shop.unclaim();
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(shop.getName()), Components.t(" unclaimed."));
        return true;
    }

    public boolean setType(CommandSender sender, String shopName, com.spillhuset.furious.utils.ShopType type) {
        Shop shop = getShopByName(shopName);
        if (shop == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        if (type == null) {
            Components.sendErrorMessage(sender, "Invalid shop type.");
            return false;
        }
        shop.setType(type);
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(shop.getName()), Components.t(" type set to "), Components.valueComp(type.name().toLowerCase()), Components.t("."));
        return true;
    }

    public boolean setBuyPrice(CommandSender sender, String shopName, String priceStr) {
        Shop shop = ensureShop(shopName);
        if (priceStr.equals("-")) {
            shop.setBuyDisabled();
            Components.sendSuccess(sender, Components.t("Buy disabled for shop "), Components.valueComp(shop.getName()));
            return true;
        }
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable.");
                return false;
            }
            shop.setBuyPrice(price);
            Components.sendSuccess(sender, Components.t("Buy price for shop "), Components.valueComp(shop.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(price)));
            save();
            return true;
        } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid price. Use a number, 0 for free, or '-' to disable.");
            return false;
        }
    }

    public boolean setSellPrice(CommandSender sender, String shopName, String priceStr) {
        Shop shop = ensureShop(shopName);
        if (priceStr.equals("-")) {
            shop.setSellDisabled();
            Components.sendSuccess(sender, Components.t("Sell disabled for shop "), Components.valueComp(shop.getName()));
            return true;
        }
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable.");
                return false;
            }
            shop.setSellPrice(price);
            Components.sendSuccess(sender, Components.t("Sell price for shop "), Components.valueComp(shop.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(price)));
            save();
            return true;
        } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid price. Use a number, 0 for free, or '-' to disable.");
            return false;
        }
    }

    public boolean setOpen(CommandSender sender, String shopName, boolean open) {
        Shop shop = getShopByName(shopName);
        if (shop == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        shop.setOpen(open);
        save();
        Components.sendSuccess(sender,
                Components.t("Shop "), Components.valueComp(shop.getName()),
                Components.t(open ? " is now open." : " is now closed."));
        return true;
    }

    // Player-facing features
    public void listShops(CommandSender sender) {
        if (shopIdByName.isEmpty()) {
            Components.sendInfoMessage(sender, "No shops available.");
            return;
        }
        // Build a list of shops and sort by display name (case-insensitive)
        List<Shop> list = new ArrayList<>();
        for (UUID id : shopsById.keySet()) {
            Shop s = shopsById.get(id);
            if (s != null) list.add(s);
        }
        list.sort(Comparator.comparing(o -> o.getName() == null ? "" : o.getName().toLowerCase()));
        for (Shop s : list) {
            String guildsStr = "Unclaimed";
            if (s.isClaimed()) {
                Set<String> guildNames = new LinkedHashSet<>();
                for (com.spillhuset.furious.utils.Shop.Claim c : s.getClaims()) {
                    UUID owner = plugin.guildService.getClaimOwner(c.worldId, c.chunkX, c.chunkZ);
                    if (owner != null) {
                        Guild g = plugin.guildService.getGuildById(owner);
                        if (g != null && g.getName() != null) guildNames.add(g.getName());
                        else guildNames.add(owner.toString());
                    }
                }
                if (guildNames.isEmpty()) guildsStr = "Not in guild territory";
                else guildsStr = String.join(", ", guildNames);
            }
            String typeStr = (s.getType() == com.spillhuset.furious.utils.ShopType.GUILD) ? "guild" : "player";
            Components.sendInfo(sender,
                    Components.t("Shop "), Components.valueComp(s.getName()),
                    Components.t(" | type: "), Components.t(typeStr, net.kyori.adventure.text.format.NamedTextColor.AQUA),
                    Components.t(" | guild(s): "), Components.t(guildsStr, net.kyori.adventure.text.format.NamedTextColor.GOLD)
            );
        }
    }

    private Material parseMaterial(String raw) {
        if (raw == null) return null;
        try {
            return Material.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean addItem(CommandSender sender, String shopName, String matName, int stock, String buyStr, String sellStr) {
        Shop s = ensureShop(shopName);
        String key;
        if (s.getType() == ShopType.GUILD) {
            // Only allow items from ShopGuildItem enum
            try {
                key = ShopGuildItem.valueOf(matName.toUpperCase()).name();
            } catch (IllegalArgumentException ex) {
                Components.sendErrorMessage(sender, "Unknown guild item: " + matName + ". Valid: " + java.util.Arrays.toString(ShopGuildItem.values()));
                return false;
            }
        } else {
            Material mat = parseMaterial(matName);
            if (mat == null) {
                Components.sendErrorMessage(sender, "Unknown material: " + matName);
                return false;
            }
            key = mat.name();
        }
        Shop.ItemEntry e = s.getOrCreateItem(key);
        e.setStock(e.getStock() + Math.max(0, stock));
        // Buy price parsing
        if (buyStr != null) {
            if (buyStr.equals("-")) e.setBuyDisabled();
            else {
                try {
                    double p = Double.parseDouble(buyStr);
                    if (p < 0) {
                        Components.sendErrorMessage(sender, "Buy price must be >= 0 or '-' to disable.");
                        return false;
                    }
                    e.setBuyPrice(p);
                } catch (NumberFormatException ex) {
                    Components.sendErrorMessage(sender, "Invalid buy price.");
                    return false;
                }
            }
        }
        // Sell price parsing
        if (sellStr != null) {
            if (sellStr.equals("-")) e.setSellDisabled();
            else {
                try {
                    double p = Double.parseDouble(sellStr);
                    if (p < 0) {
                        Components.sendErrorMessage(sender, "Sell price must be >= 0 or '-' to disable.");
                        return false;
                    }
                    e.setSellPrice(p);
                } catch (NumberFormatException ex) {
                    Components.sendErrorMessage(sender, "Invalid sell price.");
                    return false;
                }
            }
        }
        Components.sendSuccess(sender, Components.t("Item "), Components.valueComp(key), Components.t(" added/updated in shop "), Components.valueComp(s.getName()));
        save();
        return true;
    }

    public boolean setItemBuyPrice(CommandSender sender, String shopName, String matName, String priceStr) {
        Shop s = ensureShop(shopName);
        String key;
        if (s.getType() == ShopType.GUILD) {
            try {
                key = ShopGuildItem.valueOf(matName.toUpperCase()).name();
            } catch (IllegalArgumentException ex) {
                Components.sendErrorMessage(sender, "Unknown guild item: " + matName + ". Valid: " + java.util.Arrays.toString(ShopGuildItem.values()));
                return false;
            }
        } else {
            Material mat = parseMaterial(matName);
            if (mat == null) {
                Components.sendErrorMessage(sender, "Unknown material: " + matName);
                return false;
            }
            key = mat.name();
        }
        Shop.ItemEntry e = s.getOrCreateItem(key);
        if (priceStr.equals("-")) {
            e.setBuyDisabled();
            Components.sendSuccess(sender, Components.t("Buy disabled for item "), Components.valueComp(key), Components.t(" in shop "), Components.valueComp(s.getName()));
            save();
            return true;
        }
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable.");
                return false;
            }
            e.setBuyPrice(price);
            Components.sendSuccess(sender, Components.t("Buy price for item "), Components.valueComp(key), Components.t(" in shop "), Components.valueComp(s.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(price)));
            save();
            return true;
        } catch (NumberFormatException e1) {
            Components.sendErrorMessage(sender, "Invalid price. Use a number, 0 for free, or '-' to disable.");
            return false;
        }
    }

    public boolean setItemSellPrice(CommandSender sender, String shopName, String matName, String priceStr) {
        Shop s = ensureShop(shopName);
        String key;
        if (s.getType() == ShopType.GUILD) {
            try {
                key = ShopGuildItem.valueOf(matName.toUpperCase()).name();
            } catch (IllegalArgumentException ex) {
                Components.sendErrorMessage(sender, "Unknown guild item: " + matName + ". Valid: " + java.util.Arrays.toString(ShopGuildItem.values()));
                return false;
            }
        } else {
            Material mat = parseMaterial(matName);
            if (mat == null) {
                Components.sendErrorMessage(sender, "Unknown material: " + matName);
                return false;
            }
            key = mat.name();
        }
        Shop.ItemEntry e = s.getOrCreateItem(key);
        if (priceStr.equals("-")) {
            e.setSellDisabled();
            Components.sendSuccess(sender, Components.t("Sell disabled for item "), Components.valueComp(key), Components.t(" in shop "), Components.valueComp(s.getName()));
            save();
            return true;
        }
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable.");
                return false;
            }
            e.setSellPrice(price);
            Components.sendSuccess(sender, Components.t("Sell price for item "), Components.valueComp(key), Components.t(" in shop "), Components.valueComp(s.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(price)));
            save();
            return true;
        } catch (NumberFormatException e1) {
            Components.sendErrorMessage(sender, "Invalid price. Use a number, 0 for free, or '-' to disable.");
            return false;
        }
    }

    public boolean removeItem(CommandSender sender, String shopName, String matName) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return false;
        }
        s.removeItem(matName);
        save();
        Components.sendSuccess(sender, Components.t("Item removed from shop "), Components.valueComp(s.getName()));
        return true;
    }

    public void listItems(CommandSender sender, String shopName) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(sender, "Shop not found.");
            return;
        }
        if (s.getItems().isEmpty()) {
            Components.sendInfoMessage(sender, "No items configured.");
            return;
        }
        int count = s.getItems().size();
        Components.sendInfo(sender, Components.t("Items for shop "), Components.valueComp(s.getName()), Components.t(" (" + count + "):"));
        for (Map.Entry<String, Shop.ItemEntry> ent : s.getItems().entrySet()) {
            Shop.ItemEntry e = ent.getValue();
            // Buy component
            net.kyori.adventure.text.Component buyComp;
            if (e.isBuyEnabled()) {
                if (e.getBuyPrice() == 0)
                    buyComp = Components.t("FREE", net.kyori.adventure.text.format.NamedTextColor.AQUA);
                else
                    buyComp = Components.t(plugin.walletService.formatAmount(e.getBuyPrice()), net.kyori.adventure.text.format.NamedTextColor.AQUA);
            } else {
                buyComp = Components.t("disabled", net.kyori.adventure.text.format.NamedTextColor.GRAY);
            }
            // Sell component
            net.kyori.adventure.text.Component sellComp;
            if (e.isSellEnabled()) {
                if (e.getSellPrice() == 0)
                    sellComp = Components.t("FREE", net.kyori.adventure.text.format.NamedTextColor.AQUA);
                else
                    sellComp = Components.t(plugin.walletService.formatAmount(e.getSellPrice()), net.kyori.adventure.text.format.NamedTextColor.AQUA);
            } else {
                sellComp = Components.t("disabled", net.kyori.adventure.text.format.NamedTextColor.GRAY);
            }
            Components.sendInfo(sender,
                    Components.t(" - "),
                    Components.valueComp(ent.getKey()),
                    Components.t(" | stock: "), Components.t(String.valueOf(e.getStock()), net.kyori.adventure.text.format.NamedTextColor.GOLD),
                    Components.t(" | buy: "), buyComp,
                    Components.t(" | sell: "), sellComp
            );
        }
    }

    public boolean stock(CommandSender sender, String shopName, String matName, int amount) {
        if (amount < 0) {
            Components.sendErrorMessage(sender, "Amount must be >= 0.");
            return false;
        }
        Shop s = ensureShop(shopName);
        String key;
        if (s.getType() == ShopType.GUILD) {
            try { key = ShopGuildItem.valueOf(matName.toUpperCase()).name(); }
            catch (IllegalArgumentException ex) { Components.sendErrorMessage(sender, "Unknown guild item: " + matName); return false; }
        } else {
            Material mat = parseMaterial(matName);
            if (mat == null) { Components.sendErrorMessage(sender, "Unknown material: " + matName); return false; }
            key = mat.name();
        }
        Shop.ItemEntry e = s.getOrCreateItem(key);
        e.setStock(amount);
        save();
        Components.sendSuccess(sender, Components.t("Stock for "), Components.valueComp(key), Components.t(" in shop "), Components.valueComp(s.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(amount)));
        return true;
    }

    public boolean restock(CommandSender sender, String shopName, String matName, int amount) {
        if (amount <= 0) {
            Components.sendErrorMessage(sender, "Amount must be > 0.");
            return false;
        }
        Shop s = ensureShop(shopName);
        String key;
        if (s.getType() == ShopType.GUILD) {
            try { key = ShopGuildItem.valueOf(matName.toUpperCase()).name(); }
            catch (IllegalArgumentException ex) { Components.sendErrorMessage(sender, "Unknown guild item: " + matName); return false; }
        } else {
            Material mat = parseMaterial(matName);
            if (mat == null) { Components.sendErrorMessage(sender, "Unknown material: " + matName); return false; }
            key = mat.name();
        }
        Shop.ItemEntry e = s.getOrCreateItem(key);
        e.setStock(e.getStock() + amount);
        save();
        Components.sendSuccess(sender, Components.t("Restocked "), Components.valueComp(key), Components.t(" by "), Components.valueComp(String.valueOf(amount)), Components.t(". New stock: "), Components.valueComp(String.valueOf(e.getStock())));
        return true;
    }

    public boolean buy(Player player, String shopName, String matName, int amount) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(player, "Shop not found.");
            return false;
        }
        if (!s.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This shop is currently closed.");
            return false;
        }
        Shop.ItemEntry e = s.getItems().get(matName.toUpperCase());
        if (e == null) {
            Components.sendErrorMessage(player, "Item not available.");
            return false;
        }
        if (!e.isBuyEnabled()) {
            Components.sendErrorMessage(player, "Buying disabled for this item.");
            return false;
        }
        if (amount <= 0) amount = 1;
        if (e.getStock() < amount) {
            Components.sendErrorMessage(player, "Not enough stock.");
            return false;
        }
        double total = e.getBuyPrice() * amount;
        if (e.getBuyPrice() > 0) {
            boolean ok = plugin.walletService.subBalance(player.getUniqueId(), total, "Shop buy: " + matName + " x" + amount + " from " + shopName);
            if (!ok) {
                Components.sendErrorMessage(player, "Insufficient funds.");
                return false;
            }
        }
        Material mat = parseMaterial(matName);
        if (mat == null) {
            Components.sendErrorMessage(player, "Invalid material.");
            return false;
        }
        HashMap<Integer, ItemStack> rem = player.getInventory().addItem(new ItemStack(mat, amount));
        if (!rem.isEmpty()) {
            // Inventory full; revert wallet charge if any
            if (e.getBuyPrice() > 0)
                plugin.walletService.addBalance(player.getUniqueId(), total, "Refund: inventory full");
            Components.sendErrorMessage(player, "Not enough inventory space.");
            return false;
        }
        e.setStock(e.getStock() - amount);
        Components.sendSuccess(player, Components.t("Purchased "), Components.valueComp(mat.name()), Components.t(" x" + amount + " for "), Components.valueComp(plugin.walletService.formatAmount(total)));
        return true;
    }

    public boolean sell(Player player, String shopName, String matName, int amount) {
        Shop s = getShopByName(shopName);
        if (s == null) {
            Components.sendErrorMessage(player, "Shop not found.");
            return false;
        }
        if (!s.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This shop is currently closed.");
            return false;
        }
        Shop.ItemEntry e = s.getItems().get(matName.toUpperCase());
        if (e == null) {
            Components.sendErrorMessage(player, "Item not accepted.");
            return false;
        }
        if (!e.isSellEnabled()) {
            Components.sendErrorMessage(player, "Selling disabled for this item.");
            return false;
        }
        if (amount <= 0) amount = 1;
        Material mat = parseMaterial(matName);
        if (mat == null) {
            Components.sendErrorMessage(player, "Invalid material.");
            return false;
        }
        ItemStack take = new ItemStack(mat, amount);
        HashMap<Integer, ItemStack> notRemoved = player.getInventory().removeItem(take);
        if (!notRemoved.isEmpty()) {
            Components.sendErrorMessage(player, "You don't have enough items.");
            return false;
        }
        double total = e.getSellPrice() * amount;
        if (e.getSellPrice() > 0) {
            plugin.walletService.addBalance(player.getUniqueId(), total, "Shop sell: " + matName + " x" + amount + " to " + shopName);
        }
        e.setStock(e.getStock() + amount);
        Components.sendSuccess(player, Components.t("Sold "), Components.valueComp(mat.name()), Components.t(" x" + amount + " for "), Components.valueComp(plugin.walletService.formatAmount(total)));
        return true;
    }

    // Find the shop that has claimed the chunk at the given location, if any
    public Shop getShopAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        java.util.UUID worldId = loc.getWorld().getUID();
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        for (Shop shop : shopsById.values()) {
            if (shop != null && shop.isClaimed()) {
                if (shop.hasClaimAt(worldId, cx, cz)) {
                    return shop;
                }
            }
        }
        return null;
    }

    // Guild shop buy flow: request (show price) and confirm (charge and apply)
    public boolean buyGuildRequest(CommandSender sender, String shopName, String itemRaw) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(sender, "Shop not found."); return false; }
        if (s.getType() != ShopType.GUILD) { Components.sendErrorMessage(sender, "This is not a guild shop."); return false; }
        if (!s.isOpen() && !(sender instanceof Player p && p.isOp())) {
            Components.sendErrorMessage(sender, "This shop is currently closed.");
            return false;
        }
        ShopGuildItem item;
        try { item = ShopGuildItem.valueOf(itemRaw.toUpperCase()); }
        catch (IllegalArgumentException ex) { Components.sendErrorMessage(sender, "Unknown guild item: " + itemRaw); return false; }
        Shop.ItemEntry e = s.getItems().get(item.name());
        if (e == null || !e.isBuyEnabled()) {
            Components.sendErrorMessage(sender, "Item not available to buy here.");
            return false;
        }
        double price = e.getBuyPrice();
        Components.sendInfo(sender,
                Components.t("Buying "), Components.valueComp(item.name()),
                Components.t(" costs "), Components.valueComp(plugin.walletService.formatAmount(price)),
                Components.t(". Run "), Components.valueComp("/shops buy " + item.name() + " confirm"), Components.t(" to purchase."));
        return true;
    }

    public boolean buyGuildConfirm(Player player, String shopName, String itemRaw) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(player, "Shop not found."); return false; }
        if (s.getType() != ShopType.GUILD) { Components.sendErrorMessage(player, "This is not a guild shop."); return false; }
        if (!s.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This shop is currently closed.");
            return false;
        }
        ShopGuildItem item;
        try { item = ShopGuildItem.valueOf(itemRaw.toUpperCase()); }
        catch (IllegalArgumentException ex) { Components.sendErrorMessage(player, "Unknown guild item: " + itemRaw); return false; }
        Shop.ItemEntry e = s.getItems().get(item.name());
        if (e == null || !e.isBuyEnabled()) { Components.sendErrorMessage(player, "Item not available to buy here."); return false; }
        if (e.getStock() <= 0) { Components.sendErrorMessage(player, "Out of stock."); return false; }
        double price = e.getBuyPrice();
        if (price > 0) {
            boolean ok = plugin.walletService.subBalance(player.getUniqueId(), price, "Guild shop buy: " + item.name() + " from " + shopName);
            if (!ok) { Components.sendErrorMessage(player, "Insufficient funds."); return false; }
        }
        // No inventory item is given; effect application can be handled elsewhere.
        e.setStock(e.getStock() - 1);
        Components.sendSuccess(player, Components.t("Purchased guild item "), Components.valueComp(item.name()), Components.t(" for "), Components.valueComp(plugin.walletService.formatAmount(price)));
        return true;
    }
}
