package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PlayerVisibilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Listener for player visibility events
 */
public class PlayerVisibilityListener implements Listener {
    private final Furious plugin;
    private final PlayerVisibilityManager playerVisibilityManager;

    /**
     * Creates a new PlayerVisibilityListener.
     *
     * @param plugin The plugin instance
     */
    public PlayerVisibilityListener(Furious plugin) {
        this.plugin = plugin;
        this.playerVisibilityManager = plugin.getPlayerVisibilityManager();
    }

    /**
     * Handles player join events to maintain visibility settings
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // Apply visibility settings for all hidden players to the joining player
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (playerVisibilityManager.isPlayerHiddenFromLocatorBar(onlinePlayer)) {
                // Hide all hidden players from the joining player
                joiningPlayer.hidePlayer(plugin, onlinePlayer);
            }
        }

        // If the joining player was previously hidden, reapply the hidden status
        if (playerVisibilityManager.isPlayerHiddenFromLocatorBar(joiningPlayer)) {
            playerVisibilityManager.hidePlayerFromLocatorBar(joiningPlayer);
        }

        // Check if the player is an operator and should be hidden
        if (joiningPlayer.isOp() && !playerVisibilityManager.isPlayerHiddenFromLocatorBar(joiningPlayer)) {
            playerVisibilityManager.hidePlayerFromLocatorBar(joiningPlayer);
        } else if (!joiningPlayer.isOp() && playerVisibilityManager.isPlayerHiddenFromLocatorBar(joiningPlayer)) {
            playerVisibilityManager.showPlayerOnLocatorBar(joiningPlayer);
        }
    }

    /**
     * Handles player command preprocessing events to detect op/deop commands
     *
     * @param event The player command preprocess event
     */
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        // Check for op/deop commands
        if (command.startsWith("/op ") || command.startsWith("/minecraft:op ")) {
            // Extract the target player name
            String[] parts = command.split(" ", 2);
            if (parts.length > 1) {
                String targetPlayerName = parts[1].trim();
                Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);

                if (targetPlayer != null) {
                    // Schedule a task to run after the command is processed
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (targetPlayer.isOp() && !playerVisibilityManager.isPlayerHiddenFromLocatorBar(targetPlayer)) {
                            playerVisibilityManager.hidePlayerFromLocatorBar(targetPlayer);
                        }
                    });
                }
            }
        } else if (command.startsWith("/deop ") || command.startsWith("/minecraft:deop ")) {
            // Extract the target player name
            String[] parts = command.split(" ", 2);
            if (parts.length > 1) {
                String targetPlayerName = parts[1].trim();
                Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);

                if (targetPlayer != null) {
                    // Schedule a task to run after the command is processed
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!targetPlayer.isOp() && playerVisibilityManager.isPlayerHiddenFromLocatorBar(targetPlayer)) {
                            playerVisibilityManager.showPlayerOnLocatorBar(targetPlayer);
                        }
                    });
                }
            }
        }
    }

    /**
     * Handles server load event to check all online players for operator status
     *
     * @param event The server load event
     */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // Schedule a task to run after the server is fully loaded
        plugin.getServer().getScheduler().runTask(plugin, this::checkAllPlayersOperatorStatus);
    }

    /**
     * Checks all online players for operator status and updates their visibility accordingly
     */
    private void checkAllPlayersOperatorStatus() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.isOp() && !playerVisibilityManager.isPlayerHiddenFromLocatorBar(player)) {
                playerVisibilityManager.hidePlayerFromLocatorBar(player);
            } else if (!player.isOp() && playerVisibilityManager.isPlayerHiddenFromLocatorBar(player)) {
                playerVisibilityManager.showPlayerOnLocatorBar(player);
            }
        }
    }
}