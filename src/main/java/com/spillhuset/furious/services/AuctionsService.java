package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Simple Auctions service implementing /auctions spec.
 * Stores: claims (set of chunks), open flag, spawn anchor, and auction entries.
 */
public class AuctionsService {
    private final Furious plugin;

    public AuctionsService(Furious plugin) {
        this.plugin = plugin;
    }

    // persistence
    private File auctionsFile;
    private FileConfiguration auctionsCfg;

    private final Set<ChunkKey> claims = new HashSet<>();
    private boolean open = true;

    // spawn anchor
    private UUID spawnWorldId;
    private Double spawnX, spawnY, spawnZ;
    private Float spawnYaw, spawnPitch;
    private UUID armorStandUuid;

    // auctions by unique name (lowercased key)
    private final Map<String, Auction> auctions = new HashMap<>();

    // pending returns for offline owners: ownerUUID -> list of item stacks
    private final Map<UUID, List<ItemStack>> pendingReturns = new HashMap<>();

    // scheduler to auto-expire auctions
    private BukkitTask expiryTask;

    public void load() {
        auctionsFile = new File(plugin.getDataFolder(), "auctions.yml");
        try {
            if (!auctionsFile.exists()) auctionsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating auctions.yml: " + e.getMessage());
        }
        auctionsCfg = YamlConfiguration.loadConfiguration(auctionsFile);
        claims.clear();
        auctions.clear();
        pendingReturns.clear();
        open = auctionsCfg.getBoolean("open", true);
        // claims
        ConfigurationSection cl = auctionsCfg.getConfigurationSection("claims");
        if (cl != null) {
            for (String key : cl.getKeys(false)) {
                String wid = cl.getString(key + ".world");
                int cx = cl.getInt(key + ".x");
                int cz = cl.getInt(key + ".z");
                try {
                    claims.add(new ChunkKey(UUID.fromString(wid), cx, cz));
                } catch (Exception ignored) {}
            }
        }
        // spawn
        ConfigurationSection sp = auctionsCfg.getConfigurationSection("spawn");
        if (sp != null) {
            try {
                String w = sp.getString("world");
                if (w != null) spawnWorldId = UUID.fromString(w);
                spawnX = sp.isSet("x") ? sp.getDouble("x") : null;
                spawnY = sp.isSet("y") ? sp.getDouble("y") : null;
                spawnZ = sp.isSet("z") ? sp.getDouble("z") : null;
                spawnYaw = sp.isSet("yaw") ? (float) sp.getDouble("yaw") : null;
                spawnPitch = sp.isSet("pitch") ? (float) sp.getDouble("pitch") : null;
                String armor = sp.getString("armorStand");
                if (armor != null) armorStandUuid = UUID.fromString(armor);
            } catch (Exception ignored) {}
        }
        // auctions
        ConfigurationSection as = auctionsCfg.getConfigurationSection("auctions");
        if (as != null) {
            for (String nameKey : as.getKeys(false)) {
                ConfigurationSection a = as.getConfigurationSection(nameKey);
                if (a == null) continue;
                try {
                    String displayName = a.getString("name", nameKey);
                    UUID owner = UUID.fromString(Objects.requireNonNull(a.getString("owner")));
                    ItemStack item = a.getItemStack("item");
                    double start = a.getDouble("start", 0);
                    Double buyout = a.isSet("buyout") ? a.getDouble("buyout") : null;
                    long end = a.getLong("end", 0);
                    double current = a.getDouble("current", start);
                    String bidderStr = a.getString("bidder");
                    UUID bidder = bidderStr == null ? null : UUID.fromString(bidderStr);
                    auctions.put(nameKey.toLowerCase(), new Auction(displayName, owner, item, start, buyout, end, current, bidder));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load auction " + nameKey + ": " + ex.getMessage());
                }
            }
        }
        // pending returns
        ConfigurationSection ret = auctionsCfg.getConfigurationSection("returns");
        if (ret != null) {
            for (String ownerKey : ret.getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(ownerKey);
                    ConfigurationSection list = ret.getConfigurationSection(ownerKey);
                    if (list == null) continue;
                    List<ItemStack> items = new ArrayList<>();
                    for (String idx : list.getKeys(false)) {
                        ItemStack it = list.getItemStack(idx);
                        if (it != null) items.add(it);
                    }
                    if (!items.isEmpty()) pendingReturns.put(owner, items);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        startExpiryScheduler();
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        out.set("open", open);
        // claims
        int i = 0;
        for (ChunkKey ck : claims) {
            ConfigurationSection cs = out.createSection("claims.c" + (i++));
            cs.set("world", ck.worldId().toString());
            cs.set("x", ck.x());
            cs.set("z", ck.z());
        }
        // spawn
        if (spawnWorldId != null && spawnX != null) {
            ConfigurationSection sp = out.createSection("spawn");
            sp.set("world", spawnWorldId.toString());
            sp.set("x", spawnX);
            sp.set("y", spawnY);
            sp.set("z", spawnZ);
            sp.set("yaw", spawnYaw);
            sp.set("pitch", spawnPitch);
            if (armorStandUuid != null) sp.set("armorStand", armorStandUuid.toString());
        }
        // auctions
        for (Auction a : auctions.values()) {
            ConfigurationSection as = out.createSection("auctions." + a.name.toLowerCase());
            as.set("name", a.name);
            as.set("owner", a.owner.toString());
            as.set("item", a.item);
            as.set("start", a.startBid);
            if (a.buyout != null) as.set("buyout", a.buyout);
            as.set("end", a.endTime);
            as.set("current", a.currentBid);
            if (a.currentBidder != null) as.set("bidder", a.currentBidder.toString());
        }
        // returns
        if (!pendingReturns.isEmpty()) {
            for (Map.Entry<UUID, List<ItemStack>> e : pendingReturns.entrySet()) {
                List<ItemStack> list = e.getValue();
                for (int idx = 0; idx < list.size(); idx++) {
                    out.set("returns." + e.getKey() + ".i" + idx, list.get(idx));
                }
            }
        }
        auctionsCfg = out;
        try {
            out.save(auctionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving auctions.yml: " + e.getMessage());
        }
    }

    // OP controls
    public enum ClaimCheck {OK, NOT_IN_CLAIM, WRONG_GUILD_TYPE}

    public ClaimCheck canClaimHere(Location loc) {
        if (loc == null || loc.getWorld() == null) return ClaimCheck.NOT_IN_CLAIM;
        UUID worldId = loc.getWorld().getUID();
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

    public boolean claim(CommandSender sender, Location loc) {
        ClaimCheck check = canClaimHere(loc);
        if (check != ClaimCheck.OK) {
            switch (check) {
                case NOT_IN_CLAIM -> Components.sendErrorMessage(sender, "This chunk is not claimed by a guild.");
                case WRONG_GUILD_TYPE -> Components.sendErrorMessage(sender, "Auctions must be in SAFE or WAR guild.");
            }
            return false;
        }
        Chunk c = loc.getChunk();
        ChunkKey key = new ChunkKey(loc.getWorld().getUID(), c.getX(), c.getZ());
        if (!claims.add(key)) {
            Components.sendErrorMessage(sender, "This chunk is already claimed for Auctions.");
            return false;
        }
        save();
        Components.sendSuccess(sender, Components.t("Auctions claimed at this chunk."));
        return true;
    }

    public boolean unclaim(CommandSender sender, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            Components.sendErrorMessage(sender, "Invalid location.");
            return false;
        }
        Chunk c = loc.getChunk();
        ChunkKey key = new ChunkKey(loc.getWorld().getUID(), c.getX(), c.getZ());
        if (!claims.remove(key)) {
            Components.sendErrorMessage(sender, "This chunk is not claimed for Auctions.");
            return false;
        }
        save();
        Components.sendSuccess(sender, Components.t("Auctions unclaimed at this chunk."));
        return true;
    }

    public boolean setOpen(CommandSender sender, boolean open) {
        this.open = open;
        save();
        Components.sendSuccess(sender, Components.t("Auctions are now "), Components.t(open ? "open" : "closed"), Components.t("."));
        return true;
    }

    public boolean spawnAnchor(CommandSender sender, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            Components.sendErrorMessage(sender, "Invalid location.");
            return false;
        }
        // remove previous armorstand
        removeArmorStand();
        // create via ArmorStandManager
        UUID id = null;
        try { if (plugin.armorStandManager != null) id = plugin.armorStandManager.create(loc, "Auctions"); } catch (Throwable ignored) {}
        if (id == null) {
            Components.sendErrorMessage(sender, "Failed to create Auctions anchor.");
            return false;
        }
        spawnWorldId = loc.getWorld().getUID();
        spawnX = loc.getX();
        spawnY = loc.getY();
        spawnZ = loc.getZ();
        spawnYaw = loc.getYaw();
        spawnPitch = loc.getPitch();
        armorStandUuid = id;
        // register and update visuals
        registerAndSetupArmorStand(id);
        save();
        Components.sendSuccess(sender, Components.t("Auctions teleport anchor set."));
        return true;
    }

    public boolean teleport(Player player) {
        if (player == null) return false;
        Location loc = getSpawnLocation();
        if (loc == null) {
            Components.sendErrorMessage(player, "Auctions teleport anchor is not set.");
            return false;
        }
        player.teleport(loc);
        Components.sendSuccess(player, Components.t("Teleported to Auctions."));
        return true;
    }

    private void removeArmorStand() {
        if (armorStandUuid == null) return;
        try {
            // unregister from manager if present
            try { if (plugin.armorStandManager != null) plugin.armorStandManager.unregister(armorStandUuid); } catch (Throwable ignored) {}
            Entity e = Bukkit.getEntity(armorStandUuid);
            if (e instanceof ArmorStand as) {
                as.remove();
            }
        } catch (Throwable ignored) {}
        armorStandUuid = null;
    }

    // Removes the Auctions teleport anchor (armor stand and stored location)
    public boolean removeSpawnAnchor(org.bukkit.command.CommandSender sender) {
        // remove any existing armor stand
        try { removeArmorStand(); } catch (Throwable ignored) {}
        // clear stored spawn fields
        spawnWorldId = null;
        spawnX = null;
        spawnY = null;
        spawnZ = null;
        spawnYaw = null;
        spawnPitch = null;
        save();
        if (sender != null) {
            Components.sendSuccess(sender, Components.t("Auctions teleport anchor removed."));
        }
        return true;
    }

    public Location getSpawnLocation() {
        if (spawnWorldId == null || spawnX == null) return null;
        World w = Bukkit.getWorld(spawnWorldId);
        if (w == null) return null;
        Location loc = new Location(w, spawnX, spawnY, spawnZ);
        if (spawnYaw != null) loc.setYaw(spawnYaw);
        if (spawnPitch != null) loc.setPitch(spawnPitch);
        return loc;
    }

    // ===== ArmorStand manager integration =====
    private void onArmorStandDeath() {
        // Only clear the reference; keep stored spawn location so ensureArmorStands can recreate it
        armorStandUuid = null;
        save();
    }

    private void updateArmorStandNameAndVisibility(ArmorStand stand) {
        if (stand == null) return;
        try { stand.customName(Component.text("Auctions", NamedTextColor.GOLD)); } catch (Throwable ignored) {}
        try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
        try {
            for (Player viewer : plugin.getServer().getOnlinePlayers()) {
                if (viewer.isOp()) viewer.showEntity(plugin, stand);
                else viewer.hideEntity(plugin, stand);
            }
        } catch (Throwable ignored) {}
    }

    private void registerAndSetupArmorStand(UUID id) {
        if (id == null) return;
        try { if (plugin.armorStandManager != null) plugin.armorStandManager.register(id, this::onArmorStandDeath); } catch (Throwable ignored) {}
        try {
            Entity ent = plugin.getServer().getEntity(id);
            if (ent instanceof ArmorStand st) updateArmorStandNameAndVisibility(st);
        } catch (Throwable ignored) {}
    }

    public void applyAuctionsArmorStandVisibilityForViewer(Player viewer) {
        if (viewer == null) return;
        if (armorStandUuid == null) return;
        try {
            Entity ent = plugin.getServer().getEntity(armorStandUuid);
            if (ent instanceof ArmorStand stand) {
                if (viewer.isOp()) viewer.showEntity(plugin, stand);
                else viewer.hideEntity(plugin, stand);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Attempt to adopt an unreferenced, managed ArmorStand into Auctions by proximity to the stored spawn.
     */
    public boolean adoptArmorStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null || stand.getWorld() == null) return false;
        org.bukkit.Location sLoc = stand.getLocation();
        org.bukkit.Location sp = getSpawnLocation();
        if (sp == null || sp.getWorld() == null) return false;
        try {
            if (!sp.getWorld().equals(sLoc.getWorld())) return false;
            if (sp.distanceSquared(sLoc) <= 4.0) {
                if (stand.getUniqueId().equals(armorStandUuid)) return true;
                armorStandUuid = stand.getUniqueId();
                registerAndSetupArmorStand(stand.getUniqueId());
                save();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public void ensureArmorStands() {
        // If we have a reference try to update visibility and name
        try {
            if (armorStandUuid != null) {
                Entity ent = plugin.getServer().getEntity(armorStandUuid);
                if (ent instanceof ArmorStand st) {
                    updateArmorStandNameAndVisibility(st);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        // If no entity present but spawn location stored, recreate
        Location loc = getSpawnLocation();
        if (loc == null) return;
        UUID id = null;
        try { if (plugin.armorStandManager != null) id = plugin.armorStandManager.create(loc, "Auctions"); } catch (Throwable ignored) {}
        if (id != null) {
            armorStandUuid = id;
            registerAndSetupArmorStand(id);
            save();
        }
    }

    // For sanitizer: claim our ArmorStand as referenced
    public boolean hasArmorStand(java.util.UUID armorStandId) {
        return armorStandId != null && armorStandId.equals(this.armorStandUuid);
    }

    private boolean isPlayerInAuctions(Player player) {
        if (player == null) return false;
        Location l = player.getLocation();
        if (l.getWorld() == null) return false;
        Chunk c = l.getChunk();
        return claims.contains(new ChunkKey(l.getWorld().getUID(), c.getX(), c.getZ()));
    }

    // Player actions
    public boolean setAuction(Player player, String name, double start, Double buyout, Integer hours) {
        if (player == null) return false;
        if (!isPlayerInAuctions(player)) {
            Components.sendErrorMessage(player, "You must be inside the Auctions area to set an item.");
            return false;
        }
        if (!open && !player.isOp()) {
            Components.sendErrorMessage(player, "Auctions are currently closed.");
            return false;
        }
        if (name == null || name.isBlank()) {
            Components.sendErrorMessage(player, "Name must not be empty.");
            return false;
        }
        String key = name.toLowerCase();
        if (auctions.containsKey(key)) {
            Components.sendErrorMessage(player, "An auction with that name already exists.");
            return false;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            Components.sendErrorMessage(player, "You must hold the item to auction in your hand.");
            return false;
        }
        // remove 1 stack from hand (the whole stack)
        ItemStack toStore = hand.clone();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        long end = 0L;
        if (hours != null && hours > 0) {
            end = Instant.now().toEpochMilli() + hours * 60L * 60L * 1000L;
        }
        Auction a = new Auction(name, player.getUniqueId(), toStore, start, buyout, end, start, null);
        auctions.put(key, a);
        save();
        Components.sendSuccess(player, Components.t("Auction created: "), Components.valueComp(name));
        return true;
    }

    public void listAuctions(CommandSender sender, String playerNameFilter) {
        List<String> lines = new ArrayList<>();
        UUID filterUuid = null;
        if (playerNameFilter != null && !playerNameFilter.isBlank()) {
            OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(playerNameFilter);
            if (op != null) filterUuid = op.getUniqueId();
        }
        long now = Instant.now().toEpochMilli();
        for (Auction a : auctions.values()) {
            if (filterUuid != null && !a.owner.equals(filterUuid)) continue;
            String itemStr = a.item == null ? "<null>" : a.item.getType().name();
            String bidStr = a.currentBid + " | " + a.startBid;
            String buyStr = a.buyout == null ? "-" : a.buyout.toString();
            String hoursLeft = "-";
            if (a.endTime > 0) {
                long leftMs = a.endTime - now;
                double hoursD = Math.max(0, leftMs / 3600000.0);
                hoursLeft = String.format(Locale.US, "%.2f", hoursD);
            }
            lines.add("- " + a.name + " | " + itemStr + " | " + bidStr + " | " + buyStr + " | " + hoursLeft + "h");
        }
        if (lines.isEmpty()) Components.sendInfoMessage(sender, "No auctions.");
        else for (String s : lines) Components.sendInfoMessage(sender, s);
    }

    public boolean bid(Player player, String name, double offer) {
        if (player == null) return false;
        Auction a = auctions.get(name.toLowerCase());
        if (a == null) {
            Components.sendErrorMessage(player, "Auction not found.");
            return false;
        }
        if (!isPlayerInAuctions(player)) {
            Components.sendErrorMessage(player, "You must be inside the Auctions area to bid.");
            return false;
        }
        if (!open && !player.isOp()) {
            Components.sendErrorMessage(player, "Auctions are currently closed.");
            return false;
        }
        if (offer <= a.currentBid) {
            Components.sendErrorMessage(player, "Your offer must be higher than current bid.");
            return false;
        }
        a.currentBid = offer;
        a.currentBidder = player.getUniqueId();
        save();
        Components.sendSuccess(player, Components.t("Bid placed on "), Components.valueComp(a.name), Components.t(" for "), Components.valueComp(String.valueOf(offer)));
        return true;
    }

    public boolean buyoutRequest(CommandSender sender, String name) {
        // Enforce open state for players (ops bypass)
        if (sender instanceof Player p) {
            if (!open && !p.isOp()) {
                Components.sendErrorMessage(sender, "Auctions are currently closed.");
                return false;
            }
        }
        Auction a = auctions.get(name.toLowerCase());
        if (a == null) {
            Components.sendErrorMessage(sender, "Auction not found.");
            return false;
        }
        if (a.buyout == null) {
            Components.sendErrorMessage(sender, "This auction has no buyout.");
            return false;
        }
        Components.sendInfoMessage(sender, "Buying out '" + a.name + "' costs " + a.buyout + ". Run /auctions buyout " + a.name + " confirm to proceed.");
        return true;
    }

    public boolean buyoutConfirm(Player buyer, String name) {
        if (!open && !buyer.isOp()) {
            Components.sendErrorMessage(buyer, "Auctions are currently closed.");
            return false;
        }
        Auction a = auctions.get(name.toLowerCase());
        if (a == null) {
            Components.sendErrorMessage(buyer, "Auction not found.");
            return false;
        }
        if (a.buyout == null) {
            Components.sendErrorMessage(buyer, "This auction has no buyout.");
            return false;
        }
        double price = Math.max(0, a.buyout);
        UUID buyerId = buyer.getUniqueId();
        if (!plugin.walletService.subBalance(buyerId, price, "Auction buyout: " + a.name)) {
            Components.sendErrorMessage(buyer, "You cannot afford this buyout.");
            return false;
        }
        double sellerAmount = price * 0.9;
        plugin.walletService.addBalance(a.owner, sellerAmount, "Auction sold: " + a.name + " (90% after fee)");
        // give item
        giveOrDrop(buyer, a.item);
        auctions.remove(a.name.toLowerCase());
        save();
        Components.sendSuccess(buyer, Components.t("You bought out "), Components.valueComp(a.name), Components.t("."));
        return true;
    }

    public boolean cancel(Player owner, String name) {
        if (!open && !owner.isOp()) {
            Components.sendErrorMessage(owner, "Auctions are currently closed.");
            return false;
        }
        Auction a = auctions.get(name.toLowerCase());
        if (a == null) {
            Components.sendErrorMessage(owner, "Auction not found.");
            return false;
        }
        if (!a.owner.equals(owner.getUniqueId())) {
            Components.sendErrorMessage(owner, "Only the owner can cancel this auction.");
            return false;
        }
        // As specified: same as buyout confirm with owner as buyer
        if (a.buyout == null) {
            Components.sendErrorMessage(owner, "This auction has no buyout.");
            return false;
        }
        double price = Math.max(0, a.buyout);
        if (!plugin.walletService.subBalance(owner.getUniqueId(), price, "Auction cancel buyout: " + a.name)) {
            Components.sendErrorMessage(owner, "You cannot afford to cancel (buyout).");
            return false;
        }
        double sellerAmount = price * 0.9; // owner receives 90% back
        plugin.walletService.addBalance(owner.getUniqueId(), sellerAmount, "Auction cancel refund (90%): " + a.name);
        giveOrDrop(owner, a.item);
        auctions.remove(a.name.toLowerCase());
        save();
        Components.sendSuccess(owner, Components.t("Auction canceled and item returned."));
        return true;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        HashMap<Integer, ItemStack> rem = player.getInventory().addItem(item.clone());
        for (ItemStack it : rem.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), it);
        }
    }

    // ===== Expiration handling =====
    private void startExpiryScheduler() {
        stopExpiryScheduler();
        // run every 60 seconds on main thread
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processExpirationsTick, 20L * 60, 20L * 60);
    }

    private void stopExpiryScheduler() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }

    private void processExpirationsTick() {
        if (auctions.isEmpty()) return;
        long now = Instant.now().toEpochMilli();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Auction> e : auctions.entrySet()) {
            Auction a = e.getValue();
            if (a.endTime > 0 && a.endTime <= now) {
                // expired: return item to owner, notify users, remove auction
                OfflinePlayer owner = Bukkit.getOfflinePlayer(a.owner);
                Player ownerOnline = owner != null ? owner.getPlayer() : null;
                if (ownerOnline != null) {
                    giveOrDrop(ownerOnline, a.item);
                    Components.sendSuccess(ownerOnline, Components.t("Your auction expired: "), Components.valueComp(a.name), Components.t(". Item returned."));
                } else {
                    enqueueReturn(a.owner, a.item);
                }
                // notify highest bidder if online
                if (a.currentBidder != null) {
                    Player bidder = Bukkit.getPlayer(a.currentBidder);
                    if (bidder != null && bidder.isOnline()) {
                        Components.sendInfo(bidder, Components.t("Auction expired: "), Components.valueComp(a.name), Components.t(". No winner; item returned to owner."));
                    }
                }
                toRemove.add(e.getKey());
            }
        }
        if (!toRemove.isEmpty()) {
            for (String key : toRemove) auctions.remove(key);
            save();
        }
    }

    private void enqueueReturn(UUID owner, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        List<ItemStack> list = pendingReturns.computeIfAbsent(owner, k -> new ArrayList<>());
        list.add(item.clone());
    }

    public void processPendingReturns(Player player) {
        if (player == null) return;
        List<ItemStack> list = pendingReturns.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return;
        int count = 0;
        for (ItemStack it : new ArrayList<>(list)) {
            giveOrDrop(player, it);
            count++;
        }
        list.clear();
        save();
        Components.sendSuccess(player, Components.t("Delivered "), Components.valueComp(String.valueOf(count)), Components.t(" expired auction item(s)."));
    }

    public void shutdown() {
        stopExpiryScheduler();
        save();
    }

    public record ChunkKey(UUID worldId, int x, int z) {}

    public List<String> getAuctionNames() {
        List<String> names = new ArrayList<>();
        for (Auction a : auctions.values()) names.add(a.name);
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public List<String> getAuctionNamesWithBuyout() {
        List<String> names = new ArrayList<>();
        for (Auction a : auctions.values()) if (a.buyout != null) names.add(a.name);
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public List<String> getAuctionNamesOwnedBy(UUID owner) {
        List<String> names = new ArrayList<>();
        for (Auction a : auctions.values()) if (a.owner.equals(owner)) names.add(a.name);
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public static class Auction {
        public final String name;
        public final UUID owner;
        public final ItemStack item;
        public final double startBid;
        public final Double buyout;
        public final long endTime; // epoch millis, 0 for no auto-end
        public double currentBid;
        public UUID currentBidder;

        public Auction(String name, UUID owner, ItemStack item, double startBid, Double buyout, long endTime, double currentBid, UUID currentBidder) {
            this.name = name;
            this.owner = owner;
            this.item = item;
            this.startBid = startBid;
            this.buyout = buyout;
            this.endTime = endTime;
            this.currentBid = currentBid;
            this.currentBidder = currentBidder;
        }
    }
}
