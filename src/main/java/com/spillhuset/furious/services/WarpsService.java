package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.Warp;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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

public class WarpsService {
    private final Furious plugin;

    // name -> warp
    private final Map<String, Warp> warps = new HashMap<>();

    private File warpsFile;
    private FileConfiguration warpsConfig;

    public WarpsService(Furious instance) {
        this.plugin = instance.getInstance();
    }

    public void load() {
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        try {
            if (!warpsFile.exists()) warpsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating warps.yml: " + e.getMessage());
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);

        warps.clear();
        ConfigurationSection root = warpsConfig.getConfigurationSection("warps");
        if (root != null) {
            for (String name : root.getKeys(false)) {
                ConfigurationSection ws = root.getConfigurationSection(name);
                if (ws == null) continue;
                try {
                    UUID world = UUID.fromString(Objects.requireNonNull(ws.getString("world")));
                    double x = ws.getDouble("x");
                    double y = ws.getDouble("y");
                    double z = ws.getDouble("z");
                    float yaw = (float) ws.getDouble("yaw");
                    float pitch = (float) ws.getDouble("pitch");
                    double cost = ws.getDouble("cost", 0.0d);
                    String password = ws.getString("password", null);
                    String portal = ws.getString("portal", null);
                    Warp warp = new Warp(name, world, x, y, z, yaw, pitch, cost, password, portal);
                    String armorStr = ws.getString("armorStand", null);
                    if (armorStr != null) {
                        try {
                            warp.setArmorStandUuid(UUID.fromString(armorStr));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    warps.put(name.toLowerCase(), warp);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load warp " + name + ": " + e.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection root = out.createSection("warps");
        for (Warp warp : warps.values()) {
            ConfigurationSection ws = root.createSection(warp.getName());
            ws.set("world", warp.getWorld().toString());
            ws.set("x", warp.getX());
            ws.set("y", warp.getY());
            ws.set("z", warp.getZ());
            ws.set("yaw", warp.getYaw());
            ws.set("pitch", warp.getPitch());
            ws.set("cost", warp.getCost());
            if (warp.getPassword() != null) ws.set("password", warp.getPassword());
            if (warp.getPortalTarget() != null) ws.set("portal", warp.getPortalTarget());
            if (warp.getArmorStandUuid() != null) ws.set("armorStand", warp.getArmorStandUuid().toString());
        }
        warpsConfig = out;
        try {
            out.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving warps.yml: " + e.getMessage());
        }
    }

    public @Nullable Warp getWarp(String name) {
        if (name == null) return null;
        return warps.get(name.toLowerCase());

    }

    public Set<String> getWarpNames() {
        Set<String> names = new HashSet<>();
        for (Warp w : warps.values()) {
            if (w != null && w.getName() != null) names.add(w.getName());
        }
        return names;
    }

    private boolean isInSafeClaim(@NotNull Location loc) {
        if (loc.getWorld() == null) return false;
        UUID worldId = loc.getWorld().getUID();
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        UUID owner = plugin.guildService.getClaimOwner(worldId, cx, cz);
        if (owner == null) return false;
        Guild g = plugin.guildService.getGuildById(owner);
        return g != null && g.getType() == GuildType.SAFE;
    }

    private String buildArmorStandName(@NotNull Warp warp) {
        String name = warp.getName();
        double c = Math.max(0, warp.getCost());
        String costStr = (Math.floor(c) == c) ? String.valueOf((long) c) : String.valueOf(c);
        String passStr = (warp.getPassword() == null || warp.getPassword().isBlank()) ? "-" : warp.getPassword();
        return "Warp: " + name + " " + costStr + " " + passStr;
    }

    private void applyArmorStandNameAndVisibility(@NotNull Warp warp) {
        if (warp.getArmorStandUuid() == null) return;
        Entity ent = plugin.getServer().getEntity(warp.getArmorStandUuid());
        if (!(ent instanceof ArmorStand stand)) return;
        try {
            stand.customName(Component.text(buildArmorStandName(warp)));
        } catch (Throwable ignored) {
        }
        try {
            stand.setCustomNameVisible(true);
        } catch (Throwable ignored) {
        }
        // Per-player visibility: show to ops, hide from others
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            try {
                if (p.isOp()) {
                    p.showEntity(plugin, stand);
                } else {
                    p.hideEntity(plugin, stand);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    // Admin actions (OP only via commands)
    public void setWarp(@NotNull CommandSender sender, @NotNull String name, @NotNull Location loc) {
        if (warps.containsKey(name.toLowerCase())) {
            Components.sendErrorMessage(sender, "Warp already exists: " + name);
            return;
        }
        if (!isInSafeClaim(loc)) {
            Components.sendErrorMessage(sender, "Warp must be set in a SAFE guild claim.");
            return;
        }
        Warp warp = new Warp(name, loc);
        // spawn armor stand like Homes
        spawnArmorStandForWarp(warp);
        applyArmorStandNameAndVisibility(warp);
        warps.put(name.toLowerCase(), warp);
        save();
        Components.sendSuccess(sender, Components.t("Warp "), Components.valueComp(name), Components.t(" set."));
    }

    public void moveWarp(@NotNull CommandSender sender, @NotNull String name, @NotNull Location loc) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: "),Components.valueComp(name));
            return;
        }
        if (!isInSafeClaim(loc)) {
            Components.sendErrorMessage(sender, "Warp may only be moved to a SAFE guild claim.");
            return;
        }
        // move armor stand if present
        try {
            if (warp.getArmorStandUuid() != null) {
                Entity ent = plugin.getServer().getEntity(warp.getArmorStandUuid());
                if (ent instanceof ArmorStand) {
                    ent.teleportAsync(loc);
                }
            }
        } catch (Throwable ignored) {
        }
        warp.setLocation(loc);
        save();
        Components.sendSuccess(sender, Components.t("Warp "), Components.valueComp(name), Components.t(" moved."));
    }

    public void renameWarp(@NotNull CommandSender sender, @NotNull String oldName, @NotNull String newName) {
        if (oldName.equalsIgnoreCase(newName)) {
            Components.sendInfoMessage(sender, "Old and new names are the same.");
            return;
        }
        Warp warp = getWarp(oldName);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: "),Components.valueComp( oldName));
            return;
        }
        if (warps.containsKey(newName.toLowerCase())) {
            Components.sendError(sender, Components.t("Another warp already has that name: "),Components.valueComp( newName));
            return;
        }
        warps.remove(oldName.toLowerCase());
        warp.setName(newName);
        // update armor stand name and visibility
        applyArmorStandNameAndVisibility(warp);
        warps.put(newName.toLowerCase(), warp);
        save();
        Components.sendSuccess(sender, Components.t("Warp "), Components.valueComp(oldName), Components.t(" renamed to "), Components.valueComp(newName));
    }

    public void setCost(@NotNull CommandSender sender, @NotNull String name, double cost) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: " ),Components.valueComp( name));
            return;
        }
        if (!Double.isFinite(cost) || cost < 0) {
            Components.sendErrorMessage(sender, "Invalid cost.");
            return;
        }
        warp.setCost(cost);
        // update stand name to reflect cost
        applyArmorStandNameAndVisibility(warp);
        save();
        Components.sendSuccess(sender, Components.t("Warp cost for "), Components.valueComp(name), Components.t(" set to "), Components.amountComp(cost, plugin.walletService));
    }

    public void setPassword(@NotNull CommandSender sender, @NotNull String name, @Nullable String password) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: " ),Components.valueComp( name));
            return;
        }
        warp.setPassword(password);
        // update stand name to reflect password
        applyArmorStandNameAndVisibility(warp);
        save();
        if (password == null || password.isBlank()) {
            Components.sendSuccess(sender, Components.t("Password cleared for warp "), Components.valueComp(name));
        } else {
            Components.sendSuccess(sender, Components.t("Password set for warp "), Components.valueComp(name));
        }
    }

    public void connectPortal(@NotNull CommandSender sender, @NotNull String name, @Nullable String target) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: " ),Components.valueComp( name));
            return;
        }
        if (target != null && getWarp(target) == null) {
            Components.sendError(sender, Components.t("Target warp not found: " ),Components.valueComp( target));
            return;
        }
        warp.setPortalTarget(target);
        save();
        if (target == null) {
            Components.sendSuccess(sender, Components.t("Portal connection cleared for "), Components.valueComp(name));
        } else {
            Components.sendSuccess(sender, Components.t("Portal connected from "), Components.valueComp(name), Components.t(" to "), Components.valueComp(target));
        }
    }

    public void removeWarp(@NotNull CommandSender sender, @NotNull String name) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(sender, Components.t("Warp not found: " ),Components.valueComp( name));
            return;
        }
        // remove armor stand
        try {
            if (warp.getArmorStandUuid() != null) {
                try { plugin.armorStandManager.unregister(warp.getArmorStandUuid()); } catch (Throwable ignored) {}
                Entity ent = plugin.getServer().getEntity(warp.getArmorStandUuid());
                if (ent instanceof ArmorStand) ent.remove();
            }
        } catch (Throwable ignored) {
        }
        warps.remove(name.toLowerCase());
        save();
        Components.sendSuccess(sender, Components.t("Warp "), Components.valueComp(name), Components.t(" removed."));
    }

    public void listWarps(@NotNull CommandSender sender) {
        if (warps.isEmpty()) {
            Components.sendInfoMessage(sender, "No warps have been set.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (Warp w : warps.values()) names.add(w.getName());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        // Header with total count
        Components.sendInfo(sender, Components.t("Warps total: "), Components.valueComp(String.valueOf(names.size())));
        for (String name : names) {
            Warp w = getWarp(name);
            if (w == null) continue;
            boolean costSet = Math.max(0, w.getCost()) > 0.0d;
            boolean passSet = w.getPassword() != null && !w.getPassword().isBlank();
            boolean portalSet = w.getPortalTarget() != null && !w.getPortalTarget().isBlank();
            Components.sendInfo(
                    sender,
                    Components.t("Warp "), Components.valueComp(name),
                    Components.t(" | cost: "), Components.t(costSet ? "YES" : "NO", costSet ? NamedTextColor.GREEN:NamedTextColor.RED),
                    (costSet ? Components.t(" (") : Components.t("")),
                    (costSet ? Components.amountComp(Math.max(0, w.getCost()), plugin.walletService) : Components.t("")),
                    (costSet ? Components.t(")") : Components.t("")),
                    Components.t(" | password: "), Components.t(passSet ? "YES" : "NO", passSet ? NamedTextColor.GREEN:NamedTextColor.RED),
                    (passSet ? Components.t(" (") : Components.t("")),
                    (passSet ? Components.valueComp(w.getPassword()) : Components.t("")),
                    (passSet ? Components.t(")") : Components.t("")),
                    Components.t(" | portal: "), Components.t(portalSet ? "YES" : "NO", portalSet ? NamedTextColor.GREEN:NamedTextColor.RED),
                    (portalSet ? Components.t(" (") : Components.t("")),
                    (portalSet ? Components.valueComp(w.getPortalTarget()) : Components.t("")),
                    (portalSet ? Components.t(")") : Components.t(""))
            );
        }
    }

    // Player action
    public boolean removeByArmorStand(java.util.UUID armorStandId) {
        for (Warp w : new java.util.ArrayList<>(warps.values())) {
            if (armorStandId.equals(w.getArmorStandUuid())) {
                warps.remove(w.getName().toLowerCase());
                save();
                return true;
            }
        }
        return false;
    }

    public boolean hasArmorStand(java.util.UUID armorStandId) {
        if (armorStandId == null) return false;
        for (Warp w : new java.util.ArrayList<>(warps.values())) {
            if (armorStandId.equals(w.getArmorStandUuid())) return true;
        }
        return false;
    }

    public void ensureArmorStands() {
        for (Warp warp : new java.util.ArrayList<>(warps.values())) {
            java.util.UUID asId = warp.getArmorStandUuid();
            Entity ent = (asId != null) ? plugin.getServer().getEntity(asId) : null;
            if (!(ent instanceof ArmorStand)) {
                spawnArmorStandForWarp(warp);
            } else {
                // Ensure registered with manager
                try { plugin.armorStandManager.register(ent.getUniqueId(), () -> removeByArmorStand(ent.getUniqueId())); } catch (Throwable ignored) {}
                applyArmorStandNameAndVisibility(warp);
            }
        }
    }

    public void teleport(@NotNull Player player, @NotNull String name, @Nullable String enteredPassword) {
        Warp warp = getWarp(name);
        if (warp == null) {
            Components.sendError(player, Components.t("Warp not found: "), Components.valueComp(name));
            return;
        }
        if (warp.getPassword() != null && (!warp.getPassword().equals(enteredPassword))) {
            Components.sendErrorMessage(player, "Incorrect password.");
            return;
        }
        double cost = Math.max(0, warp.getCost());
        if (cost > 0) {
            if (!plugin.walletService.subBalance(player.getUniqueId(), cost, "Warp teleport: " + name)) {
                Components.sendError(player, Components.t("Insufficient funds. Cost is "), Components.amountComp(cost, plugin.walletService));
                return;
            }
        }
        Location loc = warp.toLocation(plugin);
        if (loc == null) {
            Components.sendErrorMessage(player, "Warp location is invalid.");
            return;
        }
        plugin.teleportsService.queueTeleport(player, loc, "Warp: " + name);
        // Cost is already deducted above (if any). We keep it simple and do not refund on cancel per requirements.
    }

    private void spawnArmorStandForWarp(Warp warp) {
        Location location = warp.toLocation(plugin);
        if (location == null || location.getWorld() == null) return;
        try {
            java.util.UUID id = plugin.armorStandManager.create(location, buildArmorStandName(warp));
            if (id != null) {
                warp.setArmorStandUuid(id);
                // Register cleanup with armor stand manager
                try { plugin.armorStandManager.register(id, () -> removeByArmorStand(id)); } catch (Throwable ignored) {}
                // Apply per-player visibility (ops only)
                applyArmorStandNameAndVisibility(warp);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to spawn ArmorStand for warp: " + t.getMessage());
        }
    }
}
