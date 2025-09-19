package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Warp;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PortalsListener implements Listener {
    private final Furious plugin;
    private final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();
    private int visualizeTaskId = -1;

    public PortalsListener(Furious plugin) {
        this.plugin = plugin.getInstance();
        ensurePortalsDefaultsPersisted();
        // Start visualization task (custom mode only, configurable)
        startVisualizationTask();
    }

    private void ensurePortalsDefaultsPersisted() {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            boolean changed = false;
            String mode = String.valueOf(cfg.getString("portals.mode", "custom"));
            if (!cfg.isSet("portals.mode")) { cfg.set("portals.mode", mode); changed = true; }
            // visualize settings under custom
            if (!cfg.isSet("portals.custom.visualize.enabled")) { cfg.set("portals.custom.visualize.enabled", true); changed = true; }
            if (!cfg.isSet("portals.custom.visualize.tick-interval")) { cfg.set("portals.custom.visualize.tick-interval", 20); changed = true; }
            if (!cfg.isSet("portals.custom.visualize.fill-enabled")) { cfg.set("portals.custom.visualize.fill-enabled", true); changed = true; }
            if (!cfg.isSet("portals.custom.visualize.frame-color.r")) { cfg.set("portals.custom.visualize.frame-color.r", 128); changed = true; }
            if (!cfg.isSet("portals.custom.visualize.frame-color.g")) { cfg.set("portals.custom.visualize.frame-color.g", 0); changed = true; }
            if (!cfg.isSet("portals.custom.visualize.frame-color.b")) { cfg.set("portals.custom.visualize.frame-color.b", 255); changed = true; }
            if (changed) plugin.saveConfig();
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || to.getWorld() == null) return;
        // Only run when player actually changes block coordinates to reduce checks
        if (from != null && from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ() && from.getWorld() != null && from.getWorld().equals(to.getWorld())) {
            return;
        }
        // Mode: custom (region-only) or nether (require standing in nether portal blocks)
        String mode = "custom";
        try {
            mode = String.valueOf(plugin.getConfig().getString("portals.mode", "custom"));
        } catch (Throwable ignored) {}
        boolean netherOnly = "nether".equalsIgnoreCase(mode);
        if (netherOnly) {
            try {
                Material m = to.getBlock().getType();
                if (m != Material.NETHER_PORTAL) return;
            } catch (Throwable ignored) { return; }
        }
        // Simple cooldown to avoid rapid re-triggers
        long now = System.currentTimeMillis();
        Long last = lastTeleport.get(player.getUniqueId());
        if (last != null && (now - last) < 1000L) return;

        Warp portal = plugin.warpsService.findPortalAt(to);
        if (portal == null) return;
        java.util.List<String> targets = new java.util.ArrayList<>(portal.getPortalTargets());
        if (targets.isEmpty()) return;
        String targetName = targets.get(new java.util.Random().nextInt(targets.size()));
        Warp target = plugin.warpsService.getWarp(targetName);
        if (target == null) {
            Components.sendErrorMessage(player, "Portal target warp not found: " + targetName);
            return;
        }
        Location loc = target.toLocation(plugin);
        if (loc == null) {
            Components.sendErrorMessage(player, "Portal target location invalid.");
            return;
        }
        lastTeleport.put(player.getUniqueId(), now);
        plugin.teleportsService.queueTeleport(player, loc, "Portal: " + portal.getName() + " -> " + targetName);
    }

    @EventHandler
    public void onPortalBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        // Determine portal mode
        String mode = "custom";
        try {
            mode = String.valueOf(plugin.getConfig().getString("portals.mode", "custom"));
        } catch (Throwable ignored) {}
        boolean netherMode = "nether".equalsIgnoreCase(mode);

        // In nether mode, only react to Nether portal blocks or obsidian (frame)
        if (netherMode) {
            Material type = block.getType();
            if (type != Material.NETHER_PORTAL && type != Material.OBSIDIAN) return;
        }
        // Find if this break occurred inside any claimed portal region
        Warp portal = plugin.warpsService.findPortalAt(loc);
        if (portal == null) return;
        // If already no targets, nothing to do
        if (!portal.hasPortalTargets()) return;
        // Clear portal connections (keep region so it can be reconnected later)
        try {
            // Use the service API to clear and persist (null -> clear)
            plugin.warpsService.connectPortal(event.getPlayer(), portal.getName(), null);
        } catch (Throwable t) {
            // Fallback: ensure it is cleared to be safe
            portal.setPortalTargets(java.util.Collections.emptyList());
            plugin.warpsService.save();
        }
        Components.sendInfoMessage(event.getPlayer(), "Portal connections for '" + portal.getName() + "' have been broken due to block removal.");
    }

    // Visualization: draw frames and a center plane for custom-mode portal regions using particles
    private void startVisualizationTask() {
        try {
            if (visualizeTaskId != -1) return;
            int interval = 20;
            boolean enabled = true;
            boolean fillEnabled = true;
            int cr = 128, cg = 0, cb = 255;
            try {
                interval = Math.max(5, plugin.getConfig().getInt("portals.custom.visualize.tick-interval", 20));
                enabled = plugin.getConfig().getBoolean("portals.custom.visualize.enabled", true);
                fillEnabled = plugin.getConfig().getBoolean("portals.custom.visualize.fill-enabled", true);
                cr = plugin.getConfig().getInt("portals.custom.visualize.frame-color.r", 128);
                cg = plugin.getConfig().getInt("portals.custom.visualize.frame-color.g", 0);
                cb = plugin.getConfig().getInt("portals.custom.visualize.frame-color.b", 255);
            } catch (Throwable ignored) {}

            final int fInterval = interval;
            final boolean fFill = fillEnabled;

            if (!enabled) return;

            visualizeTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                String mode = "custom";
                try {
                    mode = String.valueOf(plugin.getConfig().getString("portals.mode", "custom"));
                } catch (Throwable ignored) {}
                if (!"custom".equalsIgnoreCase(mode)) return;

                for (String name : plugin.warpsService.getWarpNames()) {
                    Warp w = plugin.warpsService.getWarp(name);
                    if (w == null || !w.hasPortalRegion() || w.getPortalWorld() == null) continue;
                    World world = Bukkit.getWorld(w.getPortalWorld());
                    if (world == null) continue;
                    drawFrame(world, w);
                    if (fFill) drawCenterPlane(world, w);
                }
            }, 40L, fInterval).getTaskId();
        } catch (Throwable ignored) {
        }
    }

    private void drawFrame(World world, Warp w) {
        int x1 = w.getpMinX();
        int y1 = w.getpMinY();
        int z1 = w.getpMinZ();
        int x2 = w.getpMaxX();
        int y2 = w.getpMaxY();
        int z2 = w.getpMaxZ();
        // Step to limit particles
        int step = 2;
        for (int x = x1; x <= x2; x += step) {
            for (int y = y1; y <= y2; y += step) {
                // Z faces
                world.spawnParticle(Particle.PORTAL, x + 0.5, y + 0.5, z1 + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticle(Particle.PORTAL, x + 0.5, y + 0.5, z2 + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
            }
        }
        for (int z = z1; z <= z2; z += step) {
            for (int y = y1; y <= y2; y += step) {
                // X faces
                world.spawnParticle(Particle.PORTAL, x1 + 0.5, y + 0.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticle(Particle.PORTAL, x2 + 0.5, y + 0.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
            }
        }
        for (int x = x1; x <= x2; x += step) {
            for (int z = z1; z <= z2; z += step) {
                // Top and bottom edges
                world.spawnParticle(Particle.PORTAL, x + 0.5, y1 + 0.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticle(Particle.PORTAL, x + 0.5, y2 + 0.5, z + 0.5, 2, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void drawCenterPlane(World world, Warp w) {
        int x1 = w.getpMinX();
        int y1 = w.getpMinY();
        int z1 = w.getpMinZ();
        int x2 = w.getpMaxX();
        int y2 = w.getpMaxY();
        int z2 = w.getpMaxZ();
        int sizeX = Math.max(1, x2 - x1 + 1);
        int sizeZ = Math.max(1, z2 - z1 + 1);
        int step = 2;
        // Choose a plane perpendicular to the larger horizontal axis to look like a portal sheet
        if (sizeX >= sizeZ) {
            int midX = (x1 + x2) / 2;
            for (int y = y1; y <= y2; y += step) {
                for (int z = z1; z <= z2; z += step) {
                    world.spawnParticle(Particle.PORTAL, midX + 0.5, y + 0.5, z + 0.5, 3, 0.0, 0.0, 0.0, 0.0);
                }
            }
        } else {
            int midZ = (z1 + z2) / 2;
            for (int y = y1; y <= y2; y += step) {
                for (int x = x1; x <= x2; x += step) {
                    world.spawnParticle(Particle.PORTAL, x + 0.5, y + 0.5, midZ + 0.5, 3, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}
