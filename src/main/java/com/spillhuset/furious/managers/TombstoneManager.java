package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Tombstone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages tombstones in the game.
 */
public class TombstoneManager {
    private final Furious plugin;
    private final Map<UUID, Tombstone> tombstones; // Tombstone UUID -> Tombstone
    private final Map<UUID, Set<UUID>> playerTombstones; // Player UUID -> Set of Tombstone UUIDs
    private final File configFile;
    private FileConfiguration config;
    private final int TOMBSTONE_TIMEOUT_SECONDS;
    private final boolean OWNER_ONLY_ACCESS;
    private BukkitTask cleanupTask;

    /**
     * Creates a new TombstoneManager.
     *
     * @param plugin The plugin instance
     */
    public TombstoneManager(Furious plugin) {
        this.plugin = plugin;
        this.tombstones = new HashMap<>();
        this.playerTombstones = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "tombstones.yml");

        // Load configuration
        loadConfiguration();

        // Get configuration values from main config
        FileConfiguration mainConfig = plugin.getConfig();
        this.TOMBSTONE_TIMEOUT_SECONDS = mainConfig.getInt("tombstones.timeout-seconds", 1800); // Default: 30 minutes
        this.OWNER_ONLY_ACCESS = mainConfig.getBoolean("tombstones.owner-only-access", true);

        // Set default values in tombstones.yml if they don't exist
        if (!config.contains("timeout-seconds")) {
            config.set("timeout-seconds", TOMBSTONE_TIMEOUT_SECONDS);
        }
        if (!config.contains("owner-only-access")) {
            config.set("owner-only-access", OWNER_ONLY_ACCESS);
        }

