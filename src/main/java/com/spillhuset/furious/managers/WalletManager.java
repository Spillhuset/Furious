package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class WalletManager {
    private final Furious plugin;
    private final File walletFile;
    private final FileConfiguration walletConfig;
    private final Map<UUID, Double> wallets = new HashMap<>();

    // Currency configuration
    private String currencyName;
    private String currencyPlural;
    private String currencySymbol;
    private String currencyFormat;
    private String currencyMaterial;

    public WalletManager(Furious plugin) {
        this.plugin = plugin;
        this.walletFile = new File(plugin.getDataFolder(), "wallets.yml");
        this.walletConfig = YamlConfiguration.loadConfiguration(walletFile);
        loadWallets();
        loadCurrencyConfig();
    }

    /**
     * Loads currency configuration from the config.yml file.
     */
    private void loadCurrencyConfig() {
        FileConfiguration config = plugin.getConfig();
        currencyName = config.getString("economy.currency.name", "Scrap");
        currencyPlural = config.getString("economy.currency.plural", "Scraps");
        currencySymbol = config.getString("economy.currency.symbol", "⚙");
        currencyFormat = config.getString("economy.currency.format", "%symbol%%amount%");
        currencyMaterial = config.getString("economy.currency.material", "IRON_NUGGET");
    }

    /**
     * Reloads currency configuration from the config.yml file.
     */
    public void reloadCurrencyConfig() {
        plugin.reloadConfig();
        loadCurrencyConfig();
    }

    /**
     * Saves currency configuration to the config.yml file.
     */
    public void saveCurrencyConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("economy.currency.name", currencyName);
        config.set("economy.currency.plural", currencyPlural);
        config.set("economy.currency.symbol", currencySymbol);
        config.set("economy.currency.format", currencyFormat);
        config.set("economy.currency.material", currencyMaterial);
        plugin.saveConfig();
    }

    /**
     * Gets the currency name.
     *
     * @return the currency name
     */
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * Sets the currency name.
     *
     * @param currencyName the currency name to set
     */
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    /**
     * Gets the currency plural.
     *
     * @return the currency plural
     */
    public String getCurrencyPlural() {
        return currencyPlural;
    }

    /**
     * Sets the currency plural.
     *
     * @param currencyPlural the currency plural to set
     */
    public void setCurrencyPlural(String currencyPlural) {
        this.currencyPlural = currencyPlural;
    }

    /**
     * Gets the currency symbol.
     *
     * @return the currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Sets the currency symbol.
     *
     * @param currencySymbol the currency symbol to set
     */
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    /**
     * Gets the currency format.
     *
     * @return the currency format
     */
    public String getCurrencyFormat() {
        return currencyFormat;
    }

    /**
     * Sets the currency format.
     *
     * @param currencyFormat the currency format to set
     */
    public void setCurrencyFormat(String currencyFormat) {
        this.currencyFormat = currencyFormat;
    }

    /**
     * Gets the currency material.
     *
     * @return the currency material
     */
    public String getCurrencyMaterial() {
        return currencyMaterial;
    }

    /**
     * Sets the currency material.
     *
     * @param currencyMaterial the currency material to set
     */
    public void setCurrencyMaterial(String currencyMaterial) {
        this.currencyMaterial = currencyMaterial;
    }

    /**
     * Loads wallet data from the wallets.yml file.
     */
    private void loadWallets() {
        if (!walletFile.exists()) {
            return;
        }

        for (String uuidStr : walletConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                double balance = walletConfig.getDouble(uuidStr);
                wallets.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in wallets.yml: " + uuidStr);
            }
        }
    }

    /**
     * Saves wallet data to the wallets.yml file.
     */
    private void saveWallets() {
        for (Map.Entry<UUID, Double> entry : wallets.entrySet()) {
            walletConfig.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            walletConfig.save(walletFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save wallet data", e);
        }
    }

    /**
     * Saves wallet data to the wallets.yml file.
     * This method is public and can be called from outside the class.
     */
    public void saveWalletData() {
        saveWallets();
    }

    public void shutdown() {
        saveWallets();
        wallets.clear();
    }

    /**
     * Retrieves the balance for the specified player's wallet.
     *
     * @param player the player whose balance is to be retrieved
     * @return the balance of the player's wallet, or 0.0 if the player does not have a wallet or is an op
     */
    public double getBalance(Player player) {
        // Ops don't have wallets
        if (player.isOp()) {
            return 0.0;
        }
        return wallets.getOrDefault(player.getUniqueId(), 0.0);
    }

    /**
     * Checks if the specified player has an associated wallet.
     *
     * @param player the player whose wallet existence is being checked
     * @return true if the player's wallet exists, false otherwise or if player is an op
     */
    public boolean has(Player player) {
        // Ops don't have wallets
        if (player.isOp()) {
            return false;
        }
        return wallets.containsKey(player.getUniqueId());
    }

    /**
     * Checks if the specified player has at least the given amount of funds in their wallet.
     *
     * @param player the player whose wallet is being checked
     * @param amount the amount to check against the player's wallet balance
     * @return true if the player's wallet balance is greater than or equal to the specified amount, false otherwise or if player is an op
     */
    public boolean has(Player player, double amount) {
        // Ops don't have wallets
        if (player.isOp()) {
            return false;
        }
        return getBalance(player) >= amount;
    }

    /**
     * Withdraws a specified amount from the given player's wallet if sufficient funds are available.
     *
     * @param player the player whose wallet will be debited
     * @param amount the amount to withdraw from the player's wallet
     * @return true if the withdrawal was successful, false if the player has insufficient funds, is an op, or the amount is negative
     */
    public boolean withdraw(Player player, double amount) {
        // Ops don't have wallets
        if (player.isOp()) {
            return false;
        }

        if (amount < 0) {
            return false;
        }
        if (!has(player, amount)) {
            return false;
        }
        double newBalance = getBalance(player) - amount;
        wallets.put(player.getUniqueId(), newBalance);
        logTransaction(player.getUniqueId(), "withdraw", amount, newBalance);
        saveWallets();
        return true;
    }

    /**
     * Deposits a specified amount into the given player's wallet.
     * If the player does not already have a wallet, one will be created
     * and the specified amount will be added as the initial balance.
     *
     * @param player the player whose wallet will be credited
     * @param amount the amount to deposit into the player's wallet
     * @return true if the deposit was successful, false if the player is an op or the amount is negative
     */
    public boolean deposit(Player player, double amount) {
        // Ops don't have wallets
        if (player.isOp()) {
            return false;
        }

        if (amount < 0) {
            return false;
        }
        double newBalance = getBalance(player) + amount;
        wallets.put(player.getUniqueId(), newBalance);
        logTransaction(player.getUniqueId(), "deposit", amount, newBalance);
        saveWallets();
        return true;
    }

    /**
     * Sets the balance for the specified player's wallet.
     *
     * @param player the player whose wallet balance is to be set
     * @param amount the new balance to set in the player's wallet
     * @return true if the balance was set successfully, false if the player is an op or the amount is negative
     */
    public boolean setBalance(Player player, double amount) {
        // Ops don't have wallets
        if (player.isOp()) {
            return false;
        }

        if (amount < 0) {
            return false;
        }
        wallets.put(player.getUniqueId(), amount);
        logTransaction(player.getUniqueId(), "set_balance", amount, amount);
        saveWallets();
        return true;
    }

    /**
     * Transfers a specified amount from one player's wallet to another.
     *
     * @param from the player whose wallet will be debited
     * @param to the player whose wallet will be credited
     * @param amount the amount to transfer
     * @return true if the transfer was successful, false if either player is an op, the from player has insufficient funds, or the amount is negative
     */
    public boolean transfer(Player from, Player to, double amount) {
        // Ops don't have wallets
        if (from.isOp() || to.isOp()) {
            return false;
        }

        if (amount < 0) {
            return false;
        }
        if (!has(from, amount)) {
            return false;
        }

        double fromBalance = getBalance(from) - amount;
        double toBalance = getBalance(to) + amount;

        wallets.put(from.getUniqueId(), fromBalance);
        wallets.put(to.getUniqueId(), toBalance);

        logTransaction(from.getUniqueId(), "transfer_out", amount, fromBalance);
        logTransaction(to.getUniqueId(), "transfer_in", amount, toBalance);

        saveWallets();
        return true;
    }

    /**
     * Logs a transaction for auditing purposes.
     *
     * @param playerId the UUID of the player involved in the transaction
     * @param type the type of transaction (deposit, withdraw, set_balance, transfer_in, transfer_out)
     * @param amount the amount involved in the transaction
     * @param newBalance the new balance after the transaction
     */
    private void logTransaction(UUID playerId, String type, double amount, double newBalance) {
        plugin.getLogger().info(String.format(
            "Wallet Transaction: Player=%s, Type=%s, Amount=%s, NewBalance=%s",
            playerId, type, formatAmount(amount), formatAmount(newBalance)
        ));
    }

    /**
     * Formats the given monetary amount into a string representation that includes the currency symbol
     * and the appropriate singular or plural form of the currency name.
     *
     * @param amount the monetary amount to format
     * @return a string representation of the amount with the currency symbol and name
     */
    public String formatAmount(double amount) {
        String name = amount == 1 ? currencyName : currencyPlural;

        return currencyFormat
            .replace("%symbol%", currencySymbol)
            .replace("%amount%", String.valueOf(amount))
            .replace("%name%", name);
    }

    /**
     * Initializes a wallet for the specified player with a custom initial balance.
     *
     * @param playerId       the unique identifier of the player for whom the wallet is being initialized
     * @param initialBalance the initial balance to set in the player's wallet. If negative, the method will return early without initializing the wallet.
     */
    public void init(UUID playerId, double initialBalance) {
        if (initialBalance < 0) {
            return;
        }
        wallets.put(playerId, initialBalance);
        logTransaction(playerId, "init", initialBalance, initialBalance);
        saveWallets();
    }

    /**
     * Gets the balance for the specified player's wallet by UUID.
     *
     * @param playerId the UUID of the player whose balance is to be retrieved
     * @return the balance of the player's wallet, or 0.0 if the player does not have a wallet
     */
    public double getWallet(UUID playerId) {
        return wallets.getOrDefault(playerId, 0.0);
    }

    /**
     * Sets the balance for the specified player's wallet by UUID.
     *
     * @param playerId the UUID of the player whose wallet balance is to be set
     * @param amount   the new balance to set in the player's wallet
     */
    public void setWallet(UUID playerId, double amount) {
        if (amount < 0) {
            return;
        }
        wallets.put(playerId, amount);
        logTransaction(playerId, "set_balance", amount, amount);
        saveWallets();
    }
}
