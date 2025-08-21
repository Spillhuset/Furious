package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.Checklist.BiomesService;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;
import java.util.UUID;

public class BiomeTrackListener implements Listener {
    private final Furious plugin;

    public BiomeTrackListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        tryMark(p);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Only act when the player changes block to reduce spam
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        tryMark(event.getPlayer());
    }


    private double cfg(String key, double def) {
        try { return plugin.biomesService != null ? plugin.biomesService.getRewardAmount(key, def) : def; } catch (Throwable t) { return def; }
    }

    private void tryMark(Player p) {
        try {
            Biome biome = p.getLocation().getBlock().getBiome();
            if (plugin != null && plugin.biomesService != null) {
                String biomeKey = biome.key().asString();
                boolean includeGlobal = !p.isOp();
                BiomesService.VisitResult result = plugin.biomesService.visit(p.getUniqueId(), biomeKey, includeGlobal);
                String display = biome.key().value().toLowerCase(Locale.ROOT).replace('_', ' ');
                // Notify
                if (result.firstForPlayer()) {
                    Component action = Components.compose(NamedTextColor.YELLOW,
                            Components.t("Discovered biome: "),
                            Components.t(display, NamedTextColor.GOLD)
                    );
                    if (plugin.messageThrottle != null) plugin.messageThrottle.sendActionBarThrottled(p, action);
                    else p.sendActionBar(action);
                }
                if (result.firstForServer()) {
                    Component msg = Components.compose(NamedTextColor.AQUA,
                            Components.playerComp(p.getName()),
                            Components.t(" discovered the biome "),
                            Components.t(display, NamedTextColor.GOLD),
                            Components.t(" for the first time!")
                    );
                    if (plugin.messageThrottle != null) plugin.messageThrottle.broadcastThrottled(msg);
                    else plugin.getServer().broadcast(msg);
                }

                // Rewards
                UUID uid = p.getUniqueId();
                String type = "Biomes";
                String biomeKeyLc = biomeKey.toLowerCase(java.util.Locale.ROOT);
                // your-first-<type>
                if (result.firstForPlayer()) {
                    String marker = "player-first:" + type + ":" + uid + ":" + biomeKeyLc;
                    if (plugin.biomesService.markPaidOnce(marker)) {
                        double amt = cfg("your-first-" + type, 10);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "your-first-" + type + " " + biomeKeyLc);
                        }
                    }
                }
                // server-first-per-<type>
                if (result.firstForServer()) {
                    String marker = "server-first:" + type + ":" + biomeKeyLc;
                    if (plugin.biomesService.markPaidOnce(marker)) {
                        double amt = cfg("server-first-per-" + type, 1000);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "server-first-per-" + type + " " + biomeKeyLc);
                        }
                    }
                }
                // year-first and month-first (only if this player is registered winner)
                UUID yearWinner = plugin.biomesService.getYearFirst(biomeKey);
                if (yearWinner != null && yearWinner.equals(uid)) {
                    int yr = plugin.biomesService.getCurrentYear();
                    String marker = "year-first:" + type + ":" + yr + ":" + biomeKeyLc;
                    if (plugin.biomesService.markPaidOnce(marker)) {
                        double amt = cfg("this-year-first-per-" + type, 500);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "this-year-first-per-" + type + " " + biomeKeyLc);
                        }
                    }
                }
                UUID monthWinner = plugin.biomesService.getMonthFirst(biomeKey);
                if (monthWinner != null && monthWinner.equals(uid)) {
                    int yr = plugin.biomesService.getCurrentYear();
                    int mo = plugin.biomesService.getCurrentMonth();
                    String marker = "month-first:" + type + ":" + yr + "-" + String.format(java.util.Locale.ROOT, "%02d", mo) + ":" + biomeKeyLc;
                    if (plugin.biomesService.markPaidOnce(marker)) {
                        double amt = cfg("this-month-first-per-" + type, 500);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "this-month-first-per-" + type + " " + biomeKeyLc);
                        }
                    }
                }

                // 50%-<type>
                java.util.Set<String> visited = plugin.biomesService.getVisited(uid);
                int total = 0;
                try { total = (plugin.registryCache != null) ? plugin.registryCache.getBiomeKeysLower().size() : 0; } catch (Throwable ignored) {}
                if (total > 0) {
                    if (visited.size() * 2 >= total) {
                        String marker = "50pct:" + type + ":" + uid;
                        if (plugin.biomesService.markPaidOnce(marker)) {
                            double amt = cfg("50%-" + type, 2000);
                            if (amt > 0 && plugin.walletService != null) {
                                plugin.walletService.addBalance(uid, amt, "50%-" + type);
                            }
                        }
                    }
                }

                // Completion reward
                if (!plugin.biomesService.isCompletionRewarded(uid)) {
                    // Build set of all biome keys and check completion
                    int matched = 0;
                    try {
                        java.util.List<String> allKeys = (plugin.registryCache != null) ? plugin.registryCache.getBiomeKeysLower() : java.util.Collections.emptyList();
                        for (String key : allKeys) {
                            if (visited.contains(key)) matched++;
                        }
                    } catch (Throwable ignored) {}
                    if (total > 0 && matched >= total) {
                        double amt = cfg("complete-" + type, 10000);
                        plugin.biomesService.grantCompletionReward(uid, amt, type);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
