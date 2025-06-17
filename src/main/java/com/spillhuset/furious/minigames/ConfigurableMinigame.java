package com.spillhuset.furious.minigames;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.managers.MinigameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended minigame class that supports configuration and states
 */
public class ConfigurableMinigame extends Minigame {
    private final MinigameType type;
    private final String mapName;
    private final int minPlayers;
    private int maxPlayers;
    private MinigameState state;
    private final Map<Integer, Location> spawnPoints;
    private int inQueue;

    /**
     * Constructor for a configurable minigame
     *
     * @param plugin The plugin instance
     * @param manager The minigame manager
     * @param name The name of the minigame
     * @param type The type of the minigame
     * @param minPlayers The minimum number of players
     * @param mapName The name of the map (optional)
     */
    public ConfigurableMinigame(Furious plugin, MinigameManager manager, String name, MinigameType type, int minPlayers, String mapName) {
        super(plugin, manager, name);
        this.type = type;
        this.minPlayers = minPlayers;
        this.mapName = mapName != null ? mapName : name;
        this.state = MinigameState.DISABLED;
        this.spawnPoints = new HashMap<>();
        this.maxPlayers = 0;
        this.inQueue = 0;
    }

    /**
     * Constructor for loading a minigame from configuration
     *
     * @param plugin The plugin instance
     * @param manager The minigame manager
     * @param config The configuration section
     */
    public ConfigurableMinigame(Furious plugin, MinigameManager manager, ConfigurationSection config) {
        super(plugin, manager, config.getString("name"));
        this.type = MinigameType.getById(config.getString("type"));
        this.minPlayers = config.getInt("minPlayers");
        this.mapName = config.getString("mapName", name);
        this.state = MinigameState.valueOf(config.getString("state", "DISABLED"));
        this.spawnPoints = new HashMap<>();
        this.maxPlayers = config.getInt("maxPlayers", 0);
        this.inQueue = 0;

        // Load spawn points
        ConfigurationSection spawnSection = config.getConfigurationSection("spawnPoints");
        if (spawnSection != null) {
            for (String key : spawnSection.getKeys(false)) {
                int index = Integer.parseInt(key);
                ConfigurationSection pointSection = spawnSection.getConfigurationSection(key);
                if (pointSection != null) {
                    World world = Bukkit.getWorld(pointSection.getString("world"));
                    if (world != null) {
                        Location location = new Location(
                                world,
                                pointSection.getDouble("x"),
                                pointSection.getDouble("y"),
                                pointSection.getDouble("z"),
                                (float) pointSection.getDouble("yaw"),
                                (float) pointSection.getDouble("pitch")
                        );
                        spawnPoints.put(index, location);
                    }
                }
            }
        }
    }

    /**
     * Saves the minigame to a configuration section
     *
     * @param config The configuration section to save to
     */
    public void saveToConfig(ConfigurationSection config) {
        config.set("name", name);
        config.set("type", type.getId());
        config.set("minPlayers", minPlayers);
        config.set("maxPlayers", maxPlayers);
        config.set("mapName", mapName);
        config.set("state", state.name());

        // Save spawn points
        ConfigurationSection spawnSection = config.createSection("spawnPoints");
        for (Map.Entry<Integer, Location> entry : spawnPoints.entrySet()) {
            ConfigurationSection pointSection = spawnSection.createSection(String.valueOf(entry.getKey()));
            Location loc = entry.getValue();
            pointSection.set("world", loc.getWorld().getName());
            pointSection.set("x", loc.getX());
            pointSection.set("y", loc.getY());
            pointSection.set("z", loc.getZ());
            pointSection.set("yaw", loc.getYaw());
            pointSection.set("pitch", loc.getPitch());
        }
    }

    /**
     * Sets a spawn point for the minigame
     *
     * @param index The index of the spawn point
     * @param location The location of the spawn point
     */
    public void setSpawnPoint(int index, Location location) {
        spawnPoints.put(index, location.clone());
        maxPlayers = Math.max(maxPlayers, spawnPoints.size());
    }

