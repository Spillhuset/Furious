package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Warp;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PortalsListener implements Listener {
    private final Furious plugin;
    private final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();

    public PortalsListener(Furious plugin) { this.plugin = plugin.getInstance(); }

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
        String targetName = portal.getPortalTarget();
        if (targetName == null || targetName.isBlank()) return;
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
}
