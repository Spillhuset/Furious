package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Ensures ops are hidden from non-ops.
 * Rules:
 *  - If target is op and viewer is NOT op => hide target from viewer
 *  - Otherwise => show target to viewer
 *
 * Triggers:
 *  - On player join
 *  - After /op or /deop executed (by player or console)
 */
public class OpVisibilityListener implements Listener {
    private final Furious plugin;

    public OpVisibilityListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        applyVisibilityFor(joining);
        // Ensure sleeping check is ignored for ops
        applySleepingFlagFor(joining);

        // Also ensure the joining player's view of already-online ops is correct
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(joining)) continue;
            applyPairVisibility(joining, other);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage(); // includes leading '/'
        if (shouldRecalculate(msg)) {
            // Run a tick later so the op/deop has taken effect
            new BukkitRunnable() {
                @Override
                public void run() {
                    recalcForCommand(msg, event.getPlayer());
                }
            }.runTask(plugin);
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand();
        if (shouldRecalculate(cmd)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    recalcForCommand(cmd, null);
                }
            }.runTask(plugin);
        }
    }

    private boolean shouldRecalculate(String raw) {
        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1);
        s = s.toLowerCase();
        return s.startsWith("op ") || s.startsWith("deop ") || s.startsWith("minecraft:op ") || s.startsWith("minecraft:deop ");
    }

    private void recalcForCommand(String raw, Player issuer) {
        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1);
        String lower = s.toLowerCase();
        boolean isOpCmd = lower.startsWith("op ") || lower.startsWith("minecraft:op ");
        boolean isDeopCmd = lower.startsWith("deop ") || lower.startsWith("minecraft:deop ");
        String[] parts = s.split("\\s+");
        if (parts.length < 2) {
            // No specific target provided; just re-apply globally
            applyVisibilityAll();
            // Also apply sleeping ignore flags globally
            for (Player p : Bukkit.getOnlinePlayers()) {
                applySleepingFlagFor(p);
            }
            return;
        }
        String targetName = parts[1];
        Player target = Bukkit.getPlayerExact(targetName);
        // If target is online, apply visibility rules considering new op status
        if (target != null) {
            applyVisibilityFor(target);
            // Ensure sleeping check is ignored for ops
            applySleepingFlagFor(target);
            // Also ensure everyone else's visibility towards target is correct and vice versa
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(target)) continue;
                applyPairVisibility(other, target);
                applyPairVisibility(target, other);
            }
            // Update ArmorStand visibility for the target viewer across all systems
            applyArmorStandVisibilityForViewer(target);
        } else {
            // Target offline or not found; re-apply globally for safety
            applyVisibilityAll();
            // Also apply sleeping ignore flags globally and update ArmorStand visibility
            for (Player p : Bukkit.getOnlinePlayers()) {
                applySleepingFlagFor(p);
                applyArmorStandVisibilityForViewer(p);
            }
        }
    }

    /**
     * Apply visibility for this target with respect to all other online players.
     */
    private void applyVisibilityFor(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            applyPairVisibility(viewer, target);
        }
    }

    /**
     * Apply pairwise rule for how viewer sees target.
     */
    private void applyPairVisibility(Player viewer, Player target) {
        try {
            if (target.isOp() && !viewer.isOp()) {
                viewer.hidePlayer(plugin, target);
            } else {
                viewer.showPlayer(plugin, target);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Apply visibility rules across all pairs online.
     */
    private void applyVisibilityAll() {
        for (Player a : Bukkit.getOnlinePlayers()) {
            for (Player b : Bukkit.getOnlinePlayers()) {
                if (a.equals(b)) continue;
                applyPairVisibility(a, b);
            }
        }
    }

    // Re-apply per-viewer ArmorStand visibility for all systems (ops-only markers)
    private void applyArmorStandVisibilityForViewer(Player viewer) {
        if (viewer == null) return;
        try {
            if (plugin.shopsService != null) {
                plugin.shopsService.applyShopArmorStandVisibilityForViewer(viewer);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.banksService != null) {
                plugin.banksService.applyBankArmorStandVisibilityForViewer(viewer);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.homesService != null) {
                plugin.homesService.applyHomeArmorStandVisibility(viewer);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.guildHomesService != null) {
                plugin.guildHomesService.applyGuildHomeArmorStandVisibility(viewer);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.warpsService != null) {
                for (String name : plugin.warpsService.getWarpNames()) {
                    com.spillhuset.furious.utils.Warp w = plugin.warpsService.getWarp(name);
                    if (w == null || w.getArmorStandUuid() == null) continue;
                    org.bukkit.entity.Entity ent = plugin.getServer().getEntity(w.getArmorStandUuid());
                    if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                        if (viewer.isOp()) viewer.showEntity(plugin, stand); else viewer.hideEntity(plugin, stand);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    // Ensure ops are excluded from sleep requirement; non-ops follow normal rules
    private void applySleepingFlagFor(Player p) {
        try {
            p.setSleepingIgnored(p.isOp());
        } catch (Throwable ignored) {}
    }
}
