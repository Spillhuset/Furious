package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.Checklist.MonstersService;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Locale;
import java.util.UUID;

public class MonsterTrackListener implements Listener {
    private final Furious plugin;

    public MonsterTrackListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    private double cfg(String key, double def) {
        try { return plugin.monstersService != null ? plugin.monstersService.getRewardAmount(key, def) : def; } catch (Throwable t) { return def; }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Enemy)) return; // only track hostile monsters
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (plugin == null || plugin.monstersService == null) return;
        try {
            EntityType type = victim.getType();
            String key = type.getKey().asString();
            boolean includeGlobal = !killer.isOp();
            MonstersService.VisitResult res = plugin.monstersService.recordRemoval(killer.getUniqueId(), key, includeGlobal);
            String display = type.getKey().value().toLowerCase(Locale.ROOT).replace('_', ' ');
            if (res.firstForPlayer()) {
                Component action = Components.compose(NamedTextColor.YELLOW,
                        Components.t("Removed monster: "),
                        Components.t(display, NamedTextColor.GOLD)
                );
                if (plugin.messageThrottle != null) plugin.messageThrottle.sendActionBarThrottled(killer, action);
                else killer.sendActionBar(action);
            }
            if (res.firstForServer()) {
                Component msg = Components.compose(NamedTextColor.AQUA,
                        Components.playerComp(killer.getName()),
                        Components.t(" removed the monster "),
                        Components.t(display, NamedTextColor.GOLD),
                        Components.t(" for the first time!")
                );
                if (plugin.messageThrottle != null) plugin.messageThrottle.broadcastThrottled(msg);
                else plugin.getServer().broadcast(msg);
            }

            // Rewards
            UUID uid = killer.getUniqueId();
            String typeName = "Monsters";
            String keyLc = key.toLowerCase(java.util.Locale.ROOT);
            // your-first-Monsters
            if (res.firstForPlayer()) {
                String marker = "player-first:" + typeName + ":" + uid + ":" + keyLc;
                if (plugin.monstersService.markPaidOnce(marker)) {
                    double amt = cfg("your-first-" + typeName, 10);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "your-first-" + typeName + " " + keyLc);
                    }
                }
            }
            // server-first-per-Monsters
            if (res.firstForServer()) {
                String marker = "server-first:" + typeName + ":" + keyLc;
                if (plugin.monstersService.markPaidOnce(marker)) {
                    double amt = cfg("server-first-per-" + typeName, 1000);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "server-first-per-" + typeName + " " + keyLc);
                    }
                }
            }
            // year-first and month-first
            UUID yearWinner = plugin.monstersService.getYearFirst(key);
            if (yearWinner != null && yearWinner.equals(uid)) {
                int yr = plugin.monstersService.getCurrentYear();
                String marker = "year-first:" + typeName + ":" + yr + ":" + keyLc;
                if (plugin.monstersService.markPaidOnce(marker)) {
                    double amt = cfg("this-year-first-per-" + typeName, 500);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "this-year-first-per-" + typeName + " " + keyLc);
                    }
                }
            }
            UUID monthWinner = plugin.monstersService.getMonthFirst(key);
            if (monthWinner != null && monthWinner.equals(uid)) {
                int yr = plugin.monstersService.getCurrentYear();
                int mo = plugin.monstersService.getCurrentMonth();
                String marker = "month-first:" + typeName + ":" + yr + "-" + String.format(java.util.Locale.ROOT, "%02d", mo) + ":" + keyLc;
                if (plugin.monstersService.markPaidOnce(marker)) {
                    double amt = cfg("this-month-first-per-" + typeName, 500);
                    if (amt > 0 && plugin.walletService != null) {
                        plugin.walletService.addBalance(uid, amt, "this-month-first-per-" + typeName + " " + keyLc);
                    }
                }
            }

            // Completion and 50%
            java.util.Set<String> removed = plugin.monstersService.getRemoved(uid);
            // Build list of all hostile monster keys from cache
            java.util.List<String> allKeys;
            try {
                allKeys = (plugin.registryCache != null) ? plugin.registryCache.getHostileKeysLower() : java.util.Collections.emptyList();
            } catch (Throwable t) {
                allKeys = java.util.Collections.emptyList();
            }
            if (!allKeys.isEmpty()) {
                // 50%
                int total = allKeys.size();
                if (removed.size() * 2 >= total) {
                    String marker = "50pct:" + typeName + ":" + uid;
                    if (plugin.monstersService.markPaidOnce(marker)) {
                        double amt = cfg("50%-" + typeName, 2000);
                        if (amt > 0 && plugin.walletService != null) {
                            plugin.walletService.addBalance(uid, amt, "50%-" + typeName);
                        }
                    }
                }
                // complete
                boolean complete = true;
                for (String k : allKeys) {
                    if (!removed.contains(k)) { complete = false; break; }
                }
                if (complete && !plugin.monstersService.isCompletionRewarded(uid)) {
                    double amt = cfg("complete-" + typeName, 10000);
                    plugin.monstersService.grantCompletionReward(uid, amt, typeName);
                }
            }
        } catch (Throwable ignored) {}
    }
}
