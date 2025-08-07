package com.spillhuset.furious.entities;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Entity class representing a shop in the game.
 * Shops can sell items, claim chunks, and have a spawn location.
 *
 * The shop implements a dynamic pricing system where prices are affected by supply and demand:
 * - When players buy a lot of an item, its price gradually increases
 * - When players sell a lot of an item, its price gradually decreases
 * - Prices are always kept within 50-200% of the original base price
 */
public class Shop {
    private final UUID id;
    private String name;
    private Location spawnLocation;
    private final Set<String> claimedChunks; // Format: "worldUUID:chunkX:chunkZ"
    private final Map<Material, Double> prices; // Material -> selling price
    private final Map<Material, Integer> stock; // Material -> stock count
    private final Map<Material, Boolean> availability; // Material -> is available for purchase
    private final Map<Material, Boolean> canBuy; // Material -> can be bought from shop
    private final Map<Material, Boolean> canSell; // Material -> can be sold to shop
    private final Map<Material, Integer> buyCount; // Material -> number of times bought
    private final Map<Material, Integer> sellCount; // Material -> number of times sold
    private final Map<Material, Double> basePrice; // Material -> original base price

    /**
     * Constructor for creating a new shop.
     *
     * @param id   The unique identifier for the shop
     * @param name The name of the shop
     */
    public Shop(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.spawnLocation = null;
        this.claimedChunks = new HashSet<>();
        this.prices = new HashMap<>();
        this.stock = new HashMap<>();
        this.availability = new HashMap<>();
        this.canBuy = new HashMap<>();
        this.canSell = new HashMap<>();
        this.buyCount = new HashMap<>();
        this.sellCount = new HashMap<>();
        this.basePrice = new HashMap<>();
    }

    /**
     * Get the unique identifier of the shop.
     *
     * @return The shop's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the name of the shop.
     *
     * @return The shop's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the shop.
     *
     * @param name The new name for the shop
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the spawn location of the shop.
     *
     * @return The shop's spawn location, or null if not set
     */
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * Set the spawn location of the shop.
     *
     * @param spawnLocation The new spawn location for the shop
     */
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    /**
     * Claim a chunk for this shop.
     *
     * @param chunk The chunk to claim
     * @return true if the chunk was claimed, false if it was already claimed
     */
    public boolean claimChunk(Chunk chunk) {
        String chunkStr = formatChunkString(chunk);
        return claimedChunks.add(chunkStr);
    }

