package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manager class for handling player visibility in the locator bar
 */
public class PlayerVisibilityManager {
    private final Furious plugin;
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final String HIDDEN_TEAM_NAME = "hiddenPlayers";
    private Team hiddenTeam;
    private final File configFile;
    private FileConfiguration config;

    /**
     * Creates a new PlayerVisibilityManager.
     *
     * @param plugin The plugin instance
     */
    public PlayerVisibilityManager(Furious plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "hidden_players.yml");
        setupHiddenTeam();
        loadConfiguration();
    }

    /**
     * Loads hidden players from the configuration file.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create hidden_players.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load hidden players
        List<String> hiddenPlayersList = config.getStringList("hidden-players");
        for (String playerIdStr : hiddenPlayersList) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                hiddenPlayers.add(playerId);
                plugin.getLogger().info("Loaded hidden player: " + playerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in hidden_players.yml: " + playerIdStr);
            }
        }
    }

    /**
     * Saves hidden players to the configuration file.
     */
    private void saveConfiguration() {
        // Clear existing data
        config.set("hidden-players", null);

        // Convert UUIDs to strings
        List<String> hiddenPlayersList = new java.util.ArrayList<>();
        for (UUID playerId : hiddenPlayers) {
            hiddenPlayersList.add(playerId.toString());
        }

        // Save to config
        config.set("hidden-players", hiddenPlayersList);

        // Save the file
        try {
            config.save(configFile);
            plugin.getLogger().info("Saved " + hiddenPlayersList.size() + " hidden players to configuration");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save hidden_players.yml: " + e.getMessage());
        }
    }

    /**
     * Sets up the scoreboard team for hidden players
     */
    private void setupHiddenTeam() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

            // Try to get existing team or create a new one
            hiddenTeam = scoreboard.getTeam(HIDDEN_TEAM_NAME);
            if (hiddenTeam == null) {
                hiddenTeam = scoreboard.registerNewTeam(HIDDEN_TEAM_NAME);
            }

            // Configure team settings to hide players from the locator bar
            hiddenTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            hiddenTeam.setCanSeeFriendlyInvisibles(false);

            plugin.getLogger().info("Hidden players team setup successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting up hidden players team", e);
        }
    }

    /**
     * Hides a player from the locator bar
     *
     * @param player The player to hide
     * @return True if the player was hidden, false otherwise
     */
    public boolean hidePlayerFromLocatorBar(Player player) {
        if (player == null) {
            return false;
        }

        try {
            // Add the player to the hidden players set
            hiddenPlayers.add(player.getUniqueId());

            // Add player to the hidden team
            if (hiddenTeam != null) {
                hiddenTeam.addEntry(player.getName());
            }

            // Use multiple methods to ensure the player is hidden from the locator bar
            player.setInvisible(true);
            player.setSilent(true);

            // Hide player from all other players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }

            // Inform the player
            player.sendMessage(Component.text("You are now hidden from the locator bar.", NamedTextColor.GREEN));
            plugin.getLogger().info("Player " + player.getName() + " is now hidden from the locator bar.");

            // Save the configuration
            saveConfiguration();

            return true;
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to hide you from the locator bar!", NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error hiding player from locator bar", e);
            return false;
        }
    }

    /**
     * Shows a player on the locator bar
     *
     * @param player The player to show
     * @return True if the player was shown, false otherwise
     */
    public boolean showPlayerOnLocatorBar(Player player) {
        if (player == null) {
            return false;
        }

        try {
            // Remove the player from the hidden players set
            hiddenPlayers.remove(player.getUniqueId());

            // Remove player from the hidden team
            if (hiddenTeam != null) {
                hiddenTeam.removeEntry(player.getName());
            }

            // Reset visibility properties
            player.setInvisible(false);
            player.setSilent(false);

            // Show player to all other players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.showPlayer(plugin, player);
                }
            }

            // Inform the player
            player.sendMessage(Component.text("You are now visible on the locator bar.", NamedTextColor.GREEN));
            plugin.getLogger().info("Player " + player.getName() + " is now visible on the locator bar.");

            // Save the configuration
            saveConfiguration();

            return true;
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to show you on the locator bar!", NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error showing player on locator bar", e);
            return false;
        }
    }

    /**
     * Toggles a player's visibility on the locator bar
     *
     * @param player The player to toggle
     * @return True if the player's visibility was toggled, false otherwise
     */
    public boolean togglePlayerLocatorBarVisibility(Player player) {
        if (player == null) {
            return false;
        }

        if (isPlayerHiddenFromLocatorBar(player)) {
            return showPlayerOnLocatorBar(player);
        } else {
            return hidePlayerFromLocatorBar(player);
        }
    }

    /**
     * Checks if a player is hidden from the locator bar
     *
     * @param player The player to check
     * @return True if the player is hidden, false otherwise
     */
    public boolean isPlayerHiddenFromLocatorBar(Player player) {
        if (player == null) {
            return false;
        }

        return hiddenPlayers.contains(player.getUniqueId());
    }

    /**
     * Gets all players hidden from the locator bar
     *
     * @return A set of UUIDs of players hidden from the locator bar
     */
    public Set<UUID> getHiddenPlayers() {
        return new HashSet<>(hiddenPlayers);
    }

    /**
     * Shuts down the PlayerVisibilityManager
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down PlayerVisibilityManager...");

        // Save hidden players to configuration before shutdown
        saveConfiguration();

        // Make players visible in-game but keep their hidden status in the config
        for (UUID uuid : new HashSet<>(hiddenPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Just make them visible in-game without removing from hiddenPlayers set
                // This is different from showPlayerOnLocatorBar which would remove them from the set

                // Remove player from the hidden team
                if (hiddenTeam != null) {
                    hiddenTeam.removeEntry(player.getName());
                }

                // Reset visibility properties
                player.setInvisible(false);
                player.setSilent(false);

                // Show player to all other players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.showPlayer(plugin, player);
                    }
                }
            }
        }

        // Clean up the hidden team
        if (hiddenTeam != null) {
            try {
                // Remove all entries from the team
                for (String entry : new HashSet<>(hiddenTeam.getEntries())) {
                    hiddenTeam.removeEntry(entry);
                }

                // Unregister the team if possible
                try {
                    hiddenTeam.unregister();
                    plugin.getLogger().info("Hidden players team unregistered successfully");
                } catch (IllegalStateException e) {
                    // Team might already be unregistered or cannot be unregistered
                    plugin.getLogger().info("Could not unregister hidden players team: " + e.getMessage());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error cleaning up hidden players team", e);
            }
        }
    }
}