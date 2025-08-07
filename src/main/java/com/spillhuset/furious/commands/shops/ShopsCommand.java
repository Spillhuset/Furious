package com.spillhuset.furious.commands.shops;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command handler for shop-related commands.
 */
public class ShopsCommand implements CommandExecutor, TabCompleter {

    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Constructor for ShopsCommand.
     *
     * @param plugin The main plugin instance
     */
    public ShopsCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Register all subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("buy", new BuySubCommand(plugin));
        subCommands.put("sell", new SellSubCommand(plugin));
        subCommands.put("info", new InfoSubCommand(plugin));
        subCommands.put("create", new CreateSubCommand(plugin));
        subCommands.put("delete", new DeleteSubCommand(plugin));
        subCommands.put("claim", new ClaimSubCommand(plugin));
        subCommands.put("unclaim", new UnclaimSubCommand(plugin));
        subCommands.put("spawn", new SpawnSubCommand(plugin));
        subCommands.put("teleport", new TeleportSubCommand(plugin));
        subCommands.put("tp", new TeleportSubCommand(plugin)); // Alias for teleport
        subCommands.put("add", new AddSubCommand(plugin));
        subCommands.put("remove", new RemoveSubCommand(plugin));
        subCommands.put("stock", new StockSubCommand(plugin));
        subCommands.put("price", new PriceSubCommand(plugin));
        subCommands.put("restock", new RestockSubCommand(plugin));
        subCommands.put("toggle", new ToggleSubCommand(plugin));
    }

    /**
     * Execute the command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Show help message if no subcommand is provided
            sender.sendMessage("§6=== Shops Commands ===");
            sender.sendMessage("§e/shops create <n> §7- Create a new shop");
            sender.sendMessage("§e/shops delete <n> §7- Delete a shop (requires confirmation)");
            sender.sendMessage("§e/shops delete confirm §7- Confirm deletion of a shop");
            sender.sendMessage("§e/shops claim <n> §7- Claim the current chunk for a shop");
            sender.sendMessage("§e/shops unclaim §7- Unclaim the current chunk from the shop");
            sender.sendMessage("§e/shops spawn §7- Set the spawn location for the shop at your current location");
            sender.sendMessage("§e/shops teleport <n> §7- Teleport to a shop");
            sender.sendMessage("§e/shops add <item/material> §7- Add an item to the shop");
            sender.sendMessage("§e/shops remove <item/material> §7- Remove an item from the shop");
            sender.sendMessage("§e/shops stock <item/material> <amount> §7- Set the stock of an item");
            sender.sendMessage("§e/shops price <item/material> <cost> §7- Set the selling price of an item");
            sender.sendMessage("§e/shops restock §7- Restock all items in the shop");
            sender.sendMessage("§e/shops sell §7- Sell the item in your hand");
            sender.sendMessage("§e/shops buy <item> [count] §7- Buy an item");
            sender.sendMessage("§e/shops info [name] §7- Display shop information including pricing and stock");
            sender.sendMessage("§e/shops info <item> §7- Display information about a specific item");
            sender.sendMessage("§e/shops toggle buy <item> §7- Toggle whether an item can be bought from the shop");
            sender.sendMessage("§e/shops toggle sell <item> §7- Toggle whether an item can be sold to the shop");
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage("§cUnknown subcommand: " + subCommandName);
            return false;
        }

        // Check if the sender has permission to use this subcommand
        if (!subCommand.checkPermission(sender)) {
            return false;
        }

        // Remove the subcommand name from the arguments
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        // Execute the subcommand
        return subCommand.execute(sender, subArgs);
    }

    /**
     * Tab complete the command.
     *
     * @param sender The command sender
     * @param command The command
     * @param alias The command alias
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete subcommand names
            List<String> completions = new ArrayList<>();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                if (entry.getValue().checkPermission(sender, false)) {
                    completions.add(entry.getKey());
                }
            }
            return completions;
        } else if (args.length > 1) {
            // Tab complete subcommand arguments
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                // Remove the subcommand name from the arguments
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);

                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return new ArrayList<>();
    }
}