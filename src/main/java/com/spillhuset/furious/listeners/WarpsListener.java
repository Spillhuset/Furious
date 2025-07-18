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
import org.bukkit.event.block.BlockBreakEvent;
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
     * Handles player movement to detect when a player is between gold blocks in a portal.
     * Teleports players when they are between the gold blocks that define a portal.
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

        Location fromLocation = event.getFrom();
        Location toLocation = player.getLocation();

        // Debug log
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " moved to " + toLocation.getBlockX() + ", " + toLocation.getBlockY() + ", " + toLocation.getBlockZ());

        // Check if the player was in a portal at their previous location
        Warp fromWarp = plugin.getWarpsManager().getWarpByPortal(fromLocation);

        // Check if the player is in a portal at their new location
        Warp toWarp = plugin.getWarpsManager().getWarpByPortal(toLocation);

        // Check if the player was between gold blocks in their previous location
        boolean wasInPortal = fromWarp != null && plugin.getWarpsManager().isPlayerBetweenGoldBlocks(fromLocation, fromWarp);

        // Check if the player is between gold blocks in their current location
        boolean isInPortal = toWarp != null && plugin.getWarpsManager().isPlayerBetweenGoldBlocks(toLocation, toWarp);

        // Check if the player is in a portal area (even if not exactly between gold blocks)
        boolean isInPortalArea = toWarp != null;

        // Debug log to help diagnose why isInPortalArea might be false
        plugin.getLogger().info("[DEBUG] isInPortalArea: " + isInPortalArea + ", toWarp: " + (toWarp != null ? toWarp.getName() : "null"));

        // Only teleport if the player is between gold blocks
        // This prevents premature teleportation when the player is just near a portal
        if (isInPortal) {
            // Debug log
            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is between gold blocks for warp " + toWarp.getName());

            // Teleport the player to the warp
            plugin.getLogger().info("[DEBUG] Attempting to teleport player " + player.getName() + " to warp " + toWarp.getName());
            boolean success = plugin.getWarpsManager().teleportToWarp(player, toWarp.getName(), null);

            if (!success) {
                plugin.getLogger().info("[DEBUG] Teleport failed for player " + player.getName() + " to warp " + toWarp.getName() + 
                                       " (insufficient funds, wrong password, or lack of permission)");
            } else {
                plugin.getLogger().info("[DEBUG] Teleport success: " + success);
            }
        } else if (isInPortalArea) {
            // Debug log - player is in portal area but not between gold blocks
            plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is in portal area for warp " + toWarp.getName() + " but not between gold blocks");
        } else {
            // Debug log
            // These cases should be rare since we check for isInPortal first
            if (isInPortal) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is between gold blocks for warp " + toWarp.getName() + 
                                       " but teleportation failed for some reason");
            } else if (isInPortalArea) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is in portal area for warp " + toWarp.getName() + 
                                       " but teleportation failed for some reason");
            } else if (wasInPortal) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " was in portal for warp " + fromWarp.getName() + 
                                       " but is no longer in it");
            } else {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " is not in a portal");
            }

            // Check if the player is standing on a portal block
            Material blockType = toLocation.getBlock().getType();
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

    /**
     * Handles block break events to detect portal destruction.
     * If an op or admin breaks a portal block, the entire portal will collapse.
     *
     * @param event The block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Only process if the player is an op
        if (!player.isOp()) {
            return;
        }

        // Check if the broken block is part of a portal
        if (plugin.getWarpsManager().isPortalFrame(block) || 
            plugin.getWarpsManager().isPortalBlock(block.getType())) {

            // Try to remove the portal
            if (plugin.getWarpsManager().removePortalByPunch(player, block)) {
                // Portal was successfully removed
                plugin.getLogger().info("[DEBUG] Portal collapsed after block break by " + player.getName());
            }
        }
    }
}
