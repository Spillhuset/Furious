package com.spillhuset.furious.db;

import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.BankAccount;
import com.spillhuset.furious.utils.BankType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * SQL repository for Banks and BankAccounts. Uses simple snapshot save/load.
 * Intended to be used only when DatabaseManager is enabled.
 */
public class BanksRepository {
    private final DataSource dataSource;

    public BanksRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // Use VARCHAR(36) for UUID text representation for simplicity
            st.executeUpdate("CREATE TABLE IF NOT EXISTS banks (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(128) NOT NULL, " +
                    "interest DOUBLE NOT NULL, " +
                    "type VARCHAR(32) NOT NULL, " +
                    "open TINYINT NOT NULL, " +
                    "armor_stand VARCHAR(36) NULL, " +
                    "last_accrual BIGINT NULL" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bank_claims (" +
                    "bank_id VARCHAR(36) NOT NULL, " +
                    "world VARCHAR(36) NOT NULL, " +
                    "chunk_x INT NOT NULL, " +
                    "chunk_z INT NOT NULL, " +
                    "PRIMARY KEY (bank_id, world, chunk_x, chunk_z), " +
                    "FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS bank_accounts (" +
                    "player_id VARCHAR(36) NOT NULL, " +
                    "bank_id VARCHAR(36) NOT NULL, " +
                    "balance DOUBLE NOT NULL, " +
                    "PRIMARY KEY (player_id, bank_id), " +
                    "FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE CASCADE" +
                    ")");
        }
    }

    public void loadAll(Map<UUID, Bank> banksById,
                        Map<String, UUID> bankIdByName,
                        Map<UUID, Map<UUID, BankAccount>> accounts,
                        Map<UUID, Long> lastAccrualByBank) throws Exception {
        banksById.clear();
        bankIdByName.clear();
        accounts.clear();
        lastAccrualByBank.clear();
        try (Connection conn = dataSource.getConnection()) {
            // Load banks
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, interest, type, open, armor_stand, last_accrual FROM banks")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString(1));
                        String name = rs.getString(2);
                        double interest = rs.getDouble(3);
                        String type = rs.getString(4);
                        boolean open = rs.getInt(5) != 0;
                        String armor = rs.getString(6);
                        Long last = (Long) rs.getObject(7);
                        Bank bank = new Bank(id, name);
                        bank.setInterest(Math.max(0d, interest));
                        try {
                            bank.setType(BankType.valueOf(type == null ? "PLAYER" : type.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                        bank.setOpen(open);
                        if (armor != null && !armor.isBlank()) {
                            try { bank.setArmorStandUuid(UUID.fromString(armor)); } catch (IllegalArgumentException ignored) {}
                        }
                        if (last != null) lastAccrualByBank.put(id, last);
                        banksById.put(id, bank);
                        if (name != null) bankIdByName.put(name.toLowerCase(), id);
                    }
                }
            }
            // Load claims
            try (PreparedStatement ps = conn.prepareStatement("SELECT bank_id, world, chunk_x, chunk_z FROM bank_claims")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID bid = UUID.fromString(rs.getString(1));
                        String world = rs.getString(2);
                        int cx = rs.getInt(3);
                        int cz = rs.getInt(4);
                        Bank bank = banksById.get(bid);
                        if (bank != null) {
                            try { bank.addClaim(UUID.fromString(world), cx, cz); } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            }
            // Load accounts
            try (PreparedStatement ps = conn.prepareStatement("SELECT player_id, bank_id, balance FROM bank_accounts")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID pid = UUID.fromString(rs.getString(1));
                        UUID bid = UUID.fromString(rs.getString(2));
                        double bal = rs.getDouble(3);
                        Map<UUID, BankAccount> perBank = accounts.computeIfAbsent(pid, x -> new HashMap<>());
                        BankAccount acct = new BankAccount(bid, pid);
                        acct.setBalance(bal);
                        perBank.put(bid, acct);
                    }
                }
            }
        }
    }

    public void saveAll(Collection<Bank> banks,
                        Map<UUID, Map<UUID, BankAccount>> accounts,
                        Map<UUID, Long> lastAccrualByBank) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM bank_accounts");
                st.executeUpdate("DELETE FROM bank_claims");
                st.executeUpdate("DELETE FROM banks");
            }
            // Insert banks
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO banks(id, name, interest, type, open, armor_stand, last_accrual) VALUES(?,?,?,?,?,?,?)")) {
                for (Bank bank : banks) {
                    ps.setString(1, bank.getId().toString());
                    ps.setString(2, bank.getName());
                    ps.setDouble(3, Math.max(0d, bank.getInterest()));
                    ps.setString(4, bank.getType().name());
                    ps.setInt(5, bank.isOpen() ? 1 : 0);
                    ps.setString(6, bank.getArmorStandUuid() == null ? null : bank.getArmorStandUuid().toString());
                    Long last = lastAccrualByBank.get(bank.getId());
                    if (last == null) ps.setObject(7, null); else ps.setLong(7, last);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            // Insert claims
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bank_claims(bank_id, world, chunk_x, chunk_z) VALUES(?,?,?,?)")) {
                for (Bank bank : banks) {
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
                    "INSERT INTO bank_accounts(player_id, bank_id, balance) VALUES(?,?,?)")) {
                for (Map.Entry<UUID, Map<UUID, BankAccount>> e : accounts.entrySet()) {
                    UUID pid = e.getKey();
                    Map<UUID, BankAccount> perBank = e.getValue();
                    if (perBank == null) continue;
                    for (BankAccount acct : perBank.values()) {
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
    }
}
