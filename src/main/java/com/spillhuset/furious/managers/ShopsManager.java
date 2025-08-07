package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Shop;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manager class for handling shop operations like buying and selling items.
 */
public class ShopsManager {
    private final Furious plugin;
    private final WalletManager walletManager;
    private final TeleportManager teleportManager;

    // Map to store shops (name -> shop)
    private final Map<String, Shop> shops;

    // Map to store item prices (Material -> price) - Legacy, will be removed
    private final Map<Material, Double> itemPrices;

    /**
     * Constructor for ShopsManager.
     *
     * @param plugin The main plugin instance
     */
    public ShopsManager(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
        this.teleportManager = plugin.getTeleportManager();
        this.shops = new HashMap<>();
        this.itemPrices = new HashMap<>();

        // Initialize with some default prices
        initializeDefaultPrices();
    }

    /**
     * Initialize default prices for common items.
     * In a real implementation, these would likely be loaded from a configuration file.
     */
    private void initializeDefaultPrices() {
        // Basic materials
        itemPrices.put(Material.STONE, 1.0);
        itemPrices.put(Material.DIRT, 0.5);
        itemPrices.put(Material.GRASS_BLOCK, 1.0);
        itemPrices.put(Material.COBBLESTONE, 0.75);

        // Ores and valuable materials
        itemPrices.put(Material.COAL, 2.0);
        itemPrices.put(Material.IRON_INGOT, 10.0);
        itemPrices.put(Material.GOLD_INGOT, 20.0);
        itemPrices.put(Material.DIAMOND, 100.0);
        itemPrices.put(Material.EMERALD, 50.0);

        // Logs and wood
        itemPrices.put(Material.OAK_LOG, 2.0);
        itemPrices.put(Material.SPRUCE_LOG, 2.0);
        itemPrices.put(Material.BIRCH_LOG, 2.0);
        itemPrices.put(Material.JUNGLE_LOG, 2.0);
        itemPrices.put(Material.ACACIA_LOG, 2.0);
        itemPrices.put(Material.DARK_OAK_LOG, 2.0);

        // Food
        itemPrices.put(Material.APPLE, 2.0);
        itemPrices.put(Material.BREAD, 3.0);
        itemPrices.put(Material.COOKED_BEEF, 5.0);
        itemPrices.put(Material.COOKED_CHICKEN, 4.0);
    }

    /**
     * Get the price of an item.
     *
     * @param material The material to get the price for
     * @return The price of the item, or 0 if not found
     */
    public double getItemPrice(Material material) {
        return itemPrices.getOrDefault(material, 0.0);
    }

    /**
     * Set the price of an item.
     *
     * @param material The material to set the price for
     * @param price The price to set
     */
    public void setItemPrice(Material material, double price) {
        itemPrices.put(material, price);
    }

    /**
     * Create a new shop.
     *
     * @param name The name of the shop
     * @return true if the shop was created, false if a shop with that name already exists
     */
    public boolean createShop(String name) {
        if (shops.containsKey(name.toLowerCase())) {
            return false;
        }

        Shop shop = new Shop(UUID.randomUUID(), name);
        shops.put(name.toLowerCase(), shop);
        return true;
    }

    /**
     * Get a shop by name.
     *
     * @param name The name of the shop
     * @return The shop, or null if not found
     */
    public Shop getShop(String name) {
        return shops.get(name.toLowerCase());
    }

    /**
     * Check if a shop exists.
     *
     * @param name The name of the shop
     * @return true if the shop exists, false otherwise
     */
    public boolean shopExists(String name) {
        return shops.containsKey(name.toLowerCase());
    }

    /**
     * Get all shop names.
     *
     * @return A set of all shop names
     */
    public Set<String> getAllShopNames() {
        return new HashSet<>(shops.keySet());
    }

    /**
     * Delete a shop and all its associations.
     *
     * @param shopName The name of the shop to delete
     * @return true if the shop was deleted, false if the shop doesn't exist
     */
    public boolean deleteShop(String shopName) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        // Remove the shop from the shops map
        shops.remove(shopName.toLowerCase());

