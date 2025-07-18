package com.spillhuset.furious.minigames;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.managers.MinigameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended minigame class that supports configuration and states
 */
public class ConfigurableMinigame extends Minigame {
    private final MinigameType type;
    private String mapName; // Removed final to allow updating
    private final int minPlayers;
    private int maxPlayers;
    private MinigameState state;
    private final Map<Integer, Location> spawnPoints;
    private Location lobbySpawn;
    private int inQueue;
    private boolean queueEnabled;

    /**
     * Constructor for a configurable minigame
     *
     * @param plugin     The plugin instance
     * @param manager    The minigame manager
     * @param name       The name of the minigame
     * @param type       The type of the minigame
     * @param minPlayers The minimum number of players
     * @param mapName    The name of the map (optional)
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
        this.queueEnabled = true; // Queue is enabled by default
    }

    /**
     * Constructor for loading a minigame from configuration
     *
     * @param plugin  The plugin instance
     * @param manager The minigame manager
     * @param config  The configuration section
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
        this.lobbySpawn = null;
        this.queueEnabled = config.getBoolean("queueEnabled", true); // Queue is enabled by default

        // Load spawn points
        ConfigurationSection spawnSection = config.getConfigurationSection("spawnPoints");
        if (spawnSection != null) {
            for (String key : spawnSection.getKeys(false)) {
                int index = Integer.parseInt(key);
                ConfigurationSection pointSection = spawnSection.getConfigurationSection(key);
                if (pointSection != null) {
                    String worldName = pointSection.getString("world");
                    if (worldName != null) {
                        World world = Bukkit.getWorld(worldName);
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

        // Load lobby spawn point
        ConfigurationSection lobbySection = config.getConfigurationSection("lobbySpawn");
        if (lobbySection != null) {
            String worldName = lobbySection.getString("world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    this.lobbySpawn = new Location(
                            world,
                            lobbySection.getDouble("x"),
                            lobbySection.getDouble("y"),
                            lobbySection.getDouble("z"),
                            (float) lobbySection.getDouble("yaw"),
                            (float) lobbySection.getDouble("pitch")
                    );
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
        config.set("queueEnabled", queueEnabled);

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

        // Save lobby spawn point
        if (lobbySpawn != null) {
            ConfigurationSection lobbySection = config.createSection("lobbySpawn");
            lobbySection.set("world", lobbySpawn.getWorld().getName());
            lobbySection.set("x", lobbySpawn.getX());
            lobbySection.set("y", lobbySpawn.getY());
            lobbySection.set("z", lobbySpawn.getZ());
            lobbySection.set("yaw", lobbySpawn.getYaw());
            lobbySection.set("pitch", lobbySpawn.getPitch());
        }
    }

    /**
     * Sets a spawn point for the minigame
     *
     * @param index    The index of the spawn point
     * @param location The location of the spawn point
     */
    public void setSpawnPoint(int index, Location location) {
        spawnPoints.put(index, location.clone());
        maxPlayers = Math.max(maxPlayers, spawnPoints.size());
    }

    /**
     * Gets a spawn point by index
     *
     * Note: This method is currently not used in the codebase but is maintained
     * for API completeness and potential future use.
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
     * Sets the lobby spawn point for the minigame
     *
     * @param location The location of the lobby spawn point
     */
    public void setLobbySpawn(Location location) {
        this.lobbySpawn = location;
        // Update maxPlayers if this is the first spawn point
        if (maxPlayers == 0) {
            maxPlayers = 1;
        }
    }

    /**
     * Gets the lobby spawn point for the minigame
     *
     * @return The lobby spawn point, or null if not set
     */
    public Location getLobbySpawn() {
        return lobbySpawn;
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
     * Updates the map name
     *
     * @param newMapName The new map name
     */
    public void updateMapName(String newMapName) {
        this.mapName = newMapName;
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
     * Note: This method is currently not used in the codebase but is maintained
     * for API completeness and potential future use.
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
                state == MinigameState.PREPARING || state == MinigameState.STARTED ||
                state == MinigameState.FINAL) {
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
        // If the state is COUNTDOWN or QUEUE and we have enough players, prepare the game
        if (state == MinigameState.COUNTDOWN || (state == MinigameState.QUEUE && inQueue >= minPlayers)) {
            state = MinigameState.PREPARING;
            super.startGame(players);
        }
        // If the state is PREPARING, start the game after preparation
        else if (state == MinigameState.PREPARING) {
            state = MinigameState.STARTED;
        }
    }

    @Override
    public void endGame() {
        if (state == MinigameState.PREPARING || state == MinigameState.STARTED || state == MinigameState.FINAL) {
            state = MinigameState.READY;
            super.endGame();
        }
    }

    /**
     * Checks if the queue is enabled for this minigame
     *
     * @return True if the queue is enabled, false otherwise
     */
    public boolean isQueueEnabled() {
        return queueEnabled;
    }

    /**
     * Enables the queue for this minigame
     */
    public void enableQueue() {
        this.queueEnabled = true;
    }

    /**
     * Disables the queue for this minigame
     */
    public void disableQueue() {
        this.queueEnabled = false;
    }

    /**
     * Starts the preparation countdown for the game
     * After 1 minute, transitions from PREPARING to STARTED state
     *
     * @param players The players in the game
     */
    protected void startPreparationCountdown(List<Player> players) {
        if (state != MinigameState.PREPARING) {
            return;
        }

        // Teleport players to lobby spawn
        if (lobbySpawn != null) {
            for (Player player : players) {
                player.teleport(lobbySpawn.clone());
                // Set player to adventure mode
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.sendMessage(net.kyori.adventure.text.Component.text("Game is preparing! You will be teleported to your spawn in 1 minute.",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            }
        }

        // Start a 1-minute countdown
        gameTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int countdown = 60; // 1 minute in seconds

            @Override
            public void run() {
                if (countdown <= 0) {
                    // Time's up, start the game
                    gameTask.cancel();
                    gameTask = null;

                    // Transition to STARTED state
                    setState(MinigameState.STARTED);

                    // Teleport players to their spawn points
                    teleportPlayersToGame(players);

                    // Call onGameStart to handle game-specific logic
                    onGameStart(players);
                    return;
                }

                // Announce remaining time at certain intervals
                if (countdown == 60 || countdown == 30 || countdown == 10 || countdown <= 5) {
                    for (Player player : players) {
                        player.sendMessage(net.kyori.adventure.text.Component.text("Game starts in " + countdown + " seconds!",
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                    }
                }

                countdown--;
            }
        }, 0L, 20L); // Run every second
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
