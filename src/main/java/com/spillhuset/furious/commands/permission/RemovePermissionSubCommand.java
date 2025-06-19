package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Permission;
import com.spillhuset.furious.entities.Role;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Subcommand for removing a permission from a role.
 */
public class RemovePermissionSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new RemovePermissionSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RemovePermissionSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "removepermission";
    }

    @Override
    public String getDescription() {
        return "Remove a permission from a role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm removepermission <role> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a permission from the specified role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm rp <role> <permission>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing role name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Error: Missing permission", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String roleName = args[1];
        String permissionNode = args[2];

        // Check if the role exists
        Role role = permissionManager.getRoleByName(roleName);
        if (role == null) {
            sender.sendMessage(Component.text("Error: No role with the name '" + roleName + "' exists", NamedTextColor.RED));
            return false;
        }

        // Remove the permission from the role
        boolean removed = role.removePermission(permissionNode);

        // Save the changes
        permissionManager.addRole(role);

        if (removed) {
            sender.sendMessage(Component.text("Permission '" + permissionNode + "' removed from role '" + roleName + "'", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Permission '" + permissionNode + "' is not assigned to role '" + roleName + "'", NamedTextColor.YELLOW));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partialRoleName = args[1].toLowerCase();

            // Add role names that match the partial input
            for (Role role : permissionManager.getRoles()) {
                String roleName = role.getName();
                if (roleName.toLowerCase().startsWith(partialRoleName)) {
                    completions.add(roleName);
                }
            }
        } else if (args.length == 3) {
            String roleName = args[1];
            String partialPermission = args[2].toLowerCase();

            // Get the role
            Role role = permissionManager.getRoleByName(roleName);
            if (role != null) {
                // Add permissions from the role that match the partial input
                Set<Permission> permissions = role.getPermissions();
                for (Permission permission : permissions) {
                    String permNode = permission.toString();
                    if (permNode.toLowerCase().startsWith(partialPermission)) {
                        completions.add(permNode);
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.removepermission";
    }
}