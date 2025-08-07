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

        // Only teleport if the player is between gold blocks
        // This prevents premature teleportation when the player is just near a portal
        if (isInPortal) {
            // Teleport the player to the warp
            boolean success = plugin.getWarpsManager().teleportToWarp(player, toWarp.getName(), null);
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
            plugin.getWarpsManager().removePortalByPunch(player, block);
        }
    }
}
