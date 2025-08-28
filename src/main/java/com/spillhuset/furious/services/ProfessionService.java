package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Tracks player professions (primary/secondary) and skillpoints per profession.
 */
public class ProfessionService {
    public enum Profession { MINER, LUMBERJACK, FARMER, FISHER, BUTCHER; }

    private final Furious plugin;

    private final Map<UUID, Profession> primary = new HashMap<>();
    private final Map<UUID, Profession> secondary = new HashMap<>();
    private final Map<UUID, EnumMap<Profession, Integer>> points = new HashMap<>();

    private File file;
    private FileConfiguration config;

    public ProfessionService(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    public void load() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists()) folder.mkdirs();
            file = new File(folder, "professions.yml");
            if (!file.exists()) file.createNewFile();
            config = YamlConfiguration.loadConfiguration(file);
            primary.clear();
            secondary.clear();
            points.clear();
            if (config.isConfigurationSection("players")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("players")).getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String p = config.getString("players."+key+".primary");
                        String s = config.getString("players."+key+".secondary");
                        if (p != null && !p.isEmpty()) {
                            try { primary.put(uuid, Profession.valueOf(p)); } catch (IllegalArgumentException ignored) {}
                        }
                        if (s != null && !s.isEmpty()) {
                            try { secondary.put(uuid, Profession.valueOf(s)); } catch (IllegalArgumentException ignored) {}
                        }
                        EnumMap<Profession, Integer> map = new EnumMap<>(Profession.class);
                        for (Profession prof : Profession.values()) {
                            int val = config.getInt("players."+key+".points."+prof.name(), 0);
                            map.put(prof, val);
                        }
                        points.put(uuid, map);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load professions.yml: "+e.getMessage());
        }
    }

    public void save() {
        if (config == null) return;
        config.set("players", null);
        for (UUID uuid : unionKeys()) {
            Profession p = primary.get(uuid);
            Profession s = secondary.get(uuid);
            if (p != null) config.set("players."+uuid+".primary", p.name());
            if (s != null) config.set("players."+uuid+".secondary", s.name());
            EnumMap<Profession, Integer> map = points.get(uuid);
            if (map != null) {
                for (Profession prof : Profession.values()) {
                    config.set("players."+uuid+".points."+prof.name(), map.getOrDefault(prof, 0));
                }
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save professions.yml: "+e.getMessage());
        }
    }

    private Set<UUID> unionKeys() {
        Set<UUID> set = new HashSet<>();
        set.addAll(primary.keySet());
        set.addAll(secondary.keySet());
        set.addAll(points.keySet());
        return set;
    }

    public Profession getPrimary(UUID player) { return primary.get(player); }
    public Profession getSecondary(UUID player) { return secondary.get(player); }

    public boolean setPrimary(UUID player, Profession prof) {
        if (player == null || prof == null) return false;
        primary.put(player, prof);
        ensureMap(player);
        save();
        return true;
    }

    public boolean setSecondary(UUID player, Profession prof) {
        if (player == null || prof == null) return false;
        secondary.put(player, prof);
        ensureMap(player);
        save();
        return true;
    }

    public int getPoints(UUID player, Profession prof) {
        return ensureMap(player).getOrDefault(prof, 0);
    }

    public void addPoints(UUID player, Profession prof, int amount) {
        if (player == null || prof == null || amount == 0) return;
        EnumMap<Profession, Integer> map = ensureMap(player);
        int cur = map.getOrDefault(prof, 0);
        map.put(prof, Math.max(0, cur + amount));
        // Persist lazily to avoid heavy IO: small throttle via wallet flush interval
        // For simplicity here, we save immediately to be safe
        save();
    }

    private EnumMap<Profession, Integer> ensureMap(UUID player) {
        return points.computeIfAbsent(player, k -> {
            EnumMap<Profession, Integer> m = new EnumMap<>(Profession.class);
            for (Profession prof : Profession.values()) m.put(prof, 0);
            return m;
        });
    }

    // --- Config helpers ---
    public int ptsBase(Profession prof) {
        String key = "professions.points.base."+prof.name().toLowerCase(Locale.ROOT);
        return plugin.getConfig().getInt(key, 1);
    }
    public int ptsBonus(Profession prof, String bonusKey) {
        String key = "professions.points.bonus."+prof.name().toLowerCase(Locale.ROOT)+"."+bonusKey;
        return plugin.getConfig().getInt(key, 2);
    }
}