        // Start cleanup task (runs every minute)
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredTombstones, 1200L, 1200L);
    }

    /**
     * Loads tombstone data from the configuration file.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create tombstones.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Set default values if they don't exist
        if (!config.contains("timeout-seconds")) {
            config.set("timeout-seconds", 1800); // 30 minutes in seconds
        }
        if (!config.contains("owner-only-access")) {
            config.set("owner-only-access", true);
        }

        // Load tombstones
        ConfigurationSection tombstonesSection = config.getConfigurationSection("tombstones");
        if (tombstonesSection != null) {
            for (String tombstoneIdStr : tombstonesSection.getKeys(false)) {
                try {
                    UUID tombstoneId = UUID.fromString(tombstoneIdStr);
                    ConfigurationSection tombstoneSection = tombstonesSection.getConfigurationSection(tombstoneIdStr);

                    if (tombstoneSection != null) {
                        UUID playerId = UUID.fromString(tombstoneSection.getString("player-id"));
                        String playerName = tombstoneSection.getString("player-name");
                        long creationTime = tombstoneSection.getLong("creation-time");

                        // Load location
                        ConfigurationSection locationSection = tombstoneSection.getConfigurationSection("location");
                        if (locationSection == null) {
                            plugin.getLogger().warning("Invalid location for tombstone " + tombstoneIdStr);
                            continue;
                        }

                        String worldName = locationSection.getString("world");
                        double x = locationSection.getDouble("x");
                        double y = locationSection.getDouble("y");
                        double z = locationSection.getDouble("z");
                        float yaw = (float) locationSection.getDouble("yaw");
                        float pitch = (float) locationSection.getDouble("pitch");

                        Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

                        // Find the armor stand entity
                        ArmorStand armorStand = null;
                        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1)) {
                            if (entity instanceof ArmorStand && entity.getUniqueId().toString().equals(tombstoneSection.getString("armor-stand-id"))) {
                                armorStand = (ArmorStand) entity;
                                break;
                            }
                        }

                        // If armor stand not found, skip this tombstone
                        if (armorStand == null) {
                            plugin.getLogger().warning("Could not find armor stand for tombstone " + tombstoneIdStr);
                            continue;
                        }

                        // Create the tombstone
                        Tombstone tombstone = new Tombstone(tombstoneId, playerId, playerName, location, armorStand, creationTime);

                        // Load inventory items
                        ConfigurationSection inventorySection = tombstoneSection.getConfigurationSection("inventory");
                        if (inventorySection != null) {
                            for (String slotStr : inventorySection.getKeys(false)) {
                                int slot = Integer.parseInt(slotStr);
                                ItemStack item = inventorySection.getItemStack(slotStr);
                                if (item != null) {
                                    tombstone.getInventory().setItem(slot, item);
                                }
                            }
                        }

                        // Add to maps
                        tombstones.put(tombstoneId, tombstone);
                        playerTombstones.computeIfAbsent(playerId, k -> new HashSet<>()).add(tombstoneId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading tombstone " + tombstoneIdStr + ": " + e.getMessage());
                }
            }
        }

        saveConfiguration();
    }

    /**
     * Saves tombstone data to the configuration file.
     */
    private void saveConfiguration() {
        // Clear existing data
        config.set("tombstones", null);

        // Save tombstones
        for (Map.Entry<UUID, Tombstone> entry : tombstones.entrySet()) {
            UUID tombstoneId = entry.getKey();
            Tombstone tombstone = entry.getValue();

            String tombstonePath = "tombstones." + tombstoneId.toString();

            config.set(tombstonePath + ".player-id", tombstone.getPlayerId().toString());
            config.set(tombstonePath + ".player-name", tombstone.getPlayerName());
            config.set(tombstonePath + ".creation-time", tombstone.getCreationTime());
            config.set(tombstonePath + ".armor-stand-id", tombstone.getArmorStand().getUniqueId().toString());

            // Save location
            Location location = tombstone.getLocation();
            config.set(tombstonePath + ".location.world", location.getWorld().getName());
            config.set(tombstonePath + ".location.x", location.getX());
            config.set(tombstonePath + ".location.y", location.getY());
            config.set(tombstonePath + ".location.z", location.getZ());
            config.set(tombstonePath + ".location.yaw", location.getYaw());
            config.set(tombstonePath + ".location.pitch", location.getPitch());

            // Save inventory
            for (int i = 0; i < tombstone.getInventory().getSize(); i++) {
                ItemStack item = tombstone.getInventory().getItem(i);
                if (item != null) {
                    config.set(tombstonePath + ".inventory." + i, item);
                }
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save tombstones.yml: " + e.getMessage());
        }
    }

    /**
     * Creates a tombstone for a player at their death location.
     *
     * @param player The player who died
     * @param deathLocation The location where the player died
     * @return The created tombstone, or null if creation failed
     */
    public Tombstone createTombstone(Player player, Location deathLocation) {
        // Check if player is in a minigame
        if (plugin.getMinigameManager().isInGame(player)) {
            return null;
        }

        // Check if player has admin permission or is op
        if (player.hasPermission("furious.tombstones.admin") || player.isOp()) {
            // Admin/op players don't get tombstones
            return null;
        }

        // Check if the death location is in a guild's claimed chunk
        Chunk chunk = deathLocation.getChunk();
        if (plugin.getGuildManager().isChunkClaimed(chunk)) {
            // Make sure we don't destroy guild structures by placing the tombstone
            // Find a safe location nearby
            deathLocation = findSafeLocation(deathLocation);
            if (deathLocation == null) {
                plugin.getLogger().warning("Could not find a safe location for tombstone near " + player.getName() + "'s death location");
                return null;
            }
        }

        // Create the armor stand
        ArmorStand armorStand = (ArmorStand) deathLocation.getWorld().spawnEntity(deathLocation, EntityType.ARMOR_STAND);
        armorStand.customName(Component.text("Tombstone of " + player.getName()));
        armorStand.setCustomNameVisible(true);
        armorStand.setVisible(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setCanPickupItems(false);

        // Set armor stand equipment to make it look like a tombstone with player's head
        ItemStack armorStandHead = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta armorStandSkullMeta = (org.bukkit.inventory.meta.SkullMeta) armorStandHead.getItemMeta();
        if (armorStandSkullMeta != null) {
            armorStandSkullMeta.setOwningPlayer(player);
            armorStandHead.setItemMeta(armorStandSkullMeta);
        }
        armorStand.getEquipment().setHelmet(armorStandHead);

        // Create the tombstone object
        UUID tombstoneId = UUID.randomUUID();
        Tombstone tombstone = new Tombstone(tombstoneId, player.getUniqueId(), player.getName(), deathLocation, armorStand, System.currentTimeMillis());

        // Copy player inventory to tombstone
        PlayerInventory playerInventory = player.getInventory();
        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack item = playerInventory.getItem(i);
            if (item != null) {
                tombstone.getInventory().setItem(i, item.clone());
                playerInventory.setItem(i, null);
            }
        }

        // Copy armor
        if (playerInventory.getHelmet() != null) {
            tombstone.getInventory().setItem(36, playerInventory.getHelmet().clone());
            playerInventory.setHelmet(null);
        }
        if (playerInventory.getChestplate() != null) {
            tombstone.getInventory().setItem(37, playerInventory.getChestplate().clone());
            playerInventory.setChestplate(null);
        }
        if (playerInventory.getLeggings() != null) {
            tombstone.getInventory().setItem(38, playerInventory.getLeggings().clone());
            playerInventory.setLeggings(null);
        }
        if (playerInventory.getBoots() != null) {
            tombstone.getInventory().setItem(39, playerInventory.getBoots().clone());
            playerInventory.setBoots(null);
        }

        // Copy offhand
        if (playerInventory.getItemInOffHand().getType() != Material.AIR) {
            tombstone.getInventory().setItem(40, playerInventory.getItemInOffHand().clone());
            playerInventory.setItemInOffHand(new ItemStack(Material.AIR));
        }

        // Add player head with player's name
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            playerHead.setItemMeta(skullMeta);
        }
        tombstone.getInventory().setItem(41, playerHead);

        // Add wallet with scraps
        double walletAmount = plugin.getWalletManager().getWallet(player.getUniqueId());
        if (walletAmount > 0) {
            ItemStack scrapItem = plugin.createScrapItem(walletAmount);
            tombstone.getInventory().setItem(42, scrapItem);
            plugin.getWalletManager().setWallet(player.getUniqueId(), 0);
        }

        // Add to maps
        tombstones.put(tombstoneId, tombstone);
        playerTombstones.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(tombstoneId);

        // Save configuration
        saveConfiguration();

        // Notify player
        player.sendMessage(Component.text("Your items have been stored in a tombstone at your death location.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("You have " + (TOMBSTONE_TIMEOUT_SECONDS / 60) + " minutes to retrieve them!", NamedTextColor.YELLOW));

        return tombstone;
    }

    /**
     * Finds a safe location near the given location to place a tombstone.
     *
     * @param location The original location
     * @return A safe location, or null if none found
     */
    private Location findSafeLocation(Location location) {
        // Try the original location first
        if (isSafeLocation(location)) {
            return location;
        }

        // Try locations in a 3x3 grid around the original location
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip the original location

                Location testLocation = location.clone().add(x, 0, z);
                if (isSafeLocation(testLocation)) {
                    return testLocation;
                }
            }
        }

        // Try locations in a 5x5 grid
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue; // Skip the already checked locations

                Location testLocation = location.clone().add(x, 0, z);
                if (isSafeLocation(testLocation)) {
                    return testLocation;
                }
            }
        }

        // If we still haven't found a safe location, try above the original location
        for (int y = 1; y <= 5; y++) {
            Location testLocation = location.clone().add(0, y, 0);
            if (isSafeLocation(testLocation)) {
                return testLocation;
            }
        }

        return null;
    }

    /**
     * Checks if a location is safe for placing a tombstone.
     *
     * @param location The location to check
     * @return true if the location is safe, false otherwise
     */
    private boolean isSafeLocation(Location location) {
        // Check if the block at the location is air or a non-solid block
        return location.getBlock().getType() == Material.AIR || !location.getBlock().getType().isSolid();
    }

    /**
     * Opens a tombstone inventory for a player.
     *
     * @param player The player
     * @param tombstone The tombstone
     * @return true if the inventory was opened, false otherwise
     */
    public boolean openTombstone(Player player, Tombstone tombstone) {
        // Check if the player is allowed to access this tombstone
        if (OWNER_ONLY_ACCESS && !tombstone.getPlayerId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("This tombstone belongs to " + tombstone.getPlayerName() + ".", NamedTextColor.RED));
            return false;
        }

        // Open the inventory
        player.openInventory(tombstone.getInventory());
        return true;
    }

    /**
     * Removes a tombstone.
     *
     * @param tombstoneId The ID of the tombstone to remove
     */
    public void removeTombstone(UUID tombstoneId) {
        Tombstone tombstone = tombstones.get(tombstoneId);
        if (tombstone != null) {
            // Remove the armor stand
            tombstone.getArmorStand().remove();

            // Remove from maps
            tombstones.remove(tombstoneId);
            Set<UUID> playerTombstoneSet = playerTombstones.get(tombstone.getPlayerId());
            if (playerTombstoneSet != null) {
                playerTombstoneSet.remove(tombstoneId);
                if (playerTombstoneSet.isEmpty()) {
                    playerTombstones.remove(tombstone.getPlayerId());
                }
            }

            // Save configuration
            saveConfiguration();
        }
    }

    /**
     * Checks if a tombstone is empty.
     *
     * @param tombstone The tombstone to check
     * @return true if the tombstone is empty, false otherwise
     */
    public boolean isTombstoneEmpty(Tombstone tombstone) {
        for (ItemStack item : tombstone.getInventory().getContents()) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a tombstone has expired.
     *
     * @param tombstone The tombstone to check
     * @return true if the tombstone has expired, false otherwise
     */
    public boolean isTombstoneExpired(Tombstone tombstone) {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - tombstone.getCreationTime()) / 1000;
        return elapsedSeconds >= TOMBSTONE_TIMEOUT_SECONDS;
    }

    /**
     * Cleans up expired tombstones.
     */
    private void cleanupExpiredTombstones() {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Tombstone> entry : tombstones.entrySet()) {
            Tombstone tombstone = entry.getValue();
            if (isTombstoneExpired(tombstone)) {
                toRemove.add(entry.getKey());

                // Notify the player if they're online
                Player player = Bukkit.getPlayer(tombstone.getPlayerId());
                if (player != null && player.isOnline()) {
                    player.sendMessage(Component.text("Your tombstone has expired and your items have been lost.", NamedTextColor.RED));
                }
            }
        }

        for (UUID tombstoneId : toRemove) {
            removeTombstone(tombstoneId);
        }
    }

    /**
     * Gets a tombstone by its armor stand entity.
     *
     * @param armorStand The armor stand entity
     * @return The tombstone, or null if not found
     */
    public Tombstone getTombstoneByArmorStand(ArmorStand armorStand) {
        for (Tombstone tombstone : tombstones.values()) {
            if (tombstone.getArmorStand().getUniqueId().equals(armorStand.getUniqueId())) {
                return tombstone;
            }
        }
        return null;
    }

    /**
     * Gets all tombstones for a player.
     *
     * @param playerId The ID of the player
     * @return A set of tombstones, or an empty set if none found
     */
    public Set<Tombstone> getPlayerTombstones(UUID playerId) {
        Set<Tombstone> result = new HashSet<>();
        Set<UUID> tombstoneIds = playerTombstones.get(playerId);
        if (tombstoneIds != null) {
            for (UUID tombstoneId : tombstoneIds) {
                Tombstone tombstone = tombstones.get(tombstoneId);
                if (tombstone != null) {
                    result.add(tombstone);
                }
            }
        }
        return result;
    }

    /**
     * Gets all tombstones.
     *
     * @return A collection of all tombstones
     */
    public Collection<Tombstone> getAllTombstones() {
        return tombstones.values();
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        saveConfiguration();
    }
}
