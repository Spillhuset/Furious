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
 * Subcommand for buying items from the shop.
 */
public class BuySubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for BuySubCommand.
     *
     * @param plugin The main plugin instance
     */
    public BuySubCommand(Furious plugin) {
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
        return "buy";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Buy an item from the shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops buy <item> [count] §7- Buy an item from the shop");
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
     * @param args The command arguments
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

        // Get the item name
        String itemName = args[0].toUpperCase();
        Material material;

        try {
            material = Material.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid item name: " + itemName);
            return false;
        }

        // Get the count (default to 1)
        int count = 1;
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
                if (count <= 0) {
                    sender.sendMessage("§cCount must be greater than 0");
                    return false;
                }
                if (count > 64) {
                    sender.sendMessage("§cCount must be less than or equal to 64");
                    return false;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid count: " + args[1]);
                return false;
            }
        }

        // Buy the item
        return shopsManager.buyItem(player, material, count);
    }

    /**
     * Tab complete the subcommand.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete item names
            String input = args[0].toUpperCase();
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Tab complete count
            List<String> completions = new ArrayList<>();
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
            return completions;
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
        return "furious.shops.buy";
    }
}