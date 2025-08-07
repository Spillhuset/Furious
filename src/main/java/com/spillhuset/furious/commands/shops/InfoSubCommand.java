package com.spillhuset.furious.commands.shops;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Shop;
import com.spillhuset.furious.managers.ShopsManager;
import com.spillhuset.furious.misc.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for displaying shop information.
 */
public class InfoSubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for InfoSubCommand.
     *
     * @param plugin The main plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.shopsManager = plugin.getShopsManager();
    }

    /**
     * Get the name of the subcommand.
     *
     * @return The name of the subcommand
     */
    @Override
    public String getName() {
        return "info";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Display shop information including pricing and stock";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops info [name] §7- Display shop information");
        sender.sendMessage("§e/shops info <item> §7- Display information about a specific item");
    }

    /**
     * Check if the subcommand denies non-player senders.
     *
     * @return true if non-player senders are denied, false otherwise
     */
    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    /**
     * Execute the subcommand.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        // Check if the player is looking for information about a specific item
        if (args.length == 1) {
            try {
                // Try to parse the argument as a material
                Material material = Material.valueOf(args[0].toUpperCase());
                return displayItemInfo(player, material);
            } catch (IllegalArgumentException e) {
                // If it's not a valid material, assume it's a shop name
                return displayShopInfo(player, args[0]);
            }
        } else if (args.length == 0) {
            // No arguments, display info for the shop at the player's location
            Shop shop = shopsManager.getShopByChunk(player.getLocation().getChunk());

            // Check if the player is in a shop's claimed chunk
            if (shop == null) {
                // Player is not in a shop's claimed chunk, show available shops instead
                player.sendMessage("§6=== Available Shops ===");
                boolean found = false;

                // Iterate through all shops
                for (String shopName : shopsManager.getAllShopNames()) {
                    Shop availableShop = shopsManager.getShop(shopName);
                    if (availableShop != null) {
                        player.sendMessage(String.format("§e%s", availableShop.getName()));
                        found = true;
                    }
                }

                if (!found) {
                    player.sendMessage("§cNo shops available!");
                }

                return true;
            }

            return displayShopInfo(player, shop);
        } else {
            getUsage(sender);
            return false;
        }
    }

    /**
     * Display information about a specific item.
     *
     * @param player The player requesting the information
     * @param material The material to display information about
     * @return true if the information was displayed, false otherwise
     */
    private boolean displayItemInfo(Player player, Material material) {
        // Check if the player is in a shop's claimed chunk
        Shop currentShop = shopsManager.getShopByChunk(player.getLocation().getChunk());

        if (currentShop != null) {
            // Player is inside a shop, show detailed information about the item in this shop
            if (!currentShop.getItems().contains(material)) {
                player.sendMessage("§cThis shop doesn't sell or buy §e" + material.name() + "§c!");
                return false;
            }

            double sellingPrice = currentShop.getSellingPrice(material);
            double buyingPrice = currentShop.getBuyingPrice(material);
            int stock = currentShop.getStock(material);
            boolean available = currentShop.isItemAvailable(material);
            boolean canBuy = currentShop.canBuyFromShop(material);
            boolean canSell = currentShop.canSellToShop(material);

            player.sendMessage("§6=== Item Info: §e" + material.name() + "§6 ===");
            player.sendMessage("§6Shop: §e" + currentShop.getName());
            player.sendMessage("§6Stock: §e" + stock);
            player.sendMessage("§6Buy price: §e" + sellingPrice);
            player.sendMessage("§6Sell price: §e" + buyingPrice);
            player.sendMessage("§6Status: " + (available ? "§aAvailable" : "§cUnavailable"));
            player.sendMessage("§6Can buy from shop: " + (canBuy ? "§aYes" : "§cNo"));
            player.sendMessage("§6Can sell to shop: " + (canSell ? "§aYes" : "§cNo"));

            return true;
        } else {
            // Player is outside a shop, show which shops have this item
            player.sendMessage("§6=== Shops selling §e" + material.name() + "§6 ===");

            boolean found = false;

            // Iterate through all shops to find those that sell this item
            for (String shopName : shopsManager.getAllShopNames()) {
                Shop shop = shopsManager.getShop(shopName);

                if (shop.getItems().contains(material) && shop.isItemAvailable(material)) {
                    double price = shop.getSellingPrice(material);
                    int stock = shop.getStock(material);

                    player.sendMessage(String.format("§e%s §7- Price: §e%.2f §7- Stock: §e%d",
                            shop.getName(), price, stock));
                    found = true;
                }
            }

            if (!found) {
                player.sendMessage("§cNo shops are currently selling this item!");
            }

            return true;
        }
    }

    /**
     * Display information about a specific shop.
     *
     * @param player The player requesting the information
     * @param shopName The name of the shop
     * @return true if the information was displayed, false otherwise
     */
    private boolean displayShopInfo(Player player, String shopName) {
        // Check if the shop exists
        if (!shopsManager.shopExists(shopName)) {
            player.sendMessage("§cShop §e" + shopName + "§c does not exist!");
            return false;
        }

        // Get the shop
        Shop shop = shopsManager.getShop(shopName);
        return displayShopInfo(player, shop);
    }

    /**
     * Display information about a specific shop.
     *
     * @param player The player requesting the information
     * @param shop The shop to display information about
     * @return true if the information was displayed, false otherwise
     */
    private boolean displayShopInfo(Player player, Shop shop) {
        // Display shop information
        player.sendMessage("§6=== Shop: §e" + shop.getName() + "§6 ===");

        // Get all items in the shop
        if (shop.getItems().isEmpty()) {
            player.sendMessage("§7This shop has no items.");
            return true;
        }

        player.sendMessage("§6Items:");

        // Display item information
        for (Material material : shop.getItems()) {
            double sellingPrice = shop.getSellingPrice(material);
            double buyingPrice = shop.getBuyingPrice(material);
            int stock = shop.getStock(material);
            boolean available = shop.isItemAvailable(material);

            String availabilityStr = available ? "§aAvailable" : "§cUnavailable";

            player.sendMessage(String.format("§e%s §7- Stock: §e%d §7- Buy: §e%.2f §7- Sell: §e%.2f §7- %s",
                    material.name(), stock, sellingPrice, buyingPrice, availabilityStr));
        }

        return true;
    }

    /**
     * Tab complete the subcommand.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        // TODO: Implement tab completion for shop names
        return new ArrayList<>();
    }

    /**
     * Get the permission required to use this subcommand.
     *
     * @return The permission required to use this subcommand
     */
    @Override
    public String getPermission() {
        return "furious.shops.info";
    }
}