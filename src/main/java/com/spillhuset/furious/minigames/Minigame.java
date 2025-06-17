package com.spillhuset.furious.minigames;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.MinigameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all minigames
 */
public abstract class Minigame {
    protected final Furious plugin;
    protected final MinigameManager manager;
    protected final String name;
    protected World gameWorld;
    protected boolean isRunning;
    protected List<UUID> players;
    protected BukkitTask gameTask;

    /**
     * Constructor for a minigame
     *
     * @param plugin The plugin instance
     * @param manager The minigame manager
     * @param name The name of the minigame
     */
    public Minigame(Furious plugin, MinigameManager manager, String name) {
        this.plugin = plugin;
        this.manager = manager;
        this.name = name;
        this.isRunning = false;
        this.players = new ArrayList<>();
    }

    /**
     * Starts the game with the given players
     *
     * @param players The players to start the game with
     */
    public void startGame(List<Player> players) {
        if (isRunning) {
            return;
        }

        // Create or load the game world
        createGameWorld();

        // Store player UUIDs
        this.players.clear();
        for (Player player : players) {
            this.players.add(player.getUniqueId());
        }

        // Teleport players to the game world
        teleportPlayersToGame(players);

        // Set game as running
        isRunning = true;

        // Start the game logic
        onGameStart(players);
    }

    /**
     * Creates or loads the game world
     */
    protected void createGameWorld() {
        // For ConfigurableMinigame, use the map system
        if (this instanceof ConfigurableMinigame configGame) {
            String mapName = configGame.getMapName();
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);

            // Use the WorldManager to create a map instance
            gameWorld = plugin.getWorldManager().createMapInstance(mapName, uniqueId);

            if (gameWorld == null) {
                plugin.getLogger().severe("Failed to create map instance for " + name + " using map " + mapName);
            }
        } else {
            // For regular minigames, use the old system
            gameWorld = plugin.getWorldManager().createPlayground(name);

            if (gameWorld == null) {
                plugin.getLogger().severe("Failed to create playground world for " + name);
            }
        }
    }

    /**
     * Teleports players to the game world
     *
     * @param players The players to teleport
     */
    protected void teleportPlayersToGame(List<Player> players) {
        Location spawnLocation = getSpawnLocation();

        for (Player player : players) {
            player.teleport(spawnLocation);
        }
    }

    /**
     * Gets the spawn location for the game
     *
     * @return The spawn location
     */
    protected abstract Location getSpawnLocation();

    /**
     * Called when the game starts
     *
     * @param players The players in the game
     */
    protected abstract void onGameStart(List<Player> players);

    /**
     * Ends the game
     */
    public void endGame() {
        if (!isRunning) {
            return;
        }

        // Get online players
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }

        // Call game end logic
        onGameEnd(onlinePlayers);

        // Cancel game task if it exists
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        // Set game as not running
        isRunning = false;

        // Tell manager to handle player cleanup
        manager.endGame(name, onlinePlayers);

        // Clear player list
        players.clear();

        // Delete the game world
        if (gameWorld != null) {
            World worldToDelete = gameWorld;
            // The world reference will be invalid after deletion, so clear it
            gameWorld = null;

            // Schedule the deletion for the next tick to ensure all players are out
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (this instanceof ConfigurableMinigame configGame) {
                    // For ConfigurableMinigame, use the map system
                    String mapName = configGame.getMapName();
                    String worldName = worldToDelete.getName();

                    // Extract the unique ID from the world name
                    if (worldName.startsWith(mapName + "-")) {
                        String uniqueId = worldName.substring(mapName.length() + 1);
                        plugin.getWorldManager().deleteMapInstance(mapName, uniqueId);
                    } else {
                        // Fallback to old system if the world name doesn't match the expected format
                        plugin.getWorldManager().deletePlayground(name);
                    }
                } else {
                    // For regular minigames, use the old system
                    plugin.getWorldManager().deletePlayground(name);
                }
            });
        }
    }

    /**
     * Called when the game ends
     *
     * @param players The players in the game
     */
    protected abstract void onGameEnd(List<Player> players);

    /**
     * Forces the game to end immediately
     */
    public void forceEnd() {
        endGame();
    }

    /**
     * Resets the game state
     */
    public void resetGame() {
        isRunning = false;
        players.clear();

        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
    }

    /**
     * Gets the name of the minigame
     *
     * @return The name of the minigame
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if the game is running
     *
     * @return True if the game is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets the players in the game
     *
     * @return The players in the game
     */
    public List<UUID> getPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Gets the game world
     *
     * @return The game world
     */
    public World getGameWorld() {
        return gameWorld;
    }
}
