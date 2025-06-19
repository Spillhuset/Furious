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
 * Subcommand for deleting an existing role.
 */
public class DeleteRoleSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new DeleteRoleSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteRoleSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "deleterole";
    }

    @Override
    public String getDescription() {
        return "Delete an existing role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm deleterole <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete the role with the specified name", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm dr <name>", NamedTextColor.GRAY));
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

        // Prevent deletion of default and admin roles
        if (roleName.equalsIgnoreCase("default") || roleName.equalsIgnoreCase("admin")) {
            sender.sendMessage(Component.text("Error: Cannot delete the '" + roleName + "' role as it is a system role", NamedTextColor.RED));
            return false;
        }

        // Delete the role
        permissionManager.removeRole(role.getId());

        sender.sendMessage(Component.text("Role '" + roleName + "' deleted successfully", NamedTextColor.GREEN));

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
                if (roleName.toLowerCase().startsWith(partialRoleName) &&
                    !roleName.equalsIgnoreCase("default") &&
                    !roleName.equalsIgnoreCase("admin")) {
                    completions.add(roleName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.deleterole";
    }
}