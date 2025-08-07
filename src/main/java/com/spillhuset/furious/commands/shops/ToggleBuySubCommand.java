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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcommand for toggling whether an item can be bought from a shop.
 */
public class ToggleBuySubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for ToggleBuySubCommand.
     *
     * @param plugin The main plugin instance
     */
    public ToggleBuySubCommand(Furious plugin) {
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
        return "toggle";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Toggle whether an item can be bought from the shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops toggle buy <item> §7- Toggle whether an item can be bought from the shop");
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

        // This command should be called as "/shops toggle buy <item>"
        // So we need to check if the first argument is "buy"
        if (args.length < 2 || !args[0].equalsIgnoreCase("buy")) {
            getUsage(sender);
            return false;
        }

        // Get the material
        String materialName = args[1].toUpperCase();
        Material material;

        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid material: " + materialName);
            return false;
        }

        // Get the shop at the player's location
        Shop shop = shopsManager.getShopByChunk(player.getLocation().getChunk());

        // Check if the player is in a shop's claimed chunk
        if (shop == null) {
            sender.sendMessage("§cYou are not in a shop's claimed chunk!");
            return false;
        }

        String shopName = shop.getName();

        // Check if the shop has this item
        if (!shop.getItems().contains(material)) {
            sender.sendMessage("§cThis shop doesn't have §e" + material.name() + "§c in its inventory!");
            return false;
        }

        // Toggle buying for this item
        boolean newState = shopsManager.toggleBuyItem(shopName, material);

        // Send success message
        if (newState) {
            sender.sendMessage("§aPlayers can now buy §e" + material.name() + "§a from this shop!");
        } else {
            sender.sendMessage("§cPlayers can no longer buy §e" + material.name() + "§c from this shop!");
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
        if (args.length == 1) {
            // Only suggest "buy" as the first argument
            List<String> completions = new ArrayList<>();
            if ("buy".startsWith(args[0].toLowerCase())) {
                completions.add("buy");
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            // Tab complete material names for the second argument
            String input = args[1].toUpperCase();

            // Get the shop at the player's location
            if (sender instanceof Player player) {
                Shop shop = shopsManager.getShopByChunk(player.getLocation().getChunk());
                if (shop != null) {
                    // Only suggest materials that are in the shop's inventory
                    return shop.getItems().stream()
                            .map(Material::name)
                            .filter(name -> name.startsWith(input))
                            .collect(Collectors.toList());
                }
            }

            // Fallback to all materials if we can't get the shop
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Get the permission required to use this subcommand.
     *
     * @return The permission required to use this subcommand
     */
    @Override
    public String getPermission() {
        return "furious.shops.toggle";
    }
}