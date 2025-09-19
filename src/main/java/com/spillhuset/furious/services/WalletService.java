package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class WalletService {
    private final Furious plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    public FileConfiguration accountsConfig;
    private FileConfiguration transactionsConfig;
    private File accountsFile;
    private File transactionsFile;

    // batching fields
    private final Set<UUID> dirtyBalances = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean transactionsDirty = new AtomicBoolean(false);
    private final Object configIoLock = new Object(); // guard config read/writes against async save

    // scheduler
    private BukkitTask flushTask;
    private long flushIntervalTicks = 20L * 5; // default 5s
    private int flushThreshold = 50; // number of dirty balances that triggers immediate flush
    private int flushBackoffTicks = 2; // debounce for immediate flush requests
    private long lastFlushTick = 0L;

    // Currency presentation configured via plugin config
    public static String SYMBOL = "⚙";
    public static String NAME = "scrap";
    public static String NAME_PLURAL = "scraps";
    private double INIT_BALANCE = 0.0;

    public WalletService(Furious instance) {
        this.plugin = instance;

        SYMBOL = instance.getConfig().getString("wallet.symbol", "⚙");
        NAME = instance.getConfig().getString("wallet.name", "scrap");
        NAME_PLURAL = instance.getConfig().getString("wallet.name_plural", "scraps");
        INIT_BALANCE = Math.max(INIT_BALANCE, instance.getConfig().getDouble("wallet.initial-balance", 1000.0));

        // Optional: make interval configurable
        flushIntervalTicks = instance.getConfig().getLong("wallet.flush-interval-ticks", flushIntervalTicks);
        flushThreshold = Math.max(1, instance.getConfig().getInt("wallet.flush-threshold", flushThreshold));

        // Persist any missing wallet.* keys to the active config without overwriting existing values
        ensureWalletDefaultsPersisted(instance);
    }

    private void ensureWalletDefaultsPersisted(Furious instance) {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = instance.getConfig();
            boolean changed = false;
            if (!cfg.isSet("wallet.symbol")) { cfg.set("wallet.symbol", SYMBOL != null ? SYMBOL : "⚙"); changed = true; }
            if (!cfg.isSet("wallet.name")) { cfg.set("wallet.name", NAME != null ? NAME : "scrap"); changed = true; }
            if (!cfg.isSet("wallet.name_plural")) { cfg.set("wallet.name_plural", NAME_PLURAL != null ? NAME_PLURAL : "scraps"); changed = true; }
            if (!cfg.isSet("wallet.initial-balance")) { cfg.set("wallet.initial-balance", INIT_BALANCE > 0 ? INIT_BALANCE : 1000.0); changed = true; }
            if (!cfg.isSet("wallet.flush-interval-ticks")) { cfg.set("wallet.flush-interval-ticks", flushIntervalTicks > 0 ? flushIntervalTicks : 100L); changed = true; }
            if (!cfg.isSet("wallet.flush-threshold")) { cfg.set("wallet.flush-threshold", Math.max(1, flushThreshold)); changed = true; }
            if (changed) instance.saveConfig();
        } catch (Throwable ignored) {}
    }

    public List<OfflinePlayer> getAccountNames() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (UUID uuid : balances.keySet()) {
            OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
            players.add(player);
        }
        return players;
    }

    public void load() {
        // Configuration files
        accountsFile = new File(plugin.getDataFolder(), "accounts.yml");
        transactionsFile = new File(plugin.getDataFolder(), "transactions.yml");

        try {
            if (!accountsFile.exists()) accountsFile.createNewFile();
            if (!transactionsFile.exists()) transactionsFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating wallet files: " + e.getMessage());
        }

        synchronized (configIoLock) {
            accountsConfig = YamlConfiguration.loadConfiguration(accountsFile);
            transactionsConfig = YamlConfiguration.loadConfiguration(transactionsFile);
        }

        // Balance map
        balances.clear();

        Set<String> keys;
        synchronized (configIoLock) {
            keys = accountsConfig.getKeys(false);
        }

        for (String key : keys) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance;
                synchronized (configIoLock) {
                    balance = accountsConfig.getDouble(key);
                }
                balances.put(uuid, balance);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed loading balance for UUID " + key + ": " + e.getMessage());
            }
        }
        dirtyBalances.clear();
        transactionsDirty.set(false);

        startAutoFlush();
    }

    private void markBalanceDirty(UUID uuid) {
        dirtyBalances.add(uuid);
        maybeTriggerImmediateFlush();
    }

    private void markTransactionsDirty() {
        transactionsDirty.set(true);
        maybeTriggerImmediateFlush();
    }

    private void maybeTriggerImmediateFlush() {
        // If many changes queued, request an earlier flush (still debounced)
        if (dirtyBalances.size() >= flushThreshold || transactionsDirty.get()) {
            // Debounce immediate flush to avoid spamming
            long nowTick = plugin.getServer().getCurrentTick();
            if (nowTick - lastFlushTick >= flushBackoffTicks) {
                // schedule one-off async flush
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::flushOnce);
                lastFlushTick = nowTick;
            }
        }
    }

    public void save() {
        // For compatibility: write the entire map and transactions at once
        synchronized (configIoLock) {
            for (Map.Entry<UUID, Double> e : balances.entrySet()) {
                accountsConfig.set(e.getKey().toString(), e.getValue());
            }
        }
        // Mark dirty and flush
        dirtyBalances.addAll(balances.keySet());
        transactionsDirty.set(true);
        flushNow();
    }

    public double getBalance(UUID uuid) {
        Double balance = balances.get(uuid);
        if (balance == null) {
            double init = INIT_BALANCE;
            balances.put(uuid, init);
            log(uuid, "INITIAL set to " + init);
            markBalanceDirty(uuid);
            markTransactionsDirty();
            return init;
        }
        return balance;
    }

    public void log(UUID player, String entry) {
        String line = timestamp() + " " + entry;
        String path = "players." + player;
        synchronized (configIoLock) {
            List<String> list = transactionsConfig.getStringList(path);
            list.add(line);
            transactionsConfig.set(path, list);
        }
    }

    private String timestamp() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

    public String formatAmount(double amount) {
        String unit = Math.abs(amount) == 1 ? NAME : NAME_PLURAL;
        return amount + " " + unit + " " + SYMBOL;
    }

    public void setBalance(UUID uuid, double amount, String reason) {
        double sanitized = amount;
        if (!Double.isFinite(sanitized) || sanitized < 0) {
            sanitized = 0.0;
        }
        balances.put(uuid, sanitized);
        log(uuid, "SET to " + sanitized + " due to: " + reason);
        markBalanceDirty(uuid);
        markTransactionsDirty();
    }


    public boolean addBalance(UUID uuid, double amount, String reason) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        double newBal = getBalance(uuid) + amount;
        if (!Double.isFinite(newBal)) return false;
        balances.put(uuid, newBal);
        log(uuid, "+" + amount + " (add) due to: " + reason);
        markBalanceDirty(uuid);
        markTransactionsDirty();
        return true;
    }

    public boolean subBalance(UUID uuid, double amount, String reason) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        double cur = getBalance(uuid);
        if (cur < amount) return false;
        double newBal = cur - amount;
        if (!Double.isFinite(newBal) || newBal < 0) return false;
        balances.put(uuid, newBal);
        log(uuid, "-" + amount + " (sub) due to: " + reason);
        markBalanceDirty(uuid);
        markTransactionsDirty();
        return true;
    }

    public boolean pay(UUID from, UUID to, double amount) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return false;
        if (!Double.isFinite(amount) || amount <= 0) return false;
        double fromBal = getBalance(from);
        if (fromBal < amount) return false;
        double toBal = getBalance(to);
        double newFrom = fromBal - amount;
        double newTo = toBal + amount;
        if (!Double.isFinite(newFrom) || !Double.isFinite(newTo) || newFrom < 0) return false;
        balances.put(from, newFrom);
        balances.put(to, newTo);
        log(from, "PAID -" + amount + " to " + to);
        log(to, "RECEIVED +" + amount + " from " + from);
        logGlobal("PAY " + amount + " from " + from + " to " + to);
        markBalanceDirty(from);
        markBalanceDirty(to);
        markTransactionsDirty();
        return true;
    }

    public void logGlobal(String entry) {
        String line = timestamp() + " " + entry;
        synchronized (configIoLock) {
            List<String> list = transactionsConfig.getStringList("global");
            list.add(line);
            transactionsConfig.set("global", list);
        }
    }

    public void startAutoFlush() {
        stopAutoFlush();
        if (flushIntervalTicks <= 0) return;
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushOnce, flushIntervalTicks, flushIntervalTicks);
    }

    public void stopAutoFlush() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    public void flushNow() {
        // Synchronous call point that ensures flush completes before return
        // (but perform actual IO asynchronously then wait). Simpler: run on current thread.
        // We can safely perform direct save here on calling thread if not on main thread.
        flushOnce();
    }

    private void flushOnce() {
        // snapshot and write minimal changes
        Set<UUID> toWrite = new HashSet<>(dirtyBalances);
        boolean writeTransactions = transactionsDirty.getAndSet(false);

        if (toWrite.isEmpty() && !writeTransactions) return;

        // Write accounts entries for dirty users into accountsConfig
        synchronized (configIoLock) {
            for (UUID uuid : toWrite) {
                double bal = balances.getOrDefault(uuid,0.0);
                accountsConfig.set(uuid.toString(), bal);
            }
            try {
                if (!toWrite.isEmpty()) {
                    accountsConfig.save(accountsFile);
                }
                if (writeTransactions) {
                    transactionsConfig.save(transactionsFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed saving data: " + e.getMessage());
                // If save failed, mark them dirty again so we retry next tick
                dirtyBalances.addAll(toWrite);
                transactionsDirty.set(writeTransactions || transactionsDirty.get());
                return;
            }
        }

        // Only clear dirty balances that were successfully saved
        dirtyBalances.removeAll(toWrite);
        lastFlushTick = plugin.getServer().getCurrentTick();
    }

    public List<String> getLog(UUID player) {
        return transactionsConfig.getStringList("players." + player);
    }

    public List<String> getGlobalLog() {
        return transactionsConfig.getStringList("global");
    }

    public void saveAccounts() {
        try {
            accountsConfig.save(accountsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving accounts.yml: " + e.getMessage());
        }
    }

    public void saveTransactions() {
        try {
            transactionsConfig.save(transactionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving transactions.yml: " + e.getMessage());
        }
    }

    public @Nullable Double parseAmount(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            double val = Double.parseDouble(s);
            if (!Double.isFinite(val)) return null; // reject NaN/Infinity
            if (val < 0) return null; // reject negative amounts
            return val;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
