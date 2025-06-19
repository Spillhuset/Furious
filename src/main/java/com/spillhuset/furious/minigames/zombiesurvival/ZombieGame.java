package com.spillhuset.furious.minigames.zombiesurvival;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameType;
import com.spillhuset.furious.managers.MinigameManager;
import com.spillhuset.furious.minigames.ConfigurableMinigame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * ZombieGame implementation of ConfigurableMinigame
 * A PvE minigame where players fight against zombies
 */
public class ZombieGame extends ConfigurableMinigame implements Listener {
    // Game constants
    private static final int INITIAL_SPAWN_RATE_SECONDS = 10; // 1 zombie per 10 seconds initially
    private static final int SPAWN_RATE_INCREASE_TIME = 30; // Increase spawn rate after 30 seconds
    private static final int COUNTDOWN_SECONDS = 5; // 5 seconds countdown

    // Game state
    private final Map<UUID, ZombieDifficulty> playerDifficulties;
    private int currentSpawnRate;
    private int gameTime;
    private int zombiesSpawned;

    /**
     * Constructor for ZombieGame
     *
     * @param plugin  The plugin instance
     * @param manager The minigame manager
     */
    public ZombieGame(Furious plugin, MinigameManager manager) {
        super(plugin, manager, "zombiesurvival", MinigameType.ZOMBIESURVIVAL, 1, "zombiesurvival");
        this.playerDifficulties = new HashMap<>();
        this.currentSpawnRate = INITIAL_SPAWN_RATE_SECONDS;
        this.gameTime = 0;
        this.zombiesSpawned = 0;

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected void onGameStart(List<Player> players) {
        // Set default difficulty for players who haven't chosen one
        for (Player player : players) {
            if (!playerDifficulties.containsKey(player.getUniqueId())) {
                playerDifficulties.put(player.getUniqueId(), ZombieDifficulty.NORMAL);
            }
        }

        // Set all players to survival mode and give equipment based on difficulty
        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);

            // Give equipment based on difficulty
            giveEquipment(player, playerDifficulties.get(player.getUniqueId()));
        }

