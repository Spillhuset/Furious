package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subcommand for managing player permissions.
 * This is an intermediate command that delegates to other subcommands.
 */
public class PlayerSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;
    private final Map<String, SubCommand> subCommands;
    private final Map<String, String> aliases;

    /**
     * Creates a new PlayerSubCommand.
     *
     * @param plugin The plugin instance
     */
    public PlayerSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.subCommands = new HashMap<>();
        this.aliases = new HashMap<>();

        registerSubCommands();
    }

    /**
     * Registers all player-related subcommands.
     */
    private void registerSubCommands() {
        // Player permission management
        registerSubCommand(new AddPlayerPermissionSubCommand(plugin), "ap");
        registerSubCommand(new RemovePlayerPermissionSubCommand(plugin), "rp");
        registerSubCommand(new ListPlayerPermissionsSubCommand(plugin), "lp");
        registerSubCommand(new ListPlayerRolesSubCommand(plugin), "lr");
    }

    /**
     * Registers a subcommand with an optional alias.
     *
     * @param subCommand The subcommand to register
     * @param alias The alias for the subcommand (optional)
     */
    private void registerSubCommand(SubCommand subCommand, String alias) {
        String name = subCommand.getName();

        // Map the original command names to the new structure
        if (name.equals("addplayerpermission")) {
            name = "add";
            subCommands.put("add permission", subCommand);
        } else if (name.equals("removeplayerpermission")) {
            name = "remove";
            subCommands.put("remove permission", subCommand);
        } else if (name.equals("listplayerpermissions")) {
            name = "list";
            subCommands.put("list permissions", subCommand);
        } else if (name.equals("listplayerroles")) {
            name = "list";
            subCommands.put("list roles", subCommand);
        }

        subCommands.put(name, subCommand);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
        }
    }

    @Override
    public String getName() {
        return "player";
    }

    @Override
    public String getDescription() {
        return "Manage player permissions";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm player add permission <player> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add permission to player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/perm player remove permission <player> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove permission from player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/perm player list permissions <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - List player's permissions", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/perm player list roles <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - List player's roles", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String subCommandName = args[1].toLowerCase();

        // Check if the subcommand is an alias
        if (aliases.containsKey(subCommandName)) {
            subCommandName = aliases.get(subCommandName);
        }

        // Handle special cases for multi-word subcommands
        if (args.length >= 3) {
            String potentialMultiWord = subCommandName + " " + args[2].toLowerCase();
            if (subCommands.containsKey(potentialMultiWord)) {
                subCommandName = potentialMultiWord;
            }
        }

        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown player command. Use /perm player for assistance.", NamedTextColor.RED));
            return false;
        }

        if (!subCommand.checkPermission(sender)) {
            // Message already sent by checkPermission
            return true;
        }

        // Create a new args array with the subcommand as the first element
        String[] newArgs = new String[args.length - 1];
        newArgs[0] = subCommand.getName(); // Use the original subcommand name
        System.arraycopy(args, 2, newArgs, 1, args.length - 2);

        return subCommand.execute(sender, newArgs);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // First argument after "player" - subcommands
            String partialArg = args[1].toLowerCase();

            // Add main subcommands
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                String key = entry.getKey();
                if (!key.contains(" ") && key.startsWith(partialArg)) {
                    completions.add(key);
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
        } else if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("list"))) {
            // Second argument after "player add/remove/list" - subcommands
            String partialArg = args[2].toLowerCase();

            // Add potential second words
            for (String key : subCommands.keySet()) {
                if (key.startsWith(args[1].toLowerCase() + " ") && key.split(" ")[1].startsWith(partialArg)) {
                    completions.add(key.split(" ")[1]);
                }
            }
        } else if (args.length > 2) {
            // Get the subcommand
            String subCommandName = args[1].toLowerCase();

            // Check if the subcommand is an alias
            if (aliases.containsKey(subCommandName)) {
                subCommandName = aliases.get(subCommandName);
            }

            // Handle special cases for multi-word subcommands
            if (args.length >= 3) {
                String potentialMultiWord = subCommandName + " " + args[2].toLowerCase();
                if (subCommands.containsKey(potentialMultiWord)) {
                    subCommandName = potentialMultiWord;
                }
            }

            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                // Create a new args array with the subcommand as the first element
                String[] newArgs = new String[args.length - 1];
                newArgs[0] = subCommand.getName(); // Use the original subcommand name
                System.arraycopy(args, 2, newArgs, 1, args.length - 2);

                return subCommand.tabComplete(sender, newArgs);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return null; // No specific permission for this intermediate command
    }
}