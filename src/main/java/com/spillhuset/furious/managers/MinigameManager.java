package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.minigames.ConfigurableMinigame;
import com.spillhuset.furious.minigames.Minigame;
import com.spillhuset.furious.minigames.hungergames.HungerGame;
import com.spillhuset.furious.minigames.zombiesurvival.ZombieGame;
// SpleefGame is loaded via reflection to avoid direct dependencies
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

        // Register the SpleefGame
        try {
            Class<?> spleefGameClass = Class.forName("com.spillhuset.furious.minigames.spleef.SpleefGame");
            Object spleefGame = spleefGameClass.getConstructor(Furious.class, MinigameManager.class)
                    .newInstance(plugin, this);
            games.put("spleef", (Minigame) spleefGame);
            gameQueues.put("spleef", new LinkedList<>());
            plugin.getLogger().info("Registered Spleef minigame");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register Spleef minigame: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Creates a new minigame and puts the player in edit mode
     *
     * @param name       The name of the minigame
     * @param type       The type of the minigame
     * @param minPlayers The minimum number of players
     * @param mapName    The name of the map (with _temp suffix)
     * @param player     The player creating the minigame
     * @return True if the minigame was created, false otherwise
     */
    public boolean createMinigame(String name, MinigameType type, int minPlayers, String mapName, Player player) {
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

        // Store player's inventory and game mode
        storePlayerInventory(player);
        previousGameModes.put(player.getUniqueId(), player.getGameMode());

        // Set player to creative mode and give them WorldEdit tools
        player.setGameMode(GameMode.CREATIVE);
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemStack compass = new ItemStack(Material.COMPASS);
        player.getInventory().addItem(wand);
        player.getInventory().addItem(compass);

        // Create the world and teleport the player
        if (!plugin.getWorldManager().createMapEditCopy(mapName)) {
            player.sendMessage(Component.text("Failed to create world for the minigame!", NamedTextColor.RED));
            return false;
        }

        // Add the player as an editor of the map
        if (!plugin.getWorldManager().addMapEditor(player, mapName)) {
            player.sendMessage(Component.text("Failed to add you as an editor of the map!", NamedTextColor.RED));
            return false;
        }

        // Mark player as in edit mode
        playersInEditMode.put(player.getUniqueId(), name.toLowerCase());

        // Show scoreboard with game details
        showEditScoreboard(player, game);

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
     * Enables the queue for a minigame
     *
     * @param name The name of the minigame
     * @return True if the queue was enabled, false otherwise
     */
    public boolean enableQueue(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        configGame.enableQueue();
        saveConfiguration();
        return true;
    }

    /**
     * Disables the queue for a minigame
     *
     * @param name The name of the minigame
     * @return True if the queue was disabled, false otherwise
     */
    public boolean disableQueue(String name) {
        Minigame game = games.get(name.toLowerCase());
        if (!(game instanceof ConfigurableMinigame configGame)) {
            return false;
        }

        configGame.disableQueue();
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

        // Set state to READY since queue is now empty
        configGame.setState(MinigameState.READY);

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

        // Check if we have enough players to start the game
        if (players.isEmpty() || players.size() < configGame.getMinPlayers()) {
            // Put players back in the queue
            for (Player player : players) {
                queue.add(player.getUniqueId());
            }
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

            // Check if the queue is enabled
            if (!configGame.isQueueEnabled()) {
                player.sendMessage(Component.text("The queue for this minigame is currently disabled!", NamedTextColor.RED));
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

                    // If queue is empty, change state to READY
                    if (entry.getValue().isEmpty()) {
                        configGame.setState(MinigameState.READY);
                    }
                    // If queue size is now less than minimum players but not empty, change state back to QUEUE
                    else if (entry.getValue().size() < getMinPlayers(gameName) &&
                        configGame.getState() == MinigameState.COUNTDOWN) {
                        configGame.setState(MinigameState.QUEUE);
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
            // Set state to PREPARING if it's in COUNTDOWN
            if (configGame.getState() == MinigameState.COUNTDOWN) {
                configGame.setState(MinigameState.PREPARING);

                // Start the preparation countdown
                try {
                    java.lang.reflect.Method method = ConfigurableMinigame.class.getDeclaredMethod("startPreparationCountdown", List.class);
                    method.setAccessible(true);
                    method.invoke(configGame, players);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to start preparation countdown: " + e.getMessage());
                    e.printStackTrace();
                }
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

        // Store game mode
        previousGameModes.put(player.getUniqueId(), player.getGameMode());

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

        // Restore game mode
        if (previousGameModes.containsKey(player.getUniqueId())) {
            player.setGameMode(previousGameModes.get(player.getUniqueId()));
            previousGameModes.remove(player.getUniqueId());
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
        ItemStack compass = new ItemStack(Material.COMPASS);
        player.getInventory().addItem(wand);
        player.getInventory().addItem(compass);

        // Create a temporary map name
        String originalMapName = configGame.getMapName();
        String tempMapName = originalMapName + "_temp";

        // Update the minigame to use the temporary map name
        configGame.updateMapName(tempMapName);

        // Create the world and teleport the player
        if (!plugin.getWorldManager().createMapEditCopy(tempMapName)) {
            // Restore the original map name if creation fails
            configGame.updateMapName(originalMapName);
            player.sendMessage(Component.text("Failed to create temporary world for editing!", NamedTextColor.RED));
            return false;
        }

        // Use the WorldManager to add the player as an editor of the map
        if (!plugin.getWorldManager().addMapEditor(player, tempMapName)) {
            // Restore the original map name if adding editor fails
            configGame.updateMapName(originalMapName);
            player.sendMessage(Component.text("Failed to start editing the map!", NamedTextColor.RED));
            return false;
        }

        // Get the edit world
        World editWorld = Bukkit.getWorld(plugin.getWorldManager().getMapEditName(tempMapName));
        if (editWorld != null) {
            // Place carpet blocks at existing spawn points
            placeCarpetsAtSpawnPoints(configGame, editWorld);
            player.sendMessage(Component.text("Placed carpet blocks at existing spawn points. Move them to change spawn locations.", NamedTextColor.YELLOW));
        }

        // Mark player as in edit mode
        playersInEditMode.put(player.getUniqueId(), gameName.toLowerCase());

        // Show scoreboard with game details
        showEditScoreboard(player, configGame);

        player.sendMessage(Component.text("You are now editing " + gameName + "!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place carpet blocks where you want spawn points to be.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("You can also use /minigame spawn <num> to set spawn points manually.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Use /minigame save to save your changes and convert carpets to spawn points.", NamedTextColor.YELLOW));

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
     * Sets the lobby spawn point for a minigame
     *
     * @param player The player
     * @return True if the lobby spawn point was set, false otherwise
     */
    public boolean setLobbySpawn(Player player) {
        // Check if the player is in edit mode
        if (!playersInEditMode.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be in edit mode to set the lobby spawn point!", NamedTextColor.RED));
            return false;
        }

        String gameName = playersInEditMode.get(player.getUniqueId());
        Minigame game = games.get(gameName);
        if (!(game instanceof ConfigurableMinigame configGame)) {
            player.sendMessage(Component.text("That minigame doesn't exist!", NamedTextColor.RED));
            return false;
        }

        // Set the lobby spawn point
        configGame.setLobbySpawn(player.getLocation());

        // Update the scoreboard
        showEditScoreboard(player, configGame);

        player.sendMessage(Component.text("Lobby spawn point set!", NamedTextColor.GREEN));
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
            String mapName = configGame.getMapName();
            World editWorld = Bukkit.getWorld(plugin.getWorldManager().getMapEditName(mapName));

            if (editWorld != null) {
                // Find carpet blocks and set them as spawn points
                List<Location> carpetLocations = findCarpetBlocks(editWorld);

                // Set each carpet location as a spawn point
                int spawnIndex = 1;
                for (Location carpetLoc : carpetLocations) {
                    // Create a spawn point 1 block above the carpet
                    Location spawnLoc = carpetLoc.clone().add(0, 1, 0);
                    configGame.setSpawnPoint(spawnIndex, spawnLoc);
                    spawnIndex++;

                    // Remove the carpet block
                    carpetLoc.getBlock().setType(Material.AIR);
                }

                // Update max players based on number of spawn points
                if (!carpetLocations.isEmpty()) {
                    player.sendMessage(Component.text("Found " + carpetLocations.size() + " carpet blocks and set them as spawn points.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("No carpet blocks found. No spawn points were set.", NamedTextColor.YELLOW));
                }
            }

            // Check if this is a temporary map (ends with _temp)
            if (mapName.endsWith("_temp")) {
                // Get the final map name (without _temp)
                String finalMapName = mapName.substring(0, mapName.length() - 5);

                // Remove the player as an editor (this will save the changes to the temp world)
                plugin.getWorldManager().removeMapEditor(player, true);

                // Copy the temp world to the final world
                if (plugin.getWorldManager().copyWorldPublic(mapName, finalMapName)) {
                    // Update the map name in the minigame configuration
                    configGame.updateMapName(finalMapName);

                    player.sendMessage(Component.text("Map saved as " + finalMapName + "!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Failed to copy map to final destination!", NamedTextColor.RED));
                    return false;
                }
            } else {
                // Regular save without copying
                plugin.getWorldManager().removeMapEditor(player, true);
            }
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
     * Exits edit mode without saving changes
     *
     * @param player The player
     * @return True if the player was removed from edit mode, false otherwise
     */
    public boolean exitEditMode(Player player) {
        // Check if the player is in edit mode
        if (!playersInEditMode.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not in edit mode!", NamedTextColor.RED));
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

        // Use the WorldManager to remove the player as an editor of the map without saving changes
        Minigame game = games.get(gameName);
        if (game instanceof ConfigurableMinigame) {
            plugin.getWorldManager().removeMapEditor(player, false);
        }

        // Remove the scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard newScoreboard = manager.getNewScoreboard();
        // Ensure no objectives are displayed in the sidebar
        if (player.getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null) {
            player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        }
        player.setScoreboard(newScoreboard);

        player.sendMessage(Component.text("You have exited edit mode without saving changes.", NamedTextColor.YELLOW));
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

    /**
     * Finds all carpet blocks in a world
     *
     * @param world The world to search in
     * @return A list of locations where carpet blocks are found
     */
    private List<Location> findCarpetBlocks(World world) {
        List<Location> carpetLocations = new ArrayList<>();

        // Get the world border to limit the search area
        WorldBorder border = world.getWorldBorder();
        double size = border.getSize() / 2;
        Location center = border.getCenter();

        // Define the search area
        int minX = (int) (center.getX() - size);
        int maxX = (int) (center.getX() + size);
        int minZ = (int) (center.getZ() - size);
        int maxZ = (int) (center.getZ() + size);

        // Search for carpet blocks
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Get the highest block at this x,z coordinate
                int y = world.getHighestBlockYAt(x, z);

                // Check a few blocks up and down from the highest block
                for (int dy = -5; dy <= 5; dy++) {
                    Location loc = new Location(world, x, y + dy, z);
                    if (loc.getBlock().getType().name().endsWith("CARPET")) {
                        carpetLocations.add(loc);
                    }
                }
            }
        }

        return carpetLocations;
    }

    /**
     * Places carpet blocks at spawn points when editing
     *
     * @param game The minigame
     * @param world The world to place carpets in
     */
    private void placeCarpetsAtSpawnPoints(ConfigurableMinigame game, World world) {
        // Place a carpet at each spawn point
        for (Map.Entry<Integer, Location> entry : game.getSpawnPoints().entrySet()) {
            Location spawnLoc = entry.getValue();

            // Only place carpets in the same world
            if (!spawnLoc.getWorld().getName().equals(world.getName())) {
                continue;
            }

            // Get the block below the spawn point
            Location carpetLoc = spawnLoc.clone();
            carpetLoc.setY(carpetLoc.getY() - 1);

            // Place a carpet block
            carpetLoc.getBlock().setType(Material.WHITE_CARPET);
        }
    }
}
