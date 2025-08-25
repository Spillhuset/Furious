package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import org.bukkit.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class WeeklyWorldResetService {
    private final Furious plugin;
    private final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public WeeklyWorldResetService(Furious plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        // Ensure defaults exist
        addConfigDefaults();
        // Run check on startup after a short delay, then every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndMaybeReset, 20L * 15, 20L * 60 * 5);
    }

    private void addConfigDefaults() {
        var cfg = plugin.getConfig();
        if (!cfg.isConfigurationSection("worldreset")) {
            cfg.set("worldreset.enabled", true);
            cfg.set("worldreset.day-of-month", 1);
            cfg.set("worldreset.time", "04:00");
            cfg.set("worldreset.worlds", Arrays.asList("world_nether", "world_the_end"));
            cfg.set("worldreset.broadcast", true);
        } else {
            // Ensure missing keys get sane defaults
            if (!cfg.isSet("worldreset.enabled")) cfg.set("worldreset.enabled", true);
            if (!cfg.isSet("worldreset.day-of-month")) cfg.set("worldreset.day-of-month", 1);
            if (!cfg.isSet("worldreset.time")) cfg.set("worldreset.time", "04:00");
            if (!cfg.isSet("worldreset.worlds")) cfg.set("worldreset.worlds", Arrays.asList("world_nether", "world_the_end"));
            if (!cfg.isSet("worldreset.broadcast")) cfg.set("worldreset.broadcast", true);
        }
        plugin.saveConfig();
    }

    private void checkAndMaybeReset() {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("worldreset.enabled", true)) return;

        int dayOfMonth = cfg.getInt("worldreset.day-of-month", 1);
        String timeStr = cfg.getString("worldreset.time", "04:00");
        List<String> worlds = cfg.getStringList("worldreset.worlds");
        boolean broadcast = cfg.getBoolean("worldreset.broadcast", true);

        LocalTime targetTime;
        try { targetTime = LocalTime.parse(timeStr); } catch (Exception e) { targetTime = LocalTime.of(4, 0); }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);

        String lastRunStr = cfg.getString("worldreset.last-run", null);
        ZonedDateTime lastRun = null;
        if (lastRunStr != null) {
            try { lastRun = LocalDateTime.parse(lastRunStr, ISO).atZone(zone); } catch (Exception ignored) {}
        }

        ZonedDateTime scheduled = anchorThisMonth(dayOfMonth, targetTime, now);
        // If we've already passed this month's scheduled time, and lastRun is before it (or null), we should run now.
        boolean due = now.isAfter(scheduled) && (lastRun == null || lastRun.isBefore(scheduled));

        if (due) {
            if (broadcast) Bukkit.broadcast(Component.text("Server is resetting Nether/End worlds now... Expect brief lag.").color(NamedTextColor.YELLOW));
            try {
                resetWorlds(worlds);
                // mark last run
                cfg.set("worldreset.last-run", LocalDateTime.now().format(ISO));
                plugin.saveConfig();
                if (broadcast) Bukkit.broadcast(Component.text("Nether/End reset complete. New seeds are live.").color(NamedTextColor.GREEN));
            } catch (Exception ex) {
                plugin.getLogger().warning("Monthly world reset failed: " + ex.getMessage());
                ex.printStackTrace();
                if (broadcast) Bukkit.broadcast(Component.text("Nether/End reset failed. Check console logs.").color(NamedTextColor.RED));
            }
        }
    }

    private ZonedDateTime nextOrSameThisWeek(DayOfWeek targetDay, LocalTime targetTime, ZonedDateTime ref) {
        ZonedDateTime candidate = ref.with(TemporalAdjusters.nextOrSame(targetDay)).withHour(targetTime.getHour()).withMinute(targetTime.getMinute()).withSecond(0).withNano(0);
        // If candidate is in the future but not this week (when today before target day), that's fine. If today is target and time later, it's today. If already past today+time, nextOrSame gave next week; we want this week's passed time:
        if (ref.getDayOfWeek() == targetDay) {
            ZonedDateTime todayAt = ref.withHour(targetTime.getHour()).withMinute(targetTime.getMinute()).withSecond(0).withNano(0);
            if (ref.isAfter(todayAt)) {
                // already past today time; we want today's time (passed), not next week
                candidate = todayAt;
            }
        }
        return candidate;
    }

    private ZonedDateTime anchorThisMonth(int dayOfMonth, LocalTime targetTime, ZonedDateTime ref) {
        int maxDom = ref.toLocalDate().lengthOfMonth();
        int dom = Math.max(1, Math.min(dayOfMonth, maxDom));
        return ref.withDayOfMonth(dom)
                .withHour(targetTime.getHour())
                .withMinute(targetTime.getMinute())
                .withSecond(0)
                .withNano(0);
    }

    private void resetWorlds(List<String> worldNames) {
        if (worldNames == null || worldNames.isEmpty()) return;
        World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        Location fallbackSpawn = fallback != null ? fallback.getSpawnLocation() : new Location(Bukkit.getWorld("world"), 0.5, 64, 0.5);

        for (String name : worldNames) {
            if (name == null || name.isBlank()) continue;
            World w = Bukkit.getWorld(name);
            if (w != null) {
                // Move players out
                for (Player p : w.getPlayers()) {
                    if (fallbackSpawn != null) p.teleport(fallbackSpawn);
                }
            }
            // Unload if loaded
            if (w != null) Bukkit.unloadWorld(w, false);
            // Delete folder
            deleteWorldFolder(name);
            // Recreate
            createFreshWorld(name, new Random().nextLong());
        }
    }

    private void deleteWorldFolder(String worldName) {
        File container = Bukkit.getWorldContainer();
        File folder = new File(container, worldName);
        if (!folder.exists()) return;
        // Ensure nothing holds files: small wait is sometimes helpful
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        deleteRecursively(folder);
    }

    private void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null) {
                for (File c : list) deleteRecursively(c);
            }
        }
        if (!f.delete()) {
            // Try mark for deletion on exit as fallback
            f.deleteOnExit();
        }
    }

    private void createFreshWorld(String worldName, long seed) {
        // Determine environment by conventional name
        World.Environment env = World.Environment.NORMAL;
        if ("world_nether".equalsIgnoreCase(worldName) || worldName.toLowerCase(Locale.ROOT).contains("nether")) {
            env = World.Environment.NETHER;
        } else if ("world_the_end".equalsIgnoreCase(worldName) || worldName.toLowerCase(Locale.ROOT).contains("end")) {
            env = World.Environment.THE_END;
        }
        WorldCreator creator = new WorldCreator(worldName)
                .environment(env)
                .seed(seed)
                .generateStructures(true);
        Bukkit.createWorld(creator);
    }
}