        // Start countdown
        startCountdown(players);
    }

    /**
     * Gives equipment to a player based on their chosen difficulty
     *
     * @param player     The player
     * @param difficulty The difficulty
     */
    private void giveEquipment(Player player, ZombieDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                // Diamond armor, diamond sword
                player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
                break;
            case NORMAL:
                // Iron armor, iron sword
                player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                break;
            case HARD:
                // Leather armor, wooden sword
                player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                break;
            case INSANE:
                // No armor, no weapon
                break;
        }
    }

    private void startCountdown(List<Player> players) {
        // Freeze players during countdown
        for (Player player : players) {
            player.setWalkSpeed(0);
            player.sendMessage(Component.text("Game starting in " + COUNTDOWN_SECONDS + " seconds!", NamedTextColor.GOLD));
        }

        // Schedule countdown task
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int countdown = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (countdown > 0) {
                    if (countdown <= 3 || countdown % 5 == 0) {
                        for (Player player : players) {
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Component.text("Game starting in " + countdown + " seconds!", NamedTextColor.YELLOW));
                            }
                        }
                    }
                    countdown--;
                } else {
                    // Start the game
                    for (Player player : players) {
                        if (player != null && player.isOnline()) {
                            player.setWalkSpeed(0.2f); // Reset walk speed
                            player.sendMessage(Component.text("The game has begun! Zombies will spawn every " +
                                    currentSpawnRate + " seconds.", NamedTextColor.GREEN));
                        }
                    }

                    // Cancel this task and start the game timer
                    gameTask.cancel();
                    startGameTimer();
                }
            }
        }, 20L, 20L); // Run every second
    }

    private void startGameTimer() {
        // Reset game state
        gameTime = 0;
        zombiesSpawned = 0;
        currentSpawnRate = INITIAL_SPAWN_RATE_SECONDS;

        // Schedule game timer task
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksUntilNextSpawn = currentSpawnRate;

            @Override
            public void run() {
                // Increment game time
                gameTime++;

                // Check if we should increase spawn rate
                if (gameTime == SPAWN_RATE_INCREASE_TIME) {
                    currentSpawnRate = Math.max(1, currentSpawnRate / 2); // Double spawn rate (halve the time)
                    for (UUID playerId : getPlayers()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text("Spawn rate increased! Zombies now spawn every " +
                                    currentSpawnRate + " seconds!", NamedTextColor.RED));
                        }
                    }
                }

                // Handle zombie spawning
                ticksUntilNextSpawn--;
                if (ticksUntilNextSpawn <= 0) {
                    spawnZombie();
                    ticksUntilNextSpawn = currentSpawnRate;
                }

                // Check if all players are dead
                checkAllPlayersDead();
            }
        }, 20L, 20L); // Run every second
    }

    private void checkAllPlayersDead() {
        boolean allPlayersDead = true;
        for (UUID playerId : getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                allPlayersDead = false;
                break;
            }
        }

        if (allPlayersDead) {
            endGame();
        }
    }

    /**
     * Spawns a zombie at a random location near a player
     */
    private void spawnZombie() {
        // Get a random player to spawn near
        List<Player> alivePlayers = new ArrayList<>();
        for (UUID playerId : getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                alivePlayers.add(player);
            }
        }

        if (alivePlayers.isEmpty()) {
            return;
        }

        Player targetPlayer = alivePlayers.get((int) (Math.random() * alivePlayers.size()));
        Location spawnLoc = targetPlayer.getLocation().clone();

        // Offset the spawn location
        double angle = Math.random() * 2 * Math.PI;
        double distance = 10 + Math.random() * 5; // Between 10-15 blocks away
        spawnLoc.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);

        // Ensure the spawn location is valid
        spawnLoc.setY(gameWorld.getHighestBlockYAt(spawnLoc) + 1);

        // Spawn the zombie
        gameWorld.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        zombiesSpawned++;

        // Announce the spawn
        for (UUID playerId : getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("A zombie has spawned! Total zombies: " + zombiesSpawned, NamedTextColor.YELLOW));
            }
        }
    }

    /**
     * Handles a player death in the game
     *
     * @param player The player who died
     */
    public void handlePlayerDeath(Player player) {
        if (!isRunning()) {
            return;
        }

        // Set player to spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Announce death
        for (UUID playerId : getPlayers()) {
            Player gamePlayer = Bukkit.getPlayer(playerId);
            if (gamePlayer != null && gamePlayer.isOnline()) {
                gamePlayer.sendMessage(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));
            }
        }

        // Check if all players are dead
        checkAllPlayersDead();
    }

    @Override
    protected void onGameEnd(List<Player> players) {
        // Announce game over
        for (Player player : players) {
            player.sendMessage(Component.text("Game over! You survived against " + zombiesSpawned + " zombies!", NamedTextColor.GOLD));
        }

        // Reset all players
        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Clear player difficulties
        playerDifficulties.clear();
    }

    /**
     * Handles a player clicking on a sign to select difficulty
     *
     * @param event The event
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if the player is in this game's queue
        String playerGame = manager.getPlayerGame(player);
        if (playerGame == null || !playerGame.equalsIgnoreCase(getName())) {
            return;
        }

        // Check if the player clicked a sign
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        SignSide signSide = sign.getTargetSide(player);
        String line1 = signSide.line(0).toString().toLowerCase();

        // Check if it's a difficulty sign
        if (!line1.contains("difficulty")) {
            return;
        }

        // Parse the difficulty from the sign
        String difficultyText = signSide.line(1).toString().toLowerCase();
        ZombieDifficulty difficulty = null;

        if (difficultyText.contains("easy")) {
            difficulty = ZombieDifficulty.EASY;
        } else if (difficultyText.contains("normal")) {
            difficulty = ZombieDifficulty.NORMAL;
        } else if (difficultyText.contains("hard")) {
            difficulty = ZombieDifficulty.HARD;
        } else if (difficultyText.contains("insane")) {
            difficulty = ZombieDifficulty.INSANE;
        }

        if (difficulty != null) {
            playerDifficulties.put(player.getUniqueId(), difficulty);
            player.sendMessage(Component.text("You selected " + difficulty.name() + " difficulty!", NamedTextColor.GREEN));

            // Give equipment based on selected difficulty
            giveEquipment(player, difficulty);

            event.setCancelled(true);
        }
    }

    /**
     * Enum representing different difficulty levels for the zombie game
     */
    public enum ZombieDifficulty {
        EASY(),    // Diamond armor, diamond sword
        NORMAL(),  // Iron armor, iron sword
        HARD(),    // Leather armor, wooden sword
        INSANE();   // No armor, no weapon

        ZombieDifficulty() {
        }

    }
}
