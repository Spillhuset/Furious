package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HomesService {
    private final Furious plugin;
    //          homeUUID, home
    private final Map<UUID, Home> homes = new HashMap<>();
    //          playerUUID, last homes teleport epoch millis (in-memory only)
    private final Map<UUID, Long> lastHomesTeleport = new HashMap<>();
    //          location, homeUUID
    private final Map<Location, UUID> locations = new HashMap<>();
    //          playerUUID, purchased
    private final Map<UUID, Integer> purchasedSlots = new HashMap<>();
    //          playerUUID, owned
    private final Map<UUID, Integer> owned = new HashMap<>();
    //          playerUUID, homeUUIDs
    private final Map<UUID, Set<UUID>> players = new HashMap<>();

    private FileConfiguration homesConfig;
    private File homesFile;

    public int DEFAULT_HOMES_COUNT;
    public double DEFAULT_HOMES_COST;
    public double HOMES_MULTIPLIER;
    public int TELEPORT_COOLDOWN_SECONDS;

    // batching fields
    private final Object configIoLock = new Object(); // guard config read/writes against async save
    private Set<UUID> enabledWorlds = new HashSet<>();

    public HomesService(Furious instance) {
        this.plugin = instance.getInstance();

        DEFAULT_HOMES_COUNT = instance.getConfig().getInt("homes.default", 5);
        DEFAULT_HOMES_COST = instance.getConfig().getDouble("homes.cost", 5000.0);
        HOMES_MULTIPLIER = instance.getConfig().getDouble("homes.multiplier", 1.5);
        TELEPORT_COOLDOWN_SECONDS = instance.getConfig().getInt("homes.teleport-cooldown-seconds", 1800);

        ensureHomesDefaultsPersisted(instance);
    }

    private void ensureHomesDefaultsPersisted(Furious instance) {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = instance.getConfig();
            boolean changed = false;
            if (!cfg.isSet("homes.default")) { cfg.set("homes.default", DEFAULT_HOMES_COUNT); changed = true; }
            if (!cfg.isSet("homes.cost")) { cfg.set("homes.cost", DEFAULT_HOMES_COST); changed = true; }
            if (!cfg.isSet("homes.multiplier")) { cfg.set("homes.multiplier", HOMES_MULTIPLIER); changed = true; }
            if (!cfg.isSet("homes.teleport-cooldown-seconds")) { cfg.set("homes.teleport-cooldown-seconds", TELEPORT_COOLDOWN_SECONDS); changed = true; }
            if (!cfg.isSet("homes.enabled-worlds")) { cfg.set("homes.enabled-worlds", new java.util.ArrayList<String>()); changed = true; }
            if (changed) instance.saveConfig();
        } catch (Throwable ignored) {}
    }

    public void load() {
        loadEnabledWorldsFromConfig();
        homes.clear();
        locations.clear();
        purchasedSlots.clear();
        owned.clear();
        players.clear();

        // If database enabled, load from DB; otherwise from YAML
        if (plugin.databaseManager != null && plugin.databaseManager.isEnabled()) {
            try {
                com.spillhuset.furious.db.HomesRepository repo = new com.spillhuset.furious.db.HomesRepository(plugin.databaseManager.getDataSource(), plugin.getServer());
                repo.initSchema();
                repo.loadAll(players, homes, locations, purchasedSlots);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load homes from database: " + e.getMessage());
            }
            return;
        }

        // YAML path
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        try {
            if (!homesFile.exists()) homesFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating homes files: " + e.getMessage());
        }
        synchronized (configIoLock) {
            homesConfig = YamlConfiguration.loadConfiguration(homesFile);
            ConfigurationSection section = homesConfig.getConfigurationSection("players");
            if (section == null) return;
            Set<String> keys = section.getKeys(false);
            for (String playerUID : keys) {
                try {
                    UUID uuid = UUID.fromString(playerUID);
                    ConfigurationSection playerSection = section.getConfigurationSection(playerUID);
                    if (playerSection == null) continue;
                    purchasedSlots.put(uuid, playerSection.getInt("purchased", 0));
                    ConfigurationSection homesSection = playerSection.getConfigurationSection("homes");
                    if (homesSection != null) {
                        for (String homeKey : homesSection.getKeys(false)) {
                            ConfigurationSection homeUID = homesSection.getConfigurationSection(homeKey);
                            if (homeUID == null) continue;
                            UUID homeUUID = UUID.fromString(homeUID.getName());
                            UUID worldUUID = UUID.fromString(Objects.requireNonNull(homeUID.getString("world")));
                            String name = homeUID.getString("name");
                            double x = homeUID.getDouble("x");
                            double y = homeUID.getDouble("y");
                            double z = homeUID.getDouble("z");
                            float yaw = (float) homeUID.getDouble("yaw");
                            float pitch = (float) homeUID.getDouble("pitch");
                            String armorStr = homeUID.getString("armorStand", null);
                            Location location = new Location(plugin.getServer().getWorld(worldUUID), x, y, z, yaw, pitch);
                            Home home = new Home(homeUUID, name, location, uuid);
                            if (armorStr != null) {
                                try { home.setArmorStandUuid(UUID.fromString(armorStr)); } catch (IllegalArgumentException ignored) {}
                            }
                            players.computeIfAbsent(uuid, k -> new HashSet<>()).add(homeUUID);
                            homes.put(homeUUID, home);
                            owned.put(uuid, owned.getOrDefault(uuid, 0) + 1);
                            locations.put(location, homeUUID);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Failed loading homes for UUID " + playerUID + ": " + e.getMessage());
                }
            }
        }
    }

    private void loadEnabledWorldsFromConfig() {
        List<String> ids = plugin.getConfig().getStringList("homes.enabled-worlds");
        enabledWorlds = ids.stream().map(UUID::fromString).collect(Collectors.toSet());
    }

    public int getMaximumCount(@NotNull UUID uuid) {
        return DEFAULT_HOMES_COUNT + getPurchasedCount(uuid);
    }

    public int getPurchasedCount(@NotNull UUID uuid) {
        return purchasedSlots.getOrDefault(uuid, 0);
    }

    public int getOwnedCount(@NotNull UUID uuid) {
        return owned.getOrDefault(uuid, 0);
    }

    public int getAvailableHomes(@NotNull UUID uuid) {
        return getMaximumCount(uuid) - getOwnedCount(uuid);
    }

    public @Nullable Home getHome(@NotNull UUID uuid, @NotNull String name) {
        for (Home home : homes.values()) {
            if (home.getPlayer().equals(uuid) && home.getName().equals(name)) {
                return home;
            }
        }
        return null;
    }

    public @Nullable Home getHome(@NotNull Location location) {
        return homes.get(locations.get(location));
    }

    public void save() {
        if (plugin.databaseManager != null && plugin.databaseManager.isEnabled()) {
            try {
                com.spillhuset.furious.db.HomesRepository repo = new com.spillhuset.furious.db.HomesRepository(plugin.databaseManager.getDataSource(), plugin.getServer());
                repo.initSchema();
                repo.saveAll(players, homes, purchasedSlots);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save homes to database: " + e.getMessage());
            }
        } else {
            // Persist homes.yml atomically under a lock
            synchronized (configIoLock) {
                // Rebuild configuration to avoid stale entries
                YamlConfiguration newConfig = new YamlConfiguration();
                ConfigurationSection playersRoot = newConfig.createSection("players");
                for (Map.Entry<UUID, Set<UUID>> playerEntry : players.entrySet()) {
                    UUID playerId = playerEntry.getKey();
                    ConfigurationSection playerSection = playersRoot.createSection(playerId.toString());
                    playerSection.set("purchased", purchasedSlots.getOrDefault(playerId, 0));
                    ConfigurationSection homesSection = playerSection.createSection("homes");
                    for (UUID homeId : playerEntry.getValue()) {
                        Home home = homes.get(homeId);
                        if (home == null) continue; // safety
                        ConfigurationSection homeUID = homesSection.createSection(homeId.toString());
                        homeUID.set("world", home.getWorld().toString());
                        homeUID.set("name", home.getName());
                        homeUID.set("x", home.getX());
                        homeUID.set("y", home.getY());
                        homeUID.set("z", home.getZ());
                        homeUID.set("yaw", home.getYaw());
                        homeUID.set("pitch", home.getPitch());
                        if (home.getArmorStandUuid() != null) {
                            homeUID.set("armorStand", home.getArmorStandUuid().toString());
                        }
                    }
                }
                homesConfig = newConfig;
                try {
                    newConfig.save(homesFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save homes.yml: " + e.getMessage());
                }
            }
        }
        // Persist enabled worlds in plugin config
        List<String> worldIds = enabledWorlds.stream().map(UUID::toString).collect(Collectors.toList());
        plugin.getConfig().set("homes.enabled-worlds", worldIds);
        plugin.saveConfig();
    }

    public void setHome(Player commandSender, UUID targetPlayer, String homeName) {
        if (!canSetNewHome(targetPlayer) && getHome(targetPlayer, homeName) == null) {
            Components.sendErrorMessage(commandSender, "You already have the maximum amount of homes.");
            return;
        }

        if (!isWorldEnabled(commandSender.getWorld().getUID())) {
            Components.sendErrorMessage(commandSender, "You can't set homes in this world.");
            return;
        }

        // Check guild claim/type rules: allow in FREE; allow in OWNED only if player is a member; disallow SAFE/WAR
        Location loc = commandSender.getLocation();
        if (loc.getWorld() != null) {
            UUID worldId = loc.getWorld().getUID();
            int cx = loc.getChunk().getX();
            int cz = loc.getChunk().getZ();
            UUID ownerGid = plugin.guildService.getClaimOwner(worldId, cx, cz);
            if (ownerGid != null) {
                Guild g = plugin.guildService.getGuildById(ownerGid);
                if (g != null) {
                    GuildType type = g.getType();
                    switch (type) {
                        case FREE -> {
                            // always allowed in FREE
                        }
                        case OWNED -> {
                            UUID playerGuild = plugin.guildService.getGuildIdForMember(targetPlayer);
                            if (playerGuild == null || !playerGuild.equals(ownerGid)) {
                                Components.sendErrorMessage(commandSender, "You must be a member of this guild to set a home here.");
                                return;
                            }
                        }
                        case SAFE, WAR -> {
                            Components.sendErrorMessage(commandSender, "You can't set homes in this guild territory.");
                            return;
                        }
                    }
                }
            }
        }

        boolean set = setHome(targetPlayer, homeName, commandSender.getLocation());
        if (!set) {
            Components.sendErrorMessage(commandSender, "A home with this name already exists.");
        } else {
            save();
            Components.sendSuccess(commandSender, Components.t("Home "), Components.valueComp(homeName), Components.t(" set."));
        }
    }

    public void renameHome(CommandSender sender, UUID targetPlayer, String oldName, String newName) {
        // No-op if names are identical
        if (oldName.equals(newName)) {
            Components.sendInfoMessage(sender, "Old and new names are the same.");
            return;
        }

        // Prevent duplicate names for the same player (case-sensitive to match setHome)
        Set<UUID> playerHomes = players.getOrDefault(targetPlayer, new HashSet<>());
        for (UUID homeUUID : playerHomes) {
            if (homes.get(homeUUID).getName().equals(newName)) {
                Components.sendErrorMessage(sender, "A home with this name already exists.");
                return;
            }
        }

        boolean renamed = renameHome(targetPlayer, oldName, newName);
        if (!renamed) {
            // Old name not found
            Components.sendErrorMessage(sender, "Home not found.");
        } else {
            save();
            Components.sendSuccess(sender, Components.t("Home "), Components.valueComp(oldName), Components.t(" renamed to "), Components.valueComp(newName));
        }
    }

    private boolean renameHome(UUID targetPlayer, String oldName, String newName) {
        Set<UUID> playerHomes = players.getOrDefault(targetPlayer, new HashSet<>());
        boolean oldFound = false;
        // Safety check: disallow renaming if target name already exists
        for (UUID homeUUID : playerHomes) {
            if (homes.get(homeUUID).getName().equals(newName)) {
                return false;
            }
        }
        for (UUID homeUUID : playerHomes) {
            if (homes.get(homeUUID).getName().equals(oldName)) {
                homes.get(homeUUID).setName(newName);
                // Update armor stand name if present
                try {
                    Home h = homes.get(homeUUID);
                    if (h.getArmorStandUuid() != null) {
                        Entity ent = plugin.getServer().getEntity(h.getArmorStandUuid());
                        if (ent instanceof ArmorStand stand) {
                            String playerName = plugin.getServer().getOfflinePlayer(targetPlayer).getName();
                            String asName = "Home " + newName + " by " + (playerName != null ? playerName : targetPlayer.toString());
                            try {
                                stand.customName(net.kyori.adventure.text.Component.text(asName));
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
                oldFound = true;
                break;
            }
        }
        return oldFound;
    }

    private boolean setHome(UUID targetPlayer, String homeName, @NotNull Location location) {
        Set<UUID> playerHomes = players.getOrDefault(targetPlayer, new HashSet<>());
        for (UUID homeUUID : playerHomes) {
            if (homes.get(homeUUID).getName().equals(homeName)) {
                return false;
            }
        }
        Home home = new Home(UUID.randomUUID(), homeName, location, targetPlayer);
        // Spawn an invisible marker ArmorStand to represent this home location
        try {
            if (location.getWorld() != null) {
                String playerName = plugin.getServer().getOfflinePlayer(targetPlayer).getName();
                String asName = "Home " + homeName + " by " + (playerName != null ? playerName : targetPlayer.toString());
                java.util.UUID id = plugin.armorStandManager.create(location, asName);
                if (id != null) {
                    home.setArmorStandUuid(id);
                    try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
                    // Apply per-player visibility: show for ops, hide for others
                    try {
                        org.bukkit.entity.Entity ent = plugin.getServer().getEntity(id);
                        if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                            try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                            for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                                if (viewer.isOp()) {
                                    viewer.showEntity(plugin, stand);
                                } else {
                                    viewer.hideEntity(plugin, stand);
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            // Fallback if spawning fails; home will still work via stored coordinates
            plugin.getLogger().warning("Failed to spawn ArmorStand for home: " + t.getMessage());
        }
        // Add to player's homes'
        players.computeIfAbsent(targetPlayer, k -> new HashSet<>()).add(home.getUuid());
        // Put into home list
        homes.put(home.getUuid(), home);
        // Put into location list
        locations.put(location, home.getUuid());
        // Update owned count
        owned.put(targetPlayer, owned.getOrDefault(targetPlayer, 0) + 1);
        return true;
    }


    private boolean isWorldEnabled(@NotNull UUID uuid) {
        return enabledWorlds.contains(uuid);
    }

    public void setWorldEnabled(@NotNull UUID worldId, boolean enabled) {
        if (enabled) {
            enabledWorlds.add(worldId);
        } else {
            enabledWorlds.remove(worldId);
        }
    }

    public void setWorldEnabled(CommandSender commandSender, @NotNull UUID worldId, boolean enable) {
        World world = plugin.getServer().getWorld(worldId);
        if (world == null) {
            Components.sendErrorMessage(commandSender, "Invalid world");
            return;
        }
        setWorldEnabled(worldId, enable);
        save();
        if (enable) {
            Components.sendSuccess(commandSender, Components.t("World "), Components.valueComp(world.getName()), Components.t(" enabled", NamedTextColor.GREEN), Components.t(" for homes."));
        } else {
            Components.sendSuccess(commandSender, Components.t("World "), Components.valueComp(world.getName()), Components.t(" disabled", NamedTextColor.RED), Components.t(" for homes."));
        }
    }

    public double getNextBuyCost(UUID playerId) {
        int purchased = purchasedSlots.getOrDefault(playerId, 0);
        return DEFAULT_HOMES_COST * Math.pow(HOMES_MULTIPLIER, purchased);
    }

    private boolean canSetNewHome(UUID uuid) {
        return getOwnedCount(uuid) < getMaximumCount(uuid);
    }

    public void purchaseSlot(UUID playerId) {
        purchasedSlots.put(playerId, purchasedSlots.getOrDefault(playerId, 0) + 1);
    }


    public void moveHome(Player commandSender, UUID targetPlayer, String homeName) {
        if (!isWorldEnabled(commandSender.getWorld().getUID())) {
            Components.sendErrorMessage(commandSender, "You can't set homes in this world.");
            return;
        }

        boolean moved = moveHome(targetPlayer, homeName, commandSender.getLocation());
        if (!moved) {
            Components.sendErrorMessage(commandSender, "A home with this name already exists.");
        } else {
            save();
            Components.sendSuccess(commandSender, Components.t("Home "), Components.valueComp(homeName), Components.t(" moved."));
        }
    }

    private boolean moveHome(UUID targetPlayer, String homeName, @NotNull Location location) {
        Set<UUID> playerHomes = players.getOrDefault(targetPlayer, new HashSet<>());
        for (UUID homeUUID : playerHomes) {
            Home home = homes.get(homeUUID);
            if (home != null && home.getName().equals(homeName)) {
                // Update locations index
                Location oldLoc = home.getLocation(plugin);
                if (oldLoc != null) {
                    locations.remove(oldLoc);
                }
                // Move armor stand entity if present
                try {
                    if (home.getArmorStandUuid() != null) {
                        Entity ent = plugin.getServer().getEntity(home.getArmorStandUuid());
                        if (ent instanceof ArmorStand) {
                            ent.teleportAsync(location);
                        }
                    }
                } catch (Throwable ignored) {
                }
                home.setLocation(location);
                locations.put(location, homeUUID);
                return true;
            }
        }
        return false;
    }

    public void removeHome(CommandSender commandSender, UUID targetPlayer, String homeName) {
        boolean removed = removeHome(targetPlayer, homeName);
        if (!removed) {
            Components.sendError(commandSender, Components.t("Home with name "), Components.valueComp(homeName), Components.t(" does not exist."));
        } else {
            save();
            Components.sendSuccess(commandSender, Components.t("Home "), Components.valueComp(homeName), Components.t(" removed."));
        }
    }

    private boolean removeHome(UUID targetPlayer, String homeName) {
        Set<UUID> playerHomes = players.getOrDefault(targetPlayer, new HashSet<>());
        Iterator<UUID> it = playerHomes.iterator();
        while (it.hasNext()) {
            UUID homeUUID = it.next();
            Home home = homes.get(homeUUID);
            if (home != null && home.getName().equals(homeName)) {
                // Update indices
                Location oldLoc = home.getLocation(plugin);
                if (oldLoc != null) {
                    locations.remove(oldLoc);
                }
                // Remove armor stand entity if present
                try {
                    if (home.getArmorStandUuid() != null) {
                        try { plugin.armorStandManager.unregister(home.getArmorStandUuid()); } catch (Throwable ignored) {}
                        Entity ent = plugin.getServer().getEntity(home.getArmorStandUuid());
                        if (ent instanceof ArmorStand) {
                            ent.remove();
                        }
                    }
                } catch (Throwable ignored) {
                }
                homes.remove(homeUUID);
                it.remove();
                // Decrement owned count
                owned.put(targetPlayer, Math.max(0, owned.getOrDefault(targetPlayer, 0) - 1));
                // Write back the possibly new empty set
                if (playerHomes.isEmpty()) {
                    players.remove(targetPlayer);
                } else {
                    players.put(targetPlayer, playerHomes);
                }
                return true;
            }
        }
        return false;
    }

    public void listHomes(CommandSender sender, Player player) {
        Set<UUID> playerHomes = players.getOrDefault(player.getUniqueId(), new HashSet<>());
        if (playerHomes.isEmpty()) {
            Components.sendInfoMessage(sender, "No homes set.");
        } else {
            List<Home> list = playerHomes.stream().map(homes::get).filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Home::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            // Header with total count
            Components.sendInfo(sender, Components.t("Homes total: "), Components.valueComp(String.valueOf(list.size())));
            for (Home h : list) {
                org.bukkit.Location loc = h.getLocation(plugin);
                String worldName = (loc != null && loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
                Components.sendInfo(sender,
                        Components.t("Home "), Components.valueComp(h.getName()),
                        Components.t(" | world: "), Components.valueComp(worldName)
                );
            }
        }
    }

    public void applyHomeArmorStandVisibility(org.bukkit.entity.Player viewer) {
        if (viewer == null) return;
        try {
            for (Home h : new java.util.ArrayList<>(homes.values())) {
                java.util.UUID asId = h.getArmorStandUuid();
                if (asId == null) continue;
                Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof ArmorStand stand) {
                    try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                    if (viewer.isOp()) {
                        viewer.showEntity(plugin, stand);
                    } else {
                        viewer.hideEntity(plugin, stand);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public void listHomes(CommandSender sender, OfflinePlayer player) {
        Set<UUID> playerHomes = players.getOrDefault(player.getUniqueId(), new HashSet<>());
        if (playerHomes.isEmpty()) {
            Components.sendInfo(sender, Components.playerComp(player.getName()), Components.t(" has no homes."));
        } else {
            List<Home> list = playerHomes.stream().map(homes::get).filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Home::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
            // Header with total count
            Components.sendInfo(sender, Components.playerComp(player.getName()), Components.t(" homes total: "), Components.valueComp(String.valueOf(list.size())));
            for (Home h : list) {
                org.bukkit.Location loc = h.getLocation(plugin);
                String worldName = (loc != null && loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
                Components.sendInfo(sender,
                        Components.t("Home "), Components.valueComp(h.getName()),
                        Components.t(" | world: "), Components.valueComp(worldName)
                );
            }
        }
    }

    public void listWorlds(CommandSender sender) {
        Components.sendInfo(sender, Components.t("Worlds homes status:"));
        for (World world : plugin.getServer().getWorlds()) {
            Components.sendInfo(sender, Components.valueComp(world.getName()), Components.t(": "), (isWorldEnabled(world.getUID()) ? Components.t("ENABLED", NamedTextColor.GREEN) : Components.t("DISABLED", NamedTextColor.RED)));
        }
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public void teleportHome(Player commandSender, UUID uuid, String homeName) {
        Set<UUID> playerHomes = players.getOrDefault(uuid, new HashSet<>());
        for (UUID homeUUID : playerHomes) {
            Home home = homes.get(homeUUID);
            if (home != null && home.getName().equals(homeName)) {
                Location loc = home.getLocation(plugin);
                if (loc == null || loc.getWorld() == null) {
                    Components.sendErrorMessage(commandSender, "Home location is invalid.");
                    return;
                }
                // Enforce teleport cooldown for non-ops
                if (!commandSender.isOp()) {
                    long now = System.currentTimeMillis();
                    long last = lastHomesTeleport.getOrDefault(commandSender.getUniqueId(), 0L);
                    long waitMs = (long) TELEPORT_COOLDOWN_SECONDS * 1000L - (now - last);
                    if (waitMs > 0) {
                        long remainingSec = (waitMs + 999) / 1000; // ceil to seconds
                        Components.sendErrorMessage(commandSender, "You must wait " + formatDuration(remainingSec) + " before using /homes teleport again.");
                        return;
                    }
                    lastHomesTeleport.put(commandSender.getUniqueId(), now);
                }
                plugin.teleportsService.queueTeleport(commandSender, loc, "Home: " + homeName);
                return;
            }
        }
        Components.sendError(commandSender, Components.t("Home not found: "), Components.valueComp(homeName));
    }

    public boolean removeByArmorStand(java.util.UUID armorStandId) {
        // Find the home with this ArmorStand UUID and remove it (entity already dead)
        for (java.util.Map.Entry<java.util.UUID, Home> e : new java.util.HashMap<>(homes).entrySet()) {
            Home home = e.getValue();
            if (armorStandId.equals(home.getArmorStandUuid())) {
                java.util.UUID owner = home.getPlayer();
                // update indices
                Location oldLoc = home.getLocation(plugin);
                if (oldLoc != null) {
                    locations.remove(oldLoc);
                }
                // remove from main map and player's set
                homes.remove(e.getKey());
                java.util.Set<java.util.UUID> set = players.getOrDefault(owner, new java.util.HashSet<>());
                set.remove(e.getKey());
                if (set.isEmpty()) {
                    players.remove(owner);
                } else {
                    players.put(owner, set);
                }
                owned.put(owner, Math.max(0, owned.getOrDefault(owner, 0) - 1));
                // persist
                save();
                return true;
            }
        }
        return false;
    }

    public boolean hasArmorStand(java.util.UUID armorStandId) {
        if (armorStandId == null) return false;
        for (Home h : new java.util.ArrayList<>(homes.values())) {
            if (armorStandId.equals(h.getArmorStandUuid())) return true;
        }
        return false;
    }

    /**
     * Attempt to adopt an unreferenced, managed ArmorStand into a matching Home.
     * A match is considered if the stand is in the same world and near the stored home location.
     * On adoption, updates the home UUID reference, registers with ArmorStandManager, applies visibility, and saves.
     * Returns true if adopted, false otherwise.
     */
    public boolean adoptArmorStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null || stand.getWorld() == null) return false;
        org.bukkit.Location sLoc = stand.getLocation();
        try {
            for (Home h : new java.util.ArrayList<>(homes.values())) {
                org.bukkit.Location hLoc = h.getLocation(plugin);
                if (hLoc == null || hLoc.getWorld() == null) continue;
                if (!hLoc.getWorld().equals(sLoc.getWorld())) continue;
                // Within ~2 blocks considered the same anchor spot
                if (hLoc.distanceSquared(sLoc) <= 4.0) {
                    java.util.UUID currentId = h.getArmorStandUuid();
                    // If already pointing to this stand, consider it adopted
                    if (stand.getUniqueId().equals(currentId)) return true;
                    // If the home already references an existing ArmorStand entity, do NOT adopt another one.
                    // This prevents flip-flopping between two nearby stands; the sanitizer will remove the extra.
                    if (currentId != null) {
                        org.bukkit.entity.Entity curEnt = plugin.getServer().getEntity(currentId);
                        if (curEnt instanceof org.bukkit.entity.ArmorStand) {
                            return false;
                        }
                    }
                    // Update reference and register (only if no existing entity is present)
                    h.setArmorStandUuid(stand.getUniqueId());
                    try { plugin.armorStandManager.register(stand.getUniqueId(), () -> removeByArmorStand(stand.getUniqueId())); } catch (Throwable ignored) {}
                    // Ensure name visibility and per-player ops-only visibility
                    try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                    try {
                        for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                            if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                        }
                    } catch (Throwable ignored) {}
                    // Persist update
                    save();
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // Helper: check if an ArmorStand is managed by this plugin via PDC tag
    private boolean isManagedStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null) return false;
        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "managed");
            java.lang.Byte b = stand.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.BYTE);
            return b != null && b == (byte)1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // Helper: find any managed ArmorStand near a location within the given radius (blocks)
    private org.bukkit.entity.ArmorStand findNearbyManagedStand(org.bukkit.Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return null;
        double r = Math.max(0.0, radius);
        try {
            java.util.Collection<org.bukkit.entity.Entity> ents = loc.getWorld().getNearbyEntities(loc, r, r, r);
            for (org.bukkit.entity.Entity e : ents) {
                if (e instanceof org.bukkit.entity.ArmorStand st && isManagedStand(st)) {
                    return st;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void ensureArmorStands() {
        // For each home, ensure the ArmorStand exists and is named/visible correctly
        for (Home home : new java.util.ArrayList<>(homes.values())) {
            java.util.UUID asId = home.getArmorStandUuid();
            org.bukkit.Location location = home.getLocation(plugin);
            if (location == null || location.getWorld() == null) continue;
            org.bukkit.entity.Entity ent = (asId != null) ? plugin.getServer().getEntity(asId) : null;
            if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                // Already present: ensure registered and visibility
                try { plugin.armorStandManager.register(stand.getUniqueId(), () -> removeByArmorStand(stand.getUniqueId())); } catch (Throwable ignored) {}
                try {
                    try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                    for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                        if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                    }
                } catch (Throwable ignored) {}
                continue;
            }
            // No entity currently accessible for the stored UUID. If the chunk isn't loaded, skip to avoid duplicating.
            boolean chunkLoaded;
            try {
                int cx = location.getBlockX() >> 4; int cz = location.getBlockZ() >> 4;
                chunkLoaded = location.getWorld().isChunkLoaded(cx, cz);
            } catch (Throwable ignored) {
                chunkLoaded = true; // be permissive if API not available
            }
            if (!chunkLoaded) continue;
            // Try to reuse an existing managed ArmorStand near the home location before creating a new one
            org.bukkit.entity.ArmorStand nearby = findNearbyManagedStand(location, 2.5);
            if (nearby != null) {
                if (!nearby.getUniqueId().equals(asId)) {
                    home.setArmorStandUuid(nearby.getUniqueId());
                    save();
                }
                try { plugin.armorStandManager.register(nearby.getUniqueId(), () -> removeByArmorStand(nearby.getUniqueId())); } catch (Throwable ignored) {}
                try {
                    try { nearby.setCustomNameVisible(true); } catch (Throwable ignored) {}
                    for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                        if (viewer.isOp()) viewer.showEntity(plugin, nearby); else viewer.hideEntity(plugin, nearby);
                    }
                } catch (Throwable ignored) {}
                continue;
            }
            // Spawn a new armor stand as a last resort
            try {
                String playerName = plugin.getServer().getOfflinePlayer(home.getPlayer()).getName();
                String asName = "Home " + home.getName() + " by " + (playerName != null ? playerName : home.getPlayer().toString());
                java.util.UUID id = plugin.armorStandManager.create(location, asName);
                if (id != null) {
                    home.setArmorStandUuid(id);
                    try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
                    // Per-player visibility: ops only
                    try {
                        org.bukkit.entity.Entity ent2 = plugin.getServer().getEntity(id);
                        if (ent2 instanceof org.bukkit.entity.ArmorStand newStand) {
                            try { newStand.setCustomNameVisible(true); } catch (Throwable ignored) {}
                            for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                                if (viewer.isOp()) viewer.showEntity(plugin, newStand); else viewer.hideEntity(plugin, newStand);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to respawn ArmorStand for home: " + t.getMessage());
            }
        }
    }

    public List<String> getHomesNames(UUID target) {
        Set<UUID> ids = players.getOrDefault(target, Collections.emptySet());
        List<String> names = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Home h = homes.get(id);
            if (h != null) names.add(h.getName());
        }
        return names;
    }
}
