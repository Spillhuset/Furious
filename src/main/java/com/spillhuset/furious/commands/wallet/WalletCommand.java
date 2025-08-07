package com.spillhuset.furious.commands.wallet;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * Command handler for wallet-related operations.
 */
public class WalletCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;
    private final Map<String, String> aliases;

    /**
     * Creates a new WalletCommand.
     *
     * @param plugin The plugin instance
     */
    public WalletCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        this.aliases = new HashMap<>();

        registerSubCommands();
    }

    /**
     * Registers all wallet subcommands.
     */
    private void registerSubCommands() {
        // Register main subcommands
        registerSubCommand(new BalanceSubCommand(plugin), "b");
        registerSubCommand(new PaySubCommand(plugin), "p");

        // Register administrative subcommands
        registerSubCommand(new AddSubCommand(plugin), "a");
        registerSubCommand(new SubSubCommand(plugin), "s");
        registerSubCommand(new SetSubCommand(plugin), null);

        // Register help subcommand last so it has access to all other subcommands
        registerSubCommand(new HelpSubCommand(plugin, subCommands), "h");
    }

    /**
     * Registers a subcommand with an optional alias.
     *
     * @param subCommand The subcommand to register
     * @param alias      The alias for the subcommand (optional)
     */
    private void registerSubCommand(SubCommand subCommand, String alias) {
        String name = subCommand.getName();
        subCommands.put(name, subCommand);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // If no subcommand given
        if (args.length == 0) {
            SubCommand helpCommand = subCommands.get("help");
            return helpCommand.execute(sender, args);
        }

        // Set the subcommand name
        String subCommandName = args[0].toLowerCase();

        // Check if the subcommand is an alias
        if (aliases.containsKey(subCommandName)) {
            subCommandName = aliases.get(subCommandName);
        }

        // Set the subcommand
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown wallet command. Use ",NamedTextColor.RED).append(Component.text("/wallet help", NamedTextColor.WHITE)).append(Component.text(" for assistance.", NamedTextColor.RED)));
            return true;
        }

        if (!subCommand.checkPermission(sender)) {
            // Message already sent by checkPermission
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
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
