package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TeleportTask {
    private final Player player;
    private final Location destination;
    private final int delay;
    private final Furious plugin;
    private BukkitTask countdownTask;
    private final Location startLocation;

    public TeleportTask(Player player, Location destination, int delay, Furious plugin) {
        this.player = player;
        this.destination = destination;
        this.delay = delay;
        this.plugin = plugin;
        this.startLocation = player.getLocation();
    }

    public void start() {
        player.sendMessage(Component.text("Don't move! Teleporting in " + delay + " seconds...",
                NamedTextColor.YELLOW));

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
                    plugin.getTeleportManager().cancelTeleportTask(player);
                    return;
                }

                if (secondsLeft <= 3 || secondsLeft == 5) {
                    player.sendMessage(Component.text("Teleporting in " + secondsLeft + "...",
                            NamedTextColor.YELLOW));
                }

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 second
    }

    private boolean hasPlayerMoved() {
        Location currentLoc = player.getLocation();
        return startLocation.getWorld() != currentLoc.getWorld() ||
                startLocation.getX() != currentLoc.getX() ||
                startLocation.getY() != currentLoc.getY() ||
                startLocation.getZ() != currentLoc.getZ();
    }

    private void complete() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        player.sendMessage(Component.text("Teleporting...", NamedTextColor.GREEN));
        player.teleport(destination);
    }

    public void cancel() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }
}