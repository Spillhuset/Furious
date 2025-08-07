package com.spillhuset.furious.commands.shops;

import com.spillhuset.furious.Furious;
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
 * Subcommand for removing an item from a shop.
 */
public class RemoveSubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for RemoveSubCommand.
     *
     * @param plugin The main plugin instance
     */
    public RemoveSubCommand(Furious plugin) {
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
        return "remove";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Remove an item from a shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops remove <item/material> §7- Remove an item from the shop");
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

        if (args.length < 1) {
            getUsage(sender);
            return false;
        }

        // Get the material
        String materialName = args[0].toUpperCase();
        Material material;

        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid material: " + materialName);
            return false;
        }

        // Get the shop at the player's location
        com.spillhuset.furious.entities.Shop shop = shopsManager.getShopByChunk(player.getLocation().getChunk());

        // Check if the player is in a shop's claimed chunk
        if (shop == null) {
            sender.sendMessage("§cYou are not in a shop's claimed chunk!");
            return false;
        }

        String shopName = shop.getName();

        // Remove the item from the shop
        if (shopsManager.removeItemFromShop(shopName, material)) {
            sender.sendMessage("§aRemoved §e" + material.name() + "§a from shop §e" + shopName + "§a!");
            return true;
        } else {
            sender.sendMessage("§cFailed to remove item! It may not be in the shop.");
            return false;
        }
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
            // Tab complete material names
            String input = args[0].toUpperCase();
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
        return "furious.shops.remove";
    }
}