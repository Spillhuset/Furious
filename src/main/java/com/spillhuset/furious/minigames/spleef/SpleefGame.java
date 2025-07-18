package com.spillhuset.furious.minigames.spleef;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.MinigameState;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the Spleef minigame
 */
public class SpleefGame extends ConfigurableMinigame implements Listener {
    private final List<UUID> activePlayers = new ArrayList<>();
    private final Map<UUID, Boolean> canMove = new HashMap<>();
    private BukkitTask graceTask;
    private BukkitTask winnerTask;

    /**
     * Constructor for SpleefGame
     *
     * @param plugin The plugin instance
     * @param manager The minigame manager
     */
    public SpleefGame(Furious plugin, MinigameManager manager) {
        // Use 16 as max players (will be adjusted based on spawn points)
        super(plugin, manager, "Spleef", MinigameType.SPLEEF, 2, "spleef");
    }

    @Override
    protected void onGameStart(List<Player> players) {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Clear active players list and add all players
        activePlayers.clear();
        for (Player player : players) {
            activePlayers.add(player.getUniqueId());

            // Give each player a shovel
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.IRON_SHOVEL));

            // Set player to adventure mode
            player.setGameMode(GameMode.ADVENTURE);

            // Initialize movement restriction
            canMove.put(player.getUniqueId(), false);

            // Send message
            player.sendMessage(Component.text("Break snow blocks or hit other players to knock them into the water!", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("You have 5 seconds grace period where you can rotate but not move.", NamedTextColor.YELLOW));
        }

        // Start grace period
        startGracePeriod();
    }

    /**
     * Starts the grace period where players can rotate but not move
     */
    private void startGracePeriod() {
        graceTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int countdown = 5; // 5 seconds grace period

            @Override
            public void run() {
                if (countdown <= 0) {
                    // Grace period over, allow movement
                    for (UUID playerId : activePlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            canMove.put(playerId, true);
                            player.sendMessage(Component.text("Grace period over! You can now move!", NamedTextColor.GREEN));
                        }
                    }

                    // Cancel task
                    graceTask.cancel();
                    graceTask = null;
                    return;
                }

                // Announce remaining time
                for (UUID playerId : activePlayers) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(Component.text("Grace period: " + countdown + " seconds remaining", NamedTextColor.YELLOW));
                    }
                }

                countdown--;
            }
        }, 0L, 20L); // Run every second
    }

    @Override
    protected void onGameEnd(List<Player> players) {
        // Unregister event listener
        HandlerList.unregisterAll(this);

        // Cancel tasks if they exist
        if (graceTask != null) {
            graceTask.cancel();
            graceTask = null;
        }

        if (winnerTask != null) {
            winnerTask.cancel();
            winnerTask = null;
        }

        // Clear lists
        activePlayers.clear();
        canMove.clear();
    }

    /**
     * Handles player elimination when they hit water
     *
     * @param player The player to eliminate
     */
    private void eliminatePlayer(Player player) {
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }

        // Remove from active players
        activePlayers.remove(player.getUniqueId());

        // Announce elimination
        Bukkit.broadcast(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));

        // Teleport to lobby to watch
        if (getLobbySpawn() != null) {
            player.teleport(getLobbySpawn().clone());
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("You have been eliminated! You can watch the rest of the game from the lobby.", NamedTextColor.RED));
        }

        // Check if we should transition to FINAL state
        if (activePlayers.size() == 2) {
            setState(MinigameState.FINAL);
            Bukkit.broadcast(Component.text("The game is now in the final stage with 2 players remaining!", NamedTextColor.GOLD));
        }

        // Check if we have a winner
        if (activePlayers.size() == 1) {
            UUID winnerId = activePlayers.get(0);
            Player winner = Bukkit.getPlayer(winnerId);

            if (winner != null && winner.isOnline()) {
                // Announce winner
                Bukkit.broadcast(Component.text(winner.getName() + " has won the game!", NamedTextColor.GOLD));

                // Teleport winner to lobby
                if (getLobbySpawn() != null) {
                    winner.teleport(getLobbySpawn().clone());
                    winner.setGameMode(GameMode.ADVENTURE);
                    winner.sendMessage(Component.text("Congratulations! You have won the game!", NamedTextColor.GOLD));
                }

                // Schedule end game after 1 minute
                winnerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    endGame();
                }, 20L * 60L); // 60 seconds
            } else {
                // No winner found, end game immediately
                endGame();
            }
        }
    }

    /**
     * Event handler for player movement
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is in this game
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }

        // Check if player can move during grace period
        if (!canMove.getOrDefault(player.getUniqueId(), true)) {
            // Allow rotation but not movement
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                // Player is trying to move horizontally, cancel it
                Location newLoc = new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch());
                event.setTo(newLoc);
            }
            return;
        }

        // Check if player has hit water
        if (player.getLocation().getBlock().getType() == Material.WATER) {
            eliminatePlayer(player);
        }
    }

    /**
     * Event handler for block breaking
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Check if player is in this game
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }

        // Check if player can move during grace period
        if (!canMove.getOrDefault(player.getUniqueId(), true)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot break blocks during the grace period!", NamedTextColor.RED));
            return;
        }

        // Only allow breaking snow blocks
        if (event.getBlock().getType() != Material.SNOW_BLOCK) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You can only break snow blocks!", NamedTextColor.RED));
        }
    }

    /**
     * Event handler for player damage
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Check if both entities are players
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        // Check if both players are in this game
        if (!activePlayers.contains(damaged.getUniqueId()) || !activePlayers.contains(damager.getUniqueId())) {
            return;
        }

        // Check if damager can move during grace period
        if (!canMove.getOrDefault(damager.getUniqueId(), true)) {
            event.setCancelled(true);
            damager.sendMessage(Component.text("You cannot hit players during the grace period!", NamedTextColor.RED));
            return;
        }

        // Check if damager is holding a shovel
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (item.getType() != Material.IRON_SHOVEL) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage but apply knockback
        event.setCancelled(true);

        // Apply knockback
        Vector knockback = damaged.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize().multiply(1.5);
        knockback.setY(0.5); // Add some upward force
        damaged.setVelocity(knockback);
    }
}