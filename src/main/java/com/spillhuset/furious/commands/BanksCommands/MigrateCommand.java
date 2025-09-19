package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.db.BanksRepository;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.BankAccount;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
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
            Components.sendInfoMessage(sender, "Usage: /banks migrate <yaml|database> <yaml|database>");
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
                Components.sendSuccessMessage(sender, "Migrated banks from YAML to Database and switched storage to database.");
            } else if (from.equals("database") && to.equals("yaml")) {
                migrateDbToYaml();
                setDatabaseEnabled(false);
                Components.sendSuccessMessage(sender, "Migrated banks from Database to YAML and switched storage to yaml.");
            } else {
                Components.sendInfoMessage(sender, "Usage: /banks migrate <yaml|database> <yaml|database>");
            }
        } catch (Exception ex) {
            Components.sendErrorMessage(sender, "Migration failed: " + ex.getMessage());
        }
        return true;
    }

    private void migrateYamlToDb() throws Exception {
        // Need database ready for writing
        if (plugin.databaseManager == null || !plugin.databaseManager.isEnabled()) {
            throw new IllegalStateException("Database is not enabled in config; configure database section first.");
        }
        // Load YAML from file directly
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File banksFile = new File(dataFolder, "banks.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(banksFile);
        Map<UUID, Bank> banksById = new HashMap<>();
        Map<UUID, Long> lastAccrual = new HashMap<>();
        // banks
        ConfigurationSection banksSec = cfg.getConfigurationSection("banks");
        if (banksSec != null) {
            for (String idStr : banksSec.getKeys(false)) {
                UUID id = UUID.fromString(idStr);
                ConfigurationSection b = banksSec.getConfigurationSection(idStr);
                if (b == null) continue;
                String name = b.getString("name");
                Bank bank = new Bank(id, name);
                bank.setInterest(b.getDouble("interest", 0.0d));
                String typeStr = b.getString("type", "PLAYER");
                try { bank.setType(com.spillhuset.furious.utils.BankType.valueOf(typeStr.toUpperCase())); } catch (IllegalArgumentException ignored) {}
                bank.setOpen(b.getBoolean("open", true));
                // claims
                ConfigurationSection claimsSec = b.getConfigurationSection("claims");
                if (claimsSec != null) {
                    for (String key : claimsSec.getKeys(false)) {
                        ConfigurationSection c = claimsSec.getConfigurationSection(key);
                        if (c == null) continue;
                        try {
                            UUID wid = UUID.fromString(c.getString("world"));
                            int cx = c.getInt("chunkX");
                            int cz = c.getInt("chunkZ");
                            bank.addClaim(wid, cx, cz);
                        } catch (Exception ignored) {}
                    }
                } else {
                    String worldStr = b.getString("world");
                    if (worldStr != null && !worldStr.isBlank()) {
                        try {
                            UUID wid = UUID.fromString(worldStr);
                            int cx = b.getInt("chunkX");
                            int cz = b.getInt("chunkZ");
                            bank.claim(wid, cx, cz);
                        } catch (Exception ignored) {}
                    }
                }
                String armor = b.getString("armorStand");
                if (armor != null && !armor.isBlank()) {
                    try { bank.setArmorStandUuid(UUID.fromString(armor)); } catch (Exception ignored) {}
                }
                long last = b.getLong("lastAccrual", 0L);
                if (last > 0) lastAccrual.put(id, last);
                banksById.put(id, bank);
            }
        }
        // accounts
        Map<UUID, Map<UUID, BankAccount>> accounts = new HashMap<>();
        ConfigurationSection accSec = cfg.getConfigurationSection("accounts");
        if (accSec != null) {
            for (String playerStr : accSec.getKeys(false)) {
                UUID pid = UUID.fromString(playerStr);
                ConfigurationSection psec = accSec.getConfigurationSection(playerStr);
                if (psec == null) continue;
                Map<UUID, BankAccount> map = new HashMap<>();
                for (String bankIdStr : psec.getKeys(false)) {
                    UUID bid = UUID.fromString(bankIdStr);
                    double bal = psec.getDouble(bankIdStr, 0.0d);
                    BankAccount acct = new BankAccount(bid, pid);
                    acct.setBalance(bal);
                    map.put(bid, acct);
                }
                accounts.put(pid, map);
            }
        }
        BanksRepository repo = new BanksRepository(plugin.databaseManager.getDataSource());
        repo.initSchema();
        repo.saveAll(banksById.values(), accounts, lastAccrual);
    }

    private void migrateDbToYaml() throws Exception {
        // Ensure DB enabled to read
        if (plugin.databaseManager == null || !plugin.databaseManager.isEnabled()) {
            throw new IllegalStateException("Database must be enabled to read from it.");
        }
        BanksRepository repo = new BanksRepository(plugin.databaseManager.getDataSource());
        repo.initSchema();
        Map<UUID, Bank> banksById = new HashMap<>();
        Map<String, UUID> bankIdByName = new HashMap<>();
        Map<UUID, Map<UUID, BankAccount>> accounts = new HashMap<>();
        Map<UUID, Long> last = new HashMap<>();
        repo.loadAll(banksById, bankIdByName, accounts, last);
        // Write YAML file
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File banksFile = new File(dataFolder, "banks.yml");
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection banksSec = cfg.createSection("banks");
        for (Bank bank : banksById.values()) {
            ConfigurationSection b = banksSec.createSection(bank.getId().toString());
            b.set("name", bank.getName());
            b.set("interest", Math.max(0d, bank.getInterest()));
            b.set("type", bank.getType().name());
            b.set("open", bank.isOpen());
            if (bank.getArmorStandUuid() != null) b.set("armorStand", bank.getArmorStandUuid().toString());
            // claims
            int i = 0;
            for (Bank.Claim c : bank.getClaims()) {
                ConfigurationSection cs = b.createSection("claims." + (i++));
                cs.set("world", c.worldId.toString());
                cs.set("chunkX", c.chunkX);
                cs.set("chunkZ", c.chunkZ);
            }
            // accrual
            Long l = last.get(bank.getId());
            if (l != null) b.set("lastAccrual", l);
        }
        ConfigurationSection accSec = cfg.createSection("accounts");
        for (Map.Entry<UUID, Map<UUID, BankAccount>> e : accounts.entrySet()) {
            String pid = e.getKey().toString();
            ConfigurationSection pSec = accSec.createSection(pid);
            for (BankAccount acct : e.getValue().values()) {
                pSec.set(acct.getBankId().toString(), acct.getBalance());
            }
        }
        try {
            cfg.save(banksFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDatabaseEnabled(boolean enabled) throws IOException {
        // Persist new mode to config.yml
        File cfgFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        ConfigurationSection db = cfg.getConfigurationSection("database");
        if (db == null) db = cfg.createSection("database");
        db.set("enabled", enabled ? "enable" : "disable");
        cfg.save(cfgFile);
        // Reload Bukkit config and reinitialize DB manager to apply changes
        plugin.reloadConfig();
        if (plugin.databaseManager != null) {
            try { plugin.databaseManager.shutdown(); } catch (Throwable ignored) {}
            plugin.databaseManager.init();
        }
        // Reload banks into the new mode
        plugin.banksService.load();
    }

    private Map<UUID, Map<UUID, BankAccount>> getAccountsSnapshot() {
        // BanksService stores accounts internally; we can only access via reflection or via save-load trick.
        // Use save-load YAML trick to get accounts map safely.
        try { plugin.banksService.save(); } catch (Throwable ignored) {}
        Map<UUID, Map<UUID, BankAccount>> result = new HashMap<>();
        try {
            File file = new File(plugin.getDataFolder(), "banks.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection accSec = cfg.getConfigurationSection("accounts");
            if (accSec != null) {
                for (String playerStr : accSec.getKeys(false)) {
                    UUID pid = UUID.fromString(playerStr);
                    ConfigurationSection psec = accSec.getConfigurationSection(playerStr);
                    if (psec == null) continue;
                    Map<UUID, BankAccount> map = new HashMap<>();
                    for (String bankIdStr : psec.getKeys(false)) {
                        UUID bid = UUID.fromString(bankIdStr);
                        double bal = psec.getDouble(bankIdStr, 0.0d);
                        BankAccount acct = new BankAccount(bid, pid);
                        acct.setBalance(bal);
                        map.put(bid, acct);
                    }
                    result.put(pid, map);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private Map<UUID, Long> getLastAccrualSnapshot() {
        // Save current YAML so we can read lastAccrual fields
        try { plugin.banksService.save(); } catch (Throwable ignored) {}
        Map<UUID, Long> map = new HashMap<>();
        try {
            File file = new File(plugin.getDataFolder(), "banks.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection banksSec = cfg.getConfigurationSection("banks");
            if (banksSec != null) {
                for (String idStr : banksSec.getKeys(false)) {
                    UUID id = UUID.fromString(idStr);
                    ConfigurationSection b = banksSec.getConfigurationSection(idStr);
                    if (b == null) continue;
                    long last = b.getLong("lastAccrual", 0L);
                    if (last > 0) map.put(id, last);
                }
            }
        } catch (Throwable ignored) {}
        return map;
    }

    @Override
    public String getName() { return "migrate"; }

    @Override
    public String getPermission() { return "furious.banks.migrate"; }
}
