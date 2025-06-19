package com.spillhuset.furious.minigames.hungergames;

import com.spillhuset.furious.Furious;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Manages container registration and filling for hunger games
 */
public class ContainerRegistry {
    private final Furious plugin;
    private final Map<String, Map<Location, ContainerType>> containerLocations;
    private final Map<String, BukkitTask> restockTasks;
    private FileConfiguration config;
    private File configFile;

    // Item pools for different rarity levels
    private final List<Material> commonItems;
    private final List<Material> uncommonItems;
    private final List<Material> rareItems;

    // Default restock time in seconds
    private static final int DEFAULT_RESTOCK_TIME = 300; // 5 minutes

    /**
     * Constructor for ContainerRegistry
     *
     * @param plugin The plugin instance
     */
    public ContainerRegistry(Furious plugin) {
        this.plugin = plugin;
        this.containerLocations = new HashMap<>();
        this.restockTasks = new HashMap<>();

        // Initialize item pools
        this.commonItems = new ArrayList<>();
        this.uncommonItems = new ArrayList<>();
        this.rareItems = new ArrayList<>();

        // Load configuration
        loadConfiguration();
        initializeItemPools();
    }

    /**
     * Loads the configuration from file
     */
    private void loadConfiguration() {
        configFile = new File(plugin.getDataFolder(), "containers.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create containers.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Set default values if they don't exist
        if (!config.contains("restockTime")) {
            config.set("restockTime", DEFAULT_RESTOCK_TIME);
        }

        // Load container locations
        ConfigurationSection mapsSection = config.getConfigurationSection("maps");
        if (mapsSection != null) {
            for (String mapName : mapsSection.getKeys(false)) {
                ConfigurationSection mapSection = mapsSection.getConfigurationSection(mapName);
                if (mapSection != null) {
                    Map<Location, ContainerType> containers = new HashMap<>();

                    ConfigurationSection containersSection = mapSection.getConfigurationSection("containers");
                    if (containersSection != null) {
                        for (String key : containersSection.getKeys(false)) {
                            ConfigurationSection containerSection = containersSection.getConfigurationSection(key);
                            if (containerSection != null) {
                                String worldName = containerSection.getString("world");
                                World world = plugin.getServer().getWorld(worldName);
                                if (world != null) {
                                    Location location = new Location(
                                            world,
                                            containerSection.getDouble("x"),
                                            containerSection.getDouble("y"),
                                            containerSection.getDouble("z")
                                    );
                                    ContainerType type = ContainerType.valueOf(containerSection.getString("type"));
                                    containers.put(location, type);
                                }
                            }
                        }
                    }

                    containerLocations.put(mapName, containers);
                }
            }
        }

        saveConfiguration();
    }

    /**
     * Saves the configuration to file
     */
    public void saveConfiguration() {
        try {
            // Save container locations
            ConfigurationSection mapsSection = config.createSection("maps");
            for (Map.Entry<String, Map<Location, ContainerType>> mapEntry : containerLocations.entrySet()) {
                ConfigurationSection mapSection = mapsSection.createSection(mapEntry.getKey());
                ConfigurationSection containersSection = mapSection.createSection("containers");

                int index = 0;
                for (Map.Entry<Location, ContainerType> containerEntry : mapEntry.getValue().entrySet()) {
                    Location location = containerEntry.getKey();
                    ContainerType type = containerEntry.getValue();

                    ConfigurationSection containerSection = containersSection.createSection(String.valueOf(index));
                    containerSection.set("world", location.getWorld().getName());
                    containerSection.set("x", location.getX());
                    containerSection.set("y", location.getY());
                    containerSection.set("z", location.getZ());
                    containerSection.set("type", type.name());

                    index++;
                }
            }

            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save containers.yml", e);
        }
    }

    /**
     * Initializes the item pools for different rarity levels
     */
    private void initializeItemPools() {
        // Common items (weapons, food, basic resources)
        commonItems.add(Material.WOODEN_SWORD);
        commonItems.add(Material.STONE_SWORD);
        commonItems.add(Material.WOODEN_AXE);
        commonItems.add(Material.STONE_AXE);
        commonItems.add(Material.BREAD);
        commonItems.add(Material.COOKED_BEEF);
        commonItems.add(Material.COOKED_CHICKEN);
        commonItems.add(Material.APPLE);
        commonItems.add(Material.LEATHER_HELMET);
        commonItems.add(Material.LEATHER_CHESTPLATE);
        commonItems.add(Material.LEATHER_LEGGINGS);
        commonItems.add(Material.LEATHER_BOOTS);
        commonItems.add(Material.STICK);
        commonItems.add(Material.ARROW);
        commonItems.add(Material.COBBLESTONE);
        commonItems.add(Material.OAK_PLANKS);

        // Uncommon items (better weapons, armor, and resources)
        uncommonItems.add(Material.IRON_SWORD);
        uncommonItems.add(Material.IRON_AXE);
        uncommonItems.add(Material.BOW);
        uncommonItems.add(Material.CROSSBOW);
        uncommonItems.add(Material.GOLDEN_APPLE);
        uncommonItems.add(Material.IRON_HELMET);
        uncommonItems.add(Material.IRON_CHESTPLATE);
        uncommonItems.add(Material.IRON_LEGGINGS);
        uncommonItems.add(Material.IRON_BOOTS);
        uncommonItems.add(Material.SHIELD);
        uncommonItems.add(Material.FISHING_ROD);
        uncommonItems.add(Material.FLINT_AND_STEEL);

        // Rare items (best weapons, armor, and special items)
        rareItems.add(Material.DIAMOND_SWORD);
        rareItems.add(Material.DIAMOND_AXE);
        rareItems.add(Material.DIAMOND_HELMET);
        rareItems.add(Material.DIAMOND_CHESTPLATE);
        rareItems.add(Material.DIAMOND_LEGGINGS);
        rareItems.add(Material.DIAMOND_BOOTS);
        rareItems.add(Material.ENCHANTED_GOLDEN_APPLE);
        rareItems.add(Material.ENDER_PEARL);
        rareItems.add(Material.TNT);
        rareItems.add(Material.LAVA_BUCKET);
    }

    /**
     * Registers a container at the specified location
     *
     * @param mapName The name of the map
     * @param location The location of the container
     * @param type The type of the container
     */
    public void registerContainer(String mapName, Location location, ContainerType type) {
        Map<Location, ContainerType> containers = containerLocations.computeIfAbsent(mapName, k -> new HashMap<>());
        containers.put(location.clone(), type);
        saveConfiguration();
    }

    /**
     * Unregisters a container at the specified location
     *
     * @param mapName The name of the map
     * @param location The location of the container
     */
    public void unregisterContainer(String mapName, Location location) {
        Map<Location, ContainerType> containers = containerLocations.get(mapName);
        if (containers != null) {
            containers.remove(location);
            saveConfiguration();
        }
    }

    /**
     * Fills all containers in a world with items
     *
     * @param mapName The name of the map
     * @param world The world to fill containers in
     */
    public void fillContainers(String mapName, World world) {
        Map<Location, ContainerType> containers = containerLocations.get(mapName);
        if (containers == null || containers.isEmpty()) {
            plugin.getLogger().warning("No containers registered for map " + mapName);
            return;
        }

        for (Map.Entry<Location, ContainerType> entry : containers.entrySet()) {
            Location location = entry.getKey().clone();
            location.setWorld(world);
            ContainerType type = entry.getValue();

            Block block = world.getBlockAt(location);
            BlockState state = block.getState();

            if (state instanceof Container container) {
                fillContainer(container, type);
            }
        }
    }

    /**
     * Fills a container with items based on its type
     *
     * @param container The container to fill
     * @param type The type of the container
     */
    private void fillContainer(Container container, ContainerType type) {
        Inventory inventory = container.getInventory();
        inventory.clear();

        // Fill the container based on its type
        switch (type) {
            case BARREL:
                // Barrels have consistent content
                fillBarrel(container);
                break;
            case COMMON:
                // Common chests have mostly common items
                fillRandomContainer(inventory, 0.7, 0.25, 0.05);
                break;
            case UNCOMMON:
                // Uncommon chests have a mix of common and uncommon items
                fillRandomContainer(inventory, 0.4, 0.5, 0.1);
                break;
            case RARE:
                // Rare chests have better items
                fillRandomContainer(inventory, 0.2, 0.5, 0.3);
                break;
        }

        container.update();
    }

    /**
     * Fills a barrel with consistent content
     *
     * @param container The barrel to fill
     */
    private void fillBarrel(Container container) {
        Inventory inventory = container.getInventory();

        // Barrels always contain the same items
        if (container instanceof Barrel) {
            // Add some food items
            inventory.addItem(new ItemStack(Material.BREAD, 5));
            inventory.addItem(new ItemStack(Material.COOKED_BEEF, 3));
            inventory.addItem(new ItemStack(Material.APPLE, 2));

            // Add some basic resources
            inventory.addItem(new ItemStack(Material.STICK, 8));
            inventory.addItem(new ItemStack(Material.ARROW, 16));
        }
    }

    /**
     * Fills a container with random items based on rarity probabilities
     *
     * @param inventory The inventory to fill
     * @param commonChance The chance of a common item
     * @param uncommonChance The chance of an uncommon item
     * @param rareChance The chance of a rare item
     */
    private void fillRandomContainer(Inventory inventory, double commonChance, double uncommonChance, double rareChance) {
        // Determine how many items to add (3-8)
        int itemCount = ThreadLocalRandom.current().nextInt(3, 9);

        for (int i = 0; i < itemCount; i++) {
            // Determine the rarity of the item
            double random = Math.random();
            Material material;

            if (random < commonChance) {
                // Common item
                material = commonItems.get(ThreadLocalRandom.current().nextInt(commonItems.size()));
            } else if (random < commonChance + uncommonChance) {
                // Uncommon item
                material = uncommonItems.get(ThreadLocalRandom.current().nextInt(uncommonItems.size()));
            } else {
                // Rare item
                material = rareItems.get(ThreadLocalRandom.current().nextInt(rareItems.size()));
            }

            // Create the item with random amount and durability
            ItemStack item = createRandomizedItem(material);

            // Add the item to a random slot in the inventory
            int slot = ThreadLocalRandom.current().nextInt(inventory.getSize());
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item);
            } else {
                inventory.addItem(item);
            }
        }
    }

    /**
     * Creates a randomized item with varying amount and durability
     *
     * @param material The material of the item
     * @return The randomized item
     */
    private ItemStack createRandomizedItem(Material material) {
        // Determine the amount (1-5 for most items, 1 for weapons/armor)
        int amount = 1;
        if (!material.name().contains("SWORD") &&
            !material.name().contains("AXE") &&
            !material.name().contains("HELMET") &&
            !material.name().contains("CHESTPLATE") &&
            !material.name().contains("LEGGINGS") &&
            !material.name().contains("BOOTS") &&
            !material.name().contains("SHIELD") &&
            !material.name().contains("BOW") &&
            !material.name().contains("CROSSBOW")) {
            amount = ThreadLocalRandom.current().nextInt(1, 6);
        }

        ItemStack item = new ItemStack(material, amount);

        // Randomize durability for damageable items
        if (material.getMaxDurability() > 0) {
            short maxDurability = material.getMaxDurability();
            // Set durability to 70-100% of max
            int durability = (int) (maxDurability * (0.7 + Math.random() * 0.3));
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
                damageable.setDamage(durability);
                item.setItemMeta(damageable);
            }
        }

        return item;
    }

    /**
     * Starts a restock task for a map
     *
     * @param mapName The name of the map
     * @param world The world to restock containers in
     */
    public void startRestockTask(String mapName, World world) {
        // Cancel any existing restock task for this map
        stopRestockTask(mapName);

        // Get the restock time from config
        int restockTime = config.getInt("restockTime", DEFAULT_RESTOCK_TIME);

        // Start a new restock task
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> fillContainers(mapName, world),
                restockTime * 20L, // Initial delay (convert seconds to ticks)
                restockTime * 20L  // Repeat interval (convert seconds to ticks)
        );

        restockTasks.put(mapName, task);
    }

    /**
     * Stops the restock task for a map
     *
     * @param mapName The name of the map
     */
    public void stopRestockTask(String mapName) {
        BukkitTask task = restockTasks.remove(mapName);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Gets the restock time for containers
     *
     * @return The restock time in seconds
     */
    public int getRestockTime() {
        return config.getInt("restockTime", DEFAULT_RESTOCK_TIME);
    }

    /**
     * Sets the restock time for containers
     *
     * @param seconds The restock time in seconds
     */
    public void setRestockTime(int seconds) {
        config.set("restockTime", seconds);
        saveConfiguration();
    }

    /**
     * Shuts down the container registry
     */
    public void shutdown() {
        // Cancel all restock tasks
        for (BukkitTask task : restockTasks.values()) {
            task.cancel();
        }
        restockTasks.clear();

        // Save configuration
        saveConfiguration();
    }

    /**
     * Enum representing different types of containers
     */
    public enum ContainerType {
        BARREL,   // Consistent content
        COMMON,   // Common items
        UNCOMMON, // Mix of common and uncommon items
        RARE      // Better items
    }
}
