package com.spillhuset.furious.misc;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TeleportTask {
    private final Player player;
    private final Location destination;
    private final int delay;
    private final Furious plugin;
    private BukkitTask countdownTask;
    private final Location startLocation;
    private final PotionEffect nauseaEffect;
    private final PotionEffect blindnessEffect;

    public TeleportTask(Player player, Location destination, int delay, Furious plugin) {
        this.player = player;
        this.destination = destination;
        this.delay = delay;
        this.plugin = plugin;
        this.startLocation = player.getLocation();
        // Create a nausea effect that lasts for the duration of the countdown plus 1 second
        this.nauseaEffect = new PotionEffect(PotionEffectType.NAUSEA, (delay + 5) * 20, 0);
        this.blindnessEffect = new PotionEffect(PotionEffectType.BLINDNESS,(delay+2)*20,3);
    }

    public void start() {
        player.sendMessage(Component.text("Don't move! Teleporting in " + delay + " seconds...",
                NamedTextColor.YELLOW));

        // Apply nausea effect
        player.addPotionEffect(nauseaEffect);
        player.addPotionEffect(blindnessEffect);

        // Display initial countdown in action bar
        updateActionBar(delay);

        countdownTask = new BukkitRunnable() {
            private int secondsLeft = delay;

            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    complete();
                    return;
                }

                // Check if player moved
                if (hasPlayerMoved()) {
                    cancel();
                    // We don't need to call cancelTeleportTask here as it would show the message twice
                    // The task is already being removed in the TeleportManager
                    player.sendMessage(Component.text("Teleport cancelled due to movement!", NamedTextColor.RED));
                    return;
                }

                // Update action bar with countdown timer
                updateActionBar(secondsLeft);

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 second
    }

    private boolean hasPlayerMoved() {
        Location currentLoc = player.getLocation();

        // Check if player changed worlds
        if (startLocation.getWorld() != currentLoc.getWorld()) {
            return true;
        }

        // Allow for small movements (0.2 blocks in any direction)
        double distanceSquared = startLocation.distanceSquared(currentLoc);
        return distanceSquared > 0.2 * 0.2; // 0.2 blocks squared
    }

    private void complete() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // Remove nausea effect
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        // Clear action bar
        clearActionBar();

        player.sendMessage(Component.text("Teleporting...", NamedTextColor.GREEN));
        player.teleport(destination);

        // Remove player from teleport tasks to reset their teleporting state
        // Use false to not show a message about cancellation
        plugin.getTeleportManager().cancelTeleportTask(player, false);
    }

    public void cancel() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        // Remove nausea effect
        player.removePotionEffect(PotionEffectType.NAUSEA);

        // Clear action bar
        clearActionBar();
    }

    /**
     * Updates the action bar with the current countdown time
     *
     * @param secondsLeft The number of seconds left in the countdown
     */
    private void updateActionBar(int secondsLeft) {
        player.sendActionBar(Component.text("Teleporting in " + secondsLeft + "s. Don't move!", NamedTextColor.YELLOW));
    }

    /**
     * Clears the action bar
     */
    private void clearActionBar() {
        player.sendActionBar(Component.empty());
    }
}
