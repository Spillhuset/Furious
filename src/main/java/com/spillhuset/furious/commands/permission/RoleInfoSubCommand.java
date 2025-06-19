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
 * Subcommand for displaying detailed information about a specific role.
 */
public class RoleInfoSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new RoleInfoSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RoleInfoSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "roleinfo";
    }

    @Override
    public String getDescription() {
        return "Display detailed information about a role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm roleinfo <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Display detailed information about the specified role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm ri <name>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing role name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String roleName = args[1];

        // Check if the role exists
        Role role = permissionManager.getRoleByName(roleName);
        if (role == null) {
            sender.sendMessage(Component.text("Error: No role with the name '" + roleName + "' exists", NamedTextColor.RED));
            return false;
        }

        // Display role information
        sender.sendMessage(Component.text("=== Role Information: " + role.getName() + " ===", NamedTextColor.GOLD));

        if (!role.getDescription().isEmpty()) {
            sender.sendMessage(Component.text("Description: ", NamedTextColor.YELLOW)
                    .append(Component.text(role.getDescription(), NamedTextColor.WHITE)));
        }

        // Display permissions
        Set<Permission> permissions = role.getPermissions();
        if (permissions.isEmpty()) {
            sender.sendMessage(Component.text("Permissions: ", NamedTextColor.YELLOW)
                    .append(Component.text("None", NamedTextColor.GRAY)));
        } else {
            sender.sendMessage(Component.text("Permissions:", NamedTextColor.YELLOW));

            // Group permissions by their first segment (e.g., "furious.bank.*" -> "furious.bank")
            java.util.Map<String, List<Permission>> groupedPermissions = new java.util.HashMap<>();

            for (Permission permission : permissions) {
                String node = permission.getNode();
                String group = node.contains(".") ? node.substring(0, node.indexOf(".")) : node;

                groupedPermissions.computeIfAbsent(group, k -> new ArrayList<>()).add(permission);
            }

            // Display grouped permissions
            for (String group : groupedPermissions.keySet()) {
                sender.sendMessage(Component.text("  " + group + ":", NamedTextColor.GRAY));

                for (Permission permission : groupedPermissions.get(group)) {
                    Component permComponent = Component.text("    - ", NamedTextColor.GRAY);

                    if (permission.isNegated()) {
                        permComponent = permComponent.append(Component.text("-", NamedTextColor.RED));
                    }

                    permComponent = permComponent.append(Component.text(permission.getNode(),
                            permission.isNegated() ? NamedTextColor.RED : NamedTextColor.GREEN));

                    sender.sendMessage(permComponent);
                }
            }
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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.roleinfo";
    }
}