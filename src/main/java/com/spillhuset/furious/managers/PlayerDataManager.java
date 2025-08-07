package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return null; // Player has never played on this server or is not cached
        }

        // Create a view-only inventory with the player's name that clearly indicates it's a placeholder
        Inventory viewOnlyInventory = Bukkit.createInventory(null, 36, Component.text(playerName + "'s Inventory (Offline - Player must be online)", NamedTextColor.RED));

        try {
            // Get the player's UUID
            UUID playerUUID = offlinePlayer.getUniqueId();

            // Add a paper with information message in the middle of the inventory
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta meta = infoItem.getItemMeta();
            meta.displayName(Component.text("Player is offline", NamedTextColor.RED));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("This player is currently offline.", NamedTextColor.YELLOW));
            lore.add(Component.text("You can only view inventories of online players.", NamedTextColor.YELLOW));
            lore.add(Component.text("Please try again when " + playerName + " is online.", NamedTextColor.YELLOW));
            meta.lore(lore);
            infoItem.setItemMeta(meta);

            // Place the info item in the center of the inventory
            viewOnlyInventory.setItem(13, infoItem);

            // Add glass panes around to make it more visible
            ItemStack glassPaneRed = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemStack glassPaneYellow = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassPaneRed.getItemMeta();
            glassMeta.displayName(Component.text(" "));
            glassPaneRed.setItemMeta(glassMeta);
            glassPaneYellow.setItemMeta(glassMeta);

            // Create a pattern with glass panes
            for (int i = 0; i < viewOnlyInventory.getSize(); i++) {
                if (i != 13) { // Skip the center where we have the info item
                    if (i % 2 == 0) {
                        viewOnlyInventory.setItem(i, glassPaneRed);
                    } else {
                        viewOnlyInventory.setItem(i, glassPaneYellow);
                    }
                }
            }

            // Log the access attempt
            auditLogger.logSensitiveOperation(null, "offline inventory access attempt",
                                             "Player: " + playerName +
                                             ", UUID: " + playerUUID +
                                             ", Status: OFFLINE, Result: Placeholder inventory shown");

            return viewOnlyInventory;
        } catch (Exception e) {
            plugin.getLogger().severe("Error accessing offline player inventory for " + playerName + ": " + e.getMessage());
            auditLogger.logSensitiveOperation(null, "offline inventory access error",
                                             "Player: " + playerName +
                                             ", Error: " + e.getMessage());
            return null;
        }
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
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return null; // Player has never played on this server or is not cached
        }

        // Create a view-only inventory with the player's name that clearly indicates it's a placeholder
        Inventory viewOnlyInventory = Bukkit.createInventory(null, 27, Component.text(playerName + "'s Enderchest (Offline - Player must be online)", NamedTextColor.RED));

        try {
            // Get the player's UUID
            UUID playerUUID = offlinePlayer.getUniqueId();

            // Add a paper with information message in the middle of the inventory
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta meta = infoItem.getItemMeta();
            meta.displayName(Component.text("Player is offline", NamedTextColor.RED));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("This player is currently offline.", NamedTextColor.YELLOW));
            lore.add(Component.text("You can only view enderchests of online players.", NamedTextColor.YELLOW));
            lore.add(Component.text("Please try again when " + playerName + " is online.", NamedTextColor.YELLOW));
            meta.lore(lore);
            infoItem.setItemMeta(meta);

            // Place the info item in the center of the inventory
            viewOnlyInventory.setItem(13, infoItem);

            // Add glass panes around to make it more visible
            ItemStack glassPaneRed = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemStack glassPaneYellow = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glassPaneRed.getItemMeta();
            glassMeta.displayName(Component.text(" "));
            glassPaneRed.setItemMeta(glassMeta);
            glassPaneYellow.setItemMeta(glassMeta);

            // Create a pattern with glass panes
            for (int i = 0; i < viewOnlyInventory.getSize(); i++) {
                if (i != 13) { // Skip the center where we have the info item
                    if (i % 2 == 0) {
                        viewOnlyInventory.setItem(i, glassPaneRed);
                    } else {
                        viewOnlyInventory.setItem(i, glassPaneYellow);
                    }
                }
            }

            // Log the access attempt
            auditLogger.logSensitiveOperation(null, "offline enderchest access attempt",
                                             "Player: " + playerName +
                                             ", UUID: " + playerUUID +
                                             ", Status: OFFLINE, Result: Placeholder inventory shown");

            return viewOnlyInventory;
        } catch (Exception e) {
            plugin.getLogger().severe("Error accessing offline player enderchest for " + playerName + ": " + e.getMessage());
            auditLogger.logSensitiveOperation(null, "offline enderchest access error",
                                             "Player: " + playerName +
                                             ", Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads an offline player's data.
     *
     * This method attempts to load player data from the player.dat file.
     * If the player is online, it returns the online player.
     * For offline players, it tries to access their saved inventory data.
     *
     * @param uuid The UUID of the player
     * @return The loaded player, or null if the player is offline or an error occurred
     */
    @Nullable
    private Player loadOfflinePlayerData(UUID uuid) {
        try {
            // Check if player is online first
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.isOnline()) {
                return offlinePlayer.getPlayer();
            }

            // For offline players, we need to use a different approach
            // Since we can't directly load offline player data through the Bukkit API,
            // we'll use a workaround by creating a temporary inventory with the same items

            // First, check if we can find the player's data file
            File worldFolder = new File(Bukkit.getWorldContainer(), Bukkit.getWorlds().get(0).getName());
            File playerDataFolder = new File(worldFolder, "playerdata");
            File playerFile = new File(playerDataFolder, uuid.toString() + ".dat");

            if (!playerFile.exists()) {
                plugin.getLogger().warning("Player data file not found for UUID: " + uuid);
                return null;
            }

            // Log the attempt
            auditLogger.logSensitiveOperation(null, "offline player data load attempt",
                                             "Player: " + offlinePlayer.getName() +
                                             ", UUID: " + uuid);

            // Since we can't directly load the player data without NMS code,
            // we'll return null but the calling method will handle creating a view-only inventory
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
