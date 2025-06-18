package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for combat-related events
 */
public class CombatListener implements Listener {
    private final Furious plugin;

    public CombatListener(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player damage by entity events to track combat
     */
    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if the damaged entity is a player
        if (!(event.getEntity() instanceof Player damaged)) {
            return;
        }

        // Get the damager
        Entity damager = event.getDamager();

        // If the damager is a player, put both players in combat
        if (damager instanceof Player attacker) {
            plugin.getCombatManager().enterCombat(damaged);
            plugin.getCombatManager().enterCombat(attacker);
        } else {
            // If the damager is not a player (e.g., a mob), still put the damaged player in combat
            plugin.getCombatManager().enterCombat(damaged);
        }
    }

    /**
     * Handle player death events to exit combat
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Exit combat on death
        if (plugin.getCombatManager().isInCombat(player)) {
            plugin.getCombatManager().exitCombat(player);
        }
    }

    /**
     * Handle player quit events to exit combat
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Exit combat on quit
        if (plugin.getCombatManager().isInCombat(player)) {
            plugin.getCombatManager().exitCombat(player);
        }
    }
}