package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocksService {
    private final Furious plugin;

    // worldUUID -> map of "x,y,z" -> ownerUUID
    private final Map<UUID, Map<String, UUID>> locks = new ConcurrentHashMap<>();

    private File locksFile;
    private FileConfiguration locksConfig;

    // enabled worlds set, configured under plugin config key "locks.enabled-worlds"
    private java.util.Set<java.util.UUID> enabledWorlds = new java.util.HashSet<>();

    public LocksService(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    public void load() {
        loadEnabledWorldsFromConfig();
        locksFile = new File(plugin.getDataFolder(), "locks.yml");
        try {
            if (!locksFile.exists()) {
                locksFile.getParentFile().mkdirs();
                locksFile.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed creating locks.yml: " + e.getMessage());
        }
        locksConfig = YamlConfiguration.loadConfiguration(locksFile);
        locks.clear();
        ConfigurationSection root = locksConfig.getConfigurationSection("locks");
        if (root != null) {
            for (String worldKey : root.getKeys(false)) {
                try {
                    UUID worldId = UUID.fromString(worldKey);
                    ConfigurationSection worldSec = root.getConfigurationSection(worldKey);
                    if (worldSec == null) continue;
                    Map<String, UUID> map = new ConcurrentHashMap<>();
                    for (String coord : worldSec.getKeys(false)) {
                        try {
                            String ownerStr = worldSec.getString(coord);
                            if (ownerStr == null) continue;
                            map.put(coord, UUID.fromString(ownerStr));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    locks.put(worldId, map);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        // Persist locks.yml
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection root = out.createSection("locks");
        for (Map.Entry<UUID, Map<String, UUID>> e : locks.entrySet()) {
            ConfigurationSection ws = root.createSection(e.getKey().toString());
            for (Map.Entry<String, UUID> le : e.getValue().entrySet()) {
                ws.set(le.getKey(), le.getValue().toString());
            }
        }
        locksConfig = out;
        try {
            out.save(locksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving locks.yml: " + e.getMessage());
        }
        // Persist enabled worlds list into plugin config
        List<String> worldIds = new ArrayList<>();
        for (UUID id : enabledWorlds) worldIds.add(id.toString());
        plugin.getConfig().set("locks.enabled-worlds", worldIds);
        plugin.saveConfig();
    }

    private void loadEnabledWorldsFromConfig() {
        List<String> ids = plugin.getConfig().getStringList("locks.enabled-worlds");
        HashSet<java.util.UUID> set = new HashSet<>();
        for (String s : ids) {
            try { set.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        enabledWorlds = set;
    }

    public boolean isWorldEnabled(java.util.UUID worldId) {
        return enabledWorlds.contains(worldId);
    }

    public void setWorldEnabled(java.util.UUID worldId, boolean enabled) {
        if (enabled) enabledWorlds.add(worldId); else enabledWorlds.remove(worldId);
    }

    public void setWorldEnabled(CommandSender sender, UUID worldId, boolean enable) {
        World world = plugin.getServer().getWorld(worldId);
        if (world == null) {
            Components.sendErrorMessage(sender, "Invalid world");
            return;
        }
        setWorldEnabled(worldId, enable);
        save();
        if (enable) {
            Components.sendSuccess(sender,
                    Components.t("World "),
                    Components.valueComp(world.getName()),
                    Components.t(" enabled", NamedTextColor.GREEN),
                    Components.t(" for locks."));
        } else {
            Components.sendSuccess(sender,
                    Components.t("World "),
                    Components.valueComp(world.getName()),
                    Components.t(" disabled", NamedTextColor.RED),
                    Components.t(" for locks."));
        }
    }

    public void listWorlds(org.bukkit.command.CommandSender sender) {
        Components.sendInfo(sender, Components.t("Worlds locks status:"));
        for (org.bukkit.World w : plugin.getServer().getWorlds()) {
            boolean en = isWorldEnabled(w.getUID());
            Components.sendInfo(sender,
                    Components.valueComp(w.getName()),
                    Components.t(": "),
                    (en ? Components.t("ENABLED", NamedTextColor.GREEN)
                            : Components.t("DISABLED", NamedTextColor.RED))
            );
        }
    }

    private String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private String key(Location loc) {
        return key(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean isLocked(Block block) {
        return getOwner(block) != null;
    }

    private BlockFace leftOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };
    }

    private Set<Block> relatedGroup(Block block) {
        Set<Block> set = new LinkedHashSet<>();
        if (block == null) return set;
        set.add(block);
        Material type = block.getType();
        if (block.getBlockData() instanceof Chest chest) {
            if (chest.getType() != Chest.Type.SINGLE) {
                BlockFace left = leftOf(chest.getFacing());
                BlockFace offset = (chest.getType() == Chest.Type.LEFT) ? left : left.getOppositeFace();
                Block other = block.getRelative(offset);
                if (other.getType() == type && other.getBlockData() instanceof Chest) {
                    set.add(other);
                }
            }
        } else if (block.getBlockData() instanceof Door door) {
            // include both halves
            if (door.getHalf() == Bisected.Half.TOP) {
                set.add(block.getRelative(BlockFace.DOWN));
            } else {
                set.add(block.getRelative(BlockFace.UP));
            }
            // try to include adjacent paired door
            BlockFace facing = door.getFacing();
            BlockFace left = leftOf(facing);
            Block[] candidates = new Block[]{block.getRelative(left), block.getRelative(left.getOppositeFace())};
            for (Block cand : candidates) {
                if (cand.getType() == type && cand.getBlockData() instanceof Door d2) {
                    if (d2.getFacing() == facing && d2.getHinge() != door.getHinge()) {
                        set.add(cand);
                        // include its other half
                        if (d2.getHalf() == Bisected.Half.TOP) {
                            set.add(cand.getRelative(BlockFace.DOWN));
                        } else {
                            set.add(cand.getRelative(BlockFace.UP));
                        }
                        break;
                    }
                }
            }
        }
        return set;
    }

    public UUID getOwner(Block block) {
        if (block == null) return null;
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return null;
        Map<String, UUID> map = locks.get(world.getUID());
        if (map == null) return null;
        return map.get(key(loc));
    }

    public boolean canAccess(UUID playerId, Block block) {
        if (block == null) return true;
        UUID owner = getOwner(block);
        return owner == null || owner.equals(playerId);
    }

    public boolean lockBlock(UUID playerId, Block block) {
        if (block == null) return false;
        World world = block.getWorld();
        locks.computeIfAbsent(world.getUID(), k -> new ConcurrentHashMap<>());
        Map<String, UUID> map = locks.get(world.getUID());
        Set<Block> group = relatedGroup(block);
        // deny if any block is already locked by someone else
        for (Block b : group) {
            UUID owner = map.get(key(b.getLocation()));
            if (owner != null && !owner.equals(playerId)) {
                return false;
            }
        }
        for (Block b : group) {
            String k = key(b.getLocation());
            map.put(k, playerId);
        }
        return true;
    }

    public boolean unlockBlock(UUID requesterId, boolean requesterIsOp, Block block) {
        if (block == null) return false;
        World world = block.getWorld();
        Map<String, UUID> map = locks.get(world.getUID());
        if (map == null) return false;
        Set<Block> group = relatedGroup(block);
        // check ownership on at least one block
        UUID foundOwner = null;
        for (Block b : group) {
            UUID o = map.get(key(b.getLocation()));
            if (o != null) { foundOwner = o; break; }
        }
        if (foundOwner == null) return false;
        if (!requesterIsOp && !foundOwner.equals(requesterId)) return false;
        for (Block b : group) {
            map.remove(key(b.getLocation()));
        }
        return true;
    }
}
