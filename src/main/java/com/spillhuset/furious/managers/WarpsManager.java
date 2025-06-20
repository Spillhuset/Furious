package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Portal;
import com.spillhuset.furious.entities.Warp;
import com.spillhuset.furious.utils.EncryptionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import java.util.ArrayList;

/**
 * Manages warps in the game.
 */
public class WarpsManager {
    private final Furious plugin;
    private final Map<String, Warp> warps; // Warp Name -> Warp
    private final Map<Location, Warp> portalLocations; // Portal Location -> Warp (supports multiple portals per warp)
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

                        // Gold block locations
                        double gold1X = 0;
                        double gold1Y = 0;
                        double gold1Z = 0;
                        double gold2X = 0;
                        double gold2Y = 0;
                        double gold2Z = 0;
                        boolean hasGoldBlockLocations = false;

                        if (hasPortal) {
                            String portalWorldIdStr = warpSection.getString("portal-world");
                            if (portalWorldIdStr != null) {
                                portalWorldId = UUID.fromString(portalWorldIdStr);
                                portalX = warpSection.getDouble("portal-x");
                                portalY = warpSection.getDouble("portal-y");
                                portalZ = warpSection.getDouble("portal-z");

                                // Check if gold block locations are defined
                                if (warpSection.contains("portal-gold1-x") && 
                                    warpSection.contains("portal-gold1-y") && 
                                    warpSection.contains("portal-gold1-z") && 
                                    warpSection.contains("portal-gold2-x") && 
                                    warpSection.contains("portal-gold2-y") && 
                                    warpSection.contains("portal-gold2-z")) {

                                    gold1X = warpSection.getDouble("portal-gold1-x");
                                    gold1Y = warpSection.getDouble("portal-gold1-y");
                                    gold1Z = warpSection.getDouble("portal-gold1-z");
                                    gold2X = warpSection.getDouble("portal-gold2-x");
                                    gold2Y = warpSection.getDouble("portal-gold2-y");
                                    gold2Z = warpSection.getDouble("portal-gold2-z");
                                    hasGoldBlockLocations = true;
                                }
                            }
                        }

                        Warp warp = new Warp(warpId, warpName, creatorId, worldId, x, y, z, yaw, pitch,
                                cost, password, hasPortal, portalFilling, portalWorldId, portalX, portalY, portalZ);

                        // Set gold block locations if they exist
                        if (hasPortal && hasGoldBlockLocations && warp.hasPortal()) {
                            Portal portal = warp.getPortal();
                            if (portal != null && portal.getLocation() != null && portal.getLocation().getWorld() != null) {
                                World portalWorld = portal.getLocation().getWorld();
                                Location gold1Location = new Location(portalWorld, gold1X, gold1Y, gold1Z);
                                Location gold2Location = new Location(portalWorld, gold2X, gold2Y, gold2Z);
                                portal.setGoldBlock1(gold1Location);
                                portal.setGoldBlock2(gold2Location);
                                plugin.getLogger().info("[DEBUG] Loaded gold block locations for portal in warp: " + warpName);
                            }
                        }

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

