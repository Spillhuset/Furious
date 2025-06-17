package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages homes in the game.
 */
public class HomesManager {
    private final Furious plugin;
    private final Map<UUID, Map<String, Home>> playerHomes; // Player UUID -> (Home Name -> Home)
    private final Map<UUID, Map<String, Home>> guildHomes; // Guild UUID -> (Home Name -> Home)
    private final File configFile;
    private FileConfiguration config;

    // Configuration values
    private final int DEFAULT_MAX_PLAYER_HOMES;
    private final int DEFAULT_MAX_GUILD_HOMES;
    private final double BASE_HOME_COST;
    private final double COST_MULTIPLIER;
    private final double GUILD_COST_MULTIPLIER;
    private List<String> DISABLED_WORLDS;
    private final List<String> DISABLED_WORLD_TYPES;

    /**
     * Creates a new HomesManager.
     *
     * @param plugin The plugin instance
     */
    public HomesManager(Furious plugin) {
        this.plugin = plugin;
        this.playerHomes = new HashMap<>();
        this.guildHomes = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "homes.yml");

        // Load configuration values
        this.DEFAULT_MAX_PLAYER_HOMES = plugin.getConfig().getInt("homes.default-max-player-homes", 3);
        this.DEFAULT_MAX_GUILD_HOMES = plugin.getConfig().getInt("homes.default-max-guild-homes", 1);
        this.BASE_HOME_COST = plugin.getConfig().getDouble("homes.base-home-cost", 100.0);
        this.COST_MULTIPLIER = plugin.getConfig().getDouble("homes.cost-multiplier", 1.5);
        this.GUILD_COST_MULTIPLIER = plugin.getConfig().getDouble("homes.guild-cost-multiplier", 3.0);
        this.DISABLED_WORLDS = plugin.getConfig().getStringList("homes.disabled-worlds");
        this.DISABLED_WORLD_TYPES = plugin.getConfig().getStringList("homes.disabled-world-types");

