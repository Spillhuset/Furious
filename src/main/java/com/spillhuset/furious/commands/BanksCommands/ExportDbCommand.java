package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.BankAccount;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/**
 * Admin command: Export current in-memory banks and accounts to an SQLite database file.
 * Usage: /banks exportdb [filename]
 * Default filename: banks.sqlite in the plugin data folder.
 */
public class ExportDbCommand implements SubCommandInterface {
    private final Furious plugin;

    public ExportDbCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String fileName = (args.length >= 2 && args[1] != null && !args[1].isBlank()) ? args[1] : "banks.sqlite";
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File dbFile = new File(dataFolder, fileName);

        try {
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            try (Connection conn = DriverManager.getConnection(url)) {
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    // Drop existing tables
                    st.executeUpdate("DROP TABLE IF EXISTS bank_claims");
                    st.executeUpdate("DROP TABLE IF EXISTS accounts");
                    st.executeUpdate("DROP TABLE IF EXISTS banks");
                    // Create tables
                    st.executeUpdate("CREATE TABLE banks (" +
                            "id TEXT PRIMARY KEY, " +
                            "name TEXT NOT NULL, " +
                            "interest REAL NOT NULL, " +
                            "type TEXT NOT NULL, " +
                            "open INTEGER NOT NULL, " +
                            "armor_stand TEXT NULL, " +
                            "last_accrual INTEGER NULL" +
                            ")");
                    st.executeUpdate("CREATE TABLE bank_claims (" +
                            "bank_id TEXT NOT NULL, " +
                            "world TEXT NOT NULL, " +
                            "chunk_x INTEGER NOT NULL, " +
                            "chunk_z INTEGER NOT NULL, " +
                            "PRIMARY KEY (bank_id, world, chunk_x, chunk_z), " +
                            "FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE" +
                            ")");
                    st.executeUpdate("CREATE TABLE accounts (" +
                            "player_id TEXT NOT NULL, " +
                            "bank_id TEXT NOT NULL, " +
                            "balance REAL NOT NULL, " +
                            "PRIMARY KEY (player_id, bank_id), " +
                            "FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE" +
                            ")");
                }

                // Insert banks
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO banks(id, name, interest, type, open, armor_stand, last_accrual) VALUES(?,?,?,?,?,?,?)")) {
                    for (Bank bank : plugin.banksService.getBanks()) {
                        ps.setString(1, bank.getId().toString());
                        ps.setString(2, bank.getName());
                        ps.setDouble(3, Math.max(0d, bank.getInterest()));
                        ps.setString(4, bank.getType().name());
                        ps.setInt(5, bank.isOpen() ? 1 : 0);
                        ps.setString(6, bank.getArmorStandUuid() == null ? null : bank.getArmorStandUuid().toString());
                        // We cannot access lastAccrual map directly; export 0, loader can backfill later
                        ps.setObject(7, null);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Insert claims
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO bank_claims(bank_id, world, chunk_x, chunk_z) VALUES(?,?,?,?)")) {
                    for (Bank bank : plugin.banksService.getBanks()) {
                        for (Bank.Claim c : bank.getClaims()) {
                            ps.setString(1, bank.getId().toString());
                            ps.setString(2, c.worldId.toString());
                            ps.setInt(3, c.chunkX);
                            ps.setInt(4, c.chunkZ);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                // Insert accounts
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO accounts(player_id, bank_id, balance) VALUES(?,?,?)")) {
                    Map<UUID, Map<UUID, BankAccount>> snapshot = getAccountsSnapshot();
                    for (Map.Entry<UUID, Map<UUID, BankAccount>> e : snapshot.entrySet()) {
                        UUID pid = e.getKey();
                        for (BankAccount acct : e.getValue().values()) {
                            ps.setString(1, pid.toString());
                            ps.setString(2, acct.getBankId().toString());
                            ps.setDouble(3, acct.getBalance());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                conn.commit();
            }
            Components.sendSuccessMessage(sender, "Exported banks to SQLite: " + dbFile.getName());
        } catch (Exception ex) {
            Components.sendErrorMessage(sender, "Export failed: " + ex.getMessage());
        }
        return true;
    }

    private Map<UUID, Map<UUID, BankAccount>> getAccountsSnapshot() {
        // BanksService does not expose the accounts map; rebuild via public getters
        // by querying balances per player UUIDs visible in the YAML store paths is not available here.
        // Therefore, we infer by iterating online/offline known players is not reliable.
        // As a compromise, we leverage BanksService.getAccountsBalances through online players where possible,
        // but to ensure we export everything, we parse the YAML directly through BanksService save/load path.
        // Simpler and robust approach: call plugin.banksService.save(), then read accounts from banks.yml here.
        // This avoids reflection and keeps export self-contained.
        try {
            plugin.banksService.save();
        } catch (Throwable ignored) {}
        Map<UUID, Map<UUID, BankAccount>> result = new HashMap<>();
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "banks.yml");
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            org.bukkit.configuration.ConfigurationSection accSec = cfg.getConfigurationSection("accounts");
            if (accSec != null) {
                for (String playerStr : accSec.getKeys(false)) {
                    UUID pid = UUID.fromString(playerStr);
                    org.bukkit.configuration.ConfigurationSection psec = accSec.getConfigurationSection(playerStr);
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

    @Override
    public String getName() {
        return "exportdb";
    }

    @Override
    public String getPermission() {
        return "furious.banks.exportdb";
    }
}
