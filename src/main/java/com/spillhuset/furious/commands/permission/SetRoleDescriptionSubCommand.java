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
 * Subcommand for setting the description of a role.
 */
public class SetRoleDescriptionSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new SetRoleDescriptionSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SetRoleDescriptionSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "setroledescription";
    }

    @Override
    public String getDescription() {
        return "Set the description of a role";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm setroledescription <name> <description>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set the description of the specified role", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm srd <name> <description>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing role name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Error: Missing description", NamedTextColor.RED));
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

        // Build the description from the remaining arguments
        StringBuilder descriptionBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            descriptionBuilder.append(args[i]).append(" ");
        }
        String description = descriptionBuilder.toString().trim();

        // Set the description
        role.setDescription(description);

        // Save the changes
        permissionManager.addRole(role);

        sender.sendMessage(Component.text("Description of role '" + roleName + "' set to: ", NamedTextColor.GREEN)
                .append(Component.text(description, NamedTextColor.WHITE)));

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
        return "furious.permission.setroledescription";
    }
}