package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player combat status
 */
public class CombatManager {
    private final Furious plugin;
    private final Map<UUID, Long> combatTimers;
    private final Map<UUID, BukkitTask> combatTasks;
    private final int COMBAT_COOLDOWN_SECONDS = 10;

    public CombatManager(Furious plugin) {
        this.plugin = plugin;
        this.combatTimers = new HashMap<>();
        this.combatTasks = new HashMap<>();
    }

    /**
     * Puts a player in combat state
     *
     * @param player The player to put in combat
     */
    public void enterCombat(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing combat task
        BukkitTask existingTask = combatTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Set combat timer
        combatTimers.put(playerId, System.currentTimeMillis() + (COMBAT_COOLDOWN_SECONDS * 1000));

        // Update scoreboard
        updateCombatDisplay(player);

        // Schedule task to exit combat after cooldown
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                exitCombat(player);
            }
        }.runTaskLater(plugin, COMBAT_COOLDOWN_SECONDS * 20); // 20 ticks = 1 second

        combatTasks.put(playerId, task);

        // Notify player
        player.sendMessage(Component.text("You are now in combat!", NamedTextColor.RED));
    }

    /**
     * Removes a player from combat state
     *
     * @param player The player to remove from combat
     */
    public void exitCombat(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove combat timer
        combatTimers.remove(playerId);

        // Cancel combat task
        BukkitTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Clear scoreboard
        clearCombatDisplay(player);

        // Notify player
        player.sendMessage(Component.text("You are no longer in combat.", NamedTextColor.GREEN));
    }

    /**
     * Checks if a player is in combat
     *
     * @param player The player to check
     * @return True if the player is in combat, false otherwise
     */
    public boolean isInCombat(Player player) {
        return combatTimers.containsKey(player.getUniqueId());
    }

    /**
     * Gets the remaining combat time for a player
     *
     * @param player The player to check
     * @return The remaining combat time in seconds, or 0 if the player is not in combat
     */
    public int getRemainingCombatTime(Player player) {
        Long endTime = combatTimers.get(player.getUniqueId());
        if (endTime == null) {
            return 0;
        }

        long remainingMillis = endTime - System.currentTimeMillis();
        return (int) Math.max(0, remainingMillis / 1000);
    }

    /**
     * Updates the combat display for a player
     *
     * @param player The player to update the display for
     */
    private void updateCombatDisplay(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("combat");

        if (objective == null) {
            objective = scoreboard.registerNewObjective("combat", Criteria.DUMMY, Component.text("Combat Status", NamedTextColor.RED), RenderType.INTEGER);
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        Score score = objective.getScore(player.getName());
        score.setScore(getRemainingCombatTime(player));
    }

    /**
     * Clears the combat display for a player
     *
     * @param player The player to clear the display for
     */
    private void clearCombatDisplay(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("combat");

        if (objective != null) {
            objective.unregister();
        }
    }

    /**
     * Shuts down the combat manager
     */
    public void shutdown() {
        // Cancel all combat tasks
        for (BukkitTask task : combatTasks.values()) {
            task.cancel();
        }

        combatTasks.clear();
        combatTimers.clear();
    }
}
