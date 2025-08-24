package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * On chunk load, cleans up orphan ArmorStands that belong to this plugin
 * and ensures missing ArmorStands for homes/warps/guild homes/shops/banks are recreated.
 */
public class ChunkArmorStandSanitizer implements Listener {
    private final Furious plugin;
    private final NamespacedKey managedKey;
    // Debounce ensureArmorStands to avoid running it for every chunk load
    private static volatile long lastEnsureMs = 0L;
    private static volatile boolean ensureScheduled = false;
    // Debounce per-chunk sanitize to avoid repeated scans in a very short window
    private static final Map<String, Long> recentlySanitizedChunks = new ConcurrentHashMap<>();

    public ChunkArmorStandSanitizer(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.managedKey = new NamespacedKey(this.plugin, "managed");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Skip if the same chunk was sanitized very recently (within 1s)
        try {
            Chunk chunk = event.getChunk();
            String key = buildChunkKey(chunk);
            long now = System.currentTimeMillis();
            final long WINDOW_MS = 1000L;
            Long last = recentlySanitizedChunks.get(key);
            if (last != null && (now - last) < WINDOW_MS) {
                return;
            }
            recentlySanitizedChunks.put(key, now);
        } catch (Throwable ignored) {}
        // Defer a tick to ensure entities are fully available after load
        plugin.getServer().getScheduler().runTask(plugin, () -> sanitizeChunk(event.getChunk()));
    }

    private String buildChunkKey(Chunk c) {
        try {
            return c.getWorld().getUID() + ":" + c.getX() + ":" + c.getZ();
        } catch (Throwable t) {
            return c.getX() + ":" + c.getZ();
        }
    }

    private void sanitizeChunk(Chunk chunk) {
        try {
            for (Entity entity : chunk.getEntities()) {
                if (!(entity instanceof ArmorStand stand)) continue;

                if (!isManagedStand(stand)) continue; // ignore stands not belonging to this plugin
                try {
                    if (plugin.getConfig().getBoolean("debug.armorstands", false))
                        plugin.getLogger().info("Checking stand: name=" + stand.getName() + " id=" + stand.getUniqueId());
                } catch (Throwable ignored) {}
                UUID id = stand.getUniqueId();
                boolean referenced = false;
                try {
                    if (plugin.homesService != null) referenced = plugin.homesService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }
                try {
                    if (plugin.guildHomesService != null)
                        referenced = referenced || plugin.guildHomesService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }
                try {
                    if (plugin.warpsService != null) referenced = referenced || plugin.warpsService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }
                try {
                    if (plugin.shopsService != null) referenced = referenced || plugin.shopsService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }
                try {
                    if (plugin.banksService != null) referenced = referenced || plugin.banksService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }
                try {
                    if (plugin.auctionsService != null) referenced = referenced || plugin.auctionsService.hasArmorStand(id);
                } catch (Throwable ignored) {
                }

                if (!referenced) {
                    // Try to adopt this stand into a matching object before removing
                    boolean adopted = false;
                    try { if (plugin.homesService != null) adopted = plugin.homesService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    try { if (!adopted && plugin.guildHomesService != null) adopted = plugin.guildHomesService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    try { if (!adopted && plugin.warpsService != null) adopted = plugin.warpsService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    try { if (!adopted && plugin.shopsService != null) adopted = plugin.shopsService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    try { if (!adopted && plugin.banksService != null) adopted = plugin.banksService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    try { if (!adopted && plugin.auctionsService != null) adopted = plugin.auctionsService.adoptArmorStand(stand); } catch (Throwable ignored) {}
                    if (!adopted) {
                        // Not linked to any known object; remove the ArmorStand entity
                        try {
                            stand.remove();
                        } catch (Throwable ignored) {
                        }
                        try {
                            plugin.getLogger().info("Removed orphan ArmorStand in chunk (" + chunk.getX() + "," + chunk.getZ() + ") id=" + id+" name="+stand.getName());
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // After cleanup, schedule a debounced ensure to recreate any missing stands (run at most once per window)
        long now = System.currentTimeMillis();
        final long DEBOUNCE_MS = 2000L; // 2 seconds
        if (!ensureScheduled && (now - lastEnsureMs) >= DEBOUNCE_MS) {
            ensureScheduled = true;
            lastEnsureMs = now;
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        if (plugin.armorStandManager != null) plugin.armorStandManager.ensureArmorStands();
                    } catch (Throwable ignored) {
                    } finally {
                        ensureScheduled = false;
                    }
                });
            } catch (Throwable ignored) {
                ensureScheduled = false;
            }
        }
    }

    private boolean isManagedStand(ArmorStand stand) {
        // Prefer persistent data tag set by ArmorStandManager.create()
        try {
            PersistentDataContainer pdc = stand.getPersistentDataContainer();
            Byte b = pdc.get(managedKey, PersistentDataType.BYTE);
            if (b != null && b == (byte) 1) return true;
        } catch (Throwable ignored) {
        }
        // No longer use name-prefix heuristic to avoid deleting other plugins' holograms.
        // Only stands explicitly tagged with our managed key are considered ours.
        return false;
    }

}
