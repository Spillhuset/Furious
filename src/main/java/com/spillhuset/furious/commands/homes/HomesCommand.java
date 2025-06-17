package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command handler for home-related commands.
 */
public class HomesCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new HomesCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Registers all homes subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("set", new SetSubCommand(plugin));
        subCommands.put("move", new MoveSubCommand(plugin));
        subCommands.put("rename", new RenameSubCommand(plugin));
        subCommands.put("delete", new DeleteSubCommand(plugin));
        subCommands.put("list", new ListSubCommand(plugin));
        subCommands.put("tp", new TeleportSubCommand(plugin));
        subCommands.put("buy", new BuySubCommand(plugin));
        subCommands.put("world", new WorldSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Default to teleport to default home if no args provided
            if (sender instanceof Player player) {
                return plugin.getHomesManager().teleportToPlayerHome(player, "default");
            } else {
                showHelp(sender);
                return true;
            }
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            // If the first arg isn't a subcommand, assume it's a home name for teleport
            if (sender instanceof Player player) {
                return plugin.getHomesManager().teleportToPlayerHome(player, args[0]);
            } else {
                showHelp(sender);
                return true;
            }
        }

        // Check permissions
        if (!subCommand.checkPermission(sender)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    /**
     * Shows help information for homes commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Homes Commands:", NamedTextColor.GOLD));

        // Display commands based on permissions
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.checkPermission(sender, false)) {
                sender.sendMessage(Component.text("/homes " + subCommand.getName() + " - " + subCommand.getDescription(), NamedTextColor.YELLOW));
            }
        }

        // Show admin commands if the sender has permission
        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("Admin Commands:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/homes set <player> <name> - Set a home for another player", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/homes move <player> <name> - Move a home for another player", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/homes rename <player> <oldname> <newname> - Rename a home for another player", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/homes delete <player> <name> - Delete a home for another player", NamedTextColor.YELLOW));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            // Add subcommands
            for (SubCommand subCmd : subCommands.values()) {
                if (subCmd.getName().startsWith(partial) && subCmd.checkPermission(sender, false)) {
                    completions.add(subCmd.getName());
                }
            }

            // Add home names for teleport
            if (sender instanceof Player player) {
                plugin.getHomesManager().getPlayerHomes(player.getUniqueId()).forEach(home -> {
                    if (home.getName().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });
            }
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }
}
