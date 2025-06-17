package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.MinigameManager;
import com.spillhuset.furious.minigames.hungergames.ContainerRegistry;
import com.spillhuset.furious.minigames.hungergames.ContainerRegistry.ContainerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listener for container-related events
 */
public class ContainerListener implements Listener {
    private final Furious plugin;
    private final MinigameManager minigameManager;
    private final ContainerRegistry containerRegistry;

    /**
     * Constructor for ContainerListener
     *
     * @param plugin The plugin instance
     * @param containerRegistry The container registry
     */
    public ContainerListener(Furious plugin, ContainerRegistry containerRegistry) {
        this.plugin = plugin;
        this.minigameManager = plugin.getMinigameManager();
        this.containerRegistry = containerRegistry;
    }

    /**
     * Handles block place events to register containers
     *
     * @param event The block place event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockState state = block.getState();

        // Check if the player is in edit mode
        if (!minigameManager.isPlayerInEditMode(player)) {
            return;
        }

        // Check if the block is a container
        if (!(state instanceof Container)) {
            return;
        }

        // Don't register ender chests
        if (block.getType() == Material.ENDER_CHEST) {
            player.sendMessage(Component.text("Ender chests are not registered for automatic filling.", NamedTextColor.YELLOW));
            return;
        }

        // Get the map name
        String mapName = minigameManager.getPlayerEditingMap(player);
        if (mapName == null) {
            return;
        }

        // Determine the container type based on the block type
        ContainerType type;
        if (block.getType() == Material.BARREL) {
            type = ContainerType.BARREL;
        } else if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
            // Ask the player what type of chest this should be
            askChestType(player, block, mapName);
            // The actual registration will happen when the player responds
            return;
        } else {
            // Other container types (like hoppers, dispensers, etc.) are registered as common
            type = ContainerType.COMMON;
        }

        // Register the container
        containerRegistry.registerContainer(mapName, block.getLocation(), type);
        player.sendMessage(Component.text("Container registered as " + type.name() + " type.", NamedTextColor.GREEN));
    }

    /**
     * Handles block break events to unregister containers
     *
     * @param event The block break event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockState state = block.getState();

        // Check if the player is in edit mode
        if (!minigameManager.isPlayerInEditMode(player)) {
            return;
        }

        // Check if the block is a container
        if (!(state instanceof Container)) {
            return;
        }

        // Get the map name
        String mapName = minigameManager.getPlayerEditingMap(player);
        if (mapName == null) {
            return;
        }

        // Unregister the container
        containerRegistry.unregisterContainer(mapName, block.getLocation());
        player.sendMessage(Component.text("Container unregistered.", NamedTextColor.YELLOW));
    }

    /**
     * Asks the player what type of chest this should be
     *
     * @param player The player
     * @param block The block
     * @param mapName The map name
     */
    private void askChestType(Player player, Block block, String mapName) {
        // Send message to player
        player.sendMessage(Component.text("What type of chest is this?", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("1. Common (mostly common items)", NamedTextColor.GREEN));
        player.sendMessage(Component.text("2. Uncommon (mix of common and uncommon items)", NamedTextColor.BLUE));
        player.sendMessage(Component.text("3. Rare (better items)", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Type the number in chat or click on this message.", NamedTextColor.GRAY));

        // Register a chat listener for the player's response
        plugin.getServer().getPluginManager().registerEvents(new ChestTypeResponseListener(plugin, player, block, mapName, containerRegistry), plugin);
    }
}