package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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

/**
 * Guild homes service. Similar to HomesService but keyed to a Guild UUID and
 * with the constraint that homes must be inside the guild's claimed chunks.
 * ArmorStand naming convention: "Home of <guildName>: <homeName>".
 */
public class GuildHomesService {
    private final Furious plugin;

    // homeUUID -> Home
    private final Map<UUID, Home> homes = new HashMap<>();
    // location -> homeUUID (for fast reverse lookups if needed)
    private final Map<Location, UUID> locations = new HashMap<>();
    // guildUUID -> set of homeUUIDs
    private final Map<UUID, Set<UUID>> guildHomes = new HashMap<>();

    private final Object ioLock = new Object();
    private FileConfiguration config;
    private File file;

    public GuildHomesService(Furious instance) {
        this.plugin = instance.getInstance();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "guild_homes.yml");
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating guild homes file: " + e.getMessage());
        }
        synchronized (ioLock) {
            config = YamlConfiguration.loadConfiguration(file);
        }
        homes.clear();
        guildHomes.clear();
        locations.clear();
        synchronized (ioLock) {
            ConfigurationSection root = config.getConfigurationSection("guilds");
            if (root == null) return;
            for (String gidStr : root.getKeys(false)) {
                UUID gid;
                try {
                    gid = UUID.fromString(gidStr);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                ConfigurationSection gSec = root.getConfigurationSection(gidStr);
                if (gSec == null) continue;
                ConfigurationSection homesSec = gSec.getConfigurationSection("homes");
                if (homesSec == null) continue;
                for (String hidStr : homesSec.getKeys(false)) {
                    ConfigurationSection hSec = homesSec.getConfigurationSection(hidStr);
                    if (hSec == null) continue;
                    try {
                        UUID homeUUID = UUID.fromString(hSec.getName());
                        UUID worldUUID = UUID.fromString(Objects.requireNonNull(hSec.getString("world")));
                        String name = hSec.getString("name");
                        double x = hSec.getDouble("x");
                        double y = hSec.getDouble("y");
                        double z = hSec.getDouble("z");
                        float yaw = (float) hSec.getDouble("yaw");
                        float pitch = (float) hSec.getDouble("pitch");
                        String armorStr = hSec.getString("armorStand", null);
                        Location location = new Location(plugin.getServer().getWorld(worldUUID), x, y, z, yaw, pitch);
                        Home home = new Home(homeUUID, name, location, gid);
                        if (armorStr != null) {
                            try { home.setArmorStandUuid(UUID.fromString(armorStr)); } catch (IllegalArgumentException ignored) {}
                        }
                        guildHomes.computeIfAbsent(gid, k -> new HashSet<>()).add(homeUUID);
                        homes.put(homeUUID, home);
                        locations.put(location, homeUUID);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public void save() {
        synchronized (ioLock) {
            YamlConfiguration newCfg = new YamlConfiguration();
            ConfigurationSection root = newCfg.createSection("guilds");
            for (Map.Entry<UUID, Set<UUID>> e : guildHomes.entrySet()) {
                UUID gid = e.getKey();
                ConfigurationSection gSec = root.createSection(gid.toString());
                ConfigurationSection homesSec = gSec.createSection("homes");
                for (UUID hid : e.getValue()) {
                    Home h = homes.get(hid);
                    if (h == null) continue;
                    ConfigurationSection hSec = homesSec.createSection(hid.toString());
                    hSec.set("world", h.getWorld().toString());
                    hSec.set("name", h.getName());
                    hSec.set("x", h.getX());
                    hSec.set("y", h.getY());
                    hSec.set("z", h.getZ());
                    hSec.set("yaw", h.getYaw());
                    hSec.set("pitch", h.getPitch());
                    if (h.getArmorStandUuid() != null) {
                        hSec.set("armorStand", h.getArmorStandUuid().toString());
                    }
                }
            }
            config = newCfg;
            try {
                newCfg.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save guild_homes.yml: " + e.getMessage());
            }
        }
    }

    public @Nullable Home getHome(@NotNull UUID guildId, @NotNull String name) {
        Set<UUID> set = guildHomes.getOrDefault(guildId, Collections.emptySet());
        for (UUID id : set) {
            Home h = homes.get(id);
            if (h != null && name.equals(h.getName())) return h;
        }
        return null;
    }

    public List<String> getHomesNames(@NotNull UUID guildId) {
        Set<UUID> set = guildHomes.getOrDefault(guildId, Collections.emptySet());
        List<String> names = new ArrayList<>(set.size());
        for (UUID id : set) {
            Home h = homes.get(id);
            if (h != null) names.add(h.getName());
        }
        return names;
    }

    private boolean isInGuildClaims(@NotNull UUID guildId, @NotNull Location loc) {
        if (loc.getWorld() == null) return false;
        UUID worldId = loc.getWorld().getUID();
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        UUID owner = plugin.guildService.getClaimOwner(worldId, cx, cz);
        return guildId.equals(owner);
    }

    private String armorStandName(UUID guildId, String homeName) {
        Guild g = plugin.guildService.getGuildById(guildId);
        String guildName = (g != null ? g.getName() : guildId.toString());
        return "Home of " + guildName + ": " + homeName;
    }

    public void listHomes(CommandSender sender, UUID guildId) {
        List<String> names = getHomesNames(guildId);
        if (names.isEmpty()) {
            Components.sendInfo(sender, Component.text("This guild has no homes."));
        } else {
            Components.sendSuccess(sender, Components.t("Guild homes: "), Components.t(String.join(", ", names)));
        }
    }

    public void teleportHome(Player player, UUID guildId, String homeName) {
        Set<UUID> set = guildHomes.getOrDefault(guildId, Collections.emptySet());
        for (UUID id : set) {
            Home h = homes.get(id);
            if (h != null && homeName.equals(h.getName())) {
                Location loc = h.getLocation(plugin);
                if (loc == null || loc.getWorld() == null) {
                    Components.sendErrorMessage(player, "Guild home location is invalid.");
                    return;
                }
                plugin.teleportsService.queueTeleport(player, loc, "Guild home: " + homeName);
                return;
            }
        }
        Components.sendError(player, Components.t("Guild home not found: "), Components.valueComp(homeName));
    }

    public void setHome(Player actor, UUID guildId, String homeName) {
        World world = actor.getWorld();
        if (!plugin.guildService.isWorldEnabled(world.getUID())) {
            Components.sendErrorMessage(actor, "Guilds are disabled in this world.");
            return;
        }
        Location loc = actor.getLocation();
        if (!isInGuildClaims(guildId, loc)) {
            Components.sendErrorMessage(actor, "Guild homes must be placed inside your guild's claims.");
            return;
        }
        boolean ok = setHome(guildId, homeName, loc);
        if (!ok) {
            Components.sendErrorMessage(actor, "A guild home with this name already exists.");
        } else {
            save();
            Components.sendSuccess(actor, Components.t("Guild home "), Components.valueComp(homeName), Components.t(" set."));
        }
    }

    private boolean setHome(UUID guildId, String homeName, @NotNull Location location) {
        if (getHome(guildId, homeName) != null) return false;
        Home home = new Home(UUID.randomUUID(), homeName, location, guildId);
        try {
            if (location.getWorld() != null) {
                String asName = armorStandName(guildId, homeName);
                java.util.UUID id = plugin.armorStandManager.create(location, asName);
                if (id != null) {
                    home.setArmorStandUuid(id);
                    try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
                    // Apply per-player visibility: show to ops, hide from others
                    try {
                        org.bukkit.entity.Entity ent = plugin.getServer().getEntity(id);
                        if (ent instanceof org.bukkit.entity.ArmorStand st) {
                            for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                                if (viewer.isOp()) viewer.showEntity(plugin, st); else viewer.hideEntity(plugin, st);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to spawn ArmorStand for guild home: " + t.getMessage());
        }
        guildHomes.computeIfAbsent(guildId, k -> new HashSet<>()).add(home.getUuid());
        homes.put(home.getUuid(), home);
        locations.put(location, home.getUuid());
        return true;
    }

    public void moveHome(Player actor, UUID guildId, String homeName) {
        World world = actor.getWorld();
        if (!plugin.guildService.isWorldEnabled(world.getUID())) {
            Components.sendErrorMessage(actor, "Guilds are disabled in this world.");
            return;
        }
        Location loc = actor.getLocation();
        if (!isInGuildClaims(guildId, loc)) {
            Components.sendErrorMessage(actor, "Guild homes must be moved inside your guild's claims.");
            return;
        }
        boolean ok = moveHome(guildId, homeName, loc);
        if (!ok) {
            Components.sendErrorMessage(actor, "Guild home not found: " + homeName);
        } else {
            save();
            Components.sendSuccess(actor, Components.t("Guild home "), Components.valueComp(homeName), Components.t(" moved."));
        }
    }

    private boolean moveHome(UUID guildId, String homeName, @NotNull Location location) {
        Set<UUID> set = guildHomes.getOrDefault(guildId, Collections.emptySet());
        for (UUID id : set) {
            Home h = homes.get(id);
            if (h != null && homeName.equals(h.getName())) {
                Location old = h.getLocation(plugin);
                if (old != null) locations.remove(old);
                try {
                    if (h.getArmorStandUuid() != null) {
                        Entity ent = plugin.getServer().getEntity(h.getArmorStandUuid());
                        if (ent instanceof ArmorStand) {
                            ent.teleportAsync(location);
                        }
                    }
                } catch (Throwable ignored) {}
                h.setLocation(location);
                locations.put(location, id);
                return true;
            }
        }
        return false;
    }

    public void removeHome(CommandSender sender, UUID guildId, String homeName) {
        boolean ok = removeHome(guildId, homeName);
        if (!ok) {
            Components.sendError(sender, Components.t("Guild home with name "), Components.valueComp(homeName), Components.t(" does not exist."));
        } else {
            save();
            Components.sendSuccess(sender, Components.t("Guild home "), Components.valueComp(homeName), Components.t(" removed."));
        }
    }

    private boolean removeHome(UUID guildId, String homeName) {
        Set<UUID> set = guildHomes.getOrDefault(guildId, new HashSet<>());
        Iterator<UUID> it = set.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Home h = homes.get(id);
            if (h != null && homeName.equals(h.getName())) {
                Location old = h.getLocation(plugin);
                if (old != null) locations.remove(old);
                try {
                    if (h.getArmorStandUuid() != null) {
                        try { plugin.armorStandManager.unregister(h.getArmorStandUuid()); } catch (Throwable ignored) {}
                        Entity ent = plugin.getServer().getEntity(h.getArmorStandUuid());
                        if (ent instanceof ArmorStand) {
                            ent.remove();
                        }
                    }
                } catch (Throwable ignored) {}
                homes.remove(id);
                it.remove();
                if (set.isEmpty()) guildHomes.remove(guildId);
                return true;
            }
        }
        return false;
    }

    public void renameHome(CommandSender sender, UUID guildId, String oldName, String newName) {
        if (oldName.equals(newName)) {
            Components.sendInfoMessage(sender, "Old and new names are the same.");
            return;
        }
        // disallow duplicate target name
        for (UUID id : guildHomes.getOrDefault(guildId, Collections.emptySet())) {
            Home h = homes.get(id);
            if (h != null && newName.equals(h.getName())) {
                Components.sendErrorMessage(sender, "A guild home with this name already exists.");
                return;
            }
        }
        boolean ok = renameHome(guildId, oldName, newName);
        if (!ok) {
            Components.sendErrorMessage(sender, "Guild home not found.");
        } else {
            save();
            Components.sendSuccess(sender, Components.t("Guild home "), Components.valueComp(oldName), Components.t(" renamed to "), Components.valueComp(newName));
        }
    }

    private boolean renameHome(UUID guildId, String oldName, String newName) {
        for (UUID id : guildHomes.getOrDefault(guildId, Collections.emptySet())) {
            Home h = homes.get(id);
            if (h != null && oldName.equals(h.getName())) {
                h.setName(newName);
                // Update armor stand name
                try {
                    if (h.getArmorStandUuid() != null) {
                        Entity ent = plugin.getServer().getEntity(h.getArmorStandUuid());
                        if (ent instanceof ArmorStand stand) {
                            String asName = armorStandName(guildId, newName);
                            try { stand.customName(Component.text(asName)); } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
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
     * Attempt to adopt an unreferenced, managed ArmorStand into a matching Guild Home
     * by proximity to the stored home location.
     */
    public boolean adoptArmorStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null || stand.getWorld() == null) return false;
        org.bukkit.Location sLoc = stand.getLocation();
        try {
            for (Home h : new java.util.ArrayList<>(homes.values())) {
                org.bukkit.Location hLoc = h.getLocation(plugin);
                if (hLoc == null || hLoc.getWorld() == null) continue;
                if (!hLoc.getWorld().equals(sLoc.getWorld())) continue;
                if (hLoc.distanceSquared(sLoc) <= 4.0) {
                    if (stand.getUniqueId().equals(h.getArmorStandUuid())) return true;
                    h.setArmorStandUuid(stand.getUniqueId());
                    try { plugin.armorStandManager.register(stand.getUniqueId(), () -> removeByArmorStand(stand.getUniqueId())); } catch (Throwable ignored) {}
                    try {
                        for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                            if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                        }
                    } catch (Throwable ignored) {}
                    save();
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean removeByArmorStand(java.util.UUID armorStandId) {
        for (java.util.Map.Entry<java.util.UUID, Home> e : new java.util.HashMap<>(homes).entrySet()) {
            Home h = e.getValue();
            if (armorStandId.equals(h.getArmorStandUuid())) {
                // find guild mapping containing this home id
                java.util.UUID gid = h.getPlayer();
                java.util.Set<java.util.UUID> set = guildHomes.getOrDefault(gid, new java.util.HashSet<>());
                set.remove(h.getUuid());
                if (set.isEmpty()) guildHomes.remove(gid); else guildHomes.put(gid, set);
                Location old = h.getLocation(plugin);
                if (old != null) locations.remove(old);
                homes.remove(e.getKey());
                save();
                return true;
            }
        }
        return false;
    }

    public void ensureArmorStands() {
        for (Home home : new java.util.ArrayList<>(homes.values())) {
            java.util.UUID asId = home.getArmorStandUuid();
            org.bukkit.entity.Entity ent = (asId != null) ? plugin.getServer().getEntity(asId) : null;
            if (!(ent instanceof org.bukkit.entity.ArmorStand stand)) {
                Location location = home.getLocation(plugin);
                if (location == null || location.getWorld() == null) continue;
                try {
                    String asName = armorStandName(home.getPlayer(), home.getName());
                    java.util.UUID id = plugin.armorStandManager.create(location, asName);
                    if (id != null) {
                        home.setArmorStandUuid(id);
                        try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
                        try {
                            org.bukkit.entity.Entity ent2 = plugin.getServer().getEntity(id);
                            if (ent2 instanceof org.bukkit.entity.ArmorStand newStand) { newStand.setInvisible(true); }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to respawn ArmorStand for guild home: " + t.getMessage());
                }
            } else {
                try {
                    String asName = armorStandName(home.getPlayer(), home.getName());
                    stand.customName(net.kyori.adventure.text.Component.text(asName));
                } catch (Throwable ignored) {}
                try { plugin.armorStandManager.register(stand.getUniqueId(), () -> removeByArmorStand(stand.getUniqueId())); } catch (Throwable ignored) {}
                // Ensure per-player visibility for existing stand
                try {
                    for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                        if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    public void applyGuildHomeArmorStandVisibility(org.bukkit.entity.Player viewer) {
        if (viewer == null) return;
        try {
            for (Home h : new java.util.ArrayList<>(homes.values())) {
                java.util.UUID asId = h.getArmorStandUuid();
                if (asId == null) continue;
                org.bukkit.entity.Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                    if (viewer.isOp()) {
                        viewer.showEntity(plugin, stand);
                    } else {
                        viewer.hideEntity(plugin, stand);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean isWorldEnabled(UUID worldId) {
        return plugin.guildService.isWorldEnabled(worldId);
    }
}
