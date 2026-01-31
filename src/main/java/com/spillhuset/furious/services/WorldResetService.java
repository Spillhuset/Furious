package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class WorldResetService {
    private final Furious plugin;

    public WorldResetService(Furious plugin) {
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
            cfg.set("worldreset.broadcast", true);
            
            // Default worlds schedule
            ConfigurationSection worldsSec = cfg.createSection("worldreset.worlds");
            
            ConfigurationSection nether = worldsSec.createSection("world_nether");
            nether.set("day-of-month", 1);
            nether.set("time", "04:00");
            
            ConfigurationSection end = worldsSec.createSection("world_the_end");
            end.set("day-of-month", 1);
            end.set("time", "04:00");
        } else {
            // Ensure missing global keys
            if (!cfg.isSet("worldreset.enabled")) cfg.set("worldreset.enabled", true);
            if (!cfg.isSet("worldreset.broadcast")) cfg.set("worldreset.broadcast", true);

            // Migration from old flat list to individual configs
            if (cfg.isList("worldreset.worlds")) {
                List<String> worldNames = cfg.getStringList("worldreset.worlds");
                int oldDay = cfg.getInt("worldreset.day-of-month", 1);
                String oldTime = cfg.getString("worldreset.time", "04:00");

                cfg.set("worldreset.worlds", null); // Clear old list
                ConfigurationSection worldsSec = cfg.createSection("worldreset.worlds");
                for (String name : worldNames) {
                    ConfigurationSection wSec = worldsSec.createSection(name);
                    wSec.set("day-of-month", oldDay);
                    wSec.set("time", oldTime);
                }
                // Cleanup old global keys that are now per-world
                cfg.set("worldreset.day-of-month", null);
                cfg.set("worldreset.time", null);
                cfg.set("worldreset.day-of-week", null);
            }
        }
        
        // Cleanup old global last-run if it exists (though we move to per-world)
        if (cfg.isSet("worldreset.last-run")) {
            cfg.set("worldreset.last-run", null);
        }
        
        plugin.saveConfig();
    }

    private void checkAndMaybeReset() {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("worldreset.enabled", true)) return;

        ConfigurationSection worldsSec = cfg.getConfigurationSection("worldreset.worlds");
        if (worldsSec == null) return;

        boolean globalBroadcast = cfg.getBoolean("worldreset.broadcast", true);
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);

        for (String worldName : worldsSec.getKeys(false)) {
            ConfigurationSection wCfg = worldsSec.getConfigurationSection(worldName);
            if (wCfg == null) continue;

            int dayOfMonth = wCfg.getInt("day-of-month", 1);
            String timeStr = wCfg.getString("time", "04:00");
            long lastRunMillis = wCfg.getLong("last-run", 0);

            LocalTime targetTime;
            try { targetTime = LocalTime.parse(timeStr); } catch (Exception e) { targetTime = LocalTime.of(4, 0); }

            ZonedDateTime lastRun = lastRunMillis > 0
                    ? Instant.ofEpochMilli(lastRunMillis).atZone(zone)
                    : null;

            ZonedDateTime scheduled = anchorThisMonth(dayOfMonth, targetTime, now);
            
            // Check if due
            if (now.isAfter(scheduled) && (lastRun == null || lastRun.isBefore(scheduled))) {
                performReset(worldName, globalBroadcast);
                // Mark last run for this specific world
                wCfg.set("last-run", System.currentTimeMillis());
                plugin.saveConfig();
            }
        }
    }

    private void performReset(String worldName, boolean broadcast) {
        if (broadcast) {
            Bukkit.broadcast(Component.text("Server is resetting world '" + worldName + "' now... Expect brief lag.").color(NamedTextColor.YELLOW));
        }

        try {
            // Compute fallback spawn defensively
            Location fallbackSpawn = null;
            World fallbackWorld = Bukkit.getWorld("world");
            if (fallbackWorld == null && !Bukkit.getWorlds().isEmpty()) {
                fallbackWorld = Bukkit.getWorlds().getFirst();
            }
            if (fallbackWorld != null) {
                fallbackSpawn = fallbackWorld.getSpawnLocation();
            }

            World w = Bukkit.getWorld(worldName);
            if (w != null) {
                // Move players out
                if (fallbackSpawn != null) {
                    for (Player p : w.getPlayers()) {
                        p.teleport(fallbackSpawn);
                    }
                }
                // Unload
                Bukkit.unloadWorld(w, false);
            }

            // Async deletion followed by sync recreation
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                deleteWorldFolder(worldName);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    createFreshWorld(worldName, new Random().nextLong());
                    plugin.getLogger().info("World reset complete for: " + worldName);
                    if (broadcast) {
                        Bukkit.broadcast(Component.text("World reset complete for " + worldName + ". New seeds are live.").color(NamedTextColor.GREEN));
                    }
                });
            });
        } catch (Exception ex) {
            plugin.getLogger().warning("World reset failed for " + worldName + ": " + ex.getMessage());
            ex.printStackTrace();
            if (broadcast) {
                Bukkit.broadcast(Component.text("World reset failed for " + worldName + ". Check console logs.").color(NamedTextColor.RED));
            }
        }
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

    private void deleteWorldFolder(String worldName) {
        File container = Bukkit.getWorldContainer();
        File folder = new File(container, worldName);
        if (!folder.exists()) return;
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
