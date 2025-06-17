package com.spillhuset.furious.minigames.hungergames;

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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * HungerGame implementation of ConfigurableMinigame
 */
public class HungerGame extends ConfigurableMinigame {
    private static final int GAME_DURATION_SECONDS = 600; // 10 minutes
    private static final int GRACE_PERIOD_SECONDS = 30; // 30 seconds
    private static final int COUNTDOWN_SECONDS = 10; // 10 seconds

    private final Random random;
    private final ContainerRegistry containerRegistry;
    private List<UUID> alivePlayers;
    private boolean graceperiod;
    private int timeRemaining;

    /**
     * Constructor for HungerGame
     *
     * @param plugin The plugin instance
     * @param manager The minigame manager
     */
    public HungerGame(Furious plugin, MinigameManager manager) {
        super(plugin, manager, "hungergame", MinigameType.HUNGERGAME, 2, "hungergame");
        this.random = new Random();
        this.containerRegistry = plugin.getContainerRegistry();
        this.alivePlayers = new ArrayList<>();
        this.graceperiod = false;
        this.timeRemaining = GAME_DURATION_SECONDS;
    }

    @Override
    protected Location getSpawnLocation() {
        if (gameWorld == null) {
            createGameWorld();
        }

        // Get the spawn location of the world
        Location spawn = gameWorld.getSpawnLocation();

        // Ensure the spawn location is safe
        spawn.getBlock().setType(Material.BEDROCK);
        spawn.add(0, 1, 0);

        return spawn;
    }

    @Override
    protected void onGameStart(List<Player> players) {
        // Reset game state
        alivePlayers.clear();
        for (Player player : players) {
            alivePlayers.add(player.getUniqueId());
        }

        // Set all players to survival mode
        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);

            // Give starting items
            giveStartingItems(player);
        }

        // Fill containers with items
        if (gameWorld != null) {
            containerRegistry.fillContainers(getMapName(), gameWorld);

            // Start restock task
            containerRegistry.startRestockTask(getMapName(), gameWorld);
        }

        // Start countdown
        startCountdown(players);
    }

    private void giveStartingItems(Player player) {
        // Give basic items
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BREAD, 5));
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
                    if (countdown <= 5 || countdown % 5 == 0) {
                        for (UUID playerId : alivePlayers) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Component.text("Game starting in " + countdown + " seconds!", NamedTextColor.YELLOW));
                            }
                        }
                    }
                    countdown--;
                } else {
                    // Start the game
                    for (UUID playerId : alivePlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            player.setWalkSpeed(0.2f); // Reset walk speed
                            player.sendMessage(Component.text("The game has begun! Grace period: " + GRACE_PERIOD_SECONDS + " seconds", NamedTextColor.GREEN));
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
        // Start with grace period
        graceperiod = true;
        timeRemaining = GAME_DURATION_SECONDS;

        // Schedule game timer task
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int gracePeriodRemaining = GRACE_PERIOD_SECONDS;

            @Override
            public void run() {
                // Handle grace period
                if (graceperiod) {
                    gracePeriodRemaining--;

                    if (gracePeriodRemaining <= 0) {
                        graceperiod = false;
                        for (UUID playerId : alivePlayers) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Component.text("Grace period has ended! PvP is now enabled!", NamedTextColor.RED));
                            }
                        }
                    } else if (gracePeriodRemaining <= 5 || gracePeriodRemaining % 10 == 0) {
                        for (UUID playerId : alivePlayers) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Component.text("Grace period ends in " + gracePeriodRemaining + " seconds!", NamedTextColor.YELLOW));
                            }
                        }
                    }
                }

                // Handle game time
                timeRemaining--;

                // Check for game end conditions
                if (timeRemaining <= 0 || alivePlayers.size() <= 1) {
                    endGame();
                    return;
                }

                // Periodic announcements
                if (timeRemaining % 60 == 0) {
                    int minutesLeft = timeRemaining / 60;
                    for (UUID playerId : alivePlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Component.text(minutesLeft + " minutes remaining!", NamedTextColor.YELLOW));
                        }
                    }
                }
            }
        }, 20L, 20L); // Run every second
    }

    /**
     * Handles a player death in the game
     *
     * @param player The player who died
     */
    public void handlePlayerDeath(Player player) {
        if (!isRunning || !alivePlayers.contains(player.getUniqueId())) {
            return;
        }

        // Remove player from alive players
        alivePlayers.remove(player.getUniqueId());

        // Set player to spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Announce death
        Bukkit.getServer().broadcast(Component.text(player.getName() + " has been eliminated! " + alivePlayers.size() + " players remaining.", NamedTextColor.RED));

        // Check if game should end
        if (alivePlayers.size() <= 1) {
            endGame();
        }
    }

    @Override
    protected void onGameEnd(List<Player> players) {
        // Announce winner if there is one
        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.get(0);
            Player winner = Bukkit.getPlayer(winnerId);

            if (winner != null && winner.isOnline()) {
                Bukkit.getServer().broadcast(Component.text(winner.getName() + " has won the Hunger Games!", NamedTextColor.GOLD));
            }
        } else {
            Bukkit.getServer().broadcast(Component.text("The Hunger Games has ended with no winner!", NamedTextColor.GOLD));
        }

        // Stop the restock task
        containerRegistry.stopRestockTask(getMapName());

        // Reset all players
        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    /**
     * Checks if PvP is allowed (grace period is over)
     *
     * @return True if PvP is allowed, false otherwise
     */
    public boolean isPvPAllowed() {
        return isRunning && !graceperiod;
    }

    /**
     * Checks if a player is alive in the game
     *
     * @param player The player to check
     * @return True if the player is alive, false otherwise
     */
    public boolean isPlayerAlive(Player player) {
        return isRunning && alivePlayers.contains(player.getUniqueId());
    }
}
