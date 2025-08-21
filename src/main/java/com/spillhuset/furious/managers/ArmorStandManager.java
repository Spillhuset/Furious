package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for ArmorStand lifecycle. Services register a cleanup callback per ArmorStand UUID,
 * and this manager invokes it on ArmorStand death, removing the need for service-specific listeners.
 */
public class ArmorStandManager {
    private final Furious plugin;
    private final Map<UUID, Runnable> deathHandlers = new ConcurrentHashMap<>();

    public ArmorStandManager(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    /**
     * Ensure all domain-specific marker ArmorStands exist and are properly registered/visible.
     * Delegates to each service's ensureArmorStands() method.
     */
    public void ensureArmorStands() {
        try { if (plugin.homesService != null) plugin.homesService.ensureArmorStands(); } catch (Throwable ignored) {}
        try { if (plugin.guildHomesService != null) plugin.guildHomesService.ensureArmorStands(); } catch (Throwable ignored) {}
        try { if (plugin.warpsService != null) plugin.warpsService.ensureArmorStands(); } catch (Throwable ignored) {}
        try { if (plugin.shopsService != null) plugin.shopsService.ensureArmorStands(); } catch (Throwable ignored) {}
        try { if (plugin.banksService != null) plugin.banksService.ensureArmorStands(); } catch (Throwable ignored) {}
    }

    /**
     * Convenience: create an ArmorStand at the given location with the provided name and
     * sensible defaults (marker, no gravity, invulnerable, custom name visible, etc.).
     * Returns the ArmorStand UUID for storage, or null if creation fails.
     * Note: This method does NOT auto-register any death handler; services should call register() as needed.
     */
    public UUID create(Location location, String name) {
        if (location == null || location.getWorld() == null) return null;
        try {
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            // Common defaults used across services
            try { stand.setMarker(true); } catch (Throwable ignored) {}
            // Make the stand itself invisible to avoid any body being visible to non-op players
            // (ops will still see the name hologram if visible, and per-player show/hide logic remains in services)
            try { stand.setInvisible(true); } catch (Throwable ignored) {}
            try { stand.setGravity(false); } catch (Throwable ignored) {}
            // Defer showing names until services apply per-player visibility (prevents non-op flicker)
            try { stand.setCustomNameVisible(false); } catch (Throwable ignored) {}
            try { stand.setArms(false); } catch (Throwable ignored) {}
            try { stand.setBasePlate(false); } catch (Throwable ignored) {}
            try { stand.setSmall(false); } catch (Throwable ignored) {}
            try { stand.setCollidable(false); } catch (Throwable ignored) {}
            try { stand.setSilent(true); } catch (Throwable ignored) {}
            try { stand.setInvulnerable(true); } catch (Throwable ignored) {}
            // Tag as managed by this plugin for later cleanup
            try {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "managed");
                stand.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
            } catch (Throwable ignored) {}
            // Name assignment via Adventure API if available
            try {
                if (name != null) {
                    stand.customName(net.kyori.adventure.text.Component.text(name));
                }
            } catch (Throwable ignored) {
                // Fallback omitted to avoid deprecated API warnings; name may be unset on older APIs.
            }
            return stand.getUniqueId();
        } catch (Throwable t) {
            try { plugin.getLogger().warning("Failed to create ArmorStand: " + t.getMessage()); } catch (Throwable ignored) {}
            return null;
        }
    }

    /**
     * Register a cleanup handler to be invoked when the ArmorStand with given UUID dies.
     * If a handler already exists, it will be replaced.
     */
    public void register(UUID armorStandId, Runnable onDeath) {
        if (armorStandId == null || onDeath == null) return;
        deathHandlers.put(armorStandId, onDeath);
    }

    /**
     * Unregister a previously registered handler for the given ArmorStand UUID.
     */
    public void unregister(UUID armorStandId) {
        if (armorStandId == null) return;
        deathHandlers.remove(armorStandId);
    }

    /**
     * Clear all handlers (typically not needed except on plugin disable).
     */
    public void clear() {
        deathHandlers.clear();
    }

    /**
     * Handle death of an ArmorStand with the given UUID by running and removing its registered handler.
     */
    public void handleArmorStandDeath(UUID id) {
        if (id == null) return;
        Runnable handler = deathHandlers.remove(id);
        if (handler != null) {
            try {
                handler.run();
                try { plugin.getLogger().info("ArmorStand data cleaned by manager: " + id); } catch (Throwable ignored) {}
            } catch (Throwable t) {
                try { plugin.getLogger().warning("ArmorStandManager handler threw exception: " + t.getMessage()); } catch (Throwable ignored) {}
            }
        }
    }
}
