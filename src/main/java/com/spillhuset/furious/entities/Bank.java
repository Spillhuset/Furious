package com.spillhuset.furious.entities;

import org.bukkit.Chunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a bank in the Furious economy system.
 * Banks allow players to store their scraps (currency) safely.
 * Banks can claim chunks to establish their physical presence in the world.
 */
public class Bank {
    private String name;
    private final Map<UUID, Double> accounts = new HashMap<>();
    private final Set<String> claimedChunks = new HashSet<>();
    private double interestRate = 0.0;

    /**
     * Creates a new bank with the specified name.
     *
     * @param name the name of the bank
     */
    public Bank(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the bank.
     *
     * @return the bank's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the balance of a player's account in this bank.
     *
     * @param playerId the UUID of the player
     * @return the balance of the player's account, or 0.0 if the player doesn't have an account
     */
    public double getBalance(UUID playerId) {
        return accounts.getOrDefault(playerId, 0.0);
    }

    /**
     * Checks if a player has an account in this bank.
     *
     * @param playerId the UUID of the player
     * @return true if the player has an account, false otherwise
     */
    public boolean hasAccount(UUID playerId) {
        return accounts.containsKey(playerId);
    }

    /**
     * Checks if a player has at least the specified amount in their account.
     *
     * @param playerId the UUID of the player
     * @param amount the amount to check
     * @return true if the player has at least the specified amount, false otherwise
     */
    public boolean hasAmount(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    /**
     * Creates a new account for a player with the specified initial balance.
     *
     * @param playerId the UUID of the player
     * @param initialBalance the initial balance for the account
     * @return true if the account was created successfully, false if the player already has an account or the initial balance is negative
     */
    public boolean createAccount(UUID playerId, double initialBalance) {
        if (hasAccount(playerId) || initialBalance < 0) {
            return false;
        }
        accounts.put(playerId, initialBalance);
        return true;
    }

    /**
     * Deposits the specified amount into a player's account.
     *
     * @param playerId the UUID of the player
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false if the amount is negative
     */
    public boolean deposit(UUID playerId, double amount) {
        if (amount < 0) {
            return false;
        }
        double newBalance = getBalance(playerId) + amount;
        accounts.put(playerId, newBalance);
        return true;
    }

    /**
     * Withdraws the specified amount from a player's account.
     *
     * @param playerId the UUID of the player
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false if the player doesn't have enough funds or the amount is negative
     */
    public boolean withdraw(UUID playerId, double amount) {
        if (amount < 0 || !hasAmount(playerId, amount)) {
            return false;
        }
        double newBalance = getBalance(playerId) - amount;
        accounts.put(playerId, newBalance);
        return true;
    }

    /**
     * Sets the balance of a player's account.
     *
     * @param playerId the UUID of the player
     * @param amount the new balance
     * @return true if the balance was set successfully, false if the amount is negative
     */
    public boolean setBalance(UUID playerId, double amount) {
        if (amount < 0) {
            return false;
        }
        accounts.put(playerId, amount);
        return true;
    }

    /**
     * Gets all accounts in this bank.
     *
     * @return a map of player UUIDs to account balances
     */
    public Map<UUID, Double> getAccounts() {
        return new HashMap<>(accounts);
    }

    /**
     * Formats a chunk into a string representation.
     *
     * @param chunk the chunk to format
     * @return a string representation of the chunk in the format "world:x:z"
     */
    public String formatChunk(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Claims a chunk for this bank.
     *
     * @param chunk the chunk to claim
     * @return true if the chunk was claimed successfully, false if it was already claimed by this bank
     */
    public boolean claimChunk(Chunk chunk) {
        return claimedChunks.add(formatChunk(chunk));
    }

    /**
     * Unclaims a chunk from this bank.
     *
     * @param chunk the chunk to unclaim
     * @return true if the chunk was unclaimed successfully, false if it wasn't claimed by this bank
     */
    public boolean unclaimChunk(Chunk chunk) {
        return claimedChunks.remove(formatChunk(chunk));
    }

    /**
     * Checks if a chunk is claimed by this bank.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is claimed by this bank, false otherwise
     */
    public boolean isChunkClaimed(Chunk chunk) {
        return claimedChunks.contains(formatChunk(chunk));
    }

    /**
     * Gets all chunks claimed by this bank.
     *
     * @return a set of string representations of claimed chunks
     */
    public Set<String> getClaimedChunks() {
        return new HashSet<>(claimedChunks);
    }

    /**
     * Gets the number of chunks claimed by this bank.
     *
     * @return the number of claimed chunks
     */
    public int getClaimedChunkCount() {
        return claimedChunks.size();
    }

    /**
     * Gets the interest rate of the bank.
     *
     * @return the interest rate as a decimal (e.g., 0.05 for 5%)
     */
    public double getInterestRate() {
        return interestRate;
    }

    /**
     * Sets the interest rate of the bank.
     *
     * @param interestRate the interest rate as a decimal (e.g., 0.05 for 5%)
     * @return true if the interest rate was set successfully, false if the interest rate is negative
     */
    public boolean setInterestRate(double interestRate) {
        if (interestRate < 0) {
            return false;
        }
        this.interestRate = interestRate;
        return true;
    }

    /**
     * Renames the bank.
     *
     * @param newName the new name for the bank
     */
    public void rename(String newName) {
        this.name = newName;
    }

    /**
     * Applies interest to all accounts in the bank.
     * The interest is calculated as balance * interestRate.
     */
    public void applyInterest() {
        if (interestRate <= 0) {
            return; // No interest to apply
        }

        for (Map.Entry<UUID, Double> entry : accounts.entrySet()) {
            UUID playerId = entry.getKey();
            double balance = entry.getValue();
            double interest = balance * interestRate;
            accounts.put(playerId, balance + interest);
        }
    }

    /**
     * Deletes an account from the bank.
     *
     * @param playerId the UUID of the player whose account should be deleted
     * @return true if the account was deleted, false if the player doesn't have an account
     */
    public boolean deleteAccount(UUID playerId) {
        if (!hasAccount(playerId)) {
            return false;
        }
        accounts.remove(playerId);
        return true;
    }
}
