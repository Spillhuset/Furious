package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages banks in the Furious economy system.
 */
public class BankManager {
    private final Furious plugin;
    private final File banksFile;
    private final FileConfiguration banksConfig;
    private final Map<String, Bank> banks = new HashMap<>();
    private final String DEFAULT_BANK_NAME = "RubberBank";
    private final double DEFAULT_INITIAL_BALANCE = 100.0;

    /**
     * Creates a new BankManager.
     *
     * @param plugin the Furious plugin instance
     */
    public BankManager(Furious plugin) {
        this.plugin = plugin;
        this.banksFile = new File(plugin.getDataFolder(), "banks.yml");
        this.banksConfig = YamlConfiguration.loadConfiguration(banksFile);

        // Create the default bank if it doesn't exist
        if (!banks.containsKey(DEFAULT_BANK_NAME)) {
            banks.put(DEFAULT_BANK_NAME, new Bank(DEFAULT_BANK_NAME));
        }

        loadBanks();
    }

    /**
     * Loads bank data from the banks.yml file.
     */
    private void loadBanks() {
        if (!banksFile.exists()) {
            return;
        }

        // Load bank names
        if (banksConfig.contains("banks")) {
            for (String bankName : banksConfig.getConfigurationSection("banks").getKeys(false)) {
                Bank bank = new Bank(bankName);
                banks.put(bankName, bank);

                // Load accounts for this bank
                String accountsPath = "banks." + bankName + ".accounts";
                if (banksConfig.contains(accountsPath)) {
                    for (String uuidStr : banksConfig.getConfigurationSection(accountsPath).getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(uuidStr);
                            double balance = banksConfig.getDouble(accountsPath + "." + uuidStr);
                            bank.setBalance(uuid, balance);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in banks.yml: " + uuidStr);
                        }
                    }
                }

                // Load claimed chunks for this bank
                String chunksPath = "banks." + bankName + ".chunks";
                if (banksConfig.contains(chunksPath)) {
                    ConfigurationSection chunksSection = banksConfig.getConfigurationSection(chunksPath);
                    if (chunksSection != null) {
                        for (String chunkKey : chunksSection.getKeys(false)) {
                            if (banksConfig.getBoolean(chunksPath + "." + chunkKey)) {
                                bank.getClaimedChunks().add(chunkKey);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Saves bank data to the banks.yml file.
     */
    private void saveBanks() {
        // Clear existing data
        banksConfig.set("banks", null);

        // Save each bank and its accounts
        for (Map.Entry<String, Bank> entry : banks.entrySet()) {
            String bankName = entry.getKey();
            Bank bank = entry.getValue();

            // Save accounts for this bank
            Map<UUID, Double> accounts = bank.getAccounts();
            for (Map.Entry<UUID, Double> accountEntry : accounts.entrySet()) {
                String path = "banks." + bankName + ".accounts." + accountEntry.getKey().toString();
                banksConfig.set(path, accountEntry.getValue());
            }

            // Save claimed chunks for this bank
            for (String chunkKey : bank.getClaimedChunks()) {
                String path = "banks." + bankName + ".chunks." + chunkKey;
                banksConfig.set(path, true);
            }
        }

        try {
            banksConfig.save(banksFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save bank data", e);
        }
    }

    /**
     * Saves bank data to the banks.yml file.
     * This method is public and can be called from outside the class.
     */
    public void saveBankData() {
        saveBanks();
    }

    /**
     * Shuts down the BankManager, saving all data.
     */
    public void shutdown() {
        saveBanks();
        banks.clear();
    }

    /**
     * Gets a bank by name.
     *
     * @param bankName the name of the bank
     * @return the bank, or null if no bank with the given name exists
     */
    public Bank getBank(String bankName) {
        return banks.get(bankName);
    }

    /**
     * Gets the default bank (RubberBank).
     *
     * @return the default bank
     */
    public Bank getDefaultBank() {
        return banks.get(DEFAULT_BANK_NAME);
    }

    /**
     * Creates a new bank with the given name.
     *
     * @param bankName the name of the bank
     * @return true if the bank was created successfully, false if a bank with the given name already exists
     */
    public boolean createBank(String bankName) {
        if (banks.containsKey(bankName)) {
            return false;
        }
        banks.put(bankName, new Bank(bankName));
        saveBanks();
        return true;
    }

    /**
     * Gets the balance of a player's account in the specified bank.
     *
     * @param player the player
     * @param bankName the name of the bank
     * @return the balance, or 0.0 if the player doesn't have an account in the bank, the bank doesn't exist, or the player is an op
     */
    public double getBalance(Player player, String bankName) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return 0.0;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return 0.0;
        }
        return bank.getBalance(player.getUniqueId());
    }

    /**
     * Gets the balance of a player's account in the default bank.
     *
     * @param player the player
     * @return the balance, or 0.0 if the player doesn't have an account in the default bank or the player is an op
     */
    public double getBalance(Player player) {
        return getBalance(player, DEFAULT_BANK_NAME);
    }

    /**
     * Checks if a player has an account in the specified bank.
     *
     * @param player the player
     * @param bankName the name of the bank
     * @return true if the player has an account in the bank, false otherwise, if the bank doesn't exist, or if the player is an op
     */
    public boolean hasAccount(Player player, String bankName) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return false;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }
        return bank.hasAccount(player.getUniqueId());
    }

    /**
     * Checks if a player has an account in the default bank.
     *
     * @param player the player
     * @return true if the player has an account in the default bank, false otherwise or if the player is an op
     */
    public boolean hasAccount(Player player) {
        return hasAccount(player, DEFAULT_BANK_NAME);
    }

    /**
     * Creates an account for a player in the specified bank with the given initial balance.
     *
     * @param player the player
     * @param bankName the name of the bank
     * @param initialBalance the initial balance
     * @return true if the account was created successfully, false if the player already has an account in the bank,
     *         the initial balance is negative, the bank doesn't exist, or the player is an op
     */
    public boolean createAccount(Player player, String bankName, double initialBalance) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return false;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }
        boolean success = bank.createAccount(player.getUniqueId(), initialBalance);
        if (success) {
            saveBanks();
            logTransaction(player.getUniqueId(), bankName, "create_account", initialBalance, initialBalance);
        }
        return success;
    }

    /**
     * Creates an account for a player in the default bank with the given initial balance.
     *
     * @param player the player
     * @param initialBalance the initial balance
     * @return true if the account was created successfully, false if the player already has an account in the default bank,
     *         the initial balance is negative, or the player is an op
     */
    public boolean createAccount(Player player, double initialBalance) {
        return createAccount(player, DEFAULT_BANK_NAME, initialBalance);
    }

    /**
     * Creates an account for a player in the default bank with the default initial balance (100.0).
     *
     * @param player the player
     * @return true if the account was created successfully, false if the player already has an account in the default bank
     *         or the player is an op
     */
    public boolean createAccount(Player player) {
        return createAccount(player, DEFAULT_BANK_NAME, DEFAULT_INITIAL_BALANCE);
    }

    /**
     * Deposits the specified amount into a player's account in the given bank.
     *
     * @param player the player
     * @param bankName the name of the bank
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false if the amount is negative, the bank doesn't exist, or the player is an op
     */
    public boolean deposit(Player player, String bankName, double amount) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return false;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        double oldBalance = bank.getBalance(player.getUniqueId());
        boolean success = bank.deposit(player.getUniqueId(), amount);

        if (success) {
            double newBalance = bank.getBalance(player.getUniqueId());
            saveBanks();
            logTransaction(player.getUniqueId(), bankName, "deposit", amount, newBalance);
        }

        return success;
    }

