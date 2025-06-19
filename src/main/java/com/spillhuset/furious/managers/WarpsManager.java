package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Warp;
import com.spillhuset.furious.utils.EncryptionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages warps in the game.
 */
public class WarpsManager {
    private final Furious plugin;
    private final Map<String, Warp> warps; // Warp Name -> Warp
    private final Map<Location, Warp> portalLocations; // Portal Location -> Warp
    private final File configFile;
    private FileConfiguration config;
    private final EncryptionUtil encryptionUtil;

    // Configuration values
    private final double DEFAULT_WARP_COST;
    private final List<String> DISABLED_WORLDS;
    private final List<String> DISABLED_WORLD_TYPES;

    /**
     * Creates a new WarpsManager.
     *
     * @param plugin The plugin instance
     */
    public WarpsManager(Furious plugin) {
        this.plugin = plugin;
        this.warps = new HashMap<>();
        this.portalLocations = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "warps.yml");
        this.encryptionUtil = plugin.getEncryptionUtil();

        // Load configuration values
        this.DEFAULT_WARP_COST = plugin.getConfig().getDouble("warps.default-cost", 0.0);
        this.DISABLED_WORLDS = plugin.getConfig().getStringList("warps.disabled-worlds");
        this.DISABLED_WORLD_TYPES = plugin.getConfig().getStringList("warps.disabled-world-types");