        plugin.getLogger().log(Level.INFO, "Shop '" + shopName + "' has been deleted");
        return true;
    }

    /**
     * Claim a chunk for a shop.
     *
     * @param shopName The name of the shop
     * @param chunk The chunk to claim
     * @return true if the chunk was claimed, false if the shop doesn't exist or the chunk is already claimed
     */
    public boolean claimChunk(String shopName, Chunk chunk) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        // Check if the chunk is already claimed by another shop
        for (Shop otherShop : shops.values()) {
            if (otherShop != shop) {
                for (String chunkStr : otherShop.getClaimedChunks()) {
                    if (chunkStr.equals(formatChunkString(chunk))) {
                        return false;
                    }
                }
            }
        }

        return shop.claimChunk(chunk);
    }

    /**
     * Format a chunk as a string for storage.
     *
     * @param chunk The chunk to format
     * @return The formatted chunk string
     */
    private String formatChunkString(Chunk chunk) {
        return chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Get the shop that owns a specific chunk.
     *
     * @param chunk The chunk to check
     * @return The shop that owns the chunk, or null if no shop owns it
     */
    public Shop getShopByChunk(Chunk chunk) {
        String chunkStr = formatChunkString(chunk);
        for (Shop shop : shops.values()) {
            if (shop.getClaimedChunks().contains(chunkStr)) {
                return shop;
            }
        }
        return null;
    }

    /**
     * Unclaim a chunk from a shop.
     *
     * @param shopName The name of the shop
     * @param chunk The chunk to unclaim
     * @return true if the chunk was unclaimed, false if the shop doesn't exist or the chunk isn't claimed by the shop
     */
    public boolean unclaimChunk(String shopName, Chunk chunk) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.unclaimChunk(chunk);
    }

    /**
     * Set the spawn location for a shop.
     *
     * @param shopName The name of the shop
     * @param location The location to set as the spawn
     * @return true if the spawn was set, false if the shop doesn't exist
     */
    public boolean setShopSpawn(String shopName, Location location) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        shop.setSpawnLocation(location);
        return true;
    }

    /**
     * Teleport a player to a shop's spawn location.
     *
     * @param player The player to teleport
     * @param shopName The name of the shop to teleport to
     * @return true if the player was teleported, false if the shop doesn't exist or has no spawn location
     */
    public boolean teleportToShop(Player player, String shopName) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        Location spawnLocation = shop.getSpawnLocation();
        if (spawnLocation == null) {
            return false;
        }

        // If player is an operator, bypass the teleport queue
        if (player.isOp()) {
            teleportManager.forceTeleport(player, spawnLocation);
            player.sendMessage("§aTeleported to shop §e" + shopName + "§a immediately (operator bypass).");
        } else {
            // Use the teleport manager to handle the teleportation with queue
            teleportManager.teleportQueue(player, spawnLocation);
        }
        return true;
    }

    /**
     * Add an item to a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to add
     * @return true if the item was added, false if the shop doesn't exist or the item is already in the shop
     */
    public boolean addItemToShop(String shopName, Material material) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.addItem(material);
    }

    /**
     * Remove an item from a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to remove
     * @return true if the item was removed, false if the shop doesn't exist or the item isn't in the shop
     */
    public boolean removeItemFromShop(String shopName, Material material) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.removeItem(material);
    }

    /**
     * Set the stock of an item in a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to set the stock for
     * @param amount The amount to set
     * @return true if the stock was set, false if the shop doesn't exist or the item isn't in the shop
     */
    public boolean setItemStock(String shopName, Material material, int amount) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.setStock(material, amount);
    }

    /**
     * Set the price of an item in a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to set the price for
     * @param price The price to set
     * @return true if the price was set, false if the shop doesn't exist or the item isn't in the shop
     */
    public boolean setItemPrice(String shopName, Material material, double price) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.setPrice(material, price);
    }

    /**
     * Restock all items in a shop.
     *
     * @param shopName The name of the shop
     * @return true if the shop was restocked, false if the shop doesn't exist
     */
    public boolean restockShop(String shopName) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        shop.restock();
        return true;
    }

    /**
     * Toggle whether an item can be bought from a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to toggle
     * @return The new state (true if the item can now be bought, false otherwise), or false if the shop doesn't exist or the item isn't in the shop
     */
    public boolean toggleBuyItem(String shopName, Material material) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.toggleBuy(material);
    }

    /**
     * Toggle whether an item can be sold to a shop.
     *
     * @param shopName The name of the shop
     * @param material The material to toggle
     * @return The new state (true if the item can now be sold, false otherwise), or false if the shop doesn't exist or the item isn't in the shop
     */
    public boolean toggleSellItem(String shopName, Material material) {
        Shop shop = getShop(shopName);
        if (shop == null) {
            return false;
        }

        return shop.toggleSell(material);
    }

    /**
     * Sell an item from the player's hand.
     *
     * @param player The player selling the item
     * @return true if the sale was successful, false otherwise
     */
    public boolean sellItem(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Check if the player is holding an item
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cYou need to hold an item to sell!");
            return false;
        }

        Material material = itemInHand.getType();
        int count = itemInHand.getAmount();

        // Get the shop at the player's location
        Shop shop = getShopByChunk(player.getLocation().getChunk());

        // If the player is not in a shop's claimed chunk, prevent selling
        if (shop == null) {
            player.sendMessage("§cYou must be in a shop's claimed chunk to sell items!");
            return false;
        } else {
            // Check if the shop buys this item
            if (!shop.getItems().contains(material)) {
                player.sendMessage("§cThis shop doesn't buy that item!");
                return false;
            }

            // Check if selling is enabled for this item
            if (!shop.canSellToShop(material)) {
                player.sendMessage("§cThis shop is not currently buying this item!");
                return false;
            }

            // Get the buying price from the shop (1.5x the selling price)
            double price = shop.getBuyingPrice(material);

            // Check if the item can be sold
            if (price <= 0) {
                player.sendMessage("§cThis item cannot be sold to this shop!");
                return false;
            }

            // Calculate total price based on item count
            double totalPrice = price * count;

            // Remove the item from the player's hand
            player.getInventory().setItemInMainHand(null);

            // Add money to the player's wallet
            walletManager.deposit(player, totalPrice);

            // Update the shop's stock
            int currentStock = shop.getStock(material);
            shop.setStock(material, currentStock + count);

            // Send success message
            player.sendMessage(String.format("§aYou sold %d %s for %s to %s!",
                    count,
                    material.toString().toLowerCase().replace('_', ' '),
                    walletManager.formatAmount(totalPrice),
                    shop.getName()));

            plugin.getLogger().log(Level.INFO,
                    String.format("Player %s sold %d %s for %s to shop %s",
                            player.getName(),
                            count,
                            material,
                            walletManager.formatAmount(totalPrice),
                            shop.getName()));

            return true;
        }
    }

    /**
     * Buy an item for the player.
     *
     * @param player The player buying the item
     * @param material The material to buy
     * @param count The amount to buy
     * @return true if the purchase was successful, false otherwise
     */
    public boolean buyItem(Player player, Material material, int count) {
        // Check if the material is valid
        if (material == null) {
            player.sendMessage("§cInvalid item name!");
            return false;
        }

        // Get the shop at the player's location
        Shop shop = getShopByChunk(player.getLocation().getChunk());

        // If the player is not in a shop's claimed chunk, prevent buying
        if (shop == null) {
            player.sendMessage("§cYou must be in a shop's claimed chunk to buy items!");
            return false;
        } else {
            // Check if the shop sells this item
            if (!shop.getItems().contains(material)) {
                player.sendMessage("§cThis shop doesn't sell that item!");
                return false;
            }

            // Check if buying is enabled for this item
            if (!shop.canBuyFromShop(material)) {
                player.sendMessage("§cThis shop is not currently selling this item!");
                return false;
            }

            // Check if the item is available
            if (!shop.isItemAvailable(material)) {
                player.sendMessage("§cThis item is currently unavailable!");
                return false;
            }

            // Check if there's enough stock
            int stock = shop.getStock(material);
            if (stock < count) {
                player.sendMessage(String.format("§cNot enough stock! Only %d available.", stock));
                return false;
            }

            // Get the price from the shop
            double price = shop.getSellingPrice(material);
            double totalPrice = price * count;

            // Check if the player has enough money
            if (!walletManager.has(player, totalPrice)) {
                player.sendMessage(String.format("§cYou don't have enough money! You need %s.",
                        walletManager.formatAmount(totalPrice)));
                return false;
            }

            // Create the item stack
            ItemStack itemStack = new ItemStack(material, count);

            // Check if the player has enough inventory space
            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§cYour inventory is full!");
                return false;
            }

            // Withdraw money from the player's wallet
            walletManager.withdraw(player, totalPrice);

            // Update the shop's stock
            shop.setStock(material, stock - count);

            // Give the item to the player
            player.getInventory().addItem(itemStack);

            // Send success message
            player.sendMessage(String.format("§aYou bought %d %s for %s from %s!",
                    count,
                    material.toString().toLowerCase().replace('_', ' '),
                    walletManager.formatAmount(totalPrice),
                    shop.getName()));

            plugin.getLogger().log(Level.INFO,
                    String.format("Player %s bought %d %s for %s from shop %s",
                            player.getName(),
                            count,
                            material,
                            walletManager.formatAmount(totalPrice),
                            shop.getName()));

            return true;
        }
    }
}