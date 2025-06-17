package com.spillhuset.furious.commands.warps;

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
 * Main command handler for warp-related commands.
 */
public class WarpsCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new WarpsCommand.
     *
     * @param plugin The plugin instance
     */
    public WarpsCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Registers all warps subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("create", new CreateSubCommand(plugin));
        subCommands.put("relocate", new RelocateSubCommand(plugin));
        subCommands.put("cost", new CostSubCommand(plugin));
        subCommands.put("passwd", new PasswdSubCommand(plugin));
        subCommands.put("rename", new RenameSubCommand(plugin));
        subCommands.put("delete", new DeleteSubCommand(plugin));
        subCommands.put("link", new LinkSubCommand(plugin));
        subCommands.put("warp", new WarpSubCommand(plugin));
        subCommands.put("list", new ListSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            // If the first arg isn't a subcommand, assume it's a warp name for the warp command
            if (sender instanceof Player player) {
                // Create a new array with "warp" as the first argument and the original args
                String[] newArgs = new String[args.length + 1];
                newArgs[0] = "warp";
                System.arraycopy(args, 0, newArgs, 1, args.length);

                // Get the warp subcommand and execute it
                SubCommand warpCommand = subCommands.get("warp");
                if (warpCommand != null && warpCommand.checkPermission(sender)) {
                    return warpCommand.execute(sender, newArgs);
                }
            }

            showHelp(sender);
            return true;
        }

        // Check permissions
        if (!subCommand.checkPermission(sender)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    /**
     * Shows help information for warps commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Warps Commands:", NamedTextColor.GOLD));

        // Display commands based on permissions
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.checkPermission(sender, false)) {
                sender.sendMessage(Component.text("/warps " + subCommand.getName() + " - " + subCommand.getDescription(), NamedTextColor.YELLOW));
            }
        }

        // Show examples
        sender.sendMessage(Component.text("Examples:", NamedTextColor.GOLD));
        if (sender.isOp()) {
            sender.sendMessage(Component.text("/warps create spawn - Create a warp named 'spawn'", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/warps create shop cost=100 - Create a warp with a cost", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/warps create secret passwd=letmein - Create a password-protected warp", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/warps link spawn water - Link a warp to a portal with water filling", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/warps warp spawn - Teleport to the 'spawn' warp", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/warps warp secret letmein - Teleport to a password-protected warp", NamedTextColor.YELLOW));
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

            // Add warp names for direct teleport
            if (sender instanceof Player) {
                plugin.getWarpsManager().getAllWarps().forEach(warp -> {
                    if (warp.getName().startsWith(partial)) {
                        completions.add(warp.getName());
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