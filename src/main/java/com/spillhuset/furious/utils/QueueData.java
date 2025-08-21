package com.spillhuset.furious.utils;

import org.bukkit.Location;
import org.bukkit.boss.BossBar;

import java.util.UUID;

/**
 * Data holder for a queued teleport.
 * Extracted from TeleportsService to be reusable and cleaner.
 */
public class QueueData {
    public final UUID playerId;
    public final Location target;
    public final String label;
    public final long startTick;
    public final int durationSeconds;
    public final BossBar bossBar;
    public final int taskId;
    public final Location startBlockLoc;

    public QueueData(UUID playerId,
                     Location target,
                     String label,
                     long startTick,
                     int durationSeconds,
                     BossBar bossBar,
                     int taskId,
                     Location startBlockLoc) {
        this.playerId = playerId;
        this.target = target;
        this.label = label;
        this.startTick = startTick;
        this.durationSeconds = durationSeconds;
        this.bossBar = bossBar;
        this.taskId = taskId;
        this.startBlockLoc = startBlockLoc;
    }
}
