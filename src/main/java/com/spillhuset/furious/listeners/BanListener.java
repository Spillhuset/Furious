package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.BanService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanListener implements Listener {
    private final Furious plugin;

    public BanListener(Furious plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();
        if (plugin == null || plugin.banService == null) return;
        BanService.BanRecord rec = plugin.banService.getBan(name);
        if (rec == null) return;
        if (!plugin.banService.isBanned(name)) return; // cleans up expired
        String msg = "You are banned. Reason: " + rec.reason + (rec.expiresAt == null ? " (permanent)" : "\nUntil: " + new java.util.Date(rec.expiresAt));
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text(msg));
    }
}
