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
 * Tracks which biomes each player has visited and globally discovered biomes.
 */
public class BiomesService extends ChecklistService {
    private final Map<UUID, Set<String>> visitedByPlayer = new HashMap<>();
    private final Set<String> globalDiscovered = new HashSet<>();
    // First discoverers
    private final Map<String, UUID> serverFirst = new HashMap<>();
    private final Map<Integer, Map<String, UUID>> yearFirstByYear = new HashMap<>();
    private final Map<String, UUID> monthFirst = new HashMap<>(); // keyed by YYYY-MM -> (biomeKey -> uuid) stored differently
    private File file;
    private FileConfiguration config;

    private String canonicalize(String biomeName) {
        if (biomeName == null) return null;
        String s = biomeName.trim();
        if (s.isEmpty()) return null;
        // If it's already a namespaced key like "minecraft:plains", use lower-case as canonical
        if (s.contains(":")) {
            return s.toLowerCase(Locale.ROOT);
        }
        // Otherwise, try to map legacy enum constant or registry entry to namespaced key
        try {
            org.bukkit.block.Biome[] arr = org.bukkit.block.Biome.class.getEnumConstants();
            if (arr != null) {
                for (org.bukkit.block.Biome b : arr) {
                    if (b.toString().equalsIgnoreCase(s)) {
                        return b.key().asString().toLowerCase(Locale.ROOT);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback to lower-case of provided string
        return s.toLowerCase(Locale.ROOT);
    }

    public BiomesService(Furious plugin) {
        super(plugin);
    }

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
            file = new File(folder, "biomes.yml");
            if (!file.exists()) {
                file.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(file);
            visitedByPlayer.clear();
            globalDiscovered.clear();
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
                        for (String biome : list) {
                            String canon = canonicalize(biome);
                            if (canon != null) set.add(canon);
                        }
                        visitedByPlayer.put(uuid, set);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            // Load global discovered if present; otherwise compute union of visited
            List<String> globalList = config.getStringList("global");
            if (!globalList.isEmpty()) {
                for (String s : globalList) {
                    String canon = canonicalize(s);
                    if (canon != null) globalDiscovered.add(canon);
                }
            } else {
                for (Set<String> set : visitedByPlayer.values()) {
                    globalDiscovered.addAll(set);
                }
            }
            // Load firsts
            if (config.isConfigurationSection("first.server")) {
                for (String biome : Objects.requireNonNull(config.getConfigurationSection("first.server")).getKeys(false)) {
                    String canon = canonicalize(biome);
                    String uuidStr = config.getString("first.server." + biome);
                    try {
                        if (canon != null && uuidStr != null) serverFirst.put(canon, UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {
                    }
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
                        for (String biome : Objects.requireNonNull(config.getConfigurationSection("first.year.winners." + yearKey)).getKeys(false)) {
                            String uuidStr = config.getString("first.year.winners." + yearKey + "." + biome);
                            String canon = canonicalize(biome);
                            try {
                                if (uuidStr != null && canon != null) map.put(canon, UUID.fromString(uuidStr));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        yearFirstByYear.put(yr, map);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            // For month, only current month should be active; historical not required by spec
            if (my == currentYear && config.isConfigurationSection("first.month.winners")) {
                // We may store at key like YYYY-MM
                String ymKey = currentYear + "-" + String.format(java.util.Locale.ROOT, "%02d", currentMonth);
                if (config.isConfigurationSection("first.month.winners." + ymKey)) {
                    for (String biome : Objects.requireNonNull(config.getConfigurationSection("first.month.winners." + ymKey)).getKeys(false)) {
                        String uuidStr = config.getString("first.month.winners." + ymKey + "." + biome);
                        String canon = canonicalize(biome);
                        try {
                            if (uuidStr != null && canon != null) monthFirst.put(canon, UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            ensurePeriodCurrent();
            // Load completion rewarded list via base helper
            loadRewards(config);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load biomes.yml: " + e.getMessage());
        }
    }

    public void save() {
        if (config == null) return;
        config.set("players", null);
        for (Map.Entry<UUID, Set<String>> entry : visitedByPlayer.entrySet()) {
            config.set("players." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        config.set("global", new ArrayList<>(globalDiscovered));
        // Save firsts
        config.set("first.server", null);
        for (Map.Entry<String, UUID> e : serverFirst.entrySet()) {
            config.set("first.server." + e.getKey(), e.getValue().toString());
        }
        config.set("first.year.current", currentYear);
        config.set("first.year.winners", null);
        for (Map.Entry<Integer, Map<String, UUID>> e : yearFirstByYear.entrySet()) {
            int yr = e.getKey();
            for (Map.Entry<String, UUID> bi : e.getValue().entrySet()) {
                config.set("first.year.winners." + yr + "." + bi.getKey(), bi.getValue().toString());
            }
        }
        config.set("first.month.currentYear", currentYear);
        config.set("first.month.currentMonth", currentMonth);
        String ymKey = currentYear + "-" + String.format(java.util.Locale.ROOT, "%02d", currentMonth);
        config.set("first.month.winners." + ymKey, null);
        for (Map.Entry<String, UUID> e : monthFirst.entrySet()) {
            config.set("first.month.winners." + ymKey + "." + e.getKey(), e.getValue().toString());
        }
        // Save completion rewarded list via base helper
        saveRewards(config);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save biomes.yml: " + e.getMessage());
        }
    }

    /**
     * Legacy: record visit without exposing whether it was first-time.
     */
    public void markVisited(UUID playerId, String biomeName) {
        visit(playerId, biomeName);
    }

    /**
     * Records a visit and returns flags indicating first-time for player/server.
     */
    public VisitResult visit(UUID playerId, String biomeName) {
        return visit(playerId, biomeName, true);
    }

    /**
     * Records a visit and returns flags indicating first-time for player/server.
     *
     * @param includeGlobal if false, do not update global discovery and never flag firstForServer
     */
    public VisitResult visit(UUID playerId, String biomeName, boolean includeGlobal) {
        boolean firstForPlayer = false;
        boolean firstForServer = false;
        if (playerId == null || biomeName == null) return new VisitResult(false, false);
        String key = canonicalize(biomeName);
        if (key == null) return new VisitResult(false, false);
        ensurePeriodCurrent();
        Set<String> set = visitedByPlayer.computeIfAbsent(playerId, k -> new HashSet<>());
        if (!set.contains(key)) {
            set.add(key);
            firstForPlayer = true;
        }
        if (includeGlobal) {
            if (!globalDiscovered.contains(key)) {
                globalDiscovered.add(key);
                firstForServer = true;
                // server-first winner
                serverFirst.putIfAbsent(key, playerId);
            }
        }
        if (includeGlobal) {
            // year-first winner (for current year)
            Map<String, UUID> yearMap = yearFirstByYear.computeIfAbsent(currentYear, k -> new HashMap<>());
            yearMap.putIfAbsent(key, playerId);
            // month-first winner (only for current month)
            monthFirst.putIfAbsent(key, playerId);
        }
        return new VisitResult(firstForPlayer, firstForServer);
    }

    public boolean hasVisited(UUID playerId, String biomeName) {
        if (playerId == null || biomeName == null) return false;
        Set<String> set = visitedByPlayer.get(playerId);
        if (set == null) return false;
        String key = canonicalize(biomeName);
        if (key == null) return false;
        return set.contains(key);
    }

    public Set<String> getVisited(UUID playerId) {
        return visitedByPlayer.getOrDefault(playerId, Collections.emptySet());
    }

    public Set<UUID> getAllTrackedPlayers() {
        return Collections.unmodifiableSet(visitedByPlayer.keySet());
    }

    public List<OfflinePlayer> resolveAllKnownPlayers() {
        // Prefer tracked players; if empty, fall back to server list
        if (!visitedByPlayer.isEmpty()) {
            return visitedByPlayer.keySet().stream()
                    .map(uuid -> plugin.getServer().getOfflinePlayer(uuid))
                    .collect(Collectors.toList());
        }
        return Arrays.asList(plugin.getServer().getOfflinePlayers());
    }

    public record VisitResult(boolean firstForPlayer, boolean firstForServer) {
    }

    public void clearAllFirsts() {
        serverFirst.clear();
        yearFirstByYear.clear();
        monthFirst.clear();
        globalDiscovered.clear();
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
        String canon = canonicalize(key);
        if (canon == null) return null;
        return serverFirst.get(canon);
    }

    @Override
    public java.util.UUID getYearFirst(String key) {
        String canon = canonicalize(key);
        if (canon == null) return null;
        ensurePeriodCurrent();
        Map<String, java.util.UUID> map = yearFirstByYear.get(currentYear);
        if (map == null) return null;
        return map.get(canon);
    }

    @Override
    public java.util.UUID getMonthFirst(String key) {
        String canon = canonicalize(key);
        if (canon == null) return null;
        ensurePeriodCurrent();
        return monthFirst.get(canon);
    }
}
