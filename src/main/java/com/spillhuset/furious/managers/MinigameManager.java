package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.minigames.ConfigurableMinigame;
import com.spillhuset.furious.minigames.Minigame;
import com.spillhuset.furious.minigames.hungergames.HungerGame;
import com.spillhuset.furious.minigames.zombiesurvival.ZombieGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MinigameManager {
    private final Furious plugin;
    private final WorldManager worldManager;
    private final Map<String, Minigame> games;
    private final Map<UUID, String> playerGameMap;
    private final Map<String, Queue<UUID>> gameQueues;
    private final Map<String, BukkitTask> queueTimers;
    private final Map<UUID, ItemStack[]> inventories;
    private final Map<UUID, ItemStack[]> armorContents;
    private final Map<UUID, Location> previousLocations;
    private final Map<UUID, GameMode> previousGameModes;
    private final Map<UUID, String> playersInEditMode;
    private FileConfiguration config;
    private File configFile;

    public MinigameManager(Furious plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        this.games = new HashMap<>();
        this.playerGameMap = new ConcurrentHashMap<>();
        this.gameQueues = new HashMap<>();
        this.queueTimers = new HashMap<>();
        this.inventories = new HashMap<>();
        this.armorContents = new HashMap<>();
        this.previousLocations = new HashMap<>();
        this.previousGameModes = new HashMap<>();
        this.playersInEditMode = new HashMap<>();

        loadConfiguration();
        registerGames();
    }

    private void registerGames() {
        // Register the HungerGame
        HungerGame hungerGame = new HungerGame(plugin, this);
        games.put("hungergame", hungerGame);
        gameQueues.put("hungergame", new LinkedList<>());

        // Register the ZombieGame
        ZombieGame zombieGame = new ZombieGame(plugin, this);
        games.put("zombiesurvival", zombieGame);
        gameQueues.put("zombiesurvival", new LinkedList<>());
    }

    private void loadConfiguration() {
        configFile = new File(plugin.getDataFolder(), "minigames.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create minigames.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Set default values if they don't exist
        if (!config.contains("queueTime.hungergame")) {
            config.set("queueTime.hungergame", 300); // 5 minutes in seconds
        }

        // Load configurable minigames
        ConfigurationSection gamesSection = config.getConfigurationSection("games");
        if (gamesSection != null) {
            for (String key : gamesSection.getKeys(false)) {
                ConfigurationSection gameSection = gamesSection.getConfigurationSection(key);
                if (gameSection != null) {
                    ConfigurableMinigame game = new ConfigurableMinigame(plugin, this, gameSection);
                    games.put(game.getName().toLowerCase(), game);
                    gameQueues.put(game.getName().toLowerCase(), new LinkedList<>());
                }
            }
        }

        saveConfiguration();
    }

    private void saveConfiguration() {
        // Save configurable minigames
        ConfigurationSection gamesSection = config.createSection("games");
        for (Minigame game : games.values()) {
            if (game instanceof ConfigurableMinigame configGame) {
                ConfigurationSection gameSection = gamesSection.createSection(configGame.getName().toLowerCase());
                configGame.saveToConfig(gameSection);
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save minigames.yml: " + e.getMessage());
        }
    }

    /**
     * Creates a new minigame
     *
     * @param name       The name of the minigame
     * @param type       The type of the minigame
     * @param minPlayers The minimum number of players
     * @param mapName    The name of the map (optional)
     * @return True if the minigame was created, false otherwise
     */
    public boolean createMinigame(String name, MinigameType type, int minPlayers, String mapName) {
        // Check if a game with this name already exists
        if (games.containsKey(name.toLowerCase())) {
            return false;
        }

        // Create the minigame
        ConfigurableMinigame game = new ConfigurableMinigame(plugin, this, name, type, minPlayers, mapName);
        games.put(name.toLowerCase(), game);
        gameQueues.put(name.toLowerCase(), new LinkedList<>());

        // Save the configuration
        saveConfiguration();

        return true;
    }

    /**
     * Enables a minigame
     *
     * @param name The name of the minigame
     * @return True if the minigame was enabled, false otherwise
     */
    public boolean enableMinigame(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        configGame.enable();
        saveConfiguration();
        return true;
    }

    /**
     * Disables a minigame
     *
     * @param name The name of the minigame
     * @return True if the minigame was disabled, false otherwise
     */
    public boolean disableMinigame(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        configGame.disable();
        saveConfiguration();
        return true;
    }

    /**
     * Stops a minigame
     *
     * @param name The name of the minigame
     * @return True if the minigame was stopped, false otherwise
     */
    public boolean stopMinigame(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        configGame.stop();

        // Clear the queue
        gameQueues.get(name.toLowerCase()).clear();

        // Cancel any queue timer
        if (queueTimers.containsKey(name.toLowerCase())) {
            queueTimers.get(name.toLowerCase()).cancel();
            queueTimers.remove(name.toLowerCase());
        }

        saveConfiguration();
        return true;
    }

    /**
     * Starts a minigame
     *
     * @param name The name of the minigame
     * @return True if the minigame was started, false otherwise
     */
    public boolean startMinigame(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        if (configGame.getState() != MinigameState.COUNTDOWN) {
            return false;
        }

        // Get the players from the queue
        Queue<UUID> queue = gameQueues.get(name.toLowerCase());
        List<Player> players = new ArrayList<>();
        while (!queue.isEmpty() && players.size() < configGame.getMaxPlayers()) {
            UUID playerId = queue.poll();
            Player player = null;
            if (playerId != null) {
                player = Bukkit.getPlayer(playerId);
            }
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        if (players.isEmpty()) {
            return false;
        }

        // Start the game
        configGame.startGame(players);
        return true;
    }

    public void joinQueue(Player player, String gameName) {
        // Check if player is an op
        if (player.isOp()) {
            player.sendMessage(Component.text("Ops cannot participate in minigames!", NamedTextColor.RED));
            return;
        }

        if (!games.containsKey(gameName)) {
            player.sendMessage(Component.text("That minigame doesn't exist!", NamedTextColor.RED));
            return;
        }

        // Check if player is already in a queue
        for (Map.Entry<String, Queue<UUID>> entry : gameQueues.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) {
                player.sendMessage(Component.text("You are already in a queue for " + entry.getKey() + "!", NamedTextColor.RED));
                return;
            }
        }

        // Check if player is already in a game
        if (playerGameMap.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a game!", NamedTextColor.RED));
            return;
        }

        // Get the game and check if it's enabled
        Minigame game = games.get(gameName);
        if (game instanceof ConfigurableMinigame configGame) {
            if (configGame.getState() == MinigameState.DISABLED) {
                player.sendMessage(Component.text("This minigame is currently disabled!", NamedTextColor.RED));
                return;
            }

            // Update state to QUEUE if it's READY
            if (configGame.getState() == MinigameState.READY) {
                configGame.setState(MinigameState.QUEUE);
            }
        }

        Queue<UUID> queue = gameQueues.get(gameName);
        queue.add(player.getUniqueId());

        player.sendMessage(Component.text("You have joined the queue for " + gameName + "!", NamedTextColor.GREEN));

        // Check if we should start the game
        int minPlayers = getMinPlayers(gameName);
        int maxPlayers = getMaxPlayers(gameName);

        // Update the game state if it's a ConfigurableMinigame
        if (game instanceof ConfigurableMinigame configGame) {
            configGame.setInQueue(queue.size());

            // Update game state based on queue size
            if (queue.size() >= minPlayers && configGame.getState() == MinigameState.QUEUE) {
                configGame.setState(MinigameState.COUNTDOWN);
            }
        }

        if (queue.size() >= maxPlayers) {
            // Start the game immediately if we have max players
            startGame(gameName);
        } else if (queue.size() >= minPlayers && !queueTimers.containsKey(gameName)) {
            // Start a timer if we have minimum players and no timer is running
            int queueTime = config.getInt("queueTime." + gameName);

            // Announce that the game will start soon
            for (UUID playerId : queue) {
                Player queuedPlayer = Bukkit.getPlayer(playerId);
                if (queuedPlayer != null) {
                    queuedPlayer.sendMessage(Component.text("Game will start in " + (queueTime / 60) + " minutes!", NamedTextColor.YELLOW));
                }
            }

            // Schedule the game to start
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startGame(gameName);
                queueTimers.remove(gameName);
            }, queueTime * 20L); // Convert seconds to ticks

            queueTimers.put(gameName, task);
        }
    }

    public void leaveQueue(Player player) {
        for (Map.Entry<String, Queue<UUID>> entry : gameQueues.entrySet()) {
            if (entry.getValue().remove(player.getUniqueId())) {
                String gameName = entry.getKey();
                player.sendMessage(Component.text("You have left the queue for " + gameName + "!", NamedTextColor.GREEN));

                // Update game state if it's a ConfigurableMinigame
                Minigame game = games.get(gameName);
                if (game instanceof ConfigurableMinigame configGame) {
                    configGame.setInQueue(entry.getValue().size());

                    // If queue size is now less than minimum players, change state back to READY
                    if (entry.getValue().size() < getMinPlayers(gameName) &&
                        configGame.getState() == MinigameState.COUNTDOWN) {
                        configGame.setState(MinigameState.READY);
                    }
                }

                // Check if we need to cancel the timer
                if (entry.getValue().size() < getMinPlayers(gameName) && queueTimers.containsKey(gameName)) {
                    queueTimers.get(gameName).cancel();
                    queueTimers.remove(gameName);

                    // Announce that the game start has been cancelled
                    for (UUID playerId : entry.getValue()) {
                        Player queuedPlayer = Bukkit.getPlayer(playerId);
                        if (queuedPlayer != null) {
                            queuedPlayer.sendMessage(Component.text("Not enough players, game start cancelled!", NamedTextColor.RED));
                        }
                    }
                }

                return;
            }
        }

        player.sendMessage(Component.text("You are not in any queue!", NamedTextColor.RED));
    }

    private void startGame(String gameName) {
        Queue<UUID> queue = gameQueues.get(gameName);
        Minigame game = games.get(gameName);

        if (queue.isEmpty()) {
            return;
        }

        // Get the players from the queue
        List<Player> players = new ArrayList<>();
        while (!queue.isEmpty() && players.size() < getMaxPlayers(gameName)) {
            UUID playerId = queue.poll();
            Player player = null;
            if (playerId != null) {
                player = Bukkit.getPlayer(playerId);
            }
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        if (players.size() < getMinPlayers(gameName)) {
            // Not enough players, put them back in the queue
            for (Player player : players) {
                queue.add(player.getUniqueId());
                player.sendMessage(Component.text("Not enough players to start the game!", NamedTextColor.RED));
            }
            return;
        }

        // Update game state if it's a ConfigurableMinigame
        if (game instanceof ConfigurableMinigame configGame) {
            // Set state to STARTED if it's in COUNTDOWN
            if (configGame.getState() == MinigameState.COUNTDOWN) {
                configGame.setState(MinigameState.STARTED);
            }
        }

        // Store player inventories and teleport them to the game world
        for (Player player : players) {
            // Store inventory
            storePlayerInventory(player);

            // Add player to the game
            playerGameMap.put(player.getUniqueId(), gameName);

            // Set game mode to survival
            player.setGameMode(GameMode.SURVIVAL);

            // Announce
            player.sendMessage(Component.text("The game is starting!", NamedTextColor.GREEN));
        }

        // Start the game
        game.startGame(players);
    }

    public void endGame(String gameName, List<Player> players) {
        // Restore player inventories and teleport them back
        for (Player player : players) {
            // Remove from game
            playerGameMap.remove(player.getUniqueId());

            // Restore inventory
            restorePlayerInventory(player);

            // Announce
            player.sendMessage(Component.text("The game has ended!", NamedTextColor.GREEN));
        }

        // Reset the game
        games.get(gameName).resetGame();
    }

    private void storePlayerInventory(Player player) {
        // Store inventory
        inventories.put(player.getUniqueId(), player.getInventory().getContents());

        // Store armor pieces individually
        ItemStack[] armor = new ItemStack[4];
        armor[0] = player.getInventory().getBoots();
        armor[1] = player.getInventory().getLeggings();
        armor[2] = player.getInventory().getChestplate();
        armor[3] = player.getInventory().getHelmet();
        armorContents.put(player.getUniqueId(), armor);

        // Store location
        previousLocations.put(player.getUniqueId(), player.getLocation());

        // Clear inventory
        player.getInventory().clear();

        // Clear armor pieces individually
        player.getInventory().setBoots(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setHelmet(null);
    }

    private void restorePlayerInventory(Player player) {
        // Restore inventory
        if (inventories.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(inventories.get(player.getUniqueId()));
            inventories.remove(player.getUniqueId());
        }

        // Restore armor pieces individually
        if (armorContents.containsKey(player.getUniqueId())) {
            ItemStack[] armor = armorContents.get(player.getUniqueId());
            player.getInventory().setBoots(armor[0]);
            player.getInventory().setLeggings(armor[1]);
            player.getInventory().setChestplate(armor[2]);
            player.getInventory().setHelmet(armor[3]);
            armorContents.remove(player.getUniqueId());
        }

        // Teleport back to previous location
        if (previousLocations.containsKey(player.getUniqueId())) {
            player.teleport(previousLocations.get(player.getUniqueId()));
            previousLocations.remove(player.getUniqueId());
        }
    }

    public void playerQuit(Player player) {
        // Remove from queue
        for (Queue<UUID> queue : gameQueues.values()) {
            queue.remove(player.getUniqueId());
        }

        // Remove from game
        if (playerGameMap.containsKey(player.getUniqueId())) {
            String gameName = playerGameMap.get(player.getUniqueId());
            playerGameMap.remove(player.getUniqueId());

            // Restore inventory on next login
            // This will be handled by a listener
        }

        // Handle player quitting while in edit mode
        if (playersInEditMode.containsKey(player.getUniqueId())) {
            String gameName = playersInEditMode.get(player.getUniqueId());
            playersInEditMode.remove(player.getUniqueId());

            // Remove the player as an editor without saving changes
            if (plugin.getWorldManager().isPlayerEditingMap(player)) {
                plugin.getWorldManager().removeMapEditor(player, false);
            }

            // Restore inventory on next login
            // This will be handled by a listener
        }
    }

    public void shutdown() {
        // Cancel all timers
        for (BukkitTask task : queueTimers.values()) {
            task.cancel();
        }
        queueTimers.clear();

        // End all games
        for (Map.Entry<String, Minigame> entry : games.entrySet()) {
            entry.getValue().forceEnd();
        }

        // Handle players in edit mode
        for (UUID playerId : new HashSet<>(playersInEditMode.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Remove the player as an editor without saving changes
                if (plugin.getWorldManager().isPlayerEditingMap(player)) {
                    plugin.getWorldManager().removeMapEditor(player, false);
                }

                // Restore inventory
                restorePlayerInventory(player);

                // Restore game mode
                if (previousGameModes.containsKey(playerId)) {
                    player.setGameMode(previousGameModes.get(playerId));
                }
            }
        }
        playersInEditMode.clear();
        previousGameModes.clear();

        // Restore all inventories for online players
        for (UUID playerId : inventories.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                restorePlayerInventory(player);
            }
        }

        saveConfiguration();
    }

    public Minigame getGame(String gameName) {
        return games.get(gameName);
    }

    public boolean isInGame(Player player) {
        return playerGameMap.containsKey(player.getUniqueId());
    }

    public String getPlayerGame(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

    public Set<String> getGameNames() {
        return games.keySet();
    }

    public int getQueueSize(String gameName) {
        return gameQueues.getOrDefault(gameName, new LinkedList<>()).size();
    }

    public int getMinPlayers(String gameName) {
        Minigame game = games.get(gameName);
        if (game instanceof ConfigurableMinigame configGame) {
            return configGame.getMinPlayers();
        }
        return config.getInt("minPlayers." + gameName);
    }

    public int getMaxPlayers(String gameName) {
        Minigame game = games.get(gameName);
        if (game instanceof ConfigurableMinigame configGame) {
            return configGame.getMaxPlayers();
        }
        return config.getInt("maxPlayers." + gameName);
    }

    /**
     * Checks if a player is in edit mode
     *
     * @param player The player
     * @return True if the player is in edit mode, false otherwise
     */
    public boolean isPlayerInEditMode(Player player) {
        return playersInEditMode.containsKey(player.getUniqueId());
    }

    /**
     * Gets the name of the map a player is editing
     *
     * @param player The player
     * @return The name of the map, or null if the player is not in edit mode
     */
    public String getPlayerEditingMap(Player player) {
        return playersInEditMode.get(player.getUniqueId());
    }

    /**
     * Puts a player in edit mode for a minigame
     *
     * @param player   The player
     * @param gameName The name of the minigame
     * @return True if the player was put in edit mode, false otherwise
     */
    public boolean editMinigame(Player player, String gameName) {
        // Check if the player is already in edit mode
        if (playersInEditMode.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in edit mode!", NamedTextColor.RED));
            return false;
        }

        // Check if the minigame exists
        Minigame game = games.get(gameName.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            player.sendMessage(Component.text("That minigame doesn't exist!", NamedTextColor.RED));
            return false;
        }

        // Store player's inventory and game mode
        storePlayerInventory(player);
        previousGameModes.put(player.getUniqueId(), player.getGameMode());

        // Set player to creative mode and give them a WorldEdit wand
        player.setGameMode(GameMode.CREATIVE);
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        player.getInventory().addItem(wand);

        // Use the WorldManager to add the player as an editor of the map
        if (!plugin.getWorldManager().addMapEditor(player, configGame.getMapName())) {
            player.sendMessage(Component.text("Failed to start editing the map!", NamedTextColor.RED));
            return false;
        }

        // Mark player as in edit mode
        playersInEditMode.put(player.getUniqueId(), gameName.toLowerCase());

        // Show scoreboard with game details
        showEditScoreboard(player, configGame);

        player.sendMessage(Component.text("You are now editing " + gameName + "!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Use /minigame spawn <num> to set spawn points.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Use /minigame save to save your changes.", NamedTextColor.YELLOW));

        return true;
    }

    /**
     * Sets a spawn point for a minigame
     *
     * @param player      The player
     * @param spawnNumber The spawn point number
     * @return True if the spawn point was set, false otherwise
     */
    public boolean setSpawnPoint(Player player, int spawnNumber) {
        // Check if the player is in edit mode
        if (!playersInEditMode.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be in edit mode to set spawn points!", NamedTextColor.RED));
            return false;
        }

        String gameName = playersInEditMode.get(player.getUniqueId());
        Minigame game = games.get(gameName);
        if (!(game instanceof ConfigurableMinigame configGame)) {
            player.sendMessage(Component.text("That minigame doesn't exist!", NamedTextColor.RED));
            return false;
        }

        // Set the spawn point
        configGame.setSpawnPoint(spawnNumber, player.getLocation());

        // Update the scoreboard
        showEditScoreboard(player, configGame);

        player.sendMessage(Component.text("Spawn point " + spawnNumber + " set!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Saves changes to a minigame and exits edit mode
     *
     * @param player The player
     * @return True if the changes were saved, false otherwise
     */
    public boolean saveMinigameChanges(Player player) {
        // Check if the player is in edit mode
        if (!playersInEditMode.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be in edit mode to save changes!", NamedTextColor.RED));
            return false;
        }

        String gameName = playersInEditMode.get(player.getUniqueId());
        playersInEditMode.remove(player.getUniqueId());

        // Restore player's inventory and game mode
        restorePlayerInventory(player);
        if (previousGameModes.containsKey(player.getUniqueId())) {
            player.setGameMode(previousGameModes.get(player.getUniqueId()));
            previousGameModes.remove(player.getUniqueId());
        }

        // Use the WorldManager to remove the player as an editor of the map
        Minigame game = games.get(gameName);
        if (game instanceof ConfigurableMinigame configGame) {
            plugin.getWorldManager().removeMapEditor(player, true);
        }

        // Remove the scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard newScoreboard = manager.getNewScoreboard();
        // Ensure no objectives are displayed in the sidebar
        if (player.getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null) {
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        }
        player.setScoreboard(newScoreboard);

        // Save the configuration
        saveConfiguration();

        player.sendMessage(Component.text("Changes saved!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Shows a scoreboard with game details to a player in edit mode
     *
     * @param player The player
     * @param game   The minigame
     */
    private void showEditScoreboard(Player player, ConfigurableMinigame game) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("minigame_edit", Criteria.DUMMY, Component.text("Minigame Editor", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore("Name: " + game.getName()).setScore(7);
        objective.getScore("Type: " + game.getType().getDisplayName()).setScore(6);
        objective.getScore("Map: " + game.getMapName()).setScore(5);
        objective.getScore("Min Players: " + game.getMinPlayers()).setScore(4);
        objective.getScore("Max Players: " + game.getMaxPlayers()).setScore(3);
        objective.getScore("Spawn Points: " + game.getSpawnPoints().size()).setScore(2);
        objective.getScore("State: " + game.getState().getName()).setScore(1);

        player.setScoreboard(scoreboard);
    }
}
