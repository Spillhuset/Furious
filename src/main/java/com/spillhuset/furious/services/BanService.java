package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Simple custom ban service storing bans in plugins/Furious/bans.yml
 */
public class BanService {
    private final Furious plugin;
    private final Map<String, BanRecord> bansByName = new HashMap<>(); // key: lowercase name
    private File bansFile;
    private FileConfiguration cfg;

    public BanService(Furious plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            bansFile = new File(dataFolder, "bans.yml");
            if (!bansFile.exists()) bansFile.createNewFile();
            cfg = YamlConfiguration.loadConfiguration(bansFile);
            bansByName.clear();
            if (cfg.isConfigurationSection("bans")) {
                for (String key : Objects.requireNonNull(cfg.getConfigurationSection("bans")).getKeys(false)) {
                    String path = "bans." + key + ".";
                    String name = cfg.getString(path + "name", key);
                    String reason = cfg.getString(path + "reason", "Banned");
                    long expires = cfg.getLong(path + "expires", -1);
                    String source = cfg.getString(path + "source", "CONSOLE");
                    bansByName.put(key.toLowerCase(Locale.ROOT), new BanRecord(name, reason, expires <= 0 ? null : expires, source));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load bans.yml: " + e.getMessage());
        }
    }

    public void save() {
        if (cfg == null) return;
        cfg.set("bans", null);
        for (Map.Entry<String, BanRecord> e : bansByName.entrySet()) {
            String base = "bans." + e.getKey() + ".";
            BanRecord r = e.getValue();
            cfg.set(base + "name", r.name);
            cfg.set(base + "reason", r.reason);
            cfg.set(base + "expires", r.expiresAt == null ? -1 : r.expiresAt);
            cfg.set(base + "source", r.source);
        }
        try {
            cfg.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bans.yml: " + e.getMessage());
        }
    }

    public void banByName(String name, String reason, Long expiresAtMillis, String source) {
        if (name == null) return;
        String key = name.toLowerCase(Locale.ROOT);
        bansByName.put(key, new BanRecord(name, reason, expiresAtMillis, source));
        save();
    }

    public boolean unbanByName(String name) {
        if (name == null) return false;
        BanRecord removed = bansByName.remove(name.toLowerCase(Locale.ROOT));
        if (removed != null) save();
        return removed != null;
    }

    public boolean isBanned(String name) {
        BanRecord r = getBan(name);
        if (r == null) return false;
        if (r.expiresAt != null && r.expiresAt <= System.currentTimeMillis()) {
            // expired -> cleanup
            bansByName.remove(name.toLowerCase(Locale.ROOT));
            save();
            return false;
        }
        return true;
    }

    public BanRecord getBan(String name) {
        if (name == null) return null;
        return bansByName.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<BanRecord> list() { return Collections.unmodifiableCollection(bansByName.values()); }

    public static class BanRecord {
        public final String name;
        public final String reason;
        public final Long expiresAt; // null = permanent
        public final String source;
        public BanRecord(String name, String reason, Long expiresAt, String source) {
            this.name = name;
            this.reason = reason;
            this.expiresAt = expiresAt;
            this.source = source;
        }
    }
}
