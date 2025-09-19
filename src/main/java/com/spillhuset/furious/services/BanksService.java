package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BanksService {
    private final Furious plugin;

    private final Map<UUID, Bank> banksById = new HashMap<>();
    private final Map<String, UUID> bankIdByName = new HashMap<>();
    // player -> (bankId -> account)
    private final Map<UUID, Map<UUID, BankAccount>> accounts = new HashMap<>();
    // last accrual time per bank (millis since epoch)
    private final Map<UUID, Long> lastAccrualByBank = new HashMap<>();
    private Integer interestTaskId = null;

    private File banksFile;
    private FileConfiguration banksCfg;

    public BanksService(Furious plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // If SQL database is enabled, load from DB; otherwise, use YAML file
        if (plugin.databaseManager != null && plugin.databaseManager.isEnabled()) {
            try {
                com.spillhuset.furious.db.BanksRepository repo = new com.spillhuset.furious.db.BanksRepository(plugin.databaseManager.getDataSource());
                repo.initSchema();
                banksById.clear();
                bankIdByName.clear();
                accounts.clear();
                lastAccrualByBank.clear();
                repo.loadAll(banksById, bankIdByName, accounts, lastAccrualByBank);
                return;
            } catch (Exception e) {
                try { plugin.getLogger().warning("Banks: failed to load from database, falling back to YAML: " + e.getMessage()); } catch (Throwable ignored) {}
            }
        }
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        banksFile = new File(dataFolder, "banks.yml");
        if (!banksFile.exists()) {
            try {
                banksFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        banksCfg = YamlConfiguration.loadConfiguration(banksFile);

        banksById.clear();
        bankIdByName.clear();
        accounts.clear();
        lastAccrualByBank.clear();

        ConfigurationSection banksSec = banksCfg.getConfigurationSection("banks");
        if (banksSec != null) {
            for (String idStr : banksSec.getKeys(false)) {
                UUID id = UUID.fromString(idStr);
                ConfigurationSection b = banksSec.getConfigurationSection(idStr);
                if (b == null) continue;
                String name = b.getString("name");
                Bank bank = new Bank(id, name);
                bank.setInterest(b.getDouble("interest", 0.0d));
                String typeStr = b.getString("type", "PLAYER");
                try {
                    bank.setType(BankType.valueOf(typeStr.toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
                bank.setOpen(b.getBoolean("open", true));
                // Load multiple claims if present; otherwise, load legacy single fields
                ConfigurationSection claimsSec = b.getConfigurationSection("claims");
                if (claimsSec != null) {
                    for (String key : claimsSec.getKeys(false)) {
                        ConfigurationSection c = claimsSec.getConfigurationSection(key);
                        if (c == null) continue;
                        String wStr = c.getString("world");
                        if (wStr == null || wStr.isBlank()) continue;
                        try {
                            UUID wid = UUID.fromString(wStr);
                            int cx = c.getInt("chunkX");
                            int cz = c.getInt("chunkZ");
                            bank.addClaim(wid, cx, cz);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                } else {
                    String worldStr = b.getString("world");
                    if (worldStr != null && !worldStr.isBlank()) {
                        UUID worldId = UUID.fromString(worldStr);
                        int cx = b.getInt("chunkX");
                        int cz = b.getInt("chunkZ");
                        bank.claim(worldId, cx, cz);
                    }
                }
                String armorStandStr = b.getString("armorStand");
                if (armorStandStr != null && !armorStandStr.isBlank()) {
                    try {
                        bank.setArmorStandUuid(UUID.fromString(armorStandStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                long last = b.getLong("lastAccrual", 0L);
                lastAccrualByBank.put(id, last);
                banksById.put(id, bank);
                if (name != null) bankIdByName.put(name.toLowerCase(), id);
            }
        }

        ConfigurationSection accSec = banksCfg.getConfigurationSection("accounts");
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
    }

    public void save() {
        // If SQL database is enabled, persist to DB snapshot and return; else use YAML
        if (plugin.databaseManager != null && plugin.databaseManager.isEnabled()) {
            try {
                com.spillhuset.furious.db.BanksRepository repo = new com.spillhuset.furious.db.BanksRepository(plugin.databaseManager.getDataSource());
                repo.saveAll(new ArrayList<>(banksById.values()), accounts, lastAccrualByBank);
                return;
            } catch (Exception e) {
                try { plugin.getLogger().warning("Banks: failed to save to database: " + e.getMessage()); } catch (Throwable ignored) {}
                // If save to DB fails, we still attempt YAML to not lose data
            }
        }
        banksCfg.set("banks", null);
        banksCfg.set("accounts", null);
        for (Bank bank : banksById.values()) {
            String path = "banks." + bank.getId();
            banksCfg.set(path + ".name", bank.getName());
            banksCfg.set(path + ".interest", bank.getInterest());
            banksCfg.set(path + ".type", bank.getType().name());
            banksCfg.set(path + ".open", bank.isOpen());
            if (bank.isClaimed()) {
                // Write legacy first-claim fields for backward compatibility
                banksCfg.set(path + ".world", bank.getWorldId().toString());
                banksCfg.set(path + ".chunkX", bank.getChunkX());
                banksCfg.set(path + ".chunkZ", bank.getChunkZ());
                // Also write claims list
                banksCfg.set(path + ".claims", null);
                int idx = 0;
                for (com.spillhuset.furious.utils.Bank.Claim c : bank.getClaims()) {
                    String cPath = path + ".claims." + (idx++);
                    banksCfg.set(cPath + ".world", c.worldId.toString());
                    banksCfg.set(cPath + ".chunkX", c.chunkX);
                    banksCfg.set(cPath + ".chunkZ", c.chunkZ);
                }
            }
            if (bank.getArmorStandUuid() != null) {
                banksCfg.set(path + ".armorStand", bank.getArmorStandUuid().toString());
            }
            Long last = lastAccrualByBank.get(bank.getId());
            if (last != null && last > 0L) {
                banksCfg.set(path + ".lastAccrual", last);
            }
        }
        for (Map.Entry<UUID, Map<UUID, BankAccount>> e : accounts.entrySet()) {
            UUID pid = e.getKey();
            for (BankAccount acct : e.getValue().values()) {
                banksCfg.set("accounts." + pid + "." + acct.getBankId(), acct.getBalance());
            }
        }
        try {
            banksCfg.save(banksFile);
        } catch (IOException ignored) {
        }
    }

    public Set<String> getBankNames() {
        Set<String> names = new HashSet<>();
        for (Bank b : banksById.values()) {
            if (b != null && b.getName() != null) names.add(b.getName());
        }
        return names;
    }

    public Collection<Bank> getBanks() {
        return new ArrayList<>(banksById.values());
    }

    public Bank getBankByName(String name) {
        if (name == null) return null;
        UUID id = bankIdByName.get(name.toLowerCase());
        return id == null ? null : banksById.get(id);
    }

    public boolean setType(CommandSender sender, String bankName, BankType type) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        if (type == null) {
            Components.sendErrorMessage(sender, "Invalid bank type.");
            return false;
        }
        bank.setType(type);
        save();
        Components.sendSuccess(sender, Components.t("Bank "), Components.valueComp(bank.getName()), Components.t(" type set to "), Components.valueComp(type.name().toLowerCase()), Components.t("."));
        return true;
    }

    public boolean createBank(CommandSender sender, String name) {
        if (name == null || name.isBlank()) return false;
        if (bankIdByName.containsKey(name.toLowerCase())) {
            Components.sendErrorMessage(sender, "A bank with that name already exists.");
            return false;
        }
        Bank bank = new Bank(UUID.randomUUID(), name);
        banksById.put(bank.getId(), bank);
        bankIdByName.put(name.toLowerCase(), bank.getId());
        save();
        Components.sendSuccess(sender, Components.t("Bank "), Components.valueComp(name), Components.t(" created."));
        return true;
    }

    public boolean renameBank(CommandSender sender, String oldName, String newName) {
        Bank bank = getBankByName(oldName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        if (newName == null || newName.isBlank()) {
            Components.sendErrorMessage(sender, "New name required.");
            return false;
        }
        if (bankIdByName.containsKey(newName.toLowerCase())) {
            Components.sendErrorMessage(sender, "Name already in use.");
            return false;
        }
        bankIdByName.remove(oldName.toLowerCase());
        bank.setName(newName);
        bankIdByName.put(newName.toLowerCase(), bank.getId());
        save();
        Components.sendSuccess(sender, Components.t("Bank renamed to "), Components.valueComp(newName), Components.t(" from "), Components.valueComp(oldName), Components.t("."));
        return true;
    }

    public boolean deleteBank(CommandSender sender, String name) {
        Bank bank = getBankByName(name);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        banksById.remove(bank.getId());
        bankIdByName.remove(bank.getName().toLowerCase());
        // Remove all accounts for this bank
        for (Map<UUID, BankAccount> map : accounts.values()) {
            map.remove(bank.getId());
        }
        save();
        Components.sendSuccess(sender, Components.t("Bank "), Components.valueComp(name), Components.t(" deleted."));
        return true;
    }

    public enum ClaimCheck {OK, NOT_IN_CLAIM, WRONG_GUILD_TYPE}

    public ClaimCheck canClaimHere(Location loc) {
        if (loc == null) return ClaimCheck.NOT_IN_CLAIM;
        UUID worldId = loc.getWorld() == null ? null : loc.getWorld().getUID();
        if (worldId == null) return ClaimCheck.NOT_IN_CLAIM;
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        UUID ownerGid = plugin.guildService.getClaimOwner(worldId, cx, cz);
        if (ownerGid == null) return ClaimCheck.NOT_IN_CLAIM;
        Guild g = plugin.guildService.getGuildById(ownerGid);
        if (g == null) return ClaimCheck.NOT_IN_CLAIM;
        GuildType type = g.getType();
        if (type == GuildType.SAFE || type == GuildType.WAR) return ClaimCheck.OK;
        return ClaimCheck.WRONG_GUILD_TYPE;
    }

    public boolean claimBank(CommandSender sender, String bankName, Location loc) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        ClaimCheck check = canClaimHere(loc);
        if (check != ClaimCheck.OK) {
            switch (check) {
                case NOT_IN_CLAIM -> Components.sendErrorMessage(sender, "This chunk is not claimed by a guild.");
                case WRONG_GUILD_TYPE ->
                        Components.sendErrorMessage(sender, "Bank claims must be inside SAFE or WAR guild.");
            }
            return false;
        }
        Chunk chunk = loc.getChunk();
        // Add additional claim (do not replace existing)
        if (bank.hasClaimAt(loc.getWorld().getUID(), chunk.getX(), chunk.getZ())) {
            Components.sendErrorMessage(sender, "This chunk is already claimed by this bank.");
            return false;
        }
        bank.addClaim(loc.getWorld().getUID(), chunk.getX(), chunk.getZ());
        save();
        Components.sendSuccess(sender, Components.t("Bank "), Components.valueComp(bank.getName()), Components.t(" claimed at this chunk."));
        return true;
    }

    public boolean unclaimBank(CommandSender sender, String bankName) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        // Remove armor stand (spawn) when bank is unclaimed
        removeArmorStandForBank(bank);
        bank.unclaim();
        save();
        Components.sendSuccess(sender, Components.t("Bank "), Components.valueComp(bank.getName()), Components.t(" unclaimed."));
        return true;
    }

    public boolean setInterest(CommandSender sender, String bankName, double interest) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        if (interest < 0) {
            Components.sendErrorMessage(sender, "Interest must be >= 0.");
            return false;
        }
        bank.setInterest(interest);
        save();
        Components.sendSuccess(sender, Components.t("Interest set to "), Components.valueComp(String.valueOf(interest)), Components.t("% for bank "), Components.valueComp(bank.getName()), Components.t("."));
        return true;
    }

    public boolean setOpen(CommandSender sender, String bankName, boolean open) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return false;
        }
        bank.setOpen(open);
        save();
        Components.sendSuccess(sender,
                Components.t("Bank "), Components.valueComp(bank.getName()),
                Components.t(open ? " is now open." : " is now closed."));
        return true;
    }

    private boolean isPlayerAtBank(Player player, Bank bank) {
        if (player == null || bank == null || !bank.isClaimed()) return false;
        Chunk pc = player.getLocation().getChunk();
        UUID wid = player.getWorld() == null ? null : player.getWorld().getUID();
        return bank.hasClaimAt(wid, pc.getX(), pc.getZ());
    }

    public boolean createAccount(Player player, String bankName) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(player, "Bank not found.");
            return false;
        }
        if (!bank.isClaimed()) {
            Components.sendErrorMessage(player, "This bank is not claimed yet.");
            return false;
        }
        if (!isPlayerAtBank(player, bank)) {
            Components.sendErrorMessage(player, "You must be at the bank's claimed chunk to do this.");
            return false;
        }
        if (!bank.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This bank is currently closed.");
            return false;
        }
        UUID pid = player.getUniqueId();
        Map<UUID, BankAccount> map = accounts.computeIfAbsent(pid, x -> new HashMap<>());
        if (map.containsKey(bank.getId())) {
            Components.sendErrorMessage(player, "You already have an account at this bank.");
            return false;
        }
        map.put(bank.getId(), new BankAccount(bank.getId(), pid));
        save();
        Components.sendSuccess(player, Components.t("Account created at bank "), Components.valueComp(bank.getName()), Components.t("."));
        return true;
    }

    public boolean deleteAccount(Player player, String bankName) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(player, "Bank not found.");
            return false;
        }
        if (!isPlayerAtBank(player, bank)) {
            Components.sendErrorMessage(player, "You must be at the bank's claimed chunk to do this.");
            return false;
        }
        if (!bank.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This bank is currently closed.");
            return false;
        }
        UUID pid = player.getUniqueId();
        Map<UUID, BankAccount> map = accounts.get(pid);
        if (map == null || !map.containsKey(bank.getId())) {
            Components.sendErrorMessage(player, "You don't have an account at this bank.");
            return false;
        }
        BankAccount acct = map.get(bank.getId());
        if (acct.getBalance() > 0.0d) {
            Components.sendErrorMessage(player, "Withdraw your funds before deleting the account.");
            return false;
        }
        map.remove(bank.getId());
        save();
        Components.sendSuccess(player, Components.t("Account deleted at bank "), Components.valueComp(bank.getName()), Components.t("."));
        return true;
    }

    public boolean deposit(Player player, String bankName, double amount) {
        if (amount <= 0) {
            Components.sendErrorMessage(player, "Amount must be positive.");
            return false;
        }
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(player, "Bank not found.");
            return false;
        }
        if (!isPlayerAtBank(player, bank)) {
            Components.sendErrorMessage(player, "You must be at the bank's claimed chunk to do this.");
            return false;
        }
        if (!bank.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This bank is currently closed.");
            return false;
        }
        UUID pid = player.getUniqueId();
        Map<UUID, BankAccount> map = accounts.computeIfAbsent(pid, x -> new HashMap<>());
        BankAccount acct = map.get(bank.getId());
        if (acct == null) {
            Components.sendErrorMessage(player, "You don't have an account at this bank.");
            return false;
        }
        // subtract from wallet and add to bank account
        boolean ok = plugin.walletService.subBalance(pid, amount, "Deposit to bank '" + bank.getName() + "'");
        if (!ok) {
            Components.sendErrorMessage(player, "Insufficient wallet balance.");
            return false;
        }
        acct.deposit(amount);
        save();
        Components.sendSuccess(player, Components.t("Deposited "), Components.valueComp(plugin.walletService.formatAmount(amount)), Components.t(" to "), Components.valueComp(bank.getName()), Components.t(". New bank balance: "), Components.valueComp(plugin.walletService.formatAmount(acct.getBalance())));
        return true;
    }

    public boolean withdraw(Player player, String bankName, double amount) {
        if (amount <= 0) {
            Components.sendErrorMessage(player, "Amount must be positive.");
            return false;
        }
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(player, "Bank not found.");
            return false;
        }
        if (!isPlayerAtBank(player, bank)) {
            Components.sendErrorMessage(player, "You must be at the bank's claimed chunk to do this.");
            return false;
        }
        if (!bank.isOpen() && (player == null || !player.isOp())) {
            Components.sendErrorMessage(player, "This bank is currently closed.");
            return false;
        }
        UUID pid = player.getUniqueId();
        Map<UUID, BankAccount> map = accounts.get(pid);
        if (map == null) {
            Components.sendErrorMessage(player, "You don't have an account at this bank.");
            return false;
        }
        BankAccount acct = map.get(bank.getId());
        if (acct == null) {
            Components.sendErrorMessage(player, "You don't have an account at this bank.");
            return false;
        }
        if (!acct.withdraw(amount)) {
            Components.sendErrorMessage(player, "Insufficient bank balance.");
            return false;
        }
        plugin.walletService.addBalance(pid, amount, "Withdraw from bank '" + bank.getName() + "'");
        save();
        Components.sendSuccess(player, Components.t("Withdrew "), Components.valueComp(plugin.walletService.formatAmount(amount)), Components.t(" from "), Components.valueComp(bank.getName()), Components.t(". New bank balance: "), Components.valueComp(plugin.walletService.formatAmount(acct.getBalance())));
        return true;
    }

    public List<String> suggestBankNames(String prefix) {
        List<String> list = new ArrayList<>();
        String p = prefix == null ? "" : prefix.toLowerCase();
        for (Bank b : banksById.values()) {
            if (b == null || b.getName() == null) continue;
            String name = b.getName();
            if (p.isBlank() || name.toLowerCase().startsWith(p)) list.add(name);
        }
        return list;
    }

    // Find the bank that has claimed the chunk at the given location, if any
    public Bank getBankAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        UUID worldId = loc.getWorld().getUID();
        int cx = loc.getChunk().getX();
        int cz = loc.getChunk().getZ();
        for (Bank bank : banksById.values()) {
            if (bank != null && bank.isClaimed()) {
                if (bank.hasClaimAt(worldId, cx, cz)) {
                    return bank;
                }
            }
        }
        return null;
    }

    // Spawn an ArmorStand at a specific location for the given bank
    public boolean spawnArmorStandForBank(Bank bank, Location location) {
        if (bank == null || location == null || location.getWorld() == null) return false;
        try {
            java.util.UUID id = plugin.armorStandManager.create(location, "Bank: " + bank.getName());
            if (id != null) {
                bank.setArmorStandUuid(id);
                registerAndSetupArmorStand(id, bank);
                save();
                return true;
            }
            return false;
        } catch (Throwable t) {
            try {
                plugin.getLogger().warning("Failed to spawn ArmorStand for bank: " + t.getMessage());
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    // Remove the bank's armor stand if it exists and clear reference
    public boolean removeArmorStandForBank(Bank bank) {
        if (bank == null) return false;
        java.util.UUID asId = bank.getArmorStandUuid();
        if (asId == null) return false;
        try {
            org.bukkit.entity.Entity ent = plugin.getServer().getEntity(asId);
            if (ent != null) {
                try {
                    ent.remove();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        bank.setArmorStandUuid(null);
        save();
        return true;
    }

    // Helper: compute center location of the first claimed chunk for a bank (no yaw/pitch)
    private org.bukkit.Location firstClaimCenterLocation(Bank bank, org.bukkit.World world) {
        if (bank == null || world == null || !bank.isClaimed()) return null;
        Integer cX = bank.getChunkX();
        Integer cZ = bank.getChunkZ();
        if (cX == null || cZ == null) return null;
        int bx = cX * 16 + 7;
        int bz = cZ * 16 + 7;
        int by;
        try {
            by = world.getHighestBlockYAt(bx, bz) + 1;
        } catch (Throwable ignored) {
            by = world.getSpawnLocation().getBlockY();
        }
        return new org.bukkit.Location(world, bx + 0.5, by, bz + 0.5);
    }

    // Helper: set stand name and per-viewer visibility for a bank
    private void updateArmorStandNameAndVisibility(org.bukkit.entity.ArmorStand stand, Bank bank) {
        if (stand == null || bank == null) return;
        try {
            stand.customName(net.kyori.adventure.text.Component.text("Bank: " + bank.getName()));
        } catch (Throwable ignored) {
        }
        try {
            stand.setCustomNameVisible(true);
        } catch (Throwable ignored) {
        }
        try {
            for (org.bukkit.entity.Player viewer : plugin.getServer().getOnlinePlayers()) {
                if (viewer.isOp()) viewer.showEntity(plugin, stand);
                else viewer.hideEntity(plugin, stand);
            }
        } catch (Throwable ignored) {
        }
    }

    // Helper: register armor stand with removal callback and update visuals
    private void registerAndSetupArmorStand(java.util.UUID id, Bank bank) {
        if (id == null || bank == null) return;
        try {
            plugin.armorStandManager.register(id, () -> removeArmorStandForBank(bank));
        } catch (Throwable ignored) {
        }
        try {
            org.bukkit.entity.Entity ent2 = plugin.getServer().getEntity(id);
            if (ent2 instanceof org.bukkit.entity.ArmorStand st) {
                updateArmorStandNameAndVisibility(st, bank);
            }
        } catch (Throwable ignored) {
        }
    }

    public void teleportToBank(org.bukkit.entity.Player player, String bankName) {
        Bank bank = getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(player, "Bank not found.");
            return;
        }
        if (!bank.isClaimed()) {
            Components.sendErrorMessage(player, "Bank is not claimed.");
            return;
        }
        // Prefer teleporting to the bank's ArmorStand if it exists (anchor set via /banks spawn)
        try {
            java.util.UUID asId = bank.getArmorStandUuid();
            if (asId != null) {
                org.bukkit.entity.Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof org.bukkit.entity.ArmorStand) {
                    org.bukkit.Location loc = ent.getLocation();
                    // Preserve player's look direction
                    org.bukkit.Location target = new org.bukkit.Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
                    plugin.teleportsService.queueTeleport(player, target, "Bank: " + bank.getName());
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback: center of the first claimed chunk
        org.bukkit.World world = plugin.getServer().getWorld(bank.getWorldId());
        if (world == null) {
            Components.sendErrorMessage(player, "Bank world is not loaded.");
            return;
        }
        org.bukkit.Location base = firstClaimCenterLocation(bank, world);
        if (base == null) {
            Components.sendErrorMessage(player, "Bank has no claimed chunks.");
            return;
        }
        org.bukkit.Location target = new org.bukkit.Location(base.getWorld(), base.getX(), base.getY(), base.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
        plugin.teleportsService.queueTeleport(player, target, "Bank: " + bank.getName());
    }

    // Expose a read-only snapshot of a player's bank accounts as bankName -> balance
    public Map<String, Double> getAccountsBalances(UUID playerId) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (playerId == null) return result;
        Map<UUID, BankAccount> map = accounts.get(playerId);
        if (map == null) return result;
        for (Map.Entry<UUID, BankAccount> e : map.entrySet()) {
            UUID bankId = e.getKey();
            BankAccount acct = e.getValue();
            Bank bank = banksById.get(bankId);
            String name = bank != null && bank.getName() != null ? bank.getName() : String.valueOf(bankId);
            result.put(name, acct.getBalance());
        }
        return result;
    }

    public void applyBankArmorStandVisibilityForViewer(org.bukkit.entity.Player viewer) {
        if (viewer == null) return;
        try {
            for (Bank bank : new java.util.ArrayList<>(banksById.values())) {
                java.util.UUID asId = bank.getArmorStandUuid();
                if (asId == null) continue;
                org.bukkit.entity.Entity ent = plugin.getServer().getEntity(asId);
                if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                    if (viewer.isOp()) viewer.showEntity(plugin, stand);
                    else viewer.hideEntity(plugin, stand);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Attempt to adopt an unreferenced, managed ArmorStand into a matching Bank by proximity to its claimed-chunk center.
     */
    public boolean adoptArmorStand(org.bukkit.entity.ArmorStand stand) {
        if (stand == null || stand.getWorld() == null) return false;
        org.bukkit.Location sLoc = stand.getLocation();
        try {
            for (Bank bank : new java.util.ArrayList<>(banksById.values())) {
                if (!bank.isClaimed()) continue;
                org.bukkit.World w = plugin.getServer().getWorld(bank.getWorldId());
                if (w == null) continue;
                org.bukkit.Location center = firstClaimCenterLocation(bank, w);
                if (center == null || !w.equals(sLoc.getWorld())) continue;
                if (center.distanceSquared(sLoc) <= 4.0) {
                    if (stand.getUniqueId().equals(bank.getArmorStandUuid())) return true;
                    bank.setArmorStandUuid(stand.getUniqueId());
                    registerAndSetupArmorStand(stand.getUniqueId(), bank);
                    save();
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public boolean hasArmorStand(java.util.UUID armorStandId) {
        if (armorStandId == null) return false;
        for (Bank bank : new java.util.ArrayList<>(banksById.values())) {
            if (armorStandId.equals(bank.getArmorStandUuid())) return true;
        }
        return false;
    }

    public void ensureArmorStands() {
        boolean changed = false;
        for (Bank bank : new ArrayList<>(banksById.values())) {
            if (bank == null) continue;
            UUID asId = bank.getArmorStandUuid();
            org.bukkit.entity.Entity ent = (asId != null) ? plugin.getServer().getEntity(asId) : null;
            if (!(ent instanceof org.bukkit.entity.ArmorStand stand)) {
                // Try to respawn at claimed chunk center if possible
                if (!bank.isClaimed()) continue;
                org.bukkit.World world = plugin.getServer().getWorld(bank.getWorldId());
                if (world == null) continue;
                // Respawn marker at the true center of the first claimed chunk
                Integer cX = bank.getChunkX();
                Integer cZ = bank.getChunkZ();
                if (cX == null || cZ == null) continue;
                int bx = cX * 16 + 7;
                int bz = cZ * 16 + 7;
                int by;
                try {
                    by = world.getHighestBlockYAt(bx, bz) + 1;
                } catch (Throwable ignored) {
                    by = world.getSpawnLocation().getBlockY();
                }
                org.bukkit.Location loc = new org.bukkit.Location(world, bx + 0.5, by, bz + 0.5);
                try {
                    java.util.UUID id = plugin.armorStandManager.create(loc, "Bank: " + bank.getName());
                    if (id != null) {
                        bank.setArmorStandUuid(id);
                        registerAndSetupArmorStand(id, bank);
                        changed = true;
                    }
                } catch (Throwable t) {
                    try {
                        plugin.getLogger().warning("Failed to respawn ArmorStand for bank: " + t.getMessage());
                    } catch (Throwable ignored) {
                    }
                }
            } else {
                // Ensure registered and update name/visibility
                registerAndSetupArmorStand(stand.getUniqueId(), bank);
            }
        }
        if (changed) save();
    }

    // ========= Interest Accrual =========
    private long getNow() { return System.currentTimeMillis(); }
    private static final long INTEREST_PERIOD_MILLIS = 24L * 60L * 60L * 1000L; // daily

    // Accrue interest for all banks for any full periods elapsed since last accrual
    public void accrueInterestNow() {
        long now = getNow();
        boolean changed = false;
        for (Bank bank : new ArrayList<>(banksById.values())) {
            if (bank == null) continue;
            double ratePct = Math.max(0d, bank.getInterest());
            if (ratePct <= 0d) continue; // no interest set
            long last = lastAccrualByBank.getOrDefault(bank.getId(), 0L);
            long elapsed = now - last;
            if (elapsed < INTEREST_PERIOD_MILLIS) continue;
            long periods = Math.max(1L, elapsed / INTEREST_PERIOD_MILLIS);
            double factor = Math.pow(1d + (ratePct / 100d), periods);
            // Apply to all accounts of this bank
            for (Map.Entry<UUID, Map<UUID, BankAccount>> pe : accounts.entrySet()) {
                Map<UUID, BankAccount> perBank = pe.getValue();
                if (perBank == null) continue;
                BankAccount acct = perBank.get(bank.getId());
                if (acct == null) continue;
                double before = acct.getBalance();
                if (before <= 0d) continue;
                double after = roundMoney(before * factor);
                if (after != before) {
                    acct.setBalance(after);
                    changed = true;
                }
            }
            lastAccrualByBank.put(bank.getId(), now - (elapsed % INTEREST_PERIOD_MILLIS));
        }
        if (changed) save();
    }

    private double roundMoney(double val) {
        // round to 2 decimals
        return Math.round(val * 100.0d) / 100.0d;
    }

    public void startInterestScheduler() {
        // run hourly to catch any full 24h periods elapsed
        try {
            if (interestTaskId != null) return; // already started
            // Schedule a lightweight sync task that immediately offloads the heavy work to async to avoid main-thread I/O
            interestTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                try {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::accrueInterestNow);
                } catch (Throwable ignored) {
                }
            }, 20L * 60L * 5L, 20L * 60L * 60L);
        } catch (Throwable ignored) {
        }
    }

    public void shutdown() {
        try {
            if (interestTaskId != null) {
                plugin.getServer().getScheduler().cancelTask(interestTaskId);
                interestTaskId = null;
            }
        } catch (Throwable ignored) {
        }
    }

    // ========= Reporting helpers =========
    public double getTotalBalance(UUID playerId) {
        double sum = 0d;
        Map<String, Double> map = getAccountsBalances(playerId);
        for (double v : map.values()) sum += v;
        return sum;
    }

    public Map<String, Double> getTotalsPerBank() {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Bank bank : banksById.values()) {
            if (bank == null || bank.getName() == null) continue;
            totals.put(bank.getName(), 0d);
        }
        for (Map<UUID, BankAccount> perBank : accounts.values()) {
            if (perBank == null) continue;
            for (Map.Entry<UUID, BankAccount> e : perBank.entrySet()) {
                Bank b = banksById.get(e.getKey());
                String name = (b != null && b.getName() != null) ? b.getName() : String.valueOf(e.getKey());
                totals.putIfAbsent(name, 0d);
                totals.put(name, totals.get(name) + e.getValue().getBalance());
            }
        }
        // round
        for (Map.Entry<String, Double> e : new ArrayList<>(totals.entrySet())) {
            totals.put(e.getKey(), roundMoney(e.getValue()));
        }
        return totals;
    }

    public double getGrandTotal() {
        double sum = 0d;
        for (Map<UUID, BankAccount> perBank : accounts.values()) {
            if (perBank == null) continue;
            for (BankAccount acct : perBank.values()) sum += acct.getBalance();
        }
        return roundMoney(sum);
    }
}
