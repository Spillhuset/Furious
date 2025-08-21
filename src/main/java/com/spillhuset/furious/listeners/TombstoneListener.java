package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.TombstoneService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listener wrapper that forwards events to TombstoneService logic.
 * This separates event registration concerns from the service implementation.
 */
public class TombstoneListener implements Listener {
    private final Furious plugin;
    private final TombstoneService service;

    public TombstoneListener(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.service = this.plugin.tombstoneService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (service != null) service.onPlayerDeath(event);
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (service != null) service.onInteract(event);
    }

    @EventHandler
    public void onArmorStandDamage(EntityDamageEvent event) {
        if (service != null) service.onArmorStandDamage(event);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (service != null) service.onDrop(event);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (service != null) service.onChunkUnload(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (service != null) service.onInventoryClose(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (service != null) service.onInventoryClick(event);
    }
}
