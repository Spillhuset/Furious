package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Warp;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for warp-related events.
 */
public class WarpsListener implements Listener {
    private final Furious plugin;

    /**
     * Creates a new WarpsListener.
     *
     * @param plugin The plugin instance
     */
    public WarpsListener(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player movement to detect portal entry.
     *
     * @param event The player move event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if the player has moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Skip if the player is already teleporting
        if (plugin.getTeleportManager().isPlayerTeleporting(player)) {
            return;
        }

        Location location = player.getLocation();

        // Debug log
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " moved to " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());

        // Check if the player is in a portal
        Warp warp = plugin.getWarpsManager().getWarpByPortal(location);
        if (warp != null) {
            // Debug log
            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is in portal for warp " + warp.getName());

            // Teleport the player to the warp
            plugin.getLogger().info("[DEBUG] Attempting to teleport player " + player.getName() + " to warp " + warp.getName());
            boolean success = plugin.getWarpsManager().teleportToWarp(player, warp.getName(), null);
            plugin.getLogger().info("[DEBUG] Teleport success: " + success);
        } else {
            // Debug log
            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is not in a portal");

            // Check if the player is standing on a portal block
            Material blockType = location.getBlock().getType();
            plugin.getLogger().info("[DEBUG] Player is standing on block type: " + blockType.name());
        }
    }

    /**
     * Handles player interaction with blocks to detect portal frame destruction.
     *
     * @param event The player interact event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only check left clicks on blocks
        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Check if the block is a portal frame and the player is an op
        if (player.isOp() && plugin.getWarpsManager().isPortalFrame(block)) {
            // Remove the portal
            if (plugin.getWarpsManager().removePortalByPunch(player, block)) {
                event.setCancelled(true); // Prevent the block from breaking
            }
        }
    }
}
