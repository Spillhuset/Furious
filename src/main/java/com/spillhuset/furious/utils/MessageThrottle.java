package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple throttle utility to prevent spamming action bar messages to players
 * and global broadcast messages to the server. Uses a per-player cooldown for
 * action bars and a global cooldown for broadcasts.
 */
public class MessageThrottle {
    private final Furious plugin;

    // Default cooldowns (in milliseconds)
    private static final long ACTION_BAR_COOLDOWN_MS = 1000L;
    private static final long BROADCAST_COOLDOWN_MS = 1000L;

    // Per-player last send timestamps for action bar messages
    private final Map<UUID, Long> lastActionBarAt = new ConcurrentHashMap<>();
    // Global last broadcast timestamp
    private final AtomicLong lastBroadcastAt = new AtomicLong(0L);

    public MessageThrottle(Furious plugin) {
        this.plugin = plugin != null ? plugin.getInstance() : null;
    }

    public void sendActionBarThrottled(Player player, Component component) {
        if (player == null || component == null) return;
        try {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long prev = lastActionBarAt.get(uuid);
            if (prev == null || (now - prev) >= ACTION_BAR_COOLDOWN_MS) {
                lastActionBarAt.put(uuid, now);
                // Only attempt to send if player is still online
                if (player.isOnline()) {
                    player.sendActionBar(component);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public void broadcastThrottled(Component component) {
        if (plugin == null || component == null) return;
        try {
            long now = System.currentTimeMillis();
            long prev = lastBroadcastAt.get();
            if ((now - prev) >= BROADCAST_COOLDOWN_MS) {
                lastBroadcastAt.set(now);
                plugin.getServer().broadcast(component);
            }
        } catch (Throwable ignored) {
        }
    }
}
