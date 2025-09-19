package com.spillhuset.furious.commands.HomesCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.db.HomesRepository;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Home;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MigrateCommand implements SubCommandInterface {
    private final Furious plugin;

    public MigrateCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            for (String s : Arrays.asList("yaml","database")) if (s.startsWith(args[1].toLowerCase())) list.add(s);
        } else if (args.length == 3) {
            String from = args[1].toLowerCase();
            if (from.equals("yaml")) {
                if ("database".startsWith(args[2].toLowerCase())) list.add("database");
            } else if (from.equals("database")) {
                if ("yaml".startsWith(args[2].toLowerCase())) list.add("yaml");
            } else {
                for (String s : Arrays.asList("yaml","database")) if (s.startsWith(args[2].toLowerCase())) list.add(s);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /homes migrate <yaml|database> <yaml|database>");
            return true;
        }
        String from = args[1].toLowerCase();
        String to = args[2].toLowerCase();
        if (from.equals(to)) {
            Components.sendErrorMessage(sender, "Source and target are the same: " + from);
            return true;
        }
        try {
            if (from.equals("yaml") && to.equals("database")) {
                migrateYamlToDb();
                setDatabaseEnabled(true);
                Components.sendSuccessMessage(sender, "Migrated homes from YAML to Database and switched storage to database.");
            } else if (from.equals("database") && to.equals("yaml")) {
                migrateDbToYaml();
                setDatabaseEnabled(false);
                Components.sendSuccessMessage(sender, "Migrated homes from Database to YAML and switched storage to yaml.");
            } else {
                Components.sendInfoMessage(sender, "Usage: /homes migrate <yaml|database> <yaml|database>");
            }
        } catch (Exception ex) {
            Components.sendErrorMessage(sender, "Migration failed: " + ex.getMessage());
        }
        return true;
    }

    private void migrateYamlToDb() throws Exception {
        if (plugin.databaseManager == null || !plugin.databaseManager.isEnabled()) {
            throw new IllegalStateException("Database is not enabled in config; configure database section first.");
        }
        // Load YAML
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File homesFile = new File(dataFolder, "homes.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(homesFile);
        Map<UUID, Set<UUID>> players = new HashMap<>();
        Map<UUID, Home> homes = new HashMap<>();
        Map<Location, UUID> locations = new HashMap<>();
        Map<UUID, Integer> purchased = new HashMap<>();
        ConfigurationSection root = cfg.getConfigurationSection("players");
        if (root != null) {
            for (String playerStr : root.getKeys(false)) {
                UUID pid = UUID.fromString(playerStr);
                ConfigurationSection psec = root.getConfigurationSection(playerStr);
                if (psec == null) continue;
                purchased.put(pid, psec.getInt("purchased", 0));
                ConfigurationSection hsec = psec.getConfigurationSection("homes");
                if (hsec != null) {
                    for (String homeIdStr : hsec.getKeys(false)) {
                        ConfigurationSection hs = hsec.getConfigurationSection(homeIdStr);
                        if (hs == null) continue;
                        try {
                            UUID id = UUID.fromString(homeIdStr);
                            UUID world = UUID.fromString(Objects.requireNonNull(hs.getString("world")));
                            String name = hs.getString("name");
                            double x = hs.getDouble("x");
                            double y = hs.getDouble("y");
                            double z = hs.getDouble("z");
                            float yaw = (float) hs.getDouble("yaw");
                            float pitch = (float) hs.getDouble("pitch");
                            String armor = hs.getString("armorStand", null);
                            Location loc = new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
                            Home home = new Home(id, name, loc, pid);
                            if (armor != null && !armor.isBlank()) {
                                try { home.setArmorStandUuid(UUID.fromString(armor)); } catch (Exception ignored) {}
                            }
                            players.computeIfAbsent(pid, k -> new HashSet<>()).add(id);
                            homes.put(id, home);
                            locations.put(loc, id);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        HomesRepository repo = new HomesRepository(plugin.databaseManager.getDataSource(), plugin.getServer());
        repo.initSchema();
        repo.saveAll(players, homes, purchased);
    }

    private void migrateDbToYaml() throws Exception {
        if (plugin.databaseManager == null || !plugin.databaseManager.isEnabled()) {
            throw new IllegalStateException("Database must be enabled to read from it.");
        }
        HomesRepository repo = new HomesRepository(plugin.databaseManager.getDataSource(), plugin.getServer());
        repo.initSchema();
        Map<UUID, Set<UUID>> players = new HashMap<>();
        Map<UUID, Home> homes = new HashMap<>();
        Map<Location, UUID> locations = new HashMap<>();
        Map<UUID, Integer> purchased = new HashMap<>();
        repo.loadAll(players, homes, locations, purchased);
        // Write YAML
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File homesFile = new File(dataFolder, "homes.yml");
        FileConfiguration out = new YamlConfiguration();
        ConfigurationSection root = out.createSection("players");
        for (Map.Entry<UUID, Set<UUID>> pe : players.entrySet()) {
            UUID pid = pe.getKey();
            ConfigurationSection psec = root.createSection(pid.toString());
            psec.set("purchased", purchased.getOrDefault(pid, 0));
            ConfigurationSection hsec = psec.createSection("homes");
            for (UUID id : pe.getValue()) {
                Home h = homes.get(id);
                if (h == null) continue;
                ConfigurationSection hs = hsec.createSection(id.toString());
                hs.set("world", h.getWorld().toString());
                hs.set("name", h.getName());
                hs.set("x", h.getX());
                hs.set("y", h.getY());
                hs.set("z", h.getZ());
                hs.set("yaw", h.getYaw());
                hs.set("pitch", h.getPitch());
                if (h.getArmorStandUuid() != null) hs.set("armorStand", h.getArmorStandUuid().toString());
            }
        }
        try {
            out.save(homesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDatabaseEnabled(boolean enabled) throws IOException {
        File cfgFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        org.bukkit.configuration.ConfigurationSection db = cfg.getConfigurationSection("database");
        if (db == null) db = cfg.createSection("database");
        db.set("enabled", enabled ? "enable" : "disable");
        cfg.save(cfgFile);
        plugin.reloadConfig();
        if (plugin.databaseManager != null) {
            try { plugin.databaseManager.shutdown(); } catch (Throwable ignored) {}
            plugin.databaseManager.init();
        }
        // Reload homes into the new mode
        plugin.homesService.load();
    }

    @Override
    public String getName() { return "migrate"; }

    @Override
    public String getPermission() { return "furious.homes.migrate"; }
}