    /**
     * Gets a spawn point by index
     *
     * @param index The index of the spawn point
     * @return The location of the spawn point, or null if not found
     */
    public Location getSpawnPoint(int index) {
        return spawnPoints.get(index);
    }

    /**
     * Gets all spawn points
     *
     * @return A map of spawn points
     */
    public Map<Integer, Location> getSpawnPoints() {
        return new HashMap<>(spawnPoints);
    }

    /**
     * Gets the type of the minigame
     *
     * @return The type
     */
    public MinigameType getType() {
        return type;
    }

    /**
     * Gets the minimum number of players
     *
     * @return The minimum number of players
     */
    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Gets the maximum number of players
     *
     * @return The maximum number of players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Gets the map name
     *
     * @return The map name
     */
    public String getMapName() {
        return mapName;
    }

    /**
     * Gets the current state of the minigame
     *
     * @return The state
     */
    public MinigameState getState() {
        return state;
    }

    /**
     * Sets the state of the minigame
     *
     * @param state The new state
     */
    public void setState(MinigameState state) {
        this.state = state;
    }

    /**
     * Gets the number of players in the queue
     *
     * @return The number of players in the queue
     */
    public int getInQueue() {
        return inQueue;
    }

    /**
     * Sets the number of players in the queue
     *
     * @param inQueue The number of players in the queue
     */
    public void setInQueue(int inQueue) {
        this.inQueue = inQueue;
    }

    /**
     * Enables the minigame
     */
    public void enable() {
        if (state == MinigameState.DISABLED) {
            state = MinigameState.READY;
        }
    }

    /**
     * Disables the minigame
     */
    public void disable() {
        if (state == MinigameState.READY || state == MinigameState.QUEUE) {
            state = MinigameState.DISABLED;
        }
    }

    /**
     * Stops the minigame
     */
    public void stop() {
        if (state == MinigameState.QUEUE || state == MinigameState.COUNTDOWN ||
            state == MinigameState.STARTED || state == MinigameState.FINAL) {
            state = MinigameState.DISABLED;
            inQueue = 0;
            if (isRunning()) {
                endGame();
            }
        }
    }

    @Override
    protected Location getSpawnLocation() {
        if (spawnPoints.isEmpty()) {
            // Fall back to world spawn if no spawn points are set
            if (gameWorld == null) {
                createGameWorld();
            }
            return gameWorld.getSpawnLocation();
        }

        // Use the first spawn point
        return spawnPoints.get(1).clone();
    }

    @Override
    protected void teleportPlayersToGame(List<Player> players) {
        if (spawnPoints.isEmpty() || players.size() > spawnPoints.size()) {
            // Fall back to default behavior if not enough spawn points
            super.teleportPlayersToGame(players);
            return;
        }

        // Teleport each player to a different spawn point
        int index = 1;
        for (Player player : players) {
            if (spawnPoints.containsKey(index)) {
                player.teleport(spawnPoints.get(index).clone());
                index++;
            } else {
                // Fall back to first spawn point if we run out
                player.teleport(spawnPoints.get(1).clone());
            }
        }
    }

    @Override
    public void startGame(List<Player> players) {
        // If the state is COUNTDOWN or QUEUE and we have enough players, start the game
        if (state == MinigameState.COUNTDOWN || (state == MinigameState.QUEUE && inQueue >= minPlayers)) {
            state = MinigameState.STARTED;
            super.startGame(players);
        }
    }

    @Override
    public void endGame() {
        if (state == MinigameState.STARTED || state == MinigameState.FINAL) {
            state = MinigameState.READY;
            super.endGame();
        }
    }

    @Override
    protected void onGameStart(List<Player> players) {
        // This should be implemented by specific minigame types
    }

    @Override
    protected void onGameEnd(List<Player> players) {
        // This should be implemented by specific minigame types
    }
}
