package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/**
 * Listens for ArmorStand being killed (e.g., by /kill) and removes the associated
 * warp/home/guildHome entry.
 */
public class ArmorStandLifecycleListener implements Listener {
    private final Furious plugin;

    public ArmorStandLifecycleListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand)) return;
        UUID id = entity.getUniqueId();
        boolean removed = false;
        try {
            if (plugin.warpsService != null) {
                removed = plugin.warpsService.removeByArmorStand(id) || removed;
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.homesService != null) {
                removed = plugin.homesService.removeByArmorStand(id) || removed;
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.guildHomesService != null) {
                removed = plugin.guildHomesService.removeByArmorStand(id) || removed;
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.shopsService != null) {
                removed = plugin.shopsService.removeByArmorStand(id) || removed;
            }
        } catch (Throwable ignored) {}
        if (removed) {
            try { plugin.getLogger().info("Data removed due to ArmorStand kill: " + id); } catch (Throwable ignored) {}
        }
    }
}
