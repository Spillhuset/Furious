package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Role;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for creating a new role.
 */
public class CreateRoleSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new CreateRoleSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateRoleSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "createrole";
    }

    @Override
    public String getDescription() {
        return "Create a new role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm createrole <name> [description]", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a new role with the specified name and optional description", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm cr <name> [description]", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing role name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String roleName = args[1];

        // Check if a role with this name already exists
        if (permissionManager.getRoleByName(roleName) != null) {
            sender.sendMessage(Component.text("Error: A role with the name '" + roleName + "' already exists", NamedTextColor.RED));
            return false;
        }

        // Get the description if provided
        StringBuilder descriptionBuilder = new StringBuilder();
        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                descriptionBuilder.append(args[i]).append(" ");
            }
        }
        String description = descriptionBuilder.toString().trim();

        // Create the role
        Role role = new Role(roleName, description);
        permissionManager.addRole(role);

        sender.sendMessage(Component.text("Role '" + roleName + "' created successfully", NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for this command
    }

    @Override
    public String getPermission() {
        return "furious.permission.createrole";
    }
}