package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
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

    public LocksManager(Furious plugin) {
        this.lockedBlocks = new HashMap<>();
        this.plugin = plugin;
        this.lockFile = new File(this.plugin.getDataFolder(), "locks.yml");
        this.lockConfig = YamlConfiguration.loadConfiguration(lockFile);
        this.disabledWorlds = new HashSet<>();
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
    }

    public boolean lockBlock(Player player, Block block) {
        if (!isLockable(block)) {
            return false;
        }

        // Handle double chests
        if (block.getBlockData() instanceof Chest chest) {
            Chest.Type type = chest.getType();
            if (type != Chest.Type.SINGLE) {
                Block otherHalf = getOtherChestHalf(block);
                if (otherHalf != null) {
                    lockedBlocks.put(otherHalf.getLocation(), player.getUniqueId());
                }
            }
        }
        // Handle double doors
        else if (block.getBlockData() instanceof Door door) {
            Block otherHalf = getOtherDoorHalf(block);
            if (otherHalf != null) {
                lockedBlocks.put(otherHalf.getLocation(), player.getUniqueId());
            }
        }

        lockedBlocks.put(block.getLocation(), player.getUniqueId());
        saveLocks();
        return true;
    }

    public boolean unlockBlock(Player player, Block block) {
        UUID owner = lockedBlocks.get(block.getLocation());
        if (owner == null || !owner.equals(player.getUniqueId())) {
            return false;
        }

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
        return block.getBlockData() instanceof Chest ||
                block.getBlockData() instanceof Door;
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
