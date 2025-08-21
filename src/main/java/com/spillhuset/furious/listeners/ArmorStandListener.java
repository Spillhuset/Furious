package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/**
 * A dedicated listener for ArmorStand-related events, delegating logic to ArmorStandManager.
 */
public class ArmorStandListener implements Listener {
    private final Furious plugin;

    public ArmorStandListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand)) return;
        UUID id = entity.getUniqueId();
        try {
            if (plugin.armorStandManager != null) {
                plugin.armorStandManager.handleArmorStandDeath(id);
            }
        } catch (Throwable ignored) {
        }
    }
}
