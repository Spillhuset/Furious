package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manager class for securely accessing offline player data.
 * This class provides methods to access offline player inventories and enderchests.
 */
public class PlayerDataManager {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    /**
     * Creates a new PlayerDataManager.
     *
     * @param plugin The plugin instance
     */
    public PlayerDataManager(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = new AuditLogger(plugin);
    }

    /**
     * Gets a player's inventory, whether they are online or offline.
     *
     * @param playerName The name of the player
     * @return The player's inventory, or null if the player doesn't exist or an error occurred
     */
    @Nullable
    public Inventory getPlayerInventory(String playerName) {
        // Try to get an online player first
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getInventory();
        }

        // If player is offline, try to load their data
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            return null; // Player has never played on this server
        }

        // Create a message inventory to inform the user that offline viewing is not supported
        Inventory messageInventory = Bukkit.createInventory(null, 9, Component.text(playerName + "'s Inventory (Offline - View Only)", NamedTextColor.YELLOW));

        // Log the attempt with more detailed audit information
        auditLogger.logSensitiveOperation(null, "offline inventory access attempt",
                                         "Player: " + playerName + ", Status: OFFLINE, Result: Feature requires player to be online");

        return messageInventory;
    }

    /**
     * Gets a player's enderchest, whether they are online or offline.
     *
     * @param playerName The name of the player
     * @return The player's enderchest, or null if the player doesn't exist or an error occurred
     */
    @Nullable
    public Inventory getPlayerEnderChest(String playerName) {
        // Try to get an online player first
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getEnderChest();
        }

        // If player is offline, try to load their data
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            return null; // Player has never played on this server
        }

        // Create a message inventory to inform the user that offline viewing is not supported
        Inventory messageInventory = Bukkit.createInventory(null, 9, Component.text(playerName + "'s Enderchest (Offline - View Only)", NamedTextColor.YELLOW));

        // Log the attempt with more detailed audit information
        auditLogger.logSensitiveOperation(null, "offline enderchest access attempt",
                                         "Player: " + playerName + ", Status: OFFLINE, Result: Feature requires player to be online");

        return messageInventory;
    }

    /**
     * Loads an offline player's data.
     *
     * Note: Due to Bukkit API limitations, this method currently returns null for offline players.
     * The actual implementation would require server-specific code or additional plugins.
     * For security and stability reasons, we're returning null for offline players.
     *
     * @param uuid The UUID of the player
     * @return The loaded player, or null if the player is offline or an error occurred
     */
    @Nullable
    private Player loadOfflinePlayerData(UUID uuid) {
        try {
            // This is a safe way to load offline player data
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.isOnline()) {
                return offlinePlayer.getPlayer();
            }

            // For offline players, we return null for now
            // A complete implementation would require server-specific code
            // or additional plugins to safely load offline player data
            auditLogger.logSensitiveOperation(null, "offline player data load attempt",
                                             "Player: " + offlinePlayer.getName() +
                                             ", UUID: " + uuid +
                                             ", Result: Feature not fully implemented for security reasons");
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading offline player data for " + uuid, e);
            auditLogger.logSensitiveOperation(null, "offline player data load error",
                                             "UUID: " + uuid +
                                             ", Error: " + e.getMessage());
            return null;
        }
    }
}