    /**
     * Unclaim a chunk from this shop.
     *
     * @param chunk The chunk to unclaim
     * @return true if the chunk was unclaimed, false if it wasn't claimed by this shop
     */
    public boolean unclaimChunk(Chunk chunk) {
        String chunkStr = formatChunkString(chunk);
        return claimedChunks.remove(chunkStr);
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
     * Get all chunks claimed by this shop.
     *
     * @return A set of claimed chunk strings
     */
    public Set<String> getClaimedChunks() {
        return Collections.unmodifiableSet(claimedChunks);
    }

    /**
     * Get the number of chunks claimed by this shop.
     *
     * @return The number of claimed chunks
     */
    public int getClaimedChunkCount() {
        return claimedChunks.size();
    }

    /**
     * Add a new item to the shop.
     *
     * @param material The material to add
     * @return true if the item was added, false if it already exists
     */
    public boolean addItem(Material material) {
        if (prices.containsKey(material)) {
            return false;
        }

        prices.put(material, 0.0); // Default price is 0 (unavailable)
        basePrice.put(material, 0.0); // Default base price is 0
        stock.put(material, 0); // Default stock is 0
        availability.put(material, false); // Default availability is false
        canBuy.put(material, true); // Default can buy from shop is true
        canSell.put(material, true); // Default can sell to shop is true
        buyCount.put(material, 0); // Initialize buy count
        sellCount.put(material, 0); // Initialize sell count
        return true;
    }

    /**
     * Adjust the price of an item based on supply and demand.
     * Price increases when items are bought frequently and decreases when sold frequently.
     *
     * The algorithm works as follows:
     * 1. Calculate a demand factor based on the difference between buy and sell counts
     * 2. Apply an adjustment factor (max ±30%) to the original base price
     * 3. Ensure the new price stays within 50-200% of the original price
     *
     * This creates a dynamic market where popular items become more expensive,
     * while items that players are selling a lot become cheaper.
     *
     * @param material The material to adjust price for
     * @return The new adjusted price
     */
    private double adjustPrice(Material material) {
        if (!prices.containsKey(material)) {
            return 0.0;
        }

        double originalPrice = basePrice.getOrDefault(material, 0.0);
        if (originalPrice <= 0.0) {
            return 0.0;
        }

        int bought = buyCount.getOrDefault(material, 0);
        int sold = sellCount.getOrDefault(material, 0);

        // Calculate demand factor: positive when bought more than sold, negative when sold more than bought
        double demandFactor = (bought - sold) / 100.0;

        // Adjust price based on demand factor (max ±30% from base price)
        double adjustmentFactor = Math.max(-0.3, Math.min(0.3, demandFactor));
        double newPrice = originalPrice * (1 + adjustmentFactor);

        // Ensure price doesn't go below 50% or above 200% of original price
        newPrice = Math.max(originalPrice * 0.5, Math.min(originalPrice * 2.0, newPrice));

        return newPrice;
    }

    /**
     * Remove an item from the shop.
     *
     * @param material The material to remove
     * @return true if the item was removed, false if it didn't exist
     */
    public boolean removeItem(Material material) {
        if (!prices.containsKey(material)) {
            return false;
        }

        prices.remove(material);
        stock.remove(material);
        availability.remove(material);
        canBuy.remove(material);
        canSell.remove(material);
        buyCount.remove(material);
        sellCount.remove(material);
        basePrice.remove(material);
        return true;
    }

    /**
     * Set the price of an item in the shop.
     *
     * @param material The material to set the price for
     * @param price    The price to set
     * @return true if the price was set, false if the item doesn't exist in the shop
     */
    public boolean setPrice(Material material, double price) {
        if (!prices.containsKey(material)) {
            return false;
        }

        prices.put(material, price);
        // Store the original base price
        basePrice.put(material, price);

        // Reset buy/sell counts when price is manually set
        buyCount.put(material, 0);
        sellCount.put(material, 0);

        // If price is set, make the item available
        if (price > 0) {
            availability.put(material, true);
        }

        return true;
    }

    /**
     * Get the buying price of an item in the shop.
     * This is the base price set for the item.
     *
     * @param material The material to get the price for
     * @return The buying price of the item, or 0 if not found or not available
     */
    public double getBuyingPrice(Material material) {
        if (!isItemAvailable(material)) {
            return 0.0;
        }
        return prices.getOrDefault(material, 0.0);
    }

    /**
     * Get the selling price of an item in the shop (0.7x the buying price).
     * The shop buys items from players at a lower price than it sells them.
     *
     * @param material The material to get the price for
     * @return The selling price of the item, or 0 if not found or not available
     */
    public double getSellingPrice(Material material) {
        double buyingPrice = getBuyingPrice(material);
        return buyingPrice > 0 ? buyingPrice * 0.7 : 0.0;
    }

    /**
     * Set the stock of an item in the shop.
     *
     * @param material The material to set the stock for
     * @param amount   The stock amount to set
     * @return true if the stock was set, false if the item doesn't exist in the shop
     */
    public boolean setStock(Material material, int amount) {
        if (!prices.containsKey(material)) {
            return false;
        }

        stock.put(material, Math.max(0, amount));
        return true;
    }

    /**
     * Get the stock of an item in the shop.
     *
     * @param material The material to get the stock for
     * @return The stock of the item, or 0 if not found
     */
    public int getStock(Material material) {
        return stock.getOrDefault(material, 0);
    }

    /**
     * Check if an item is available for purchase in the shop.
     *
     * @param material The material to check
     * @return true if the item is available, false otherwise
     */
    public boolean isItemAvailable(Material material) {
        return availability.getOrDefault(material, false) && getStock(material) > 0;
    }

    /**
     * Set the availability of an item in the shop.
     *
     * @param material   The material to set availability for
     * @param isAvailable Whether the item is available
     * @return true if the availability was set, false if the item doesn't exist in the shop
     */
    public boolean setAvailability(Material material, boolean isAvailable) {
        if (!prices.containsKey(material)) {
            return false;
        }
        availability.put(material, isAvailable);
        return true;
    }

    /**
     * Get all items in the shop.
     *
     * @return A set of all materials in the shop
     */
    public Set<Material> getItems() {
        return Collections.unmodifiableSet(prices.keySet());
    }

    /**
     * Restock all items in the shop to their maximum stock.
     * For now, we'll use a simple implementation that sets all stocks to 64.
     *
     * This method also resets the dynamic pricing system:
     * - Resets buy/sell counts to zero
     * - Restores all prices to their original base values
     * This effectively "resets" the supply and demand effects on prices.
     */
    public void restock() {
        for (Material material : prices.keySet()) {
            stock.put(material, 64);

            // Reset buy/sell counts
            buyCount.put(material, 0);
            sellCount.put(material, 0);

            // Restore original price
            double originalPrice = basePrice.getOrDefault(material, 0.0);
            if (originalPrice > 0) {
                prices.put(material, originalPrice);
            }
        }
    }

    /**
     * Check if an item can be bought from the shop.
     *
     * @param material The material to check
     * @return true if the item can be bought, false otherwise
     */
    public boolean canBuyFromShop(Material material) {
        return canBuy.getOrDefault(material, false);
    }

    /**
     * Check if an item can be sold to the shop.
     *
     * @param material The material to check
     * @return true if the item can be sold, false otherwise
     */
    public boolean canSellToShop(Material material) {
        return canSell.getOrDefault(material, false);
    }

    /**
     * Toggle whether an item can be bought from the shop.
     *
     * @param material The material to toggle
     * @return The new state (true if the item can now be bought, false otherwise)
     */
    public boolean toggleBuy(Material material) {
        if (!prices.containsKey(material)) {
            return false;
        }

        boolean newState = !canBuy.getOrDefault(material, true);
        canBuy.put(material, newState);
        return newState;
    }

    /**
     * Toggle whether an item can be sold to the shop.
     *
     * @param material The material to toggle
     * @return The new state (true if the item can now be sold, false otherwise)
     */
    public boolean toggleSell(Material material) {
        if (!prices.containsKey(material)) {
            return false;
        }

        boolean newState = !canSell.getOrDefault(material, true);
        canSell.put(material, newState);
        return newState;
    }

    /**
     * Buy an item from the shop, decreasing its stock.
     * This also increases the buy count for the item and adjusts its price based on demand.
     * Higher demand (more purchases) will gradually increase the price.
     *
     * @param material The material to buy
     * @param amount The amount to buy
     * @return true if the purchase was successful, false otherwise
     */
    public boolean buyFromShop(Material material, int amount) {
        if (!canBuyFromShop(material) || !isItemAvailable(material)) {
            return false;
        }

        int currentStock = getStock(material);
        if (currentStock < amount) {
            return false; // Not enough stock
        }

        // Decrease stock
        setStock(material, currentStock - amount);

        // Update buy count and adjust price
        int currentBuyCount = buyCount.getOrDefault(material, 0);
        buyCount.put(material, currentBuyCount + amount);

        // Adjust price based on new demand
        double newPrice = adjustPrice(material);
        prices.put(material, newPrice);

        return true;
    }

    /**
     * Sell an item to the shop, increasing its stock.
     * This also increases the sell count for the item and adjusts its price based on supply.
     * Higher supply (more sales to the shop) will gradually decrease the price.
     *
     * @param material The material to sell
     * @param amount The amount to sell
     * @return true if the sale was successful, false otherwise
     */
    public boolean sellToShop(Material material, int amount) {
        if (!canSellToShop(material)) {
            return false;
        }

        // Increase stock
        int currentStock = getStock(material);
        setStock(material, currentStock + amount);

        // Update sell count and adjust price
        int currentSellCount = sellCount.getOrDefault(material, 0);
        sellCount.put(material, currentSellCount + amount);

        // Adjust price based on new supply
        double newPrice = adjustPrice(material);
        prices.put(material, newPrice);

        return true;
    }
}