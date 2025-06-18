package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Listener for tracking day cycles and applying bank interest.
 * Interest is applied every 2 day-cycles, and then new random interest rates are set.
 */
public class BankInterestListener implements Listener {
    private final Furious plugin;
    private final BankManager bankManager;
    private final Map<UUID, Long> lastDayTime = new HashMap<>();
    private final Map<UUID, Integer> worldDayCounts = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask timeCheckTask;

    public BankInterestListener(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        startTimeCheckTask();
    }

    /**
     * Starts a repeating task that checks for day changes in all worlds.
     */
    private void startTimeCheckTask() {
        // Run task every 5 minutes (6000 ticks)
        timeCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkDayCycles, 20L, 6000L);
    }

    /**
     * Checks for day changes in all worlds.
     */
    private void checkDayCycles() {
        for (World world : Bukkit.getWorlds()) {
            UUID worldId = world.getUID();
            long currentTime = world.getTime();

            // Initialize tracking for this world if not already tracked
            if (!lastDayTime.containsKey(worldId)) {
                lastDayTime.put(worldId, currentTime);
                worldDayCounts.put(worldId, 0);
                continue;
            }

            long lastTime = lastDayTime.get(worldId);

            // Check if a day has passed (time cycles from 0 to 24000)
            // We detect a new day when the time wraps around (e.g., from 23000 to 0)
            if (lastTime > 18000 && currentTime < 6000) {
                // A new day has started
                lastDayTime.put(worldId, currentTime);

                // Increment day count
                int dayCount = worldDayCounts.get(worldId) + 1;
                worldDayCounts.put(worldId, dayCount);

                plugin.getLogger().info("Day cycle in world " + world.getName() + ": " + dayCount);

                // Apply interest and set new rates every 2 days
                if (dayCount >= 2) {
                    // Reset day count
                    worldDayCounts.put(worldId, 0);

                    // Apply interest to all banks
                    plugin.getLogger().info("Applying interest to all bank accounts after 2 day-cycles");
                    bankManager.applyInterest();

                    // Set new random interest rates for all banks
                    setRandomInterestRates();
                }
            } else {
                // Update last time
                lastDayTime.put(worldId, currentTime);
            }
        }
    }

    /**
     * Sets random interest rates for all banks.
     * Interest rates are between 0.00 and 1.00 with two decimal places.
     */
    private void setRandomInterestRates() {
        for (String bankName : bankManager.getBanks().keySet()) {
            // Generate random interest rate between 0.00 and 1.00 with two decimal places
            double interestRate = Math.round(random.nextDouble() * 100) / 100.0;

            // Set the new interest rate
            bankManager.setInterestRate(bankName, interestRate);
            plugin.getLogger().info("Set new random interest rate for " + bankName + ": " + String.format("%.2f%%", interestRate * 100));
        }
    }

    /**
     * Cancels the time check task when the plugin is disabled.
     * This should be called from the plugin's onDisable method.
     */
    public void shutdown() {
        if (timeCheckTask != null) {
            timeCheckTask.cancel();
            timeCheckTask = null;
        }
    }
}
