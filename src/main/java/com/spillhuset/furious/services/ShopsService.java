package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.Shop;
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
                    try { s.setType(com.spillhuset.furious.utils.ShopType.valueOf(typeStr.toUpperCase())); } catch (IllegalArgumentException ignored) {}
                    // claim
                    if (ss.getBoolean("claimed", false)) {
                        String wid = ss.getString("world", null);
                        if (wid != null) {
                            UUID worldId = UUID.fromString(wid);
                            int cx = ss.getInt("chunkX");
                            int cz = ss.getInt("chunkZ");
                            s.claim(worldId, cx, cz);
                        }
                    }
                    // global prices (optional)
                    if (ss.isSet("buy.enabled") && ss.getBoolean("buy.enabled")) s.setBuyPrice(Math.max(0, ss.getDouble("buy.price", 0)));
                    if (ss.isSet("sell.enabled") && ss.getBoolean("sell.enabled")) s.setSellPrice(Math.max(0, ss.getDouble("sell.price", 0)));
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
                            try { s.setArmorStandUuid(UUID.fromString(armor)); } catch (IllegalArgumentException ignored) {}
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
                            if (is.getBoolean("buy.enabled", false)) e.setBuyPrice(Math.max(0, is.getDouble("buy.price", 0)));
                            else e.setBuyDisabled();
                            if (is.getBoolean("sell.enabled", false)) e.setSellPrice(Math.max(0, is.getDouble("sell.price", 0)));
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
            // claim
            ss.set("claimed", s.isClaimed());
            if (s.isClaimed()) {
                ss.set("world", s.getWorldId().toString());
                ss.set("chunkX", s.getChunkX());
                ss.set("chunkZ", s.getChunkZ());
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
        try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
        try { stand.customName(net.kyori.adventure.text.Component.text(buildArmorStandName(shop))); } catch (Throwable ignored) {}
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                if (p.isOp()) p.showEntity(plugin, stand); else p.hideEntity(plugin, stand);
            } catch (Throwable ignored) {}
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
                    if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                }
            }
        } catch (Throwable ignored) {}
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
                try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
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
                try { plugin.armorStandManager.register(ent.getUniqueId(), () -> removeByArmorStand(ent.getUniqueId())); } catch (Throwable ignored) {}
                applyArmorStandVisibility(shop);
            }
        }
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
        if (name == null || name.isBlank()) { Components.sendErrorMessage(sender, "Name required."); return false; }
        if (getShopByName(name) != null) { Components.sendErrorMessage(sender, "Shop already exists."); return false; }
        ensureShop(name);
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(name), Components.t(" created."));
        return true;
    }

    public boolean deleteShop(CommandSender sender, String name) {
        Shop s = getShopByName(name);
        if (s == null) { Components.sendErrorMessage(sender, "Shop not found."); return false; }
        // remove armor stand if present
        try {
            if (s.getArmorStandUuid() != null) {
                try { plugin.armorStandManager.unregister(s.getArmorStandUuid()); } catch (Throwable ignored) {}
                Entity ent = plugin.getServer().getEntity(s.getArmorStandUuid());
                if (ent instanceof ArmorStand) ent.remove();
            }
        } catch (Throwable ignored) {}
        shopsById.remove(s.getId());
        shopIdByName.remove(s.getName().toLowerCase());
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(name), Components.t(" deleted."));
        return true;
    }

    public boolean setSpawn(CommandSender sender, String shopName, Location loc) {
        Shop s = ensureShop(shopName);
        if (loc.getWorld() == null) { Components.sendErrorMessage(sender, "Invalid world."); return false; }
        s.setSpawn(loc.getWorld().getUID(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // spawn or move armor stand
        try {
            if (s.getArmorStandUuid() != null) {
                Entity ent = plugin.getServer().getEntity(s.getArmorStandUuid());
                if (ent instanceof ArmorStand) {
                    ent.teleportAsync(loc);
                } else {
                    spawnArmorStandForShop(s);
                }
            } else {
                spawnArmorStandForShop(s);
            }
        } catch (Throwable ignored) {}
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
        } catch (Throwable ignored) {}
        save();
        if (hasStand) {
            Components.sendSuccess(sender, Components.t("Spawn set for shop "), Components.valueComp(s.getName()), Components.t(" (armorstand created)"));
        } else {
            Components.sendSuccess(sender, Components.t("Spawn set for shop "), Components.valueComp(s.getName()), Components.t(" (armorstand not created yet; will be ensured shortly)"));
            // Best-effort ensure on next tick so chunk/entity systems have settled
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try { if (plugin.armorStandManager != null) plugin.armorStandManager.ensureArmorStands(); } catch (Throwable ignored) {}
                });
            } catch (Throwable ignored) {}
        }
        return hasStand;
    }

    public boolean teleport(Player player, String shopName) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(player, "Shop not found."); return false; }
        if (!s.hasSpawn()) { Components.sendErrorMessage(player, "Shop has no spawn set."); return false; }
        var world = Bukkit.getWorld(s.getSpawnWorldId());
        if (world == null) { Components.sendErrorMessage(player, "Shop spawn world missing."); return false; }
        Location to = new Location(world, s.getSpawnX(), s.getSpawnY(), s.getSpawnZ(), s.getSpawnYaw(), s.getSpawnPitch());
        player.teleport(to);
        Components.sendSuccess(player, Components.t("Teleported to shop "), Components.valueComp(s.getName()));
        return true;
    }

    public List<String> suggestShopNames(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String n : shopIdByName.keySet()) if (n.startsWith(p)) out.add(n);
        return out;
    }

    public enum ClaimCheck { OK, NOT_IN_CLAIM, WRONG_GUILD_TYPE }

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
                case WRONG_GUILD_TYPE -> Components.sendErrorMessage(sender, "Shop claims must be inside SAFE or WAR guild.");
            }
            return false;
        }
        Chunk chunk = loc.getChunk();
        shop.claim(loc.getWorld().getUID(), chunk.getX(), chunk.getZ());
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(shop.getName()), Components.t(" claimed at this chunk."));
        return true;
    }

    public boolean unclaimShop(CommandSender sender, String shopName) {
        Shop shop = getShopByName(shopName);
        if (shop == null) { Components.sendErrorMessage(sender, "Shop not found."); return false; }
        shop.unclaim();
        save();
        Components.sendSuccess(sender, Components.t("Shop "), Components.valueComp(shop.getName()), Components.t(" unclaimed."));
        return true;
    }

    public boolean setType(CommandSender sender, String shopName, com.spillhuset.furious.utils.ShopType type) {
        Shop shop = getShopByName(shopName);
        if (shop == null) { Components.sendErrorMessage(sender, "Shop not found."); return false; }
        if (type == null) { Components.sendErrorMessage(sender, "Invalid shop type."); return false; }
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
            if (price < 0) { Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable."); return false; }
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
            if (price < 0) { Components.sendErrorMessage(sender, "Price must be >= 0 or '-' to disable."); return false; }
            shop.setSellPrice(price);
            Components.sendSuccess(sender, Components.t("Sell price for shop "), Components.valueComp(shop.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(price)));
            save();
            return true;
        } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid price. Use a number, 0 for free, or '-' to disable.");
            return false;
        }
    }

    // Player-facing features
    public void listShops(CommandSender sender) {
        if (shopIdByName.isEmpty()) { Components.sendInfoMessage(sender, "No shops available."); return; }
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
                UUID owner = plugin.guildService.getClaimOwner(s.getWorldId(), s.getChunkX(), s.getChunkZ());
                if (owner != null) {
                    Guild g = plugin.guildService.getGuildById(owner);
                    if (g != null && g.getName() != null) {
                        guildsStr = g.getName();
                    } else {
                        guildsStr = owner.toString();
                    }
                } else {
                    guildsStr = "Not in guild territory";
                }
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
        try { return Material.valueOf(raw.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    public boolean addItem(CommandSender sender, String shopName, String matName, int stock, String buyStr, String sellStr) {
        Shop s = ensureShop(shopName);
        Material mat = parseMaterial(matName);
        if (mat == null) { Components.sendErrorMessage(sender, "Unknown material: " + matName); return false; }
        Shop.ItemEntry e = s.getOrCreateItem(mat.name());
        e.setStock(e.getStock() + Math.max(0, stock));
        // Buy price parsing
        if (buyStr != null) {
            if (buyStr.equals("-")) e.setBuyDisabled();
            else {
                try {
                    double p = Double.parseDouble(buyStr);
                    if (p < 0) { Components.sendErrorMessage(sender, "Buy price must be >= 0 or '-' to disable."); return false; }
                    e.setBuyPrice(p);
                } catch (NumberFormatException ex) { Components.sendErrorMessage(sender, "Invalid buy price."); return false; }
            }
        }
        // Sell price parsing
        if (sellStr != null) {
            if (sellStr.equals("-")) e.setSellDisabled();
            else {
                try {
                    double p = Double.parseDouble(sellStr);
                    if (p < 0) { Components.sendErrorMessage(sender, "Sell price must be >= 0 or '-' to disable."); return false; }
                    e.setSellPrice(p);
                } catch (NumberFormatException ex) { Components.sendErrorMessage(sender, "Invalid sell price."); return false; }
            }
        }
        Components.sendSuccess(sender, Components.t("Item "), Components.valueComp(mat.name()), Components.t(" added/updated in shop "), Components.valueComp(s.getName()));
        save();
        return true;
    }

    public boolean removeItem(CommandSender sender, String shopName, String matName) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(sender, "Shop not found."); return false; }
        s.removeItem(matName);
        save();
        Components.sendSuccess(sender, Components.t("Item removed from shop "), Components.valueComp(s.getName()));
        return true;
    }

    public void listItems(CommandSender sender, String shopName) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(sender, "Shop not found."); return; }
        if (s.getItems().isEmpty()) { Components.sendInfoMessage(sender, "No items configured."); return; }
        int count = s.getItems().size();
        Components.sendInfo(sender, Components.t("Items for shop "), Components.valueComp(s.getName()), Components.t(" (" + count + "):"));
        for (Map.Entry<String, Shop.ItemEntry> ent : s.getItems().entrySet()) {
            Shop.ItemEntry e = ent.getValue();
            // Buy component
            net.kyori.adventure.text.Component buyComp;
            if (e.isBuyEnabled()) {
                if (e.getBuyPrice() == 0) buyComp = Components.t("FREE", net.kyori.adventure.text.format.NamedTextColor.AQUA);
                else buyComp = Components.t(plugin.walletService.formatAmount(e.getBuyPrice()), net.kyori.adventure.text.format.NamedTextColor.AQUA);
            } else {
                buyComp = Components.t("disabled", net.kyori.adventure.text.format.NamedTextColor.GRAY);
            }
            // Sell component
            net.kyori.adventure.text.Component sellComp;
            if (e.isSellEnabled()) {
                if (e.getSellPrice() == 0) sellComp = Components.t("FREE", net.kyori.adventure.text.format.NamedTextColor.AQUA);
                else sellComp = Components.t(plugin.walletService.formatAmount(e.getSellPrice()), net.kyori.adventure.text.format.NamedTextColor.AQUA);
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
        if (amount < 0) { Components.sendErrorMessage(sender, "Amount must be >= 0."); return false; }
        Shop s = ensureShop(shopName);
        Material mat = parseMaterial(matName);
        if (mat == null) { Components.sendErrorMessage(sender, "Unknown material: " + matName); return false; }
        Shop.ItemEntry e = s.getOrCreateItem(mat.name());
        e.setStock(amount);
        save();
        Components.sendSuccess(sender, Components.t("Stock for "), Components.valueComp(mat.name()), Components.t(" in shop "), Components.valueComp(s.getName()), Components.t(" set to "), Components.valueComp(String.valueOf(amount)));
        return true;
    }

    public boolean restock(CommandSender sender, String shopName, String matName, int amount) {
        if (amount <= 0) { Components.sendErrorMessage(sender, "Amount must be > 0."); return false; }
        Shop s = ensureShop(shopName);
        Material mat = parseMaterial(matName);
        if (mat == null) { Components.sendErrorMessage(sender, "Unknown material: " + matName); return false; }
        Shop.ItemEntry e = s.getOrCreateItem(mat.name());
        e.setStock(e.getStock() + amount);
        save();
        Components.sendSuccess(sender, Components.t("Restocked "), Components.valueComp(mat.name()), Components.t(" by "), Components.valueComp(String.valueOf(amount)), Components.t(". New stock: "), Components.valueComp(String.valueOf(e.getStock())));
        return true;
    }

    public boolean buy(Player player, String shopName, String matName, int amount) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(player, "Shop not found."); return false; }
        Shop.ItemEntry e = s.getItems().get(matName.toUpperCase());
        if (e == null) { Components.sendErrorMessage(player, "Item not available."); return false; }
        if (!e.isBuyEnabled()) { Components.sendErrorMessage(player, "Buying disabled for this item."); return false; }
        if (amount <= 0) amount = 1;
        if (e.getStock() < amount) { Components.sendErrorMessage(player, "Not enough stock."); return false; }
        double total = e.getBuyPrice() * amount;
        if (e.getBuyPrice() > 0) {
            boolean ok = plugin.walletService.subBalance(player.getUniqueId(), total, "Shop buy: " + matName + " x" + amount + " from " + shopName);
            if (!ok) { Components.sendErrorMessage(player, "Insufficient funds."); return false; }
        }
        Material mat = parseMaterial(matName);
        if (mat == null) { Components.sendErrorMessage(player, "Invalid material."); return false; }
        HashMap<Integer, ItemStack> rem = player.getInventory().addItem(new ItemStack(mat, amount));
        if (!rem.isEmpty()) {
            // Inventory full; revert wallet charge if any
            if (e.getBuyPrice() > 0) plugin.walletService.addBalance(player.getUniqueId(), total, "Refund: inventory full");
            Components.sendErrorMessage(player, "Not enough inventory space.");
            return false;
        }
        e.setStock(e.getStock() - amount);
        Components.sendSuccess(player, Components.t("Purchased "), Components.valueComp(mat.name()), Components.t(" x" + amount + " for "), Components.valueComp(plugin.walletService.formatAmount(total)));
        return true;
    }

    public boolean sell(Player player, String shopName, String matName, int amount) {
        Shop s = getShopByName(shopName);
        if (s == null) { Components.sendErrorMessage(player, "Shop not found."); return false; }
        Shop.ItemEntry e = s.getItems().get(matName.toUpperCase());
        if (e == null) { Components.sendErrorMessage(player, "Item not accepted."); return false; }
        if (!e.isSellEnabled()) { Components.sendErrorMessage(player, "Selling disabled for this item."); return false; }
        if (amount <= 0) amount = 1;
        Material mat = parseMaterial(matName);
        if (mat == null) { Components.sendErrorMessage(player, "Invalid material."); return false; }
        ItemStack take = new ItemStack(mat, amount);
        HashMap<Integer, ItemStack> notRemoved = player.getInventory().removeItem(take);
        if (!notRemoved.isEmpty()) { Components.sendErrorMessage(player, "You don't have enough items."); return false; }
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
                if (worldId.equals(shop.getWorldId()) && shop.getChunkX() == cx && shop.getChunkZ() == cz) {
                    return shop;
                }
            }
        }
        return null;
    }
}
