package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LocksManager {
    private final Map<Location, UUID> lockedBlocks;
    private final File lockFile;
    private final FileConfiguration lockConfig;
    private final Furious plugin;

    // Store UUIDs of worlds where locks are disabled
    private Set<UUID> disabledWorlds;

    // Maps to store player-specific lock counts by type
    private final Map<UUID, Map<LockType, Integer>> playerLockCounts;
    // Maps to store player-specific purchased additional slots by type
    private final Map<UUID, Map<LockType, Integer>> playerPurchasedSlots;

    // Default lock limits by type
    private int maxDoorLocks;
    private int maxContainerLocks;
    private int maxBlockLocks;

    // Costs to purchase additional slots
    private int doorLockCost;
    private int containerLockCost;
    private int blockLockCost;

    // Costs to create keys
    private int playerKeyCost;
    private int guildKeyCost;

    // Minimum guild role required to create guild keys
    private String guildKeyMinRole;

    // Enum to represent different lock types
    public enum LockType {
        DOOR,
        CONTAINER,
        BLOCK
    }

    public LocksManager(Furious plugin) {
        this.lockedBlocks = new HashMap<>();
        this.plugin = plugin;
        this.lockFile = new File(this.plugin.getDataFolder(), "locks.yml");
        this.lockConfig = YamlConfiguration.loadConfiguration(lockFile);
        this.disabledWorlds = new HashSet<>();
        this.playerLockCounts = new HashMap<>();
        this.playerPurchasedSlots = new HashMap<>();
        loadLocks();
    }

    private void loadLocks() {
        if (!lockFile.exists()) {
            return;
        }

        // Load locked blocks
        if (lockConfig.contains("locks")) {
            for (String locationStr : lockConfig.getConfigurationSection("locks").getKeys(false)) {
                Location location = deserializeLocation(locationStr);
                UUID owner = UUID.fromString(Objects.requireNonNull(lockConfig.getString("locks." + locationStr)));
                lockedBlocks.put(location, owner);
            }
        } else {
            // Backward compatibility with old format
            for (String locationStr : lockConfig.getKeys(false)) {
                if (locationStr.equals("disabled-worlds")) continue;

                Location location = deserializeLocation(locationStr);
                UUID owner = UUID.fromString(Objects.requireNonNull(lockConfig.getString(locationStr)));
                lockedBlocks.put(location, owner);
            }
        }

        // Load disabled worlds
        List<String> disabledWorldsList = lockConfig.getStringList("disabled-worlds");
        for (String worldUuidStr : disabledWorldsList) {
            try {
                UUID worldUuid = UUID.fromString(worldUuidStr);
                disabledWorlds.add(worldUuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid world UUID in locks.yml: " + worldUuidStr);
            }
        }

        // Load max locks per type
        maxDoorLocks = lockConfig.getInt("max-locks.doors", 5);
        maxContainerLocks = lockConfig.getInt("max-locks.containers", 10);
        maxBlockLocks = lockConfig.getInt("max-locks.blocks", 10);

        // Load purchase costs
        doorLockCost = lockConfig.getInt("lock-purchase-cost.doors", 50);
        containerLockCost = lockConfig.getInt("lock-purchase-cost.containers", 30);
        blockLockCost = lockConfig.getInt("lock-purchase-cost.blocks", 100);

        // Load key costs
        playerKeyCost = lockConfig.getInt("key-cost.player", 200);
        guildKeyCost = lockConfig.getInt("key-cost.guild", 1000);

        // Load guild key minimum role
        guildKeyMinRole = lockConfig.getString("guild-key-min-role", "MOD");

        // Load player lock counts
        if (lockConfig.contains("player-locks")) {
            for (String playerUUIDStr : lockConfig.getConfigurationSection("player-locks").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(playerUUIDStr);
                    playerLockCounts.computeIfAbsent(playerUUID, k -> new HashMap<>());
                    Map<LockType, Integer> counts = playerLockCounts.get(playerUUID);

                    for (String lockTypeStr : lockConfig.getConfigurationSection("player-locks." + playerUUIDStr).getKeys(false)) {
                        try {
                            LockType lockType = LockType.valueOf(lockTypeStr.toUpperCase());
                            int count = lockConfig.getInt("player-locks." + playerUUIDStr + "." + lockTypeStr);
                            counts.put(lockType, count);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid lock type in locks.yml: " + lockTypeStr);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid player UUID in locks.yml: " + playerUUIDStr);
                }
            }
        } else {
            // Initialize lock counts for existing locks if player-locks section doesn't exist
            for (Map.Entry<Location, UUID> entry : lockedBlocks.entrySet()) {
                UUID playerUUID = entry.getValue();
                Location location = entry.getKey();
                Block block = location.getBlock();
                LockType lockType = getLockType(block);

                // Initialize player's lock counts if not already done
                playerLockCounts.computeIfAbsent(playerUUID, k -> new HashMap<>());

                // Increment the count for this lock type
                Map<LockType, Integer> counts = playerLockCounts.get(playerUUID);
                counts.put(lockType, counts.getOrDefault(lockType, 0) + 1);
            }
        }

        // Load player purchased slots
        if (lockConfig.contains("player-purchased-slots")) {
            for (String playerUUIDStr : lockConfig.getConfigurationSection("player-purchased-slots").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(playerUUIDStr);
                    playerPurchasedSlots.computeIfAbsent(playerUUID, k -> new HashMap<>());
                    Map<LockType, Integer> purchased = playerPurchasedSlots.get(playerUUID);

                    for (String lockTypeStr : lockConfig.getConfigurationSection("player-purchased-slots." + playerUUIDStr).getKeys(false)) {
                        try {
                            LockType lockType = LockType.valueOf(lockTypeStr.toUpperCase());
                            int slots = lockConfig.getInt("player-purchased-slots." + playerUUIDStr + "." + lockTypeStr);
                            purchased.put(lockType, slots);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid lock type in locks.yml: " + lockTypeStr);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid player UUID in locks.yml: " + playerUUIDStr);
                }
            }
        }
    }

    public boolean lockBlock(Player player, Block block) {
        if (!isLockable(block)) {
            return false;
        }

        // Get the lock type
        LockType lockType = getLockType(block);
        UUID playerUUID = player.getUniqueId();

        // Check if player has reached their lock limit for this type
        if (!canCreateLock(playerUUID, lockType)) {
            int currentCount = getPlayerLockCount(playerUUID, lockType);
            int maxLocks = getPlayerMaxLocks(playerUUID, lockType);

            String typeName = lockType.name().toLowerCase();
            int cost = switch (lockType) {
                case DOOR -> doorLockCost;
                case CONTAINER -> containerLockCost;
                case BLOCK -> blockLockCost;
            };

            player.sendMessage(Component.text("You have reached your limit of " + maxLocks + " " + typeName + " locks!", NamedTextColor.RED));
            player.sendMessage(Component.text("You can purchase more slots for " + cost + " " + plugin.getWalletManager().getCurrencyName() + " each.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Use /locks buy " + typeName + " <amount> to purchase more slots.", NamedTextColor.YELLOW));
            return false;
        }

        // Handle double chests
        if (block.getBlockData() instanceof Chest chest) {
            Chest.Type type = chest.getType();
            if (type != Chest.Type.SINGLE) {
                Block otherHalf = getOtherChestHalf(block);
                if (otherHalf != null) {
                    lockedBlocks.put(otherHalf.getLocation(), playerUUID);
                }
            }
        }
        // Handle double doors
        else if (block.getBlockData() instanceof Door door) {
            Block otherHalf = getOtherDoorHalf(block);
            if (otherHalf != null) {
                lockedBlocks.put(otherHalf.getLocation(), playerUUID);
            }
        }

        // Update the player's lock count
        playerLockCounts.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Map<LockType, Integer> counts = playerLockCounts.get(playerUUID);
        counts.put(lockType, counts.getOrDefault(lockType, 0) + 1);

        lockedBlocks.put(block.getLocation(), playerUUID);
        saveLocks();
        return true;
    }

    public boolean unlockBlock(Player player, Block block) {
        UUID owner = lockedBlocks.get(block.getLocation());
        if (owner == null || !owner.equals(player.getUniqueId())) {
            return false;
        }

        // Get the lock type
        LockType lockType = getLockType(block);
        UUID playerUUID = player.getUniqueId();

        // Handle double chests
        if (block.getBlockData() instanceof Chest chest) {
            Chest.Type type = chest.getType();
            if (type != Chest.Type.SINGLE) {
                Block otherHalf = getOtherChestHalf(block);
                if (otherHalf != null) {
                    lockedBlocks.remove(otherHalf.getLocation());
                }
            }
        }
        // Handle double doors
        else if (block.getBlockData() instanceof Door) {
            Block otherHalf = getOtherDoorHalf(block);
            if (otherHalf != null) {
                lockedBlocks.remove(otherHalf.getLocation());
            }
        }

        // Update the player's lock count
        Map<LockType, Integer> counts = playerLockCounts.get(playerUUID);
        if (counts != null) {
            int currentCount = counts.getOrDefault(lockType, 0);
            if (currentCount > 0) {
                counts.put(lockType, currentCount - 1);
            }
        }

        lockedBlocks.remove(block.getLocation());
        saveLocks();
        return true;
    }

    public boolean isLocked(Block block) {
        return lockedBlocks.containsKey(block.getLocation());
    }

    public UUID getOwner(Block block) {
        return lockedBlocks.get(block.getLocation());
    }

    public boolean isLockable(Block block) {
        BlockState state = block.getState();

        // Check if it's a chest or door
        if (block.getBlockData() instanceof Chest || block.getBlockData() instanceof Door) {
            return true;
        }

        // Check for redstone components that should be treated as doors
        Material type = block.getType();
        return type == Material.LEVER ||
               type == Material.STONE_PRESSURE_PLATE ||
               type == Material.OAK_PRESSURE_PLATE ||
               type == Material.SPRUCE_PRESSURE_PLATE ||
               type == Material.BIRCH_PRESSURE_PLATE ||
               type == Material.JUNGLE_PRESSURE_PLATE ||
               type == Material.ACACIA_PRESSURE_PLATE ||
               type == Material.DARK_OAK_PRESSURE_PLATE ||
               type == Material.CRIMSON_PRESSURE_PLATE ||
               type == Material.WARPED_PRESSURE_PLATE ||
               type == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE ||
               type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE ||
               type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
               type == Material.TRIPWIRE ||
               type == Material.TRIPWIRE_HOOK ||
               type == Material.STONE_BUTTON ||
               type == Material.OAK_BUTTON ||
               type == Material.SPRUCE_BUTTON ||
               type == Material.BIRCH_BUTTON ||
               type == Material.JUNGLE_BUTTON ||
               type == Material.ACACIA_BUTTON ||
               type == Material.DARK_OAK_BUTTON ||
               type == Material.CRIMSON_BUTTON ||
               type == Material.WARPED_BUTTON ||
               type == Material.POLISHED_BLACKSTONE_BUTTON;
    }

    /**
     * Determines the lock type based on the block type.
     *
     * @param block The block to check
     * @return The lock type (DOOR, CONTAINER, or BLOCK)
     */
    public LockType getLockType(Block block) {
        if (block.getBlockData() instanceof Door) {
            return LockType.DOOR;
        } else if (block.getBlockData() instanceof Chest) {
            return LockType.CONTAINER;
        } else {
            // Check for redstone components that should be treated as doors
            Material type = block.getType();
            if (type == Material.LEVER ||
                type == Material.STONE_PRESSURE_PLATE ||
                type == Material.OAK_PRESSURE_PLATE ||
                type == Material.SPRUCE_PRESSURE_PLATE ||
                type == Material.BIRCH_PRESSURE_PLATE ||
                type == Material.JUNGLE_PRESSURE_PLATE ||
                type == Material.ACACIA_PRESSURE_PLATE ||
                type == Material.DARK_OAK_PRESSURE_PLATE ||
                type == Material.CRIMSON_PRESSURE_PLATE ||
                type == Material.WARPED_PRESSURE_PLATE ||
                type == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE ||
                type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE ||
                type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
                type == Material.TRIPWIRE ||
                type == Material.TRIPWIRE_HOOK ||
                type == Material.STONE_BUTTON ||
                type == Material.OAK_BUTTON ||
                type == Material.SPRUCE_BUTTON ||
                type == Material.BIRCH_BUTTON ||
                type == Material.JUNGLE_BUTTON ||
                type == Material.ACACIA_BUTTON ||
                type == Material.DARK_OAK_BUTTON ||
                type == Material.CRIMSON_BUTTON ||
                type == Material.WARPED_BUTTON ||
                type == Material.POLISHED_BLACKSTONE_BUTTON) {
                return LockType.DOOR;
            }
            return LockType.BLOCK;
        }
    }

    private Block getOtherChestHalf(Block chest) {
        if (!(chest.getBlockData() instanceof Chest chestData)) {
            return null;
        }
        Chest.Type type = chestData.getType();
        if (type == Chest.Type.SINGLE) {
            return null;
        }

        // Check adjacent blocks based on chest type
        Block relative = switch (chestData.getFacing()) {
            case NORTH, SOUTH -> type == Chest.Type.LEFT ?
                    chest.getRelative(1, 0, 0) :
                    chest.getRelative(-1, 0, 0);
            case EAST, WEST -> type == Chest.Type.LEFT ?
                    chest.getRelative(0, 0, 1) :
                    chest.getRelative(0, 0, -1);
            default -> null;
        };

        return relative != null && relative.getBlockData() instanceof Chest ? relative : null;
    }

    private Block getOtherDoorHalf(Block door) {
        if (!(door.getBlockData() instanceof Door doorData)) {
            return null;
        }

        // Check block above and below
        Block above = door.getRelative(0, 1, 0);
        Block below = door.getRelative(0, -1, 0);

        if (above.getBlockData() instanceof Door) {
            return above;
        } else if (below.getBlockData() instanceof Door) {
            return below;
        }

        return null;
    }

    private void saveLocks() {
        // Clear existing data
        lockConfig.set("locks", null);

        // Save locked blocks
        for (Map.Entry<Location, UUID> entry : lockedBlocks.entrySet()) {
            String locationStr = serializeLocation(entry.getKey());
            lockConfig.set("locks." + locationStr, entry.getValue().toString());
        }

        // Save disabled worlds
        List<String> disabledWorldsList = new ArrayList<>();
        for (UUID worldUuid : disabledWorlds) {
            disabledWorldsList.add(worldUuid.toString());
        }
        lockConfig.set("disabled-worlds", disabledWorldsList);

        // Save player lock counts
        lockConfig.set("player-locks", null);
        for (Map.Entry<UUID, Map<LockType, Integer>> entry : playerLockCounts.entrySet()) {
            UUID playerUUID = entry.getKey();
            Map<LockType, Integer> counts = entry.getValue();

            for (Map.Entry<LockType, Integer> countEntry : counts.entrySet()) {
                LockType lockType = countEntry.getKey();
                int count = countEntry.getValue();

                lockConfig.set("player-locks." + playerUUID + "." + lockType.name().toLowerCase(), count);
            }
        }

        // Save player purchased slots
        lockConfig.set("player-purchased-slots", null);
        for (Map.Entry<UUID, Map<LockType, Integer>> entry : playerPurchasedSlots.entrySet()) {
            UUID playerUUID = entry.getKey();
            Map<LockType, Integer> purchased = entry.getValue();

            for (Map.Entry<LockType, Integer> purchaseEntry : purchased.entrySet()) {
                LockType lockType = purchaseEntry.getKey();
                int slots = purchaseEntry.getValue();

                lockConfig.set("player-purchased-slots." + playerUUID + "." + lockType.name().toLowerCase(), slots);
            }
        }

        try {
            lockConfig.save(lockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locks: " + e.getMessage());
        }
    }

    private String serializeLocation(Location location) {
        return String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private Location deserializeLocation(String str) {
        String[] parts = str.split(",");
        return new Location(
                org.bukkit.Bukkit.getWorld(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }

    public List<Location> getLockedBlocksByPlayer(UUID playerUUID) {
        return lockedBlocks.entrySet().stream()
                .filter(entry -> entry.getValue().equals(playerUUID))
                .map(Map.Entry::getKey)
                .toList();
    }

    public void shutdown() {
        saveLocks();
    }

    /**
     * Checks if locks are enabled in the specified world.
     *
     * @param world The world to check
     * @return true if locks are enabled in the world, false otherwise
     */
    public boolean isWorldEnabled(World world) {
        return world != null && !disabledWorlds.contains(world.getUID());
    }

    /**
     * Enables locks in the specified world.
     *
     * @param world The world to enable locks in
     * @return true if the operation was successful, false otherwise
     */
    public boolean enableWorld(World world) {
        if (world == null) {
            return false;
        }

        boolean removed = disabledWorlds.remove(world.getUID());
        if (removed) {
            saveLocks();
        }
        return true;
    }

    /**
     * Disables locks in the specified world.
     *
     * @param world The world to disable locks in
     * @return true if the operation was successful, false otherwise
     */
    public boolean disableWorld(World world) {
        if (world == null) {
            return false;
        }

        boolean added = disabledWorlds.add(world.getUID());
        if (added) {
            saveLocks();
        }
        return true;
    }

    /**
     * Gets a list of all worlds and whether locks are enabled in them.
     *
     * @return A map of world names to boolean values indicating if locks are enabled
     */
    /**
     * Gets the current number of locks a player has of a specific type.
     *
     * @param playerUUID The UUID of the player
     * @param lockType The type of lock
     * @return The number of locks
     */
    public int getPlayerLockCount(UUID playerUUID, LockType lockType) {
        Map<LockType, Integer> counts = playerLockCounts.getOrDefault(playerUUID, new HashMap<>());
        return counts.getOrDefault(lockType, 0);
    }

    /**
     * Gets the maximum number of locks a player can have of a specific type.
     * This includes the base limit plus any purchased slots.
     *
     * @param playerUUID The UUID of the player
     * @param lockType The type of lock
     * @return The maximum number of locks
     */
    public int getPlayerMaxLocks(UUID playerUUID, LockType lockType) {
        int baseLimit;
        switch (lockType) {
            case DOOR:
                baseLimit = maxDoorLocks;
                break;
            case CONTAINER:
                baseLimit = maxContainerLocks;
                break;
            case BLOCK:
                baseLimit = maxBlockLocks;
                break;
            default:
                baseLimit = 0;
        }

        // Add purchased slots
        Map<LockType, Integer> purchased = playerPurchasedSlots.getOrDefault(playerUUID, new HashMap<>());
        int additionalSlots = purchased.getOrDefault(lockType, 0);

        return baseLimit + additionalSlots;
    }

    /**
     * Checks if a player can create another lock of the specified type.
     *
     * @param playerUUID The UUID of the player
     * @param lockType The type of lock
     * @return True if the player can create another lock, false otherwise
     */
    public boolean canCreateLock(UUID playerUUID, LockType lockType) {
        int currentCount = getPlayerLockCount(playerUUID, lockType);
        int maxLocks = getPlayerMaxLocks(playerUUID, lockType);

        return currentCount < maxLocks;
    }

    /**
     * Purchases additional lock slots for a player.
     *
     * @param player The player
     * @param lockType The type of lock
     * @param amount The number of slots to purchase
     * @return True if the purchase was successful, false otherwise
     */
    public boolean purchaseLockSlots(Player player, LockType lockType, int amount) {
        if (amount <= 0) {
            return false;
        }

        // Calculate cost
        int costPerSlot;
        switch (lockType) {
            case DOOR:
                costPerSlot = doorLockCost;
                break;
            case CONTAINER:
                costPerSlot = containerLockCost;
                break;
            case BLOCK:
                costPerSlot = blockLockCost;
                break;
            default:
                return false;
        }

        int totalCost = costPerSlot * amount;

        // Check if player has enough money
        if (!plugin.getWalletManager().has(player, totalCost)) {
            return false;
        }

        // Withdraw money
        if (!plugin.getWalletManager().withdraw(player, totalCost)) {
            return false;
        }

        // Add purchased slots
        UUID playerUUID = player.getUniqueId();
        playerPurchasedSlots.computeIfAbsent(playerUUID, k -> new HashMap<>());
        Map<LockType, Integer> purchased = playerPurchasedSlots.get(playerUUID);
        purchased.put(lockType, purchased.getOrDefault(lockType, 0) + amount);

        return true;
    }

    /**
     * Gets the cost to purchase an additional door lock slot.
     *
     * @return The cost in server currency
     */
    public int getDoorLockCost() {
        return doorLockCost;
    }

    /**
     * Gets the cost to purchase an additional container lock slot.
     *
     * @return The cost in server currency
     */
    public int getContainerLockCost() {
        return containerLockCost;
    }

    /**
     * Gets the cost to purchase an additional block lock slot.
     *
     * @return The cost in server currency
     */
    public int getBlockLockCost() {
        return blockLockCost;
    }

    /**
     * Gets the cost to create a player key.
     *
     * @return The cost in server currency
     */
    public int getPlayerKeyCost() {
        return playerKeyCost;
    }

    /**
     * Gets the cost to create a guild key.
     *
     * @return The cost in server currency
     */
    public int getGuildKeyCost() {
        return guildKeyCost;
    }

    /**
     * Gets the minimum guild role required to create guild keys.
     *
     * @return The minimum guild role as a string
     */
    public String getGuildKeyMinRole() {
        return guildKeyMinRole;
    }

    public Map<String, Boolean> getWorldsStatus() {
        Map<String, Boolean> worldsStatus = new HashMap<>();

        for (World world : plugin.getServer().getWorlds()) {
            // Skip game worlds
            if (world.getName().equals(plugin.getWorldManager().getGameWorldName()) ||
                world.getName().equals(plugin.getWorldManager().getGameBackupName()) ||
                world.getName().startsWith("minigame_")) {
                continue;
            }

            worldsStatus.put(world.getName(), isWorldEnabled(world));
        }

        return worldsStatus;
    }
}
