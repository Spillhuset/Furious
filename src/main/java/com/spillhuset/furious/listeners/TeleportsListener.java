package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.TeleportsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TeleportsListener implements Listener {
    private final Furious plugin;
    private final TeleportsService teleportsService;

    public TeleportsListener(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.teleportsService = this.plugin.teleportsService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!teleportsService.isQueued(player)) return;
        // Cancel if the player moved to a different block
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            teleportsService.cancelQueue(player, "movement");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (teleportsService.isQueued(player)) {
            teleportsService.cancelQueue(player, "interaction");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (teleportsService.isQueued(player)) {
                teleportsService.cancelQueue(player, "damage");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (teleportsService.isQueued(player)) {
            teleportsService.cancelQueue(player, "quit");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (teleportsService.isQueued(player)) {
            teleportsService.cancelQueue(player, "death");
        }
    }
}
