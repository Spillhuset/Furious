package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.minigames.hungergames.ContainerRegistry;
import com.spillhuset.furious.minigames.hungergames.ContainerRegistry.ContainerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for handling player responses when selecting chest types
 */
public class ChestTypeResponseListener implements Listener {
    private final Furious plugin;
    private final Player player;
    private final Block block;
    private final String mapName;
    private final ContainerRegistry containerRegistry;

    /**
     * Constructor for ChestTypeResponseListener
     *
     * @param plugin The plugin instance
     * @param player The player
     * @param block The block
     * @param mapName The map name
     * @param containerRegistry The container registry
     */
    public ChestTypeResponseListener(Furious plugin, Player player, Block block, String mapName, ContainerRegistry containerRegistry) {
        this.plugin = plugin;
        this.player = player;
        this.block = block;
        this.mapName = mapName;
        this.containerRegistry = containerRegistry;
    }

    /**
     * Handles player chat events to process chest type selection
     *
     * @param event The chat event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Only process messages from the player who placed the chest
        if (!event.getPlayer().equals(player)) {
            return;
        }

        // Get the message
        String message = event.getMessage().trim();

        // Process the response
        ContainerType type;
        switch (message) {
            case "1":
                type = ContainerType.COMMON;
                break;
            case "2":
                type = ContainerType.UNCOMMON;
                break;
            case "3":
                type = ContainerType.RARE;
                break;
            default:
                // Invalid response, ask again
                player.sendMessage(Component.text("Invalid response. Please type 1, 2, or 3.", NamedTextColor.RED));
                return;
        }

        // Cancel the chat event so the message doesn't appear in chat
        event.setCancelled(true);

        // Register the container with the selected type
        // This needs to be run on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            containerRegistry.registerContainer(mapName, block.getLocation(), type);
            player.sendMessage(Component.text("Container registered as " + type.name() + " type.", NamedTextColor.GREEN));

            // Unregister this listener
            HandlerList.unregisterAll(this);
        });
    }

    /**
     * Handles player quit events to clean up the listener
     *
     * @param event The quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // If the player quits, unregister this listener
        if (event.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }
}