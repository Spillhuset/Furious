package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.MinigameManager;
import com.spillhuset.furious.minigames.hungergames.HungerGame;
import com.spillhuset.furious.minigames.zombiesurvival.ZombieGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Listener for minigame-related events
 */
public class MinigameListener implements Listener {
    private final Furious plugin;
    private final MinigameManager manager;

    /**
     * Constructor for MinigameListener
     *
     * @param plugin The plugin instance
     */
    public MinigameListener(Furious plugin) {
        this.plugin = plugin;
        this.manager = plugin.getMinigameManager();
    }

    /**
     * Handles player death events
     *
     * @param event The player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (manager.isInGame(player)) {
            String gameName = manager.getPlayerGame(player);

            if (gameName.equals("hungergame")) {
                // Handle hunger game death
                HungerGame hungerGame = (HungerGame) manager.getGame(gameName);
                hungerGame.handlePlayerDeath(player);

                // Prevent item drops
                event.getDrops().clear();

                // Set custom death message
                event.deathMessage(null);
            } else if (gameName.equals("zombiesurvival")) {
                // Handle zombie game death
                ZombieGame zombieGame = (ZombieGame) manager.getGame(gameName);
                zombieGame.handlePlayerDeath(player);

                // Prevent item drops
                event.getDrops().clear();

                // Set custom death message
                event.deathMessage(null);
            }
        }
    }

    /**
     * Handles player quit events
     *
     * @param event The player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Handle player quitting during a game
        manager.playerQuit(player);
    }

    /**
     * Handles player join events
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // This could be used to restore inventories if a player was in a game when they quit
        // Currently handled by MinigameManager
    }

    /**
     * Handles entity damage by entity events
     *
     * @param event The entity damage by entity event
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if both entities are players
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            // Check if both players are in the same game
            if (manager.isInGame(victim) && manager.isInGame(attacker)) {
                String gameName = manager.getPlayerGame(victim);

                // Check if it's the same game
                if (gameName.equals(manager.getPlayerGame(attacker))) {
                    if (gameName.equals("hungergame")) {
                        // Check if PvP is allowed in hunger games
                        HungerGame hungerGame = (HungerGame) manager.getGame(gameName);

                        if (!hungerGame.isPvPAllowed()) {
                            // Cancel damage during grace period
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles player teleport events
     *
     * @param event The player teleport event
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Check if the player is in a minigame
        if (manager.isInGame(player)) {
            // Allow teleportation within the same world (for game mechanics)
            if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
                return;
            }

            // Cancel teleportation to other worlds
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot teleport to other worlds while in a minigame!", NamedTextColor.RED));
        }
    }
}
