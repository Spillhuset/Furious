package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple throttling helper to reduce chat/actionbar spam when many events fire at once.
 * Provides per-player throttling for action bars and global throttling for broadcasts.
 */
public class MessageThrottle {
    private final Furious plugin;

    // Per player last action bar timestamp
    private final Map<UUID, Long> lastActionBar = new ConcurrentHashMap<>();
    // Global last broadcast by message key
    private final Map<String, Long> lastBroadcastByKey = new ConcurrentHashMap<>();

    // Defaults (milliseconds)
    private long actionBarWindowMs = 1500L;
    private long broadcastWindowMs = 3000L;

    public MessageThrottle(Furious plugin) {
        this.plugin = plugin;
    }

    public void setActionBarWindowMs(long ms) { this.actionBarWindowMs = ms; }
    public void setBroadcastWindowMs(long ms) { this.broadcastWindowMs = ms; }

    public void sendActionBarThrottled(Player player, Component component) {
        try {
            long now = System.currentTimeMillis();
            UUID id = player.getUniqueId();
            long last = lastActionBar.getOrDefault(id, 0L);
            if (now - last >= actionBarWindowMs) {
                lastActionBar.put(id, now);
                player.sendActionBar(component);
            }
        } catch (Throwable ignored) {}
    }

    public void broadcastThrottled(Component component) {
        try {
            long now = System.currentTimeMillis();
            String key = PlainTextComponentSerializer.plainText().serialize(component);
            long last = lastBroadcastByKey.getOrDefault(key, 0L);
            if (now - last >= broadcastWindowMs) {
                lastBroadcastByKey.put(key, now);
                plugin.getServer().broadcast(component);
            }
        } catch (Throwable ignored) {}
    }
}
