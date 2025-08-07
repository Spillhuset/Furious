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
 * Subcommand for managing roles.
 * This is an intermediate command that delegates to other subcommands.
 */
public class RolesSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;
    private final Map<String, SubCommand> subCommands;
    private final Map<String, String> aliases;

    /**
     * Creates a new RolesSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RolesSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.subCommands = new HashMap<>();
        this.aliases = new HashMap<>();

        registerSubCommands();
    }

    /**
     * Registers all role-related subcommands.
     */
    private void registerSubCommands() {
        // Role management
        registerSubCommand(new CreateRoleSubCommand(plugin), "c");
        registerSubCommand(new DeleteRoleSubCommand(plugin), "d");
        registerSubCommand(new ListRolesSubCommand(plugin), "l");
        registerSubCommand(new RoleInfoSubCommand(plugin), "i");
        registerSubCommand(new SetRoleDescriptionSubCommand(plugin), "sd");
        registerSubCommand(new AddPermissionSubCommand(plugin), "ap");
        registerSubCommand(new RemovePermissionSubCommand(plugin), "rp");
        registerSubCommand(new AddPlayerRoleSubCommand(plugin), "ap");
        registerSubCommand(new RemovePlayerRoleSubCommand(plugin), "rp");
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
        if (name.equals("createrole")) {
            name = "create";
        } else if (name.equals("deleterole")) {
            name = "delete";
        } else if (name.equals("listroles")) {
            name = "list";
        } else if (name.equals("roleinfo")) {
            name = "info";
        } else if (name.equals("setroledescription")) {
            name = "set";
        } else if (name.equals("addpermission")) {
            name = "add";
            subCommands.put("add permission", subCommand);
        } else if (name.equals("removepermission")) {
            name = "remove";
            subCommands.put("remove permission", subCommand);
        } else if (name.equals("addplayerrole")) {
            name = "add";
            subCommands.put("add player", subCommand);
        } else if (name.equals("removeplayerrole")) {
            name = "remove";
            subCommands.put("remove player", subCommand);
        }

        subCommands.put(name, subCommand);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
        }
    }

    @Override
    public String getName() {
        return "roles";
    }

    @Override
    public String getDescription() {
        return "Manage roles";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/permissions roles create <n> [description]", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a new role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles delete <n>", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete a role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all roles", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles info <role>", NamedTextColor.YELLOW)
                .append(Component.text(" - View role information", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles set description <role> <description>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set role description", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles add permission <role> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add permission to role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles remove permission <role> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove permission from role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles add player <role> <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add role to player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/permissions roles remove player <role> <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove role from player", NamedTextColor.WHITE)));
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
            sender.sendMessage(Component.text("Unknown roles command. Use /permissions roles for assistance.", NamedTextColor.RED));
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
            // First argument after "roles" - subcommands
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
        } else if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            // Second argument after "roles add/remove" - subcommands
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