        // Load additional portals (for multiple portals per warp)
        ConfigurationSection portalsSection = config.getConfigurationSection("portals");
        if (portalsSection != null) {
            for (String portalKey : portalsSection.getKeys(false)) {
                ConfigurationSection portalSection = portalsSection.getConfigurationSection(portalKey);
                if (portalSection != null) {
                    try {
                        String warpIdStr = portalSection.getString("warp-id");
                        String worldIdStr = portalSection.getString("world");

                        if (warpIdStr == null || worldIdStr == null) {
                            plugin.getLogger().warning("Missing essential UUID field(s) in warps.yml for portal: " + portalKey);
                            continue;
                        }

                        UUID warpId = UUID.fromString(warpIdStr);
                        UUID worldId = UUID.fromString(worldIdStr);
                        double x = portalSection.getDouble("x");
                        double y = portalSection.getDouble("y");
                        double z = portalSection.getDouble("z");
                        String filling = portalSection.getString("filling", "air");

                        // Find the warp by ID
                        Warp warp = null;
                        for (Warp w : warps.values()) {
                            if (w.getId().equals(warpId)) {
                                warp = w;
                                break;
                            }
                        }

                        if (warp == null) {
                            plugin.getLogger().warning("Could not find warp with ID " + warpIdStr + " for portal: " + portalKey);
                            continue;
                        }

                        // Get the world
                        World world = Bukkit.getWorld(worldId);
                        if (world == null) {
                            plugin.getLogger().warning("Could not find world with ID " + worldIdStr + " for portal: " + portalKey);
                            continue;
                        }

                        // Create the portal location
                        Location portalLocation = new Location(world, x, y, z);

                        // Check if this portal is already in the map (from the warp section)
                        if (portalLocations.containsKey(portalLocation)) {
                            continue;
                        }

                        // Create a new Portal object
                        Portal portal = new Portal(portalLocation, filling);

                        // Check if gold block locations are defined
                        if (portalSection.contains("gold1-x") && 
                            portalSection.contains("gold1-y") && 
                            portalSection.contains("gold1-z") && 
                            portalSection.contains("gold2-x") && 
                            portalSection.contains("gold2-y") && 
                            portalSection.contains("gold2-z")) {

                            double gold1X = portalSection.getDouble("gold1-x");
                            double gold1Y = portalSection.getDouble("gold1-y");
                            double gold1Z = portalSection.getDouble("gold1-z");
                            double gold2X = portalSection.getDouble("gold2-x");
                            double gold2Y = portalSection.getDouble("gold2-y");
                            double gold2Z = portalSection.getDouble("gold2-z");

                            Location gold1Location = new Location(world, gold1X, gold1Y, gold1Z);
                            Location gold2Location = new Location(world, gold2X, gold2Y, gold2Z);

                            portal.setGoldBlock1(gold1Location);
                            portal.setGoldBlock2(gold2Location);
                        }

                        // Add the portal to the map
                        portalLocations.put(portalLocation, warp);

                        plugin.getLogger().info("[DEBUG] Loaded additional portal for warp: " + warp.getName());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in warps.yml for portal: " + portalKey);
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
        config.set("portals", null);

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

            // For backward compatibility, still save the main portal in the warp section
            config.set(path + ".has-portal", warp.hasPortal());
            config.set(path + ".portal-filling", warp.getPortalFilling());

            if (warp.hasPortal() && warp.getPortalLocation() != null) {
                Location portalLoc = warp.getPortalLocation();
                config.set(path + ".portal-world", portalLoc.getWorld().getUID().toString());
                config.set(path + ".portal-x", portalLoc.getX());
                config.set(path + ".portal-y", portalLoc.getY());
                config.set(path + ".portal-z", portalLoc.getZ());

                // Save gold block locations if they exist
                Portal portal = warp.getPortal();
                if (portal != null && portal.hasGoldBlockLocations()) {
                    Location gold1 = portal.getGoldBlock1();
                    Location gold2 = portal.getGoldBlock2();

                    config.set(path + ".portal-gold1-x", gold1.getX());
                    config.set(path + ".portal-gold1-y", gold1.getY());
                    config.set(path + ".portal-gold1-z", gold1.getZ());

                    config.set(path + ".portal-gold2-x", gold2.getX());
                    config.set(path + ".portal-gold2-y", gold2.getY());
                    config.set(path + ".portal-gold2-z", gold2.getZ());
                }
            }
        }

        // Save all portals (including those not stored in Warp objects)
        int portalCount = 0;
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            Warp warp = entry.getValue();

            // Skip if the warp doesn't exist anymore (should not happen, but just in case)
            if (!warps.containsValue(warp)) {
                continue;
            }

            String portalPath = "portals.portal" + portalCount;
            config.set(portalPath + ".warp-id", warp.getId().toString());
            config.set(portalPath + ".world", portalLoc.getWorld().getUID().toString());
            config.set(portalPath + ".x", portalLoc.getX());
            config.set(portalPath + ".y", portalLoc.getY());
            config.set(portalPath + ".z", portalLoc.getZ());

            // Try to find gold block locations for this portal
            // First check if this is the main portal stored in the warp
            boolean goldBlocksFound = false;
            if (warp.hasPortal() && warp.getPortalLocation() != null &&
                warp.getPortalLocation().getWorld().equals(portalLoc.getWorld()) &&
                warp.getPortalLocation().getBlockX() == portalLoc.getBlockX() &&
                warp.getPortalLocation().getBlockY() == portalLoc.getBlockY() &&
                warp.getPortalLocation().getBlockZ() == portalLoc.getBlockZ()) {

                Portal portal = warp.getPortal();
                if (portal != null && portal.hasGoldBlockLocations()) {
                    Location gold1 = portal.getGoldBlock1();
                    Location gold2 = portal.getGoldBlock2();

                    config.set(portalPath + ".gold1-x", gold1.getX());
                    config.set(portalPath + ".gold1-y", gold1.getY());
                    config.set(portalPath + ".gold1-z", gold1.getZ());

                    config.set(portalPath + ".gold2-x", gold2.getX());
                    config.set(portalPath + ".gold2-y", gold2.getY());
                    config.set(portalPath + ".gold2-z", gold2.getZ());

                    config.set(portalPath + ".filling", portal.getFilling());
                    goldBlocksFound = true;
                }
            }

            // If gold blocks weren't found, use default filling
            if (!goldBlocksFound) {
                config.set(portalPath + ".filling", warp.getPortalFilling());
            }

            portalCount++;
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

        // Remove all portals associated with this warp
        // First, remove the portal stored in the Warp object (for backward compatibility)
        if (warp.hasPortal() && warp.getPortalLocation() != null) {
            removePortal(warp.getPortalLocation());
        }

        // Then, find and remove all portals linked to this warp from the portalLocations map
        List<Location> locationsToRemove = new ArrayList<>();
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            if (entry.getValue().getId().equals(warp.getId())) {
                locationsToRemove.add(entry.getKey());
                removePortal(entry.getKey());
            }
        }

        // Remove all found locations from the map
        for (Location location : locationsToRemove) {
            portalLocations.remove(location);
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

        // Check if the target gold block is already part of an existing portal
        if (isGoldBlockPartOfPortal(targetBlock)) {
            player.sendMessage(Component.text("This gold block is already part of an existing portal!", NamedTextColor.RED));
            return false;
        }

        // Find the second gold block (diagonal)
        Block secondGoldBlock = findDiagonalGoldBlock(targetBlock);
        if (secondGoldBlock == null) {
            player.sendMessage(Component.text("Could not find a second gold block placed diagonally!", NamedTextColor.RED));
            return false;
        }

        // Check if the second gold block is already part of an existing portal
        if (isGoldBlockPartOfPortal(secondGoldBlock)) {
            player.sendMessage(Component.text("The second gold block is already part of an existing portal!", NamedTextColor.RED));
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
                "iron_bars", "chain"
            );

            if (!validFillings.contains(filling)) {
                player.sendMessage(Component.text("Invalid filling material! Valid options are: " + String.join(", ", validFillings), NamedTextColor.RED));
                return false;
            }
        }

        // Create portal
        Location portalLocation = createPortal(targetBlock, secondGoldBlock, filling);

        // Create a new Portal entity with both gold block locations
        Location goldBlock1Location = targetBlock.getLocation();
        Location goldBlock2Location = secondGoldBlock.getLocation();
        Portal portal = new Portal(portalLocation, goldBlock1Location, goldBlock2Location, filling);

        // Store the portal in the warp for backward compatibility
        // Note: This will overwrite the previous portal in the Warp object,
        // but we're still keeping all portals in the portalLocations map
        warp.setPortal(portal);

        // Add the new portal location to the map (without removing old ones)
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
    public boolean isPortalBlock(Material type) {
        return type == Material.CHISELED_STONE_BRICKS || type == Material.GOLD_BLOCK ||
                type == Material.WATER || type == Material.LAVA ||
                // Legacy glass block (for backward compatibility)
                type == Material.BLUE_STAINED_GLASS ||
                // Iron bars
                type == Material.IRON_BARS ||
                // Chain
                type == Material.CHAIN;
    }

    /**
     * Checks if a portal block material is walkable (players can move through it).
     *
     * @param type The material type to check
     * @return True if the material is walkable, false otherwise
     */
    public boolean isWalkablePortalBlock(Material type) {
        // Air is always walkable
        if (type == Material.AIR) return true;

        // Glass blocks are walkable
        if (type == Material.BLUE_STAINED_GLASS) return true;

        // The following materials block movement and are not walkable
        if (type == Material.WATER || type == Material.LAVA ||
            type == Material.IRON_BARS ||
            type == Material.CHAIN) {
            return false;
        }

        // Default to true for any other materials
        return true;
    }

    /**
     * Removes a portal.
     *
     * @param location The center location of the portal
     */
    private void removePortal(Location location) {
        // Find the warp associated with this portal
        Warp warp = portalLocations.get(location);
        if (warp != null && warp.hasPortal()) {
            // Use the Portal object to remove the portal
            removePortal(warp.getPortal());
        } else {
            // Fallback to the old method if we can't find a Portal object
            removePortalByLocation(location);
        }
    }

    /**
     * Removes a portal using the Portal object.
     *
     * @param portal The Portal object
     */
    private void removePortal(Portal portal) {
        if (portal == null) return;

        Location location = portal.getLocation();
        World world = location != null ? location.getWorld() : null;
        if (world == null) return;

        Set<Block> portalBlocks = new HashSet<>();

        // If we have both gold block locations, use them to define the portal area
        if (portal.hasGoldBlockLocations()) {
            Location gold1 = portal.getGoldBlock1();
            Location gold2 = portal.getGoldBlock2();

            // Get the min and max coordinates to form a box
            int minX = Math.min(gold1.getBlockX(), gold2.getBlockX());
            int minY = Math.min(gold1.getBlockY(), gold2.getBlockY());
            int minZ = Math.min(gold1.getBlockZ(), gold2.getBlockZ());
            int maxX = Math.max(gold1.getBlockX(), gold2.getBlockX());
            int maxY = Math.max(gold1.getBlockY(), gold2.getBlockY());
            int maxZ = Math.max(gold1.getBlockZ(), gold2.getBlockZ());

            // Find all portal blocks within the box
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (isPortalBlock(block.getType())) {
                            portalBlocks.add(block);
                        }
                    }
                }
            }

            plugin.getLogger().info("[DEBUG] Removed portal using gold block locations: " + 
                                   gold1.getBlockX() + "," + gold1.getBlockY() + "," + gold1.getBlockZ() + " to " +
                                   gold2.getBlockX() + "," + gold2.getBlockY() + "," + gold2.getBlockZ());
        } else {
            // Fallback to the old method if we don't have gold block locations
            removePortalByLocation(location);
            return;
        }

        // Remove all found portal blocks
        for (Block block : portalBlocks) {
            block.setType(Material.AIR);
        }

        plugin.getLogger().info("[DEBUG] Removed " + portalBlocks.size() + " portal blocks");
    }

    /**
     * Removes a portal using the center location (legacy method).
     *
     * @param location The center location of the portal
     */
    private void removePortalByLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Find the frame by searching outward from the center
        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        // Use a more comprehensive search to find all portal blocks
        int radius = 10; // Increased search radius to catch more of the portal
        Set<Block> portalBlocks = new HashSet<>();
        Set<Block> checkedBlocks = new HashSet<>();

        // Start with the center block
        Block centerBlock = world.getBlockAt(centerX, centerY, centerZ);
        Queue<Block> blocksToCheck = new LinkedList<>();
        blocksToCheck.add(centerBlock);

        // Use a breadth-first search to find all connected portal blocks
        while (!blocksToCheck.isEmpty()) {
            Block currentBlock = blocksToCheck.poll();

            // Skip if we've already checked this block
            if (checkedBlocks.contains(currentBlock)) {
                continue;
            }

            // Mark as checked
            checkedBlocks.add(currentBlock);

            // If it's a portal block, add it to our list and check its neighbors
            if (isPortalBlock(currentBlock.getType())) {
                portalBlocks.add(currentBlock);

                // Check all 26 neighboring blocks (3x3x3 cube around the current block)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            // Skip the current block
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }

                            Block neighbor = currentBlock.getRelative(dx, dy, dz);

                            // Only add to queue if we haven't checked it yet and it's within our search radius
                            if (!checkedBlocks.contains(neighbor) && 
                                Math.abs(neighbor.getX() - centerX) <= radius &&
                                Math.abs(neighbor.getY() - centerY) <= radius &&
                                Math.abs(neighbor.getZ() - centerZ) <= radius) {
                                blocksToCheck.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        // Also search in a cube around the center to catch any disconnected portal blocks
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isPortalBlock(block.getType())) {
                        portalBlocks.add(block);
                    }
                }
            }
        }

        // Remove all found portal blocks
        for (Block block : portalBlocks) {
            block.setType(Material.AIR);
        }

        plugin.getLogger().info("[DEBUG] Removed " + portalBlocks.size() + " portal blocks using legacy method");
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
        boolean success = false;
        if (player.isOp() || player.hasPermission("furious.teleport.admin")) {
            // Teleport immediately
            success = player.teleport(location);
            if (success) {
                player.sendMessage(Component.text("Teleported to warp '" + warpName + "'!", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to teleport to warp '" + warpName + "'!", NamedTextColor.RED));
            }
        } else {
            // Start teleport sequence with countdown and nausea effect
            plugin.getTeleportManager().teleportQueue(player, location);
            // teleportQueue doesn't return a success value, but it will always queue the teleport
            player.sendMessage(Component.text("Starting teleport to warp '" + warpName + "'...", NamedTextColor.YELLOW));
            success = true; // Assume success since teleportQueue doesn't return a value
        }

        return success;
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

        // If portalLocations is empty, log a warning but continue with other checks
        if (portalLocations.isEmpty()) {
            plugin.getLogger().warning("[DEBUG] portalLocations map is empty! No portals have been created yet.");
            // Don't return null here, continue with other checks
        }

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

        // If we still haven't found a portal, check if the player is standing on a walkable portal block
        // or if they're standing in AIR within a portal area
        Block block = location.getBlock();
        Material blockType = block.getType();
        plugin.getLogger().info("[DEBUG] Player is standing on block type: " + blockType.name());
        plugin.getLogger().info("[DEBUG] isPortalBlock result: " + isPortalBlock(blockType));
        plugin.getLogger().info("[DEBUG] isWalkablePortalBlock result: " + isWalkablePortalBlock(blockType));

        // Check if player is standing on a walkable portal block
        // This ensures we only detect portals with fillings that players can walk through
        if (isWalkablePortalBlock(blockType)) {
            // Search for a portal center in a larger area
            plugin.getLogger().info("[DEBUG] Player is standing on a walkable portal block, checking larger area with radius 5");
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

        // As a last resort, check if any warp has a portal with gold block locations that contain the player's location
        plugin.getLogger().info("[DEBUG] Checking if player is between gold blocks of any portal");
        int warpCount = 0;
        for (Warp w : warps.values()) {
            warpCount++;
            if (w.hasPortal()) {
                plugin.getLogger().info("[DEBUG] Checking warp " + w.getName() + " (has portal)");
                if (isPlayerBetweenGoldBlocks(location, w)) {
                    plugin.getLogger().info("[DEBUG] Player is between gold blocks for warp: " + w.getName());
                    return w;
                }
            }
        }
        plugin.getLogger().info("[DEBUG] Checked " + warpCount + " warps, none had the player between gold blocks");

        plugin.getLogger().info("[DEBUG] No portal found for location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        return null;
    }

    /**
     * Checks if a player is between the gold blocks that define a portal.
     *
     * @param location The player's location
     * @param warp The warp to check
     * @return true if the player is between the gold blocks, false otherwise
     */
    public boolean isPlayerBetweenGoldBlocks(Location location, Warp warp) {
        if (warp == null) {
            plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: warp is null");
            return false;
        }

        if (!warp.hasPortal()) {
            plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: warp " + warp.getName() + " has no portal");
            return false;
        }

        Portal portal = warp.getPortal();
        if (portal == null) {
            plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: portal is null for warp " + warp.getName());
            return false;
        }

        if (!portal.hasGoldBlockLocations()) {
            plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: portal for warp " + warp.getName() + " has no gold block locations");
            return false;
        }

        Location gold1 = portal.getGoldBlock1();
        Location gold2 = portal.getGoldBlock2();

        plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: checking for warp " + warp.getName() + 
                               " with gold blocks at " + gold1.getBlockX() + "," + gold1.getBlockY() + "," + gold1.getBlockZ() + 
                               " and " + gold2.getBlockX() + "," + gold2.getBlockY() + "," + gold2.getBlockZ());

        // Check if the player is in the same world as the portal
        if (!location.getWorld().equals(gold1.getWorld())) {
            plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: player is in a different world");
            return false;
        }

        // Get the min and max coordinates to form a box
        int minX = Math.min(gold1.getBlockX(), gold2.getBlockX());
        int minY = Math.min(gold1.getBlockY(), gold2.getBlockY());
        int minZ = Math.min(gold1.getBlockZ(), gold2.getBlockZ());
        int maxX = Math.max(gold1.getBlockX(), gold2.getBlockX());
        int maxY = Math.max(gold1.getBlockY(), gold2.getBlockY());
        int maxZ = Math.max(gold1.getBlockZ(), gold2.getBlockZ());

        // Check if the player is within the box defined by the gold blocks
        int playerX = location.getBlockX();
        int playerY = location.getBlockY();
        int playerZ = location.getBlockZ();

        plugin.getLogger().info("[DEBUG] isPlayerBetweenGoldBlocks: player at " + playerX + "," + playerY + "," + playerZ + 
                               " checking box " + minX + "," + minY + "," + minZ + " to " + maxX + "," + maxY + "," + maxZ);

        // Use exact frame boundaries for more precise portal detection
        boolean isInBox = (playerX >= minX && playerX <= maxX &&
                           playerY >= minY && playerY <= maxY &&
                           playerZ >= minZ && playerZ <= maxZ);

        if (isInBox) {
            plugin.getLogger().info("[DEBUG] Player is between gold blocks for warp: " + warp.getName());
        } else {
            plugin.getLogger().info("[DEBUG] Player is NOT between gold blocks for warp: " + warp.getName());
        }

        return isInBox;
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

        plugin.getLogger().info("[DEBUG] findWarpByBlockCoordinates checking location: " + x + ", " + y + ", " + z + " in world: " + world.getName());

        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            if (portalLoc.getWorld().equals(world) &&
                portalLoc.getBlockX() == x &&
                portalLoc.getBlockY() == y &&
                portalLoc.getBlockZ() == z) {
                plugin.getLogger().info("[DEBUG] Found exact match for portal at: " + x + ", " + y + ", " + z);
                return entry.getValue();
            }
        }

        // If no exact match was found, try a more lenient approach
        // Check if any portal is within 1 block of the given location
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            if (portalLoc.getWorld().equals(world) &&
                Math.abs(portalLoc.getBlockX() - x) <= 1 &&
                Math.abs(portalLoc.getBlockY() - y) <= 1 &&
                Math.abs(portalLoc.getBlockZ() - z) <= 1) {
                plugin.getLogger().info("[DEBUG] Found nearby portal at: " + portalLoc.getBlockX() + ", " + portalLoc.getBlockY() + ", " + portalLoc.getBlockZ());
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
                        // Remove only this specific portal
                        portalLocations.remove(checkLoc);
                        removePortal(checkLoc);

                        // Check if this was the portal stored in the Warp object
                        if (warp.hasPortal() && warp.getPortalLocation() != null && 
                            warp.getPortalLocation().getWorld().equals(checkLoc.getWorld()) &&
                            warp.getPortalLocation().getBlockX() == checkLoc.getBlockX() &&
                            warp.getPortalLocation().getBlockY() == checkLoc.getBlockY() &&
                            warp.getPortalLocation().getBlockZ() == checkLoc.getBlockZ()) {

                            // This was the portal stored in the Warp object
                            // Check if there are any other portals for this warp
                            Portal newPortal = null;
                            for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
                                if (entry.getValue().getId().equals(warp.getId())) {
                                    // Found another portal for this warp, use it as the new main portal
                                    Location portalLoc = entry.getKey();
                                    World world = portalLoc.getWorld();

                                    // Create a new Portal object for the Warp
                                    newPortal = new Portal(portalLoc, warp.getPortalFilling());
                                    break;
                                }
                            }

                            // Update the warp with the new portal or null if no other portals exist
                            warp.setPortal(newPortal);
                        }

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
     * Checks if a gold block is already part of an existing portal.
     *
     * @param block The gold block to check
     * @return true if the block is part of an existing portal, false otherwise
     */
    private boolean isGoldBlockPartOfPortal(Block block) {
        // Check if the block is a gold block
        if (block.getType() != Material.GOLD_BLOCK) {
            return false;
        }

        // Check all portals to see if this gold block is one of their corner blocks
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Warp warp = entry.getValue();
            if (warp.hasPortal()) {
                Portal portal = warp.getPortal();
                if (portal != null && portal.hasGoldBlockLocations()) {
                    Location gold1 = portal.getGoldBlock1();
                    Location gold2 = portal.getGoldBlock2();

                    // Check if this block matches either gold block location
                    if ((gold1.getWorld().equals(block.getWorld()) &&
                         gold1.getBlockX() == block.getX() &&
                         gold1.getBlockY() == block.getY() &&
                         gold1.getBlockZ() == block.getZ()) ||
                        (gold2.getWorld().equals(block.getWorld()) &&
                         gold2.getBlockX() == block.getX() &&
                         gold2.getBlockY() == block.getY() &&
                         gold2.getBlockZ() == block.getZ())) {
                        return true;
                    }
                }
            }
        }

        // Also check if the block is within the bounds of any existing portal
        // This handles cases where the gold block might be part of a portal but not one of the corner blocks
        for (Map.Entry<Location, Warp> entry : portalLocations.entrySet()) {
            Location portalLoc = entry.getKey();
            Warp warp = entry.getValue();

            if (warp.hasPortal()) {
                Portal portal = warp.getPortal();
                if (portal != null && portal.hasGoldBlockLocations()) {
                    Location gold1 = portal.getGoldBlock1();
                    Location gold2 = portal.getGoldBlock2();

                    // Get the min and max coordinates to form a box
                    int minX = Math.min(gold1.getBlockX(), gold2.getBlockX());
                    int minY = Math.min(gold1.getBlockY(), gold2.getBlockY());
                    int minZ = Math.min(gold1.getBlockZ(), gold2.getBlockZ());
                    int maxX = Math.max(gold1.getBlockX(), gold2.getBlockX());
                    int maxY = Math.max(gold1.getBlockY(), gold2.getBlockY());
                    int maxZ = Math.max(gold1.getBlockZ(), gold2.getBlockZ());

                    // Check if the block is within the portal bounds
                    if (block.getWorld().equals(gold1.getWorld()) &&
                        block.getX() >= minX && block.getX() <= maxX &&
                        block.getY() >= minY && block.getY() <= maxY &&
                        block.getZ() >= minZ && block.getZ() <= maxZ) {
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