    /**
     * Deposits the specified amount into a player's account in the default bank.
     *
     * @param player the player
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false if the amount is negative
     */
    public boolean deposit(Player player, double amount) {
        return deposit(player, DEFAULT_BANK_NAME, amount);
    }

    /**
     * Withdraws the specified amount from a player's account in the given bank.
     *
     * @param player the player
     * @param bankName the name of the bank
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false if the player doesn't have enough funds,
     *         the amount is negative, the bank doesn't exist, or the player is an op
     */
    public boolean withdraw(Player player, String bankName, double amount) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return false;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        double oldBalance = bank.getBalance(player.getUniqueId());
        boolean success = bank.withdraw(player.getUniqueId(), amount);

        if (success) {
            double newBalance = bank.getBalance(player.getUniqueId());
            saveBanks();
            logTransaction(player.getUniqueId(), bankName, "withdraw", amount, newBalance);
        }

        return success;
    }

    /**
     * Withdraws the specified amount from a player's account in the default bank.
     *
     * @param player the player
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false if the player doesn't have enough funds or the amount is negative
     */
    public boolean withdraw(Player player, double amount) {
        return withdraw(player, DEFAULT_BANK_NAME, amount);
    }

    /**
     * Transfers the specified amount from one player's account to another player's account in the same bank.
     *
     * @param from the player to transfer from
     * @param to the player to transfer to
     * @param bankName the name of the bank
     * @param amount the amount to transfer
     * @return true if the transfer was successful, false if the from player doesn't have enough funds,
     *         the amount is negative, the bank doesn't exist, or either player is an op
     */
    public boolean transfer(Player from, Player to, String bankName, double amount) {
        // Ops cannot have bank accounts
        if (from.isOp() || to.isOp()) {
            return false;
        }

        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        if (amount < 0 || !bank.hasAmount(from.getUniqueId(), amount)) {
            return false;
        }

        double fromOldBalance = bank.getBalance(from.getUniqueId());
        double toOldBalance = bank.getBalance(to.getUniqueId());

        boolean fromSuccess = bank.withdraw(from.getUniqueId(), amount);
        boolean toSuccess = bank.deposit(to.getUniqueId(), amount);

        if (fromSuccess && toSuccess) {
            double fromNewBalance = bank.getBalance(from.getUniqueId());
            double toNewBalance = bank.getBalance(to.getUniqueId());

            saveBanks();
            logTransaction(from.getUniqueId(), bankName, "transfer_out", amount, fromNewBalance);
            logTransaction(to.getUniqueId(), bankName, "transfer_in", amount, toNewBalance);

            return true;
        } else {
            // Rollback if either operation failed
            bank.setBalance(from.getUniqueId(), fromOldBalance);
            bank.setBalance(to.getUniqueId(), toOldBalance);
            return false;
        }
    }

    /**
     * Transfers the specified amount from one player's account to another player's account in the default bank.
     *
     * @param from the player to transfer from
     * @param to the player to transfer to
     * @param amount the amount to transfer
     * @return true if the transfer was successful, false if the from player doesn't have enough funds or the amount is negative
     */
    public boolean transfer(Player from, Player to, double amount) {
        return transfer(from, to, DEFAULT_BANK_NAME, amount);
    }

    /**
     * Transfers the specified amount from a player's account in one bank to their account in another bank.
     *
     * @param player the player
     * @param fromBankName the name of the bank to transfer from
     * @param toBankName the name of the bank to transfer to
     * @param amount the amount to transfer
     * @return true if the transfer was successful, false if the player doesn't have enough funds in the from bank,
     *         the amount is negative, either bank doesn't exist, or the player is an op
     */
    public boolean transferBetweenBanks(Player player, String fromBankName, String toBankName, double amount) {
        // Ops cannot have bank accounts
        if (player.isOp()) {
            return false;
        }

        Bank fromBank = banks.get(fromBankName);
        Bank toBank = banks.get(toBankName);

        if (fromBank == null || toBank == null) {
            return false;
        }

        if (amount < 0 || !fromBank.hasAmount(player.getUniqueId(), amount)) {
            return false;
        }

        double fromOldBalance = fromBank.getBalance(player.getUniqueId());
        double toOldBalance = toBank.getBalance(player.getUniqueId());

        boolean fromSuccess = fromBank.withdraw(player.getUniqueId(), amount);
        boolean toSuccess = toBank.deposit(player.getUniqueId(), amount);

        if (fromSuccess && toSuccess) {
            double fromNewBalance = fromBank.getBalance(player.getUniqueId());
            double toNewBalance = toBank.getBalance(player.getUniqueId());

            saveBanks();
            logTransaction(player.getUniqueId(), fromBankName, "transfer_to_bank", amount, fromNewBalance);
            logTransaction(player.getUniqueId(), toBankName, "transfer_from_bank", amount, toNewBalance);

            return true;
        } else {
            // Rollback if either operation failed
            fromBank.setBalance(player.getUniqueId(), fromOldBalance);
            toBank.setBalance(player.getUniqueId(), toOldBalance);
            return false;
        }
    }

    /**
     * Initializes a player's bank accounts when they first join.
     * Creates an account in the default bank with the default initial balance.
     *
     * @param playerId the UUID of the player
     */
    public void initPlayer(UUID playerId) {
        // Check if player is an op
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOp()) {
            return; // Ops cannot have bank accounts
        }

        Bank defaultBank = banks.get(DEFAULT_BANK_NAME);
        if (defaultBank == null) {
            // Create the default bank if it doesn't exist
            defaultBank = new Bank(DEFAULT_BANK_NAME);
            banks.put(DEFAULT_BANK_NAME, defaultBank);
        }

        if (!defaultBank.hasAccount(playerId)) {
            defaultBank.createAccount(playerId, DEFAULT_INITIAL_BALANCE);
            logTransaction(playerId, DEFAULT_BANK_NAME, "init", DEFAULT_INITIAL_BALANCE, DEFAULT_INITIAL_BALANCE);
            saveBanks();
        }
    }

    /**
     * Logs a bank transaction for auditing purposes.
     *
     * @param playerId the UUID of the player involved in the transaction
     * @param bankName the name of the bank
     * @param type the type of transaction (deposit, withdraw, transfer_in, transfer_out, etc.)
     * @param amount the amount involved in the transaction
     * @param newBalance the new balance after the transaction
     */
    private void logTransaction(UUID playerId, String bankName, String type, double amount, double newBalance) {
        plugin.getLogger().info(String.format(
            "Bank Transaction: Player=%s, Bank=%s, Type=%s, Amount=%.2f, NewBalance=%.2f",
            playerId, bankName, type, amount, newBalance
        ));
    }

    /**
     * Gets all banks in the system.
     *
     * @return a map of bank names to Bank objects
     */
    public Map<String, Bank> getBanks() {
        return new HashMap<>(banks);
    }

    /**
     * Renames a bank.
     *
     * @param oldName the current name of the bank
     * @param newName the new name for the bank
     * @return true if the bank was renamed successfully, false if the bank doesn't exist or a bank with the new name already exists
     */
    public boolean renameBank(String oldName, String newName) {
        if (!banks.containsKey(oldName) || banks.containsKey(newName)) {
            return false;
        }

        Bank bank = banks.get(oldName);
        bank.rename(newName);
        banks.remove(oldName);
        banks.put(newName, bank);
        saveBanks();
        return true;
    }

    /**
     * Deletes a bank.
     *
     * @param bankName the name of the bank to delete
     * @return true if the bank was deleted successfully, false if the bank doesn't exist or is the default bank
     */
    public boolean deleteBank(String bankName) {
        if (!banks.containsKey(bankName) || bankName.equals(DEFAULT_BANK_NAME)) {
            return false;
        }

        banks.remove(bankName);
        saveBanks();
        return true;
    }

    /**
     * Deletes an account from a bank.
     *
     * @param bankName the name of the bank
     * @param playerId the UUID of the player whose account should be deleted
     * @return true if the account was deleted successfully, false if the bank doesn't exist or the player doesn't have an account
     */
    public boolean deleteAccount(String bankName, UUID playerId) {
        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        boolean success = bank.deleteAccount(playerId);
        if (success) {
            saveBanks();
            logTransaction(playerId, bankName, "delete_account", 0, 0);
        }
        return success;
    }

    /**
     * Sets the balance of a player's account in the specified bank.
     *
     * @param bankName the name of the bank
     * @param playerId the UUID of the player
     * @param amount the new balance
     * @return true if the balance was set successfully, false if the bank doesn't exist or the amount is negative
     */
    public boolean setBalance(String bankName, UUID playerId, double amount) {
        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        boolean success = bank.setBalance(playerId, amount);
        if (success) {
            saveBanks();
            logTransaction(playerId, bankName, "set_balance", amount, amount);
        }
        return success;
    }

    /**
     * Sets the interest rate of a bank.
     *
     * @param bankName the name of the bank
     * @param interestRate the new interest rate as a decimal (e.g., 0.05 for 5%)
     * @return true if the interest rate was set successfully, false if the bank doesn't exist or the interest rate is negative
     */
    public boolean setInterestRate(String bankName, double interestRate) {
        Bank bank = banks.get(bankName);
        if (bank == null) {
            return false;
        }

        boolean success = bank.setInterestRate(interestRate);
        if (success) {
            saveBanks();
            plugin.getLogger().info(String.format(
                "Bank Interest Rate: Bank=%s, NewRate=%.2f%%",
                bankName, interestRate * 100
            ));
        }
        return success;
    }

    /**
     * Gets the interest rate of a bank.
     *
     * @param bankName the name of the bank
     * @return the interest rate as a decimal, or 0.0 if the bank doesn't exist
     */
    public double getInterestRate(String bankName) {
        Bank bank = banks.get(bankName);
        if (bank == null) {
            return 0.0;
        }
        return bank.getInterestRate();
    }

    /**
     * Applies interest to all accounts in all banks.
     * This should be called after 2 day-cycles.
     */
    public void applyInterest() {
        for (Bank bank : banks.values()) {
            bank.applyInterest();
        }
        saveBanks();
        plugin.getLogger().info("Applied interest to all bank accounts");
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
     * Checks if a chunk is claimed by any bank.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is claimed by any bank, false otherwise
     */
    public boolean isChunkClaimed(Chunk chunk) {
        String chunkKey = formatChunk(chunk);
        for (Bank bank : banks.values()) {
            if (bank.getClaimedChunks().contains(chunkKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the bank that owns a chunk.
     *
     * @param chunk the chunk to check
     * @return the bank that owns the chunk, or null if the chunk is not claimed by any bank
     */
    public Bank getBankByChunk(Chunk chunk) {
        String chunkKey = formatChunk(chunk);
        for (Bank bank : banks.values()) {
            if (bank.getClaimedChunks().contains(chunkKey)) {
                return bank;
            }
        }
        return null;
    }

    /**
     * Claims a chunk for a bank.
     *
     * @param bank the bank to claim the chunk for
     * @param chunk the chunk to claim
     * @param player the player who is claiming the chunk
     * @return true if the chunk was claimed successfully, false if it was already claimed by another bank
     */
    public boolean claimChunk(Bank bank, Chunk chunk, Player player) {
        // Check if the chunk is already claimed by another bank
        Bank owner = getBankByChunk(chunk);
        if (owner != null && !owner.equals(bank)) {
            player.sendMessage("This chunk is already claimed by " + owner.getName() + ".");
            return false;
        }

        // Claim the chunk for the bank
        boolean success = bank.claimChunk(chunk);
        if (success) {
            player.sendMessage("Chunk claimed for " + bank.getName() + ".");
            saveBanks();
        } else {
            player.sendMessage("This chunk is already claimed by " + bank.getName() + ".");
        }

        return success;
    }

    /**
     * Unclaims a chunk from a bank.
     *
     * @param bank the bank to unclaim the chunk from
     * @param chunk the chunk to unclaim
     * @param player the player who is unclaiming the chunk
     * @return true if the chunk was unclaimed successfully, false if it wasn't claimed by the bank
     */
    public boolean unclaimChunk(Bank bank, Chunk chunk, Player player) {
        // Check if the chunk is claimed by the bank
        if (!bank.isChunkClaimed(chunk)) {
            player.sendMessage("This chunk is not claimed by " + bank.getName() + ".");
            return false;
        }

        // Unclaim the chunk from the bank
        boolean success = bank.unclaimChunk(chunk);
        if (success) {
            player.sendMessage("Chunk unclaimed from " + bank.getName() + ".");
            saveBanks();
        }

        return success;
    }
}