        loadConfiguration();
    }

    /**
     * Loads home data from the configuration file.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load player homes
        ConfigurationSection playerHomesSection = config.getConfigurationSection("player-homes");
        if (playerHomesSection != null) {
            for (String playerIdStr : playerHomesSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    ConfigurationSection playerSection = playerHomesSection.getConfigurationSection(playerIdStr);

                    if (playerSection != null) {
                        Map<String, Home> homes = new HashMap<>();

                        for (String homeName : playerSection.getKeys(false)) {
                            ConfigurationSection homeSection = playerSection.getConfigurationSection(homeName);
                            if (homeSection != null) {
                                UUID homeId = UUID.fromString(homeSection.getString("id"));
                                UUID worldId = UUID.fromString(homeSection.getString("world"));
                                double x = homeSection.getDouble("x");
                                double y = homeSection.getDouble("y");
                                double z = homeSection.getDouble("z");
                                float yaw = (float) homeSection.getDouble("yaw");
                                float pitch = (float) homeSection.getDouble("pitch");

                                Home home = new Home(homeId, homeName, playerId, false, worldId, x, y, z, yaw, pitch);
                                homes.put(homeName.toLowerCase(), home);
                            }
                        }

                        if (!homes.isEmpty()) {
                            playerHomes.put(playerId, homes);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in homes.yml: " + playerIdStr);
                }
            }
        }

        // Load guild homes
        ConfigurationSection guildHomesSection = config.getConfigurationSection("guild-homes");
        if (guildHomesSection != null) {
            for (String guildIdStr : guildHomesSection.getKeys(false)) {
                try {
                    UUID guildId = UUID.fromString(guildIdStr);
                    ConfigurationSection guildSection = guildHomesSection.getConfigurationSection(guildIdStr);

                    if (guildSection != null) {
                        Map<String, Home> homes = new HashMap<>();

                        for (String homeName : guildSection.getKeys(false)) {
                            ConfigurationSection homeSection = guildSection.getConfigurationSection(homeName);
                            if (homeSection != null) {
                                UUID homeId = UUID.fromString(homeSection.getString("id"));
                                UUID worldId = UUID.fromString(homeSection.getString("world"));
                                double x = homeSection.getDouble("x");
                                double y = homeSection.getDouble("y");
                                double z = homeSection.getDouble("z");
                                float yaw = (float) homeSection.getDouble("yaw");
                                float pitch = (float) homeSection.getDouble("pitch");

                                Home home = new Home(homeId, homeName, guildId, true, worldId, x, y, z, yaw, pitch);
                                homes.put(homeName.toLowerCase(), home);
                            }
                        }

                        if (!homes.isEmpty()) {
                            guildHomes.put(guildId, homes);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in homes.yml: " + guildIdStr);
                }
            }
        }
    }

    /**
     * Saves home data to the configuration file.
     */
    private void saveConfiguration() {
        // Clear existing data
        config.set("player-homes", null);
        config.set("guild-homes", null);

        // Save player homes
        for (Map.Entry<UUID, Map<String, Home>> entry : playerHomes.entrySet()) {
            UUID playerId = entry.getKey();
            Map<String, Home> homes = entry.getValue();

            for (Home home : homes.values()) {
                String path = "player-homes." + playerId.toString() + "." + home.getName();
                config.set(path + ".id", home.getId().toString());
                config.set(path + ".world", home.getWorldId().toString());
                config.set(path + ".x", home.getX());
                config.set(path + ".y", home.getY());
                config.set(path + ".z", home.getZ());
                config.set(path + ".yaw", home.getYaw());
                config.set(path + ".pitch", home.getPitch());
            }
        }

        // Save guild homes
        for (Map.Entry<UUID, Map<String, Home>> entry : guildHomes.entrySet()) {
            UUID guildId = entry.getKey();
            Map<String, Home> homes = entry.getValue();

            for (Home home : homes.values()) {
                String path = "guild-homes." + guildId.toString() + "." + home.getName();
                config.set(path + ".id", home.getId().toString());
                config.set(path + ".world", home.getWorldId().toString());
                config.set(path + ".x", home.getX());
                config.set(path + ".y", home.getY());
                config.set(path + ".z", home.getZ());
                config.set(path + ".yaw", home.getYaw());
                config.set(path + ".pitch", home.getPitch());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    /**
     * Gets the maximum number of homes a player can have.
     *
     * @param player The player to check
     * @return The maximum number of homes
     */
    public int getMaxPlayerHomes(Player player) {
        int maxHomes = DEFAULT_MAX_PLAYER_HOMES;

        // Check for permission-based additional homes
        for (int i = 1; i <= 10; i++) {
            if (player.hasPermission("furious.homes.limit." + i)) {
                maxHomes = Math.max(maxHomes, i);
            }
        }

        // Add purchased homes
        int purchasedHomes = getPurchasedHomes(player.getUniqueId());

        return maxHomes + purchasedHomes;
    }

    /**
     * Gets the number of additional homes a player has purchased.
     *
     * @param playerId The UUID of the player
     * @return The number of purchased homes
     */
    public int getPurchasedHomes(UUID playerId) {
        return config.getInt("purchased-homes." + playerId.toString(), 0);
    }

    /**
     * Sets the number of additional homes a player has purchased.
     *
     * @param playerId The UUID of the player
     * @param count The number of purchased homes
     */
    public void setPurchasedHomes(UUID playerId, int count) {
        config.set("purchased-homes." + playerId.toString(), count);
        saveConfiguration();
    }

    /**
     * Gets the maximum number of homes a guild can have.
     *
     * @param guild The guild to check
     * @return The maximum number of homes
     */
    public int getMaxGuildHomes(Guild guild) {
        int maxHomes = DEFAULT_MAX_GUILD_HOMES;

        // Add purchased homes
        int purchasedHomes = getPurchasedGuildHomes(guild.getId());

        return maxHomes + purchasedHomes;
    }

    /**
     * Gets the number of additional homes a guild has purchased.
     *
     * @param guildId The UUID of the guild
     * @return The number of purchased homes
     */
    public int getPurchasedGuildHomes(UUID guildId) {
        return config.getInt("purchased-guild-homes." + guildId.toString(), 0);
    }

    /**
     * Sets the number of additional homes a guild has purchased.
     *
     * @param guildId The UUID of the guild
     * @param count The number of purchased homes
     */
    public void setPurchasedGuildHomes(UUID guildId, int count) {
        config.set("purchased-guild-homes." + guildId.toString(), count);
        saveConfiguration();
    }

    /**
     * Calculates the cost to purchase an additional home slot.
     *
     * @param player The player purchasing the home slot
     * @return The cost of the home slot
     */
    public double getHomeSlotCost(Player player) {
        int currentPurchased = getPurchasedHomes(player.getUniqueId());
        return BASE_HOME_COST * Math.pow(COST_MULTIPLIER, currentPurchased);
    }

    /**
     * Calculates the cost to purchase an additional guild home slot.
     *
     * @param guild The guild purchasing the home slot
     * @return The cost of the home slot
     */
    public double getGuildHomeSlotCost(Guild guild) {
        int currentPurchased = getPurchasedGuildHomes(guild.getId());
        return BASE_HOME_COST * GUILD_COST_MULTIPLIER * Math.pow(COST_MULTIPLIER, currentPurchased);
    }

    /**
     * Purchases an additional home slot for a player.
     *
     * @param player The player purchasing the home slot
     * @return true if the purchase was successful, false otherwise
     */
    public boolean purchaseHomeSlot(Player player) {
        double cost = getHomeSlotCost(player);

        // Check if player has enough money
        if (!plugin.getWalletManager().has(player, cost)) {
            player.sendMessage(Component.text("You don't have enough money to purchase a home slot! Cost: " + cost, NamedTextColor.RED));
            return false;
        }

        // Deduct money and add home slot
        plugin.getWalletManager().withdraw(player, cost);
        int currentPurchased = getPurchasedHomes(player.getUniqueId());
        setPurchasedHomes(player.getUniqueId(), currentPurchased + 1);

        player.sendMessage(Component.text("You purchased a new home slot for " + cost + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Purchases an additional home slot for a guild.
     *
     * @param player The player purchasing the home slot (must be guild owner)
     * @param guild The guild to purchase the home slot for
     * @return true if the purchase was successful, false otherwise
     */
    public boolean purchaseGuildHomeSlot(Player player, Guild guild) {
        // Check if player is guild owner
        if (!guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the guild owner can purchase home slots!", NamedTextColor.RED));
            return false;
        }

        double cost = getGuildHomeSlotCost(guild);

        // Check if player has enough money
        if (!plugin.getWalletManager().has(player, cost)) {
            player.sendMessage(Component.text("You don't have enough money to purchase a guild home slot! Cost: " + cost, NamedTextColor.RED));
            return false;
        }

        // Deduct money and add home slot
        plugin.getWalletManager().withdraw(player, cost);
        int currentPurchased = getPurchasedGuildHomes(guild.getId());
        setPurchasedGuildHomes(guild.getId(), currentPurchased + 1);

        player.sendMessage(Component.text("You purchased a new guild home slot for " + cost + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Checks if a world is disabled for homes.
     *
     * @param world The world to check
     * @return true if homes are disabled in this world, false otherwise
     */
    public boolean isWorldDisabled(World world) {
        // Check if world is in disabled worlds list
        if (DISABLED_WORLDS.contains(world.getName())) {
            return true;
        }

        // Check if world type is in disabled world types list
        String worldType = world.getEnvironment().name();
        return DISABLED_WORLD_TYPES.contains(worldType);
    }

    /**
     * Enables homes in the specified world.
     *
     * @param world The world to enable homes in
     * @return true if the operation was successful, false otherwise
     */
    public boolean enableWorld(World world) {
        if (world == null) {
            return false;
        }

        String worldName = world.getName();
        boolean removed = DISABLED_WORLDS.remove(worldName);
        if (removed) {
            // Update the config
            plugin.getConfig().set("homes.disabled-worlds", DISABLED_WORLDS);
            plugin.saveConfig();
        }
        return true;
    }

    /**
     * Disables homes in the specified world.
     *
     * @param world The world to disable homes in
     * @return true if the operation was successful, false otherwise
     */
    public boolean disableWorld(World world) {
        if (world == null) {
            return false;
        }

        String worldName = world.getName();
        if (!DISABLED_WORLDS.contains(worldName)) {
            DISABLED_WORLDS.add(worldName);
            // Update the config
            plugin.getConfig().set("homes.disabled-worlds", DISABLED_WORLDS);
            plugin.saveConfig();
            return true;
        }
        return false;
    }

    /**
     * Gets a list of all worlds and whether homes are enabled in them.
     *
     * @return A map of world names to boolean values indicating if homes are enabled
     */
    public Map<String, Boolean> getWorldsStatus() {
        Map<String, Boolean> worldsStatus = new HashMap<>();

        for (World world : plugin.getServer().getWorlds()) {
            // Skip game worlds
            if (world.getName().equals(plugin.getWorldManager().getGameWorldName()) ||
                world.getName().equals(plugin.getWorldManager().getGameBackupName()) ||
                world.getName().startsWith("minigame_")) {
                continue;
            }

            worldsStatus.put(world.getName(), !isWorldDisabled(world));
        }

        return worldsStatus;
    }

    /**
     * Sets a home for a player.
     *
     * @param player The player setting the home
     * @param homeName The name of the home
     * @param location The location of the home
     * @return true if the home was set, false otherwise
     */
    public boolean setPlayerHome(Player player, String homeName, Location location) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        // Check if world is disabled
        if (isWorldDisabled(location.getWorld())) {
            player.sendMessage(Component.text("You cannot set homes in this world!", NamedTextColor.RED));
            return false;
        }

        // Get player's homes
        Map<String, Home> homes = playerHomes.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        // Check if player has reached the home limit
        if (!homes.containsKey(homeName) && homes.size() >= getMaxPlayerHomes(player)) {
            player.sendMessage(Component.text("You have reached your home limit! You can purchase more home slots.", NamedTextColor.RED));
            return false;
        }

        // Create or update the home
        Home home = homes.get(homeName);
        if (home == null) {
            home = new Home(homeName, player.getUniqueId(), false, location);
            homes.put(homeName, home);
        } else {
            home.setLocation(location);
        }

        saveConfiguration();
        return true;
    }

    /**
     * Sets a home for a guild.
     *
     * @param player The player setting the home (must be authorized)
     * @param guild The guild to set the home for
     * @param homeName The name of the home
     * @param location The location of the home
     * @return true if the home was set, false otherwise
     */
    public boolean setGuildHome(Player player, Guild guild, String homeName, Location location) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        // Check if world is disabled
        if (isWorldDisabled(location.getWorld())) {
            player.sendMessage(Component.text("You cannot set homes in this world!", NamedTextColor.RED));
            return false;
        }

        // Get guild's homes
        Map<String, Home> homes = guildHomes.computeIfAbsent(guild.getId(), k -> new HashMap<>());

        // Check if guild has reached the home limit
        if (!homes.containsKey(homeName) && homes.size() >= getMaxGuildHomes(guild)) {
            player.sendMessage(Component.text("Your guild has reached its home limit! You can purchase more home slots.", NamedTextColor.RED));
            return false;
        }

        // Create or update the home
        Home home = homes.get(homeName);
        if (home == null) {
            home = new Home(homeName, guild.getId(), true, location);
            homes.put(homeName, home);
        } else {
            home.setLocation(location);
        }

        saveConfiguration();
        return true;
    }

    /**
     * Gets a player's home by name.
     *
     * @param playerId The UUID of the player
     * @param homeName The name of the home
     * @return The home, or null if not found
     */
    public Home getPlayerHome(UUID playerId, String homeName) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        Map<String, Home> homes = playerHomes.get(playerId);
        if (homes != null) {
            return homes.get(homeName);
        }
        return null;
    }

    /**
     * Gets a guild's home by name.
     *
     * @param guildId The UUID of the guild
     * @param homeName The name of the home
     * @return The home, or null if not found
     */
    public Home getGuildHome(UUID guildId, String homeName) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        Map<String, Home> homes = guildHomes.get(guildId);
        if (homes != null) {
            return homes.get(homeName);
        }
        return null;
    }

    /**
     * Gets all homes for a player.
     *
     * @param playerId The UUID of the player
     * @return A collection of the player's homes
     */
    public Collection<Home> getPlayerHomes(UUID playerId) {
        Map<String, Home> homes = playerHomes.get(playerId);
        if (homes != null) {
            return homes.values();
        }
        return Collections.emptyList();
    }

    /**
     * Gets all homes for a guild.
     *
     * @param guildId The UUID of the guild
     * @return A collection of the guild's homes
     */
    public Collection<Home> getGuildHomes(UUID guildId) {
        Map<String, Home> homes = guildHomes.get(guildId);
        if (homes != null) {
            return homes.values();
        }
        return Collections.emptyList();
    }

    /**
     * Deletes a player's home.
     *
     * @param playerId The UUID of the player
     * @param homeName The name of the home
     * @return true if the home was deleted, false otherwise
     */
    public boolean deletePlayerHome(UUID playerId, String homeName) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        Map<String, Home> homes = playerHomes.get(playerId);
        if (homes != null && homes.remove(homeName) != null) {
            saveConfiguration();
            return true;
        }
        return false;
    }

    /**
     * Deletes a guild's home.
     *
     * @param guildId The UUID of the guild
     * @param homeName The name of the home
     * @return true if the home was deleted, false otherwise
     */
    public boolean deleteGuildHome(UUID guildId, String homeName) {
        // Default home name if not provided
        if (homeName == null || homeName.isEmpty()) {
            homeName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        homeName = homeName.toLowerCase();

        Map<String, Home> homes = guildHomes.get(guildId);
        if (homes != null && homes.remove(homeName) != null) {
            saveConfiguration();
            return true;
        }
        return false;
    }

    /**
     * Renames a player's home.
     *
     * @param playerId The UUID of the player
     * @param oldName The current name of the home
     * @param newName The new name for the home
     * @return true if the home was renamed, false otherwise
     */
    public boolean renamePlayerHome(UUID playerId, String oldName, String newName) {
        // Default home names if not provided
        if (oldName == null || oldName.isEmpty()) {
            oldName = "default";
        }
        if (newName == null || newName.isEmpty()) {
            newName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        oldName = oldName.toLowerCase();
        newName = newName.toLowerCase();

        // Check if names are the same
        if (oldName.equals(newName)) {
            return true;
        }

        Map<String, Home> homes = playerHomes.get(playerId);
        if (homes != null) {
            // Check if new name already exists
            if (homes.containsKey(newName)) {
                return false;
            }

            // Get the home and rename it
            Home home = homes.remove(oldName);
            if (home != null) {
                home.setName(newName);
                homes.put(newName, home);
                saveConfiguration();
                return true;
            }
        }
        return false;
    }

    /**
     * Renames a guild's home.
     *
     * @param guildId The UUID of the guild
     * @param oldName The current name of the home
     * @param newName The new name for the home
     * @return true if the home was renamed, false otherwise
     */
    public boolean renameGuildHome(UUID guildId, String oldName, String newName) {
        // Default home names if not provided
        if (oldName == null || oldName.isEmpty()) {
            oldName = "default";
        }
        if (newName == null || newName.isEmpty()) {
            newName = "default";
        }

        // Convert to lowercase for case-insensitive comparison
        oldName = oldName.toLowerCase();
        newName = newName.toLowerCase();

        // Check if names are the same
        if (oldName.equals(newName)) {
            return true;
        }

        Map<String, Home> homes = guildHomes.get(guildId);
        if (homes != null) {
            // Check if new name already exists
            if (homes.containsKey(newName)) {
                return false;
            }

            // Get the home and rename it
            Home home = homes.remove(oldName);
            if (home != null) {
                home.setName(newName);
                homes.put(newName, home);
                saveConfiguration();
                return true;
            }
        }
        return false;
    }

    /**
     * Teleports a player to their home.
     *
     * @param player The player to teleport
     * @param homeName The name of the home
     * @return true if the player was teleported, false otherwise
     */
    public boolean teleportToPlayerHome(Player player, String homeName) {
        Home home = getPlayerHome(player.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(Component.text("Home not found!", NamedTextColor.RED));
            return false;
        }

        Location location = home.getLocation();
        if (location == null) {
            player.sendMessage(Component.text("Home world doesn't exist!", NamedTextColor.RED));
            return false;
        }

        player.teleport(location);
        player.sendMessage(Component.text("Teleported to home " + home.getName() + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Teleports a player to a guild home.
     *
     * @param player The player to teleport
     * @param guild The guild whose home to teleport to
     * @param homeName The name of the home
     * @return true if the player was teleported, false otherwise
     */
    public boolean teleportToGuildHome(Player player, Guild guild, String homeName) {
        Home home = getGuildHome(guild.getId(), homeName);
        if (home == null) {
            player.sendMessage(Component.text("Guild home not found!", NamedTextColor.RED));
            return false;
        }

        Location location = home.getLocation();
        if (location == null) {
            player.sendMessage(Component.text("Home world doesn't exist!", NamedTextColor.RED));
            return false;
        }

        player.teleport(location);
        player.sendMessage(Component.text("Teleported to guild home " + home.getName() + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void shutdown() {
        saveConfiguration();
        playerHomes.clear();
        guildHomes.clear();
    }

    /**
     * Removes all data for a player.
     *
     * @param playerId The UUID of the player to remove data for
     */
    public void removePlayerData(UUID playerId) {
        playerHomes.remove(playerId);
        config.set("purchased-homes." + playerId.toString(), null);
        saveConfiguration();
    }

    /**
     * Removes all data for a guild.
     *
     * @param guildId The UUID of the guild to remove data for
     */
    public void removeGuildData(UUID guildId) {
        guildHomes.remove(guildId);
        config.set("purchased-guild-homes." + guildId.toString(), null);
        saveConfiguration();
    }
}
