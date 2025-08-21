package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.Checklist.TamingService;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

import java.util.Locale;
import java.util.UUID;

public class TamingTrackListener implements Listener {
    private final Furious plugin;

    public TamingTrackListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    private double cfg(String key, double def) {
        try { return plugin.tamingService != null ? plugin.tamingService.getRewardAmount(key, def) : def; } catch (Throwable t) { return def; }
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        try {
            if (!(event.getEntity() instanceof Tameable)) return;
            if (!(event.getOwner() instanceof Player)) return;
            Player p = (Player) event.getOwner();
            if (plugin == null || plugin.tamingService == null) return;
            EntityType type = event.getEntity().getType();
            String key = type.getKey().asString();
            boolean includeGlobal = !p.isOp();
            TamingService.VisitResult res = plugin.tamingService.recordTamed(p.getUniqueId(), key, includeGlobal);
            String display = type.getKey().value().toLowerCase(Locale.ROOT).replace('_', ' ');
            if (res.firstForPlayer()) {
                Component action = Components.compose(NamedTextColor.YELLOW,
                        Components.t("Tamed: "),
                        Components.t(display, NamedTextColor.GOLD)
                );
                if (plugin.messageThrottle != null) plugin.messageThrottle.sendActionBarThrottled(p, action);
                else p.sendActionBar(action);
            }
            if (res.firstForServer()) {
                Component msg = Components.compose(NamedTextColor.AQUA,
                        Components.playerComp(p.getName()),
                        Components.t(" tamed "),
                        Components.t(display, NamedTextColor.GOLD),
                        Components.t(" for the first time!")
                );
                if (plugin.messageThrottle != null) plugin.messageThrottle.broadcastThrottled(msg);
                else plugin.getServer().broadcast(msg);
            }

            // Rewards
            UUID uid = p.getUniqueId();
            String typeName = "Taming";
            String keyLc = key.toLowerCase(java.util.Locale.ROOT);
            // your-first-Taming
            if (res.firstForPlayer()) {
                String marker = "player-first:" + typeName + ":" + uid + ":" + keyLc;
                if (plugin.tamingService.markPaidOnce(marker)) {
                    double amt = cfg("your-first-" + typeName, 10);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "your-first-" + typeName + " " + keyLc);
                    }
                }
            }
            // server-first-per-Taming
            if (res.firstForServer()) {
                String marker = "server-first:" + typeName + ":" + keyLc;
                if (plugin.tamingService.markPaidOnce(marker)) {
                    double amt = cfg("server-first-per-" + typeName, 1000);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "server-first-per-" + typeName + " " + keyLc);
                    }
                }
            }
            // year-first and month-first
            UUID yearWinner = plugin.tamingService.getYearFirst(key);
            if (yearWinner != null && yearWinner.equals(uid)) {
                int yr = plugin.tamingService.getCurrentYear();
                String marker = "year-first:" + typeName + ":" + yr + ":" + keyLc;
                if (plugin.tamingService.markPaidOnce(marker)) {
                    double amt = cfg("this-year-first-per-" + typeName, 500);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "this-year-first-per-" + typeName + " " + keyLc);
                    }
                }
            }
            UUID monthWinner = plugin.tamingService.getMonthFirst(key);
            if (monthWinner != null && monthWinner.equals(uid)) {
                int yr = plugin.tamingService.getCurrentYear();
                int mo = plugin.tamingService.getCurrentMonth();
                String marker = "month-first:" + typeName + ":" + yr + "-" + String.format(java.util.Locale.ROOT, "%02d", mo) + ":" + keyLc;
                if (plugin.tamingService.markPaidOnce(marker)) {
                    double amt = cfg("this-month-first-per-" + typeName, 500);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "this-month-first-per-" + typeName + " " + keyLc);
                    }
                }
            }

            // 50% and complete
            java.util.Set<String> tamed = plugin.tamingService.getTamed(uid);
            java.util.List<String> allKeys;
            try {
                allKeys = (plugin.registryCache != null) ? plugin.registryCache.getTameableKeysLower() : java.util.Collections.emptyList();
            } catch (Throwable ex) {
                allKeys = java.util.Collections.emptyList();
            }
            if (!allKeys.isEmpty()) {
                int total = allKeys.size();
                if (tamed.size() * 2 >= total) {
                    String marker = "50pct:" + typeName + ":" + uid;
                    if (plugin.tamingService.markPaidOnce(marker)) {
                        double amt = cfg("50%-" + typeName, 2000);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "50%-" + typeName);
                        }
                    }
                }
                boolean complete = true;
                for (String k : allKeys) {
                    if (!tamed.contains(k)) { complete = false; break; }
                }
                if (complete && !plugin.tamingService.isCompletionRewarded(uid)) {
                    double amt = cfg("complete-" + typeName, 10000);
                    plugin.tamingService.grantCompletionReward(uid, amt, typeName);
                }
            }
        } catch (Throwable ignored) {}
    }
}
