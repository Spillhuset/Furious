package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.Checklist.Checklist;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base service for checklist-like trackers that grant a completion reward.
 * Provides persistence of "rewards.completed" list and a unified reward method
 * that credits the player's wallet and notifies them, then marks as rewarded.
 */
public abstract class ChecklistService implements Checklist {
    protected final Furious plugin;
    protected final Set<UUID> completionRewarded = new HashSet<>();
    // Generic paid markers to avoid duplicate payouts for various milestones
    protected final Set<String> paidMarkers = new HashSet<>();

    // Common optional "firsts" tracking structures for services that need them
    protected final Map<String, UUID> serverFirst = new HashMap<>();
    protected final Map<Integer, Map<String, UUID>> yearFirstByYear = new HashMap<>();
    protected final Map<String, UUID> monthFirst = new HashMap<>();

    // Period tracking
    protected int currentYear = 0;
    protected int currentMonth = 0; // 1-12

    protected ChecklistService(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    // Subclasses must implement load/save to persist their own files after reward changes
    public abstract void load();
    public abstract void save();

    // --- Period helpers for services that track monthly/yearly "firsts" ---
    protected void ensurePeriodCurrent() {
        java.time.LocalDate now = java.time.LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();
        if (currentYear == 0) currentYear = y;
        if (currentMonth == 0) currentMonth = m;
        if (y != currentYear) {
            currentYear = y;
        }
        if (m != currentMonth) {
            currentMonth = m;
        }
    }

    public void clearMonthFirst() {
        ensurePeriodCurrent();
        monthFirst.clear();
        save();
    }

    public void clearYearFirst() {
        ensurePeriodCurrent();
        yearFirstByYear.put(currentYear, new HashMap<>());
        save();
    }

    public int getCurrentYear() {
        ensurePeriodCurrent();
        return currentYear;
    }

    public int getCurrentMonth() {
        ensurePeriodCurrent();
        return currentMonth;
    }

    public UUID getServerFirst(String key) {
        if (key == null) return null;
        return serverFirst.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    public UUID getYearFirst(String key) {
        if (key == null) return null;
        ensurePeriodCurrent();
        Map<String, UUID> map = yearFirstByYear.get(currentYear);
        if (map == null) return null;
        return map.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    public UUID getMonthFirst(String key) {
        if (key == null) return null;
        ensurePeriodCurrent();
        return monthFirst.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    // --- Reward flags persistence helpers ---
    protected void loadRewards(FileConfiguration config) {
        completionRewarded.clear();
        paidMarkers.clear();
        if (config == null) return;
        List<String> rewarded = config.getStringList("rewards.completed");
        for (String u : rewarded) {
            try { completionRewarded.add(UUID.fromString(u)); } catch (IllegalArgumentException ignored) {}
        }
        List<String> paid = config.getStringList("rewards.paid");
        paidMarkers.addAll(paid);
    }

    protected void saveRewards(FileConfiguration config) {
        if (config == null) return;
        List<String> rewarded = completionRewarded.stream().map(UUID::toString).toList();
        config.set("rewards.completed", rewarded);
        config.set("rewards.paid", new ArrayList<>(paidMarkers));
    }

    public boolean isCompletionRewarded(UUID playerId) {
        return playerId != null && completionRewarded.contains(playerId);
    }

    public void markCompletionRewarded(UUID playerId) {
        if (playerId == null) return;
        completionRewarded.add(playerId);
        save();
    }

    /** Checks and sets a unique paid marker. Returns true if it was newly marked. */
    public boolean markPaidOnce(String marker) {
        if (marker == null || marker.isEmpty()) return false;
        if (paidMarkers.contains(marker)) return false;
        paidMarkers.add(marker);
        save();
        return true;
    }

    /**
     * Grants the completion reward to player if WalletService exists, notifies them if online,
     * and marks them as rewarded. It is the caller's responsibility to call this only once
     * when completion is confirmed and not yet rewarded.
     */
    public void grantCompletionReward(UUID playerId, double amount, String checklistName) {
        if (playerId == null) return;
        // Credit wallet
        try {
            if (plugin.walletService != null) {
                plugin.walletService.addBalance(playerId, amount, checklistName + " checklist complete");
            }
        } catch (Throwable ignored) {}
        // Notify player if online
        try {
            Player p = plugin.getServer().getPlayer(playerId);
            if (p != null) {
                Component msg = Components.compose(NamedTextColor.GREEN,
                        Components.t("Congratulations! You completed the "),
                        Components.t(checklistName, NamedTextColor.GOLD),
                        Components.t(" checklist and earned "),
                        Components.amountComp(amount, plugin.walletService),
                        Components.t(".")
                );
                p.sendMessage(msg);
            }
        } catch (Throwable ignored) {}
        // Persist rewarded flag
        markCompletionRewarded(playerId);
    }

}