        loadConfiguration();
    }

    /**
     * Loads warp data from the configuration file.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create warps.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load warps
        ConfigurationSection warpsSection = config.getConfigurationSection("warps");
        if (warpsSection != null) {
            for (String warpName : warpsSection.getKeys(false)) {
                ConfigurationSection warpSection = warpsSection.getConfigurationSection(warpName);
                if (warpSection != null) {
                    try {
                        String idStr = warpSection.getString("id");
                        String creatorStr = warpSection.getString("creator");
                        String worldStr = warpSection.getString("world");

                        if (idStr == null || creatorStr == null || worldStr == null) {
                            plugin.getLogger().warning("Missing essential UUID field(s) in warps.yml for warp: " + warpName);
                            continue;
                        }

                        UUID warpId = UUID.fromString(idStr);
                        UUID creatorId = UUID.fromString(creatorStr);
                        UUID worldId = UUID.fromString(worldStr);
                        double x = warpSection.getDouble("x");
                        double y = warpSection.getDouble("y");
                        double z = warpSection.getDouble("z");
                        float yaw = (float) warpSection.getDouble("yaw");
                        float pitch = (float) warpSection.getDouble("pitch");
                        double cost = warpSection.getDouble("cost", DEFAULT_WARP_COST);
                        String encryptedPassword = warpSection.getString("password");
                        String password = null;

                        // Decrypt password if it exists and is encrypted
                        if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                            if (encryptionUtil.isEncrypted(encryptedPassword)) {
                                password = encryptionUtil.decrypt(encryptedPassword);
                                plugin.getLogger().info("Decrypted password for warp: " + warpName);
                            } else {
                                // This is a plain text password from before encryption was implemented
                                // We'll leave it as is for now, it will be encrypted when saved
                                password = encryptedPassword;
                                plugin.getLogger().info("Found plain text password for warp: " + warpName + " (will be encrypted on next save)");
                            }
                        }

                        boolean hasPortal = warpSection.getBoolean("has-portal", false);
                        String portalFilling = warpSection.getString("portal-filling", "air");

                        UUID portalWorldId = null;
                        double portalX = 0;
                        double portalY = 0;
                        double portalZ = 0;

                        if (hasPortal) {
                            String portalWorldIdStr = warpSection.getString("portal-world");
                            if (portalWorldIdStr != null) {
                                portalWorldId = UUID.fromString(portalWorldIdStr);
                                portalX = warpSection.getDouble("portal-x");
                                portalY = warpSection.getDouble("portal-y");
                                portalZ = warpSection.getDouble("portal-z");
                            }
                        }

                        Warp warp = new Warp(warpId, warpName, creatorId, worldId, x, y, z, yaw, pitch,
                                cost, password, hasPortal, portalFilling, portalWorldId, portalX, portalY, portalZ);

                        warps.put(warpName.toLowerCase(), warp);

                        // Add portal location to map if it exists
                        if (hasPortal && warp.getPortalLocation() != null) {
                            portalLocations.put(warp.getPortalLocation(), warp);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in warps.yml for warp: " + warpName);
                    }
                }
            }
        }
    }

    /**
     * Saves warp data to the configuration file.
     */
    private void saveConfiguration() {
        // Clear existing data
        config.set("warps", null);

        // Save warps
        for (Warp warp : warps.values()) {
            String path = "warps." + warp.getName();
            config.set(path + ".id", warp.getId().toString());
            config.set(path + ".creator", warp.getCreatorId().toString());
            config.set(path + ".world", warp.getWorldId().toString());
            config.set(path + ".x", warp.getX());
            config.set(path + ".y", warp.getY());
            config.set(path + ".z", warp.getZ());
            config.set(path + ".yaw", warp.getYaw());
            config.set(path + ".pitch", warp.getPitch());
            config.set(path + ".cost", warp.getCost());

            if (warp.hasPassword()) {
                String plainPassword = warp.getPassword();
                String encryptedPassword;

                // Check if the password is already encrypted
                if (encryptionUtil.isEncrypted(plainPassword)) {
                    // Already encrypted, use as is
                    encryptedPassword = plainPassword;
                } else {
                    // Encrypt the password
                    encryptedPassword = encryptionUtil.encrypt(plainPassword);
                    plugin.getLogger().info("Encrypted password for warp: " + warp.getName());
                }

                config.set(path + ".password", encryptedPassword);
            }

            config.set(path + ".has-portal", warp.hasPortal());
            config.set(path + ".portal-filling", warp.getPortalFilling());

            if (warp.hasPortal() && warp.getPortalLocation() != null) {
                Location portalLoc = warp.getPortalLocation();
                config.set(path + ".portal-world", portalLoc.getWorld().getUID().toString());
                config.set(path + ".portal-x", portalLoc.getX());
                config.set(path + ".portal-y", portalLoc.getY());
                config.set(path + ".portal-z", portalLoc.getZ());
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }

    /**
     * Checks if a world is disabled for warps.
     *
     * @param world The world to check
     * @return true if warps are disabled in this world, false otherwise
     */
    public boolean isWorldDisabled(World world) {
        // Check if world is in disabled worlds list
        if (DISABLED_WORLDS.contains(world.getName())) {
            return true;
        }

        // Check if world type is in disabled world types list
        String worldType = world.getEnvironment().name();
        return DISABLED_WORLD_TYPES.contains(worldType);
    }

    /**
     * Creates a new warp.
     *
     * @param player   The player creating the warp (must be op)
     * @param warpName The name of the warp
     * @param cost     The cost to use this warp
     * @param password The password (can be null)
     * @return true if the warp was created, false otherwise
     */
    public boolean createWarp(Player player, String warpName, double cost, String password) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can create warps!", NamedTextColor.RED));
            return false;
        }

        // Check if world is disabled
        if (isWorldDisabled(player.getWorld())) {
            player.sendMessage(Component.text("You cannot create warps in this world!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp already exists
        if (warps.containsKey(warpName)) {
            player.sendMessage(Component.text("A warp with that name already exists!", NamedTextColor.RED));
            return false;
        }

        // Create the warp
        Warp warp = new Warp(warpName, player.getUniqueId(), player.getLocation(), cost, password);
        warps.put(warpName, warp);
        saveConfiguration();

        // Success message is now handled by the calling command
        return true;
    }

    /**
     * Relocates an existing warp.
     *
     * @param player   The player relocating the warp (must be op)
     * @param warpName The name of the warp
     * @return true if the warp was relocated, false otherwise
     */
    public boolean relocateWarp(Player player, String warpName) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can relocate warps!", NamedTextColor.RED));
            return false;
        }

        // Check if world is disabled
        if (isWorldDisabled(player.getWorld())) {
            player.sendMessage(Component.text("You cannot relocate warps to this world!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.get(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Update warp location
        warp.setLocation(player.getLocation());
        saveConfiguration();

        player.sendMessage(Component.text("Warp '" + warpName + "' relocated successfully!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Updates the cost of an existing warp.
     *
     * @param player   The player updating the cost (must be op)
     * @param warpName The name of the warp
     * @param cost     The new cost
     * @return true if the cost was updated, false otherwise
     */
    public boolean setCost(Player player, String warpName, double cost) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can change warp costs!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.get(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Update warp cost
        warp.setCost(cost);
        saveConfiguration();

        player.sendMessage(Component.text("Cost for warp '" + warpName + "' set to " + cost + "!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Updates the password of an existing warp.
     *
     * @param player   The player updating the password (must be op)
     * @param warpName The name of the warp
     * @param password The new password (null to remove)
     * @return true if the password was updated, false otherwise
     */
    public boolean setPassword(Player player, String warpName, String password) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can change warp passwords!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.get(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Update warp password
        // We store the plain text password in the Warp object
        // It will be encrypted when saved to the configuration file
        warp.setPassword(password);

        // Save configuration (this will encrypt the password)
        saveConfiguration();

        if (password == null || password.isEmpty()) {
            player.sendMessage(Component.text("Password removed from warp '" + warpName + "'!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Password for warp '" + warpName + "' updated!", NamedTextColor.GREEN));
        }
        return true;
    }

    /**
     * Renames an existing warp.
     *
     * @param player  The player renaming the warp (must be op)
     * @param oldName The current name of the warp
     * @param newName The new name for the warp
     * @return true if the warp was renamed, false otherwise
     */
    public boolean renameWarp(Player player, String oldName, String newName) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can rename warps!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        oldName = oldName.toLowerCase();
        newName = newName.toLowerCase();

        // Check if names are the same
        if (oldName.equals(newName)) {
            return true;
        }

        // Check if old warp exists
        Warp warp = warps.remove(oldName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + oldName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Check if new name already exists
        if (warps.containsKey(newName)) {
            warps.put(oldName, warp); // Put the old warp back
            player.sendMessage(Component.text("A warp with the name '" + newName + "' already exists!", NamedTextColor.RED));
            return false;
        }

        // Update warp name and add to map
        warp.setName(newName);
        warps.put(newName, warp);
        saveConfiguration();

        player.sendMessage(Component.text("Warp renamed from '" + oldName + "' to '" + newName + "'!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Deletes an existing warp.
     *
     * @param player   The player deleting the warp (must be op)
     * @param warpName The name of the warp
     * @return true if the warp was deleted, false otherwise
     */
    public boolean deleteWarp(Player player, String warpName) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can delete warps!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.remove(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Remove portal if it exists
        if (warp.hasPortal() && warp.getPortalLocation() != null) {
            portalLocations.remove(warp.getPortalLocation());
            removePortal(warp.getPortalLocation());
        }

        saveConfiguration();

        player.sendMessage(Component.text("Warp '" + warpName + "' deleted successfully!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Links a warp to a portal.
     *
     * @param player   The player linking the warp (must be op)
     * @param warpName The name of the warp
     * @param filling  The material to fill the portal with (water, lava, or air)
     * @return true if the warp was linked, false otherwise
     */
    public boolean linkWarp(Player player, String warpName, String filling) {
        // Check if player is op
        if (!player.isOp()) {
            player.sendMessage(Component.text("Only operators can link warps to portals!", NamedTextColor.RED));
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.get(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Check if warp has a password
        if (warp.hasPassword()) {
            player.sendMessage(Component.text("Password-protected warps cannot be linked to portals!", NamedTextColor.RED));
            return false;
        }

        // Check if we're in a SAFE zone
        Chunk chunk = player.getLocation().getChunk();
        Guild chunkOwner = plugin.getGuildManager().getChunkOwner(chunk);
        if (chunkOwner == null || !chunkOwner.getType().isSafe()) {
            player.sendMessage(Component.text("Portals can only be created in SAFE zones!", NamedTextColor.RED));
            return false;
        }

        // Find gold blocks
        RayTraceResult rayTraceResult = player.rayTraceBlocks(5);
        if (rayTraceResult == null || rayTraceResult.getHitBlock() == null || rayTraceResult.getHitBlock().getType() != Material.GOLD_BLOCK) {
            player.sendMessage(Component.text("You must be looking at a gold block to create a portal!", NamedTextColor.RED));
            return false;
        }
        Block targetBlock = rayTraceResult.getHitBlock();

        // Find the second gold block (diagonal)
        Block secondGoldBlock = findDiagonalGoldBlock(targetBlock);
        if (secondGoldBlock == null) {
            player.sendMessage(Component.text("Could not find a second gold block placed diagonally!", NamedTextColor.RED));
            return false;
        }

        // Validate filling material
        if (filling == null) {
            filling = "air";
        } else {
            filling = filling.toLowerCase();
            // Check if filling is a valid option
            List<String> validFillings = Arrays.asList(
                "water", "real_water", "lava", "air",
                "white_glass", "orange_glass", "magenta_glass", "light_blue_glass",
                "yellow_glass", "lime_glass", "pink_glass", "gray_glass",
                "light_gray_glass", "cyan_glass", "purple_glass", "blue_glass",
                "brown_glass", "green_glass", "red_glass", "black_glass",
                "iron_bars", "oak_fence", "spruce_fence", "birch_fence",
                "jungle_fence", "acacia_fence", "dark_oak_fence", "crimson_fence",
                "warped_fence", "chain"
            );

            if (!validFillings.contains(filling)) {
                player.sendMessage(Component.text("Invalid filling material! Valid options are: " + String.join(", ", validFillings), NamedTextColor.RED));
                return false;
            }
        }

        // Create portal
        Location portalLocation = createPortal(targetBlock, secondGoldBlock, filling);

        // Update warp with portal info
        if (warp.hasPortal() && warp.getPortalLocation() != null) {
            // Remove old portal from map
            portalLocations.remove(warp.getPortalLocation());
            // Remove old portal blocks
            removePortal(warp.getPortalLocation());
        }

        warp.setHasPortal(true);
        warp.setPortalFilling(filling);
        warp.setPortalLocation(portalLocation);
        portalLocations.put(portalLocation, warp);
        saveConfiguration();

        player.sendMessage(Component.text("Warp '" + warpName + "' linked to portal successfully!", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Finds a gold block placed diagonally from the given block.
     * Supports both cubic and rectangular portal shapes.
     *
     * @param block The starting gold block
     * @return The diagonal gold block, or null if not found
     */
    private Block findDiagonalGoldBlock(Block block) {
        World world = block.getWorld();
        int startX = block.getX();
        int startY = block.getY();
        int startZ = block.getZ();

        // First, check the traditional 3D diagonals (cubic shape)
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dy = -1; dy <= 1; dy += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    Block checkBlock = block.getRelative(dx, dy, dz);
                    if (checkBlock.getType() == Material.GOLD_BLOCK) {
                        return checkBlock;
                    }
                }
            }
        }

        // If no cubic diagonal found, search for rectangular diagonals
        // Search in a larger area (up to 10 blocks in each direction)
        int searchRadius = 10;

        // Check for gold blocks that form a rectangular diagonal
        for (int x = startX - searchRadius; x <= startX + searchRadius; x++) {
            for (int y = startY - searchRadius; y <= startY + searchRadius; y++) {
                for (int z = startZ - searchRadius; z <= startZ + searchRadius; z++) {
                    // Skip the starting block itself
                    if (x == startX && y == startY && z == startZ) {
                        continue;
                    }

                    // Skip blocks that are on the same plane (not diagonal)
                    if (x == startX && y == startY) continue;
                    if (x == startX && z == startZ) continue;
                    if (y == startY && z == startZ) continue;

                    Block checkBlock = world.getBlockAt(x, y, z);
                    if (checkBlock.getType() == Material.GOLD_BLOCK) {
                        return checkBlock;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Creates a portal between two gold blocks.
     *
     * @param block1  The first gold block
     * @param block2  The second gold block
     * @param filling The material to fill the portal with
     * @return The center location of the portal
     */
    private Location createPortal(Block block1, Block block2, String filling) {
        // Get the min and max coordinates to form a box
        int minX = Math.min(block1.getX(), block2.getX());
        int minY = Math.min(block1.getY(), block2.getY());
        int minZ = Math.min(block1.getZ(), block2.getZ());
        int maxX = Math.max(block1.getX(), block2.getX());
        int maxY = Math.max(block1.getY(), block2.getY());
        int maxZ = Math.max(block1.getZ(), block2.getZ());

        World world = block1.getWorld();

        // Create the portal frame
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);

                    // Check if this is a frame edge block (only the edges of the box)
                    boolean isFrameEdge;

                    // Check if it's on an edge (intersection of two faces)
                    int onFaceCount = 0;
                    if (x == minX || x == maxX) onFaceCount++;
                    if (y == minY || y == maxY) onFaceCount++;
                    if (z == minZ || z == maxZ) onFaceCount++;

                    // It's a frame edge if it's on at least two faces (i.e., an edge)
                    isFrameEdge = (onFaceCount >= 2);

                    // If it's a frame edge, make it a frame block
                    if (isFrameEdge) {
                        if (block.getType() != Material.GOLD_BLOCK) { // Don't replace the gold blocks
                            block.setType(Material.CHISELED_STONE_BRICKS);
                        }
                    }
                    // If it's on a face but not an edge, fill with the specified material
                    else if (onFaceCount == 1) {
                        // Set the block type based on the filling material
                        switch (filling) {
                            case "water":
                                block.setType(Material.BLUE_STAINED_GLASS_PANE);
                                break;
                            case "real_water":
                                block.setType(Material.WATER);
                                break;
                            case "lava":
                                block.setType(Material.LAVA);
                                break;
                            case "white_glass":
                                block.setType(Material.WHITE_STAINED_GLASS_PANE);
                                break;
                            case "orange_glass":
                                block.setType(Material.ORANGE_STAINED_GLASS_PANE);
                                break;
                            case "magenta_glass":
                                block.setType(Material.MAGENTA_STAINED_GLASS_PANE);
                                break;
                            case "light_blue_glass":
                                block.setType(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                                break;
                            case "yellow_glass":
                                block.setType(Material.YELLOW_STAINED_GLASS_PANE);
                                break;
                            case "lime_glass":
                                block.setType(Material.LIME_STAINED_GLASS_PANE);
                                break;
                            case "pink_glass":
                                block.setType(Material.PINK_STAINED_GLASS_PANE);
                                break;
                            case "gray_glass":
                                block.setType(Material.GRAY_STAINED_GLASS_PANE);
                                break;
                            case "light_gray_glass":
                                block.setType(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                                break;
                            case "cyan_glass":
                                block.setType(Material.CYAN_STAINED_GLASS_PANE);
                                break;
                            case "purple_glass":
                                block.setType(Material.PURPLE_STAINED_GLASS_PANE);
                                break;
                            case "blue_glass":
                                block.setType(Material.BLUE_STAINED_GLASS_PANE);
                                break;
                            case "brown_glass":
                                block.setType(Material.BROWN_STAINED_GLASS_PANE);
                                break;
                            case "green_glass":
                                block.setType(Material.GREEN_STAINED_GLASS_PANE);
                                break;
                            case "red_glass":
                                block.setType(Material.RED_STAINED_GLASS_PANE);
                                break;
                            case "black_glass":
                                block.setType(Material.BLACK_STAINED_GLASS_PANE);
                                break;
                            case "iron_bars":
                                block.setType(Material.IRON_BARS);
                                break;
                            case "oak_fence":
                                block.setType(Material.OAK_FENCE);
                                break;
                            case "spruce_fence":
                                block.setType(Material.SPRUCE_FENCE);
                                break;
                            case "birch_fence":
                                block.setType(Material.BIRCH_FENCE);
                                break;
                            case "jungle_fence":
                                block.setType(Material.JUNGLE_FENCE);
                                break;
                            case "acacia_fence":
                                block.setType(Material.ACACIA_FENCE);
                                break;
                            case "dark_oak_fence":
                                block.setType(Material.DARK_OAK_FENCE);
                                break;
                            case "crimson_fence":
                                block.setType(Material.CRIMSON_FENCE);
                                break;
                            case "warped_fence":
                                block.setType(Material.WARPED_FENCE);
                                break;
                            case "chain":
                                block.setType(Material.CHAIN);
                                break;
                            default:
                                block.setType(Material.AIR);
                                break;
                        }
                    }
                    // Otherwise, it's inside the box, leave it as is
                }
            }
        }

        // Return the center location
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;
        return new Location(world, centerX, centerY, centerZ);
    }

    /**
     * Checks if a material is part of a portal (frame or filling).
     *
     * @param type The material type to check
     * @return True if the material is part of a portal, false otherwise
     */
    private boolean isPortalBlock(Material type) {
        return type == Material.CHISELED_STONE_BRICKS || type == Material.GOLD_BLOCK ||
                type == Material.WATER || type == Material.LAVA ||
                // All stained glass panes
                type == Material.WHITE_STAINED_GLASS_PANE || type == Material.ORANGE_STAINED_GLASS_PANE ||
                type == Material.MAGENTA_STAINED_GLASS_PANE || type == Material.LIGHT_BLUE_STAINED_GLASS_PANE ||
                type == Material.YELLOW_STAINED_GLASS_PANE || type == Material.LIME_STAINED_GLASS_PANE ||
                type == Material.PINK_STAINED_GLASS_PANE || type == Material.GRAY_STAINED_GLASS_PANE ||
                type == Material.LIGHT_GRAY_STAINED_GLASS_PANE || type == Material.CYAN_STAINED_GLASS_PANE ||
                type == Material.PURPLE_STAINED_GLASS_PANE || type == Material.BLUE_STAINED_GLASS_PANE ||
                type == Material.BROWN_STAINED_GLASS_PANE || type == Material.GREEN_STAINED_GLASS_PANE ||
                type == Material.RED_STAINED_GLASS_PANE || type == Material.BLACK_STAINED_GLASS_PANE ||
                // Legacy glass block (for backward compatibility)
                type == Material.BLUE_STAINED_GLASS ||
                // Iron bars
                type == Material.IRON_BARS ||
                // All fence types
                type == Material.OAK_FENCE || type == Material.SPRUCE_FENCE ||
                type == Material.BIRCH_FENCE || type == Material.JUNGLE_FENCE ||
                type == Material.ACACIA_FENCE || type == Material.DARK_OAK_FENCE ||
                type == Material.CRIMSON_FENCE || type == Material.WARPED_FENCE ||
                // Chain
                type == Material.CHAIN;
    }

    /**
     * Removes a portal.
     *
     * @param location The center location of the portal
     */
    private void removePortal(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Find the frame by searching outward from the center
        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        // Search for frame blocks in all directions
        int radius = 5; // Maximum search radius
        int minX = centerX, maxX = centerX;
        int minY = centerY, maxY = centerY;
        int minZ = centerZ, maxZ = centerZ;

        // Find the boundaries of the portal
        boolean foundMinX = false, foundMaxX = false;
        boolean foundMinY = false, foundMaxY = false;
        boolean foundMinZ = false, foundMaxZ = false;

        for (int r = 1; r <= radius; r++) {

            // Check X direction
            if (!foundMinX) {
                Block block = world.getBlockAt(centerX - r, centerY, centerZ);
                if (isPortalBlock(block.getType())) {
                    minX = centerX - r;
                    foundMinX = true;
                }
            }
            if (!foundMaxX) {
                Block block = world.getBlockAt(centerX + r, centerY, centerZ);
                if (isPortalBlock(block.getType())) {
                    maxX = centerX + r;
                    foundMaxX = true;
                }
            }

            // Check Y direction
            if (!foundMinY) {
                Block block = world.getBlockAt(centerX, centerY - r, centerZ);
                if (isPortalBlock(block.getType())) {
                    minY = centerY - r;
                    foundMinY = true;
                }
            }
            if (!foundMaxY) {
                Block block = world.getBlockAt(centerX, centerY + r, centerZ);
                if (isPortalBlock(block.getType())) {
                    maxY = centerY + r;
                    foundMaxY = true;
                }
            }

            // Check Z direction
            if (!foundMinZ) {
                Block block = world.getBlockAt(centerX, centerY, centerZ - r);
                if (isPortalBlock(block.getType())) {
                    minZ = centerZ - r;
                    foundMinZ = true;
                }
            }
            if (!foundMaxZ) {
                Block block = world.getBlockAt(centerX, centerY, centerZ + r);
                if (isPortalBlock(block.getType())) {
                    maxZ = centerZ + r;
                    foundMaxZ = true;
                }
            }

            // If we've found the frame in all directions, break
            if (foundMinX && foundMaxX && foundMinY && foundMaxY && foundMinZ && foundMaxZ) {
                break;
            }
        }

        // Remove all blocks inside the frame (including the frame itself)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    // Check if the block is part of the portal frame or filling
                    if (isPortalBlock(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Teleports a player to a warp.
     *
     * @param player   The player to teleport
     * @param warpName The name of the warp
     * @param password The password (if required)
     * @return true if the player was teleported, false otherwise
     */
    public boolean teleportToWarp(Player player, String warpName, String password) {
        // Convert to lowercase for case-insensitive comparison
        warpName = warpName.toLowerCase();

        // Check if warp exists
        Warp warp = warps.get(warpName);
        if (warp == null) {
            player.sendMessage(Component.text("Warp '" + warpName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Check password if required (ops bypass password check)
        if (warp.hasPassword() && !player.isOp()) {
            if (password == null || !password.equals(warp.getPassword())) {
                player.sendMessage(Component.text("Incorrect password for warp '" + warpName + "'!", NamedTextColor.RED));
                return false;
            }
        }

        // Check if player has enough money
        double cost = warp.getCost();
        if (cost > 0 && !player.isOp()) {
            if (!plugin.getWalletManager().has(player, cost)) {
                player.sendMessage(Component.text("You don't have enough money to use this warp! Cost: " + cost, NamedTextColor.RED));
                return false;
            }

            // Deduct money
            plugin.getWalletManager().withdraw(player, cost);
            player.sendMessage(Component.text("You paid " + cost + " to use warp '" + warpName + "'.", NamedTextColor.YELLOW));
        }

        // Get the warp location
        Location location = warp.getLocation();
        if (location == null) {
            player.sendMessage(Component.text("Warp world doesn't exist!", NamedTextColor.RED));
            return false;
        }

        // Check if player is an op or has the admin permission
        if (player.isOp() || player.hasPermission("furious.teleport.admin")) {
            // Teleport immediately
            player.teleport(location);
            player.sendMessage(Component.text("Teleported to warp '" + warpName + "'!", NamedTextColor.GREEN));
        } else {
            // Start teleport sequence with countdown and nausea effect
            plugin.getTeleportManager().teleportQueue(player, location);
            player.sendMessage(Component.text("Starting teleport to warp '" + warpName + "'...", NamedTextColor.YELLOW));
        }

        return true;
    }


    /**
     * Gets a warp by portal location.
     *
     * @param location The location to check
     * @return The warp linked to this portal, or null if not found
     */
    public Warp getWarpByPortal(Location location) {
        plugin.getLogger().info("[DEBUG] getWarpByPortal called for location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        plugin.getLogger().info("[DEBUG] portalLocations size: " + portalLocations.size());

        // Check if the exact location is a portal center (by block coordinates, not by reference)
        Warp warp = findWarpByBlockCoordinates(location);
        if (warp != null) {
            plugin.getLogger().info("[DEBUG] Found exact portal center match for warp: " + warp.getName());
            return warp;
        }

        // Log some portal locations for debugging
        int count = 0;
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            Warp portalWarp = entry.getValue();
            plugin.getLogger().info("[DEBUG] Portal #" + count + ": " + portalLoc.getBlockX() + ", " + portalLoc.getBlockY() + ", " + portalLoc.getBlockZ() + " -> " + portalWarp.getName());
            count++;
            if (count >= 5) break; // Only log the first 5 portals to avoid spam
        }

        // Check nearby blocks (portal might be larger than 1 block)
        // Use a larger search radius to better detect when a player is inside a portal
        plugin.getLogger().info("[DEBUG] Checking nearby blocks with radius 2");
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    warp = findWarpByBlockCoordinates(checkLoc);
                    if (warp != null) {
                        plugin.getLogger().info("[DEBUG] Found portal center match at offset " + x + ", " + y + ", " + z + " for warp: " + warp.getName());
                        return warp;
                    }
                }
            }
        }

        // If we still haven't found a portal, check if the player is standing on a portal block
        Block block = location.getBlock();
        Material blockType = block.getType();
        plugin.getLogger().info("[DEBUG] Player is standing on block type: " + blockType.name());
        plugin.getLogger().info("[DEBUG] isPortalBlock result: " + isPortalBlock(blockType));

        if (isPortalBlock(blockType)) {
            // Search for a portal center in a larger area
            plugin.getLogger().info("[DEBUG] Player is standing on a portal block, checking larger area with radius 5");
            for (int x = -5; x <= 5; x++) {
                for (int y = -5; y <= 5; y++) {
                    for (int z = -5; z <= 5; z++) {
                        Location checkLoc = location.clone().add(x, y, z);
                        warp = findWarpByBlockCoordinates(checkLoc);
                        if (warp != null) {
                            plugin.getLogger().info("[DEBUG] Found portal center match in larger area at offset " + x + ", " + y + ", " + z + " for warp: " + warp.getName());
                            return warp;
                        }
                    }
                }
            }
            plugin.getLogger().info("[DEBUG] No portal center found in larger area");
        }

        plugin.getLogger().info("[DEBUG] No portal found for location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        return null;
    }

    /**
     * Finds a warp by comparing the block coordinates of the portal locations.
     *
     * @param location The location to check
     * @return The warp linked to this portal, or null if not found
     */
    private Warp findWarpByBlockCoordinates(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        World world = location.getWorld();

        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            if (portalLoc.getWorld().equals(world) &&
                portalLoc.getBlockX() == x &&
                portalLoc.getBlockY() == y &&
                portalLoc.getBlockZ() == z) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Gets all warps.
     *
     * @return A collection of all warps
     */
    public Collection<Warp> getAllWarps() {
        return warps.values();
    }

    /**
     * Checks if a block is part of a portal frame.
     *
     * @param block The block to check
     * @return true if the block is part of a portal frame, false otherwise
     */
    public boolean isPortalFrame(Block block) {
        return block.getType() == Material.CHISELED_STONE_BRICKS || block.getType() == Material.GOLD_BLOCK;
    }

    /**
     * Removes a portal when an op punches the frame.
     *
     * @param player The player punching the frame (must be op)
     * @param block  The block that was punched
     * @return true if a portal was removed, false otherwise
     */
    public boolean removePortalByPunch(Player player, Block block) {
        // Check if player is op
        if (!player.isOp()) {
            return false;
        }

        // Check if the block is part of a portal (frame or filling)
        if (!isPortalFrame(block) && !isPortalBlock(block.getType())) {
            return false;
        }

        // Find the portal center by searching nearby
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Location checkLoc = block.getLocation().clone().add(x, y, z);
                    Warp warp = portalLocations.get(checkLoc);
                    if (warp != null) {
                        // Remove the portal
                        portalLocations.remove(checkLoc);
                        removePortal(checkLoc);

                        // Update the warp
                        warp.setHasPortal(false);
                        warp.setPortalLocation(null);
                        saveConfiguration();

                        player.sendMessage(Component.text("Portal for warp '" + warp.getName() + "' removed!", NamedTextColor.GREEN));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void shutdown() {
        saveConfiguration();
        warps.clear();
        portalLocations.clear();
    }
}
