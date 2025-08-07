package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Data Access Object for wallet operations.
 */
public class WalletDAO {
    private final Furious plugin;
    private final DatabaseManager databaseManager;

    /**
     * Creates a new WalletDAO.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public WalletDAO(Furious plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Saves a player's wallet balance to the database.
     *
     * @param playerId The UUID of the player
     * @param balance The player's wallet balance
     * @return true if the wallet was saved successfully, false otherwise
     */
    public boolean saveWallet(UUID playerId, double balance) {
        if (databaseManager.isUsingYaml()) {
            return false; // YAML storage is handled by WalletManager
        }

        try (Connection connection = databaseManager.getConnection()) {
            String sql;

            if (databaseManager.getStorageType() == DatabaseManager.StorageType.SQLITE) {
                sql = "INSERT OR REPLACE INTO wallets (player_id, balance) VALUES (?, ?)";
            } else {
                sql = "INSERT INTO wallets (player_id, balance) VALUES (?, ?) " +
                      "ON DUPLICATE KEY UPDATE balance = ?";
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setDouble(2, balance);

                if (databaseManager.getStorageType() != DatabaseManager.StorageType.SQLITE) {
                    statement.setDouble(3, balance);
                }

                statement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save wallet: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads all wallet balances from the database.
     *
     * @return A map of player UUIDs to wallet balances
     */
    public Map<UUID, Double> loadAllWallets() {
        Map<UUID, Double> wallets = new HashMap<>();

        if (databaseManager.isUsingYaml()) {
            return wallets; // YAML storage is handled by WalletManager
        }

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT player_id, balance FROM wallets");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                try {
                    UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                    double balance = resultSet.getDouble("balance");
                    wallets.put(playerId, balance);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in wallets table: " + resultSet.getString("player_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load wallets: " + e.getMessage(), e);
        }

        return wallets;
    }

    /**
     * Gets a player's wallet balance from the database.
     *
     * @param playerId The UUID of the player
     * @return The player's wallet balance, or 0.0 if not found
     */
    public double getWalletBalance(UUID playerId) {
        if (databaseManager.isUsingYaml()) {
            return 0.0; // YAML storage is handled by WalletManager
        }

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT balance FROM wallets WHERE player_id = ?")) {

            statement.setString(1, playerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get wallet balance: " + e.getMessage(), e);
        }

        return 0.0;
    }

    /**
     * Deletes a player's wallet from the database.
     *
     * @param playerId The UUID of the player
     * @return true if the wallet was deleted successfully, false otherwise
     */
    public boolean deleteWallet(UUID playerId) {
        if (databaseManager.isUsingYaml()) {
            return false; // YAML storage is handled by WalletManager
        }

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM wallets WHERE player_id = ?")) {

            statement.setString(1, playerId.toString());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete wallet: " + e.getMessage(), e);
            return false;
        }
    }
}