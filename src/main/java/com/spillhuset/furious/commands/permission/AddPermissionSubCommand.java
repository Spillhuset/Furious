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
 * Subcommand for adding a permission to a role.
 */
public class AddPermissionSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new AddPermissionSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AddPermissionSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "addpermission";
    }

    @Override
    public String getDescription() {
        return "Add a permission to a role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm addpermission <role> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a permission to the specified role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm ap <role> <permission>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Examples:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /perm ap admin furious.teleport.*", NamedTextColor.YELLOW)
                .append(Component.text(" - Add all teleport permissions to the admin role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  /perm ap default -furious.teleport.force", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a negated permission to the default role", NamedTextColor.WHITE)));
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

        // Add the permission to the role
        boolean added = role.addPermission(permissionNode);

        // Save the changes
        permissionManager.addRole(role);

        if (added) {
            sender.sendMessage(Component.text("Permission '" + permissionNode + "' added to role '" + roleName + "'", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Permission '" + permissionNode + "' is already assigned to role '" + roleName + "'", NamedTextColor.YELLOW));
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
            String partialPermission = args[2].toLowerCase();

            // Add common permission prefixes
            List<String> commonPermissions = List.of(
                "furious.teleport.",
                "furious.guild.",
                "furious.bank.",
                "furious.homes.",
                "furious.locks.",
                "furious.warps.",
                "furious.permission."
            );

            for (String perm : commonPermissions) {
                if (perm.startsWith(partialPermission)) {
                    completions.add(perm);
                }
            }

            // Add wildcard and negated versions
            if ("furious.".startsWith(partialPermission)) {
                completions.add("furious.*");
            }

            if ("-".startsWith(partialPermission)) {
                completions.add("-furious.");
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.addpermission";
    }
}