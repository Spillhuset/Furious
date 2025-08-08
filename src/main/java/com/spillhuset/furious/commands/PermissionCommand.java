package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.permission.*;
import com.spillhuset.furious.gui.PermissionManagerGUI;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Command handler for permission-related operations.
 */
public class PermissionCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final PermissionManager permissionManager;
    private final Map<String, SubCommand> subCommands;
    private final Map<String, String> aliases;

    public PermissionCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.subCommands = new HashMap<>();
        this.aliases = new HashMap<>();

        registerSubCommands();
    }

    /**
     * Registers all permission subcommands.
     */
    private void registerSubCommands() {
        // Register the subcommand hierarchy
        registerSubCommand(new RolesSubCommand(plugin), "r");
        registerSubCommand(new PlayerSubCommand(plugin), "p");

        // GUI command
        registerSubCommand(new GUISubCommand(plugin), "g");

        // Help command
        registerSubCommand(new HelpSubCommand(plugin, subCommands), "h");
    }

    /**
     * Registers a subcommand with an optional alias.
     *
     * @param subCommand The subcommand to register
     * @param alias The alias for the subcommand (optional)
     */
    private void registerSubCommand(SubCommand subCommand, String alias) {
        String name = subCommand.getName();
        subCommands.put(name, subCommand);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Get the help subcommand
            SubCommand helpCommand = subCommands.get("help");
            return helpCommand.execute(sender, args);
        }

        String subCommandName = args[0].toLowerCase();

        // Check if the subcommand is an alias
        if (aliases.containsKey(subCommandName)) {
            subCommandName = aliases.get(subCommandName);
        }

        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown permission command. Use /permissions help for assistance.", NamedTextColor.RED));
            return false;
        }

        if (!subCommand.checkPermission(sender)) {
            // Message already sent by checkPermission
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            String partialArg = args[0].toLowerCase();

            // Add main subcommands
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                if (subCommand.checkPermission(sender, false) && entry.getKey().startsWith(partialArg)) {
                    completions.add(entry.getKey());
                }
            }

            // Add aliases
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                String aliasKey = entry.getKey();
                SubCommand subCommand = subCommands.get(entry.getValue());
                if (subCommand != null && subCommand.checkPermission(sender, false) && aliasKey.startsWith(partialArg)) {
                    completions.add(aliasKey);
                }
            }
        } else if (args.length > 1) {
            // Get the subcommand
            String subCommandName = args[0].toLowerCase();

            // Check if the subcommand is an alias
            if (aliases.containsKey(subCommandName)) {
                subCommandName = aliases.get(subCommandName);
            }

            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }
}
