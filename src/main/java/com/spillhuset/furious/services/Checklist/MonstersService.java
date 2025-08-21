package com.spillhuset.furious.services.Checklist;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.ChecklistService;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks which hostile monsters each player has removed and global removals.
 * Also records simple firsts similarly to BiomesService.
 */
public class MonstersService extends ChecklistService {
    private final Map<UUID, Set<String>> removedByPlayer = new HashMap<>();
    private final Set<String> globalRemoved = new HashSet<>();
    private final Map<String, UUID> serverFirst = new HashMap<>();
    private final Map<Integer, Map<String, UUID>> yearFirstByYear = new HashMap<>();
    private final Map<String, UUID> monthFirst = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public MonstersService(Furious plugin) { super(plugin); }

    /**
     * Reads a reward amount for this checklist from its own YAML under rewards.amounts.<key>.
     * Falls back to global config.yml rewards.<key> and then to the provided default.
     */
    public double getRewardAmount(String key, double def) {
        try {
            double local = (config != null) ? config.getDouble("rewards.amounts." + key, Double.NaN) : Double.NaN;
            if (!Double.isNaN(local)) return local;
        } catch (Throwable ignored) {}
        try {
            return plugin.getConfig().getDouble("rewards." + key, def);
        } catch (Throwable ignored) {}
        return def;
    }

    public void load() {
        try {
            File folder = plugin.getDataFolder();
            if (!folder.exists()) folder.mkdirs();
            file = new File(folder, "monsters.yml");
            if (!file.exists()) file.createNewFile();
            config = YamlConfiguration.loadConfiguration(file);
            removedByPlayer.clear();
            globalRemoved.clear();
            serverFirst.clear();
            yearFirstByYear.clear();
            monthFirst.clear();
            completionRewarded.clear();
            if (config.isConfigurationSection("players")) {
                for (String key : Objects.requireNonNull(config.getConfigurationSection("players")).getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        List<String> list = config.getStringList("players." + key);
                        Set<String> set = new HashSet<>();
                        for (String s : list) {
                            if (s != null && !s.isEmpty()) set.add(s.toLowerCase(Locale.ROOT));
                        }
                        removedByPlayer.put(uuid, set);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            List<String> globalList = config.getStringList("global");
            if (!globalList.isEmpty()) {
                for (String s : globalList) if (s != null) globalRemoved.add(s.toLowerCase(Locale.ROOT));
            } else {
                for (Set<String> set : removedByPlayer.values()) globalRemoved.addAll(set);
            }
            // firsts
            if (config.isConfigurationSection("first.server")) {
                for (String k : Objects.requireNonNull(config.getConfigurationSection("first.server")).getKeys(false)) {
                    String uuidStr = config.getString("first.server." + k);
                    try { if (uuidStr != null) serverFirst.put(k.toLowerCase(Locale.ROOT), UUID.fromString(uuidStr)); } catch (IllegalArgumentException ignored) {}
                }
            }
            int y = config.getInt("first.year.current", 0);
            int m = config.getInt("first.month.currentMonth", 0);
            int my = config.getInt("first.month.currentYear", 0);

            if (config.isConfigurationSection("first.year.winners")) {
                for (String yearKey : Objects.requireNonNull(config.getConfigurationSection("first.year.winners")).getKeys(false)) {
                    try {
                        int yr = Integer.parseInt(yearKey);
                        Map<String, UUID> map = new HashMap<>();
                        for (String k : Objects.requireNonNull(config.getConfigurationSection("first.year.winners." + yearKey)).getKeys(false)) {
                            String uuidStr = config.getString("first.year.winners." + yearKey + "." + k);
                            try { if (uuidStr != null) map.put(k.toLowerCase(Locale.ROOT), UUID.fromString(uuidStr)); } catch (IllegalArgumentException ignored) {}
                        }
                        yearFirstByYear.put(yr, map);
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (my == currentYear && config.isConfigurationSection("first.month.winners")) {
                String ymKey = currentYear + "-" + String.format(java.util.Locale.ROOT, "%02d", currentMonth);
                if (config.isConfigurationSection("first.month.winners." + ymKey)) {
                    for (String k : Objects.requireNonNull(config.getConfigurationSection("first.month.winners." + ymKey)).getKeys(false)) {
                        String uuidStr = config.getString("first.month.winners." + ymKey + "." + k);
                        try { if (uuidStr != null) monthFirst.put(k.toLowerCase(Locale.ROOT), UUID.fromString(uuidStr)); } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
            ensurePeriodCurrent();
            // rewards
            loadRewards(config);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load monsters.yml: " + e.getMessage());
        }
    }

    public void save() {
        if (config == null) return;
        config.set("players", null);
        for (Map.Entry<UUID, Set<String>> entry : removedByPlayer.entrySet()) {
            config.set("players." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        config.set("global", new ArrayList<>(globalRemoved));
        // firsts
        config.set("first.server", null);
        for (Map.Entry<String, UUID> e : serverFirst.entrySet()) {
            config.set("first.server." + e.getKey(), e.getValue().toString());
        }
        config.set("first.year.current", currentYear);
        config.set("first.year.winners", null);
        for (Map.Entry<Integer, Map<String, UUID>> e : yearFirstByYear.entrySet()) {
            int yr = e.getKey();
            for (Map.Entry<String, UUID> me : e.getValue().entrySet()) {
                config.set("first.year.winners." + yr + "." + me.getKey(), me.getValue().toString());
            }
        }
        config.set("first.month.currentYear", currentYear);
        config.set("first.month.currentMonth", currentMonth);
        String ymKey = currentYear + "-" + String.format(java.util.Locale.ROOT, "%02d", currentMonth);
        config.set("first.month.winners." + ymKey, null);
        for (Map.Entry<String, UUID> e : monthFirst.entrySet()) {
            config.set("first.month.winners." + ymKey + "." + e.getKey(), e.getValue().toString());
        }
        // rewards persistence
        saveRewards(config);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save monsters.yml: " + e.getMessage());
        }
    }

    public record VisitResult(boolean firstForPlayer, boolean firstForServer) {}

    public VisitResult recordRemoval(UUID playerId, String monsterKey, boolean includeGlobal) {
        boolean firstForPlayer = false;
        boolean firstForServer = false;
        if (playerId == null || monsterKey == null) return new VisitResult(false, false);
        String key = monsterKey.toLowerCase(Locale.ROOT);
        ensurePeriodCurrent();
        Set<String> set = removedByPlayer.computeIfAbsent(playerId, k -> new HashSet<>());
        if (!set.contains(key)) {
            set.add(key);
            firstForPlayer = true;
        }
        if (includeGlobal) {
            if (!globalRemoved.contains(key)) {
                globalRemoved.add(key);
                firstForServer = true;
                serverFirst.putIfAbsent(key, playerId);
            }
            Map<String, UUID> yearMap = yearFirstByYear.computeIfAbsent(currentYear, k -> new HashMap<>());
            yearMap.putIfAbsent(key, playerId);
            monthFirst.putIfAbsent(key, playerId);
        }
        return new VisitResult(firstForPlayer, firstForServer);
    }

    public Set<String> getRemoved(UUID playerId) {
        return removedByPlayer.getOrDefault(playerId, Collections.emptySet());
    }

    public List<OfflinePlayer> resolveAllKnownPlayers() {
        if (!removedByPlayer.isEmpty()) {
            return removedByPlayer.keySet().stream().map(uuid -> plugin.getServer().getOfflinePlayer(uuid)).collect(Collectors.toList());
        }
        return Arrays.asList(plugin.getServer().getOfflinePlayers());
    }

    public void clearAllFirsts() {
        serverFirst.clear();
        yearFirstByYear.clear();
        monthFirst.clear();
        globalRemoved.clear();
        ensurePeriodCurrent();
        save();
    }

    @Override
    public void clearMonthFirst() {
        ensurePeriodCurrent();
        monthFirst.clear();
        save();
    }

    @Override
    public void clearYearFirst() {
        ensurePeriodCurrent();
        yearFirstByYear.put(currentYear, new HashMap<>());
        save();
    }

    @Override
    public java.util.UUID getServerFirst(String key) {
        if (key == null) return null;
        return serverFirst.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public java.util.UUID getYearFirst(String key) {
        if (key == null) return null;
        ensurePeriodCurrent();
        Map<String, java.util.UUID> map = yearFirstByYear.get(currentYear);
        if (map == null) return null;
        return map.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public java.util.UUID getMonthFirst(String key) {
        if (key == null) return null;
        ensurePeriodCurrent();
        return monthFirst.get(key.toLowerCase(java.util.Locale.ROOT));
    }
}
