package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalletManager {
    private final Furious plugin;

    public WalletManager(Furious plugin) {
        this.plugin = plugin;
    }


    public void shutdown() {
        wallets.clear();
    }

    private final Map<UUID, Double> wallets = new HashMap<>();

    /**
     * Retrieves the balance for the specified player's wallet.
     *
     * @param player the player whose balance is to be retrieved
     * @return the balance of the player's wallet, or 0.0 if the player does not have a wallet
     */
    public double getBalance(Player player) {
        return wallets.getOrDefault(player.getUniqueId(), 0.0);
    }

    /**
     * Checks if the specified player has an associated wallet.
     *
     * @param player the player whose wallet existence is being checked
     * @return true if the player's wallet exists, false otherwise
     */
    public boolean has(Player player) {
        return wallets.containsKey(player.getUniqueId());
    }

    /**
     * Checks if the specified player has at least the given amount of funds in their wallet.
     *
     * @param player the player whose wallet is being checked
     * @param amount the amount to check against the player's wallet balance
     * @return true if the player's wallet balance is greater than or equal to the specified amount, false otherwise
     */
    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Withdraws a specified amount from the given player's wallet if sufficient funds are available.
     *
     * @param player the player whose wallet will be debited
     * @param amount the amount to withdraw from the player's wallet
     * @return true if the withdrawal was successful, false if the player has insufficient funds
     */
    public boolean withdraw(Player player, double amount) {
        if (!has(player, amount)) {
            return false;
        }
        wallets.put(player.getUniqueId(), (getBalance(player) - amount));
        return true;
    }

    /**
     * Deposits a specified amount into the given player's wallet.
     * If the player does not already have a wallet, one will be created
     * and the specified amount will be added as the initial balance.
     *
     * @param player the player whose wallet will be credited
     * @param amount the amount to deposit into the player's wallet
     */
    public void deposit(Player player, double amount) {
        wallets.put(player.getUniqueId(), (getBalance(player) + amount));
    }

    /**
     * Sets the balance for the specified player's wallet.
     *
     * @param player the player whose wallet balance is to be set
     * @param amount the new balance to set in the player's wallet
     */
    public void setBalance(Player player, double amount) {
        wallets.put(player.getUniqueId(), amount);
    }

    /**
     * Formats the given monetary amount into a string representation that includes the currency symbol
     * and the appropriate singular or plural form of the currency name.
     *
     * @param amount the monetary amount to format
     * @return a string representation of the amount with the currency symbol and name
     */
    public String formatAmount(double amount) {
        return plugin.getConfig().getString("economy.currency.symbol") + amount + " " +
                (amount == 1 ? plugin.getConfig().getString("economy.currency.name") : plugin.getConfig().getString("economy.currency.plural"));
    }

    /**
     * Initializes a wallet for the specified player with a default balance of 0.0.
     *
     * @param playerId the unique identifier of the player for whom the wallet is being initialized
     */
    public void init(UUID playerId) {
        wallets.put(playerId, 0.0);
    }

}
