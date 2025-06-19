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
import java.util.Collection;
import java.util.List;

/**
 * Subcommand for listing all available roles.
 */
public class ListRolesSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new ListRolesSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListRolesSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "listroles";
    }

    @Override
    public String getDescription() {
        return "List all available roles";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm listroles", NamedTextColor.YELLOW)
                .append(Component.text(" - List all available roles", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm lr", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Collection<Role> roles = permissionManager.getRoles();

        if (roles.isEmpty()) {
            sender.sendMessage(Component.text("No roles found", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Available Roles ===", NamedTextColor.GOLD));

        for (Role role : roles) {
            Component roleComponent = Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.text(role.getName(), NamedTextColor.YELLOW));

            if (!role.getDescription().isEmpty()) {
                roleComponent = roleComponent.append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(role.getDescription(), NamedTextColor.WHITE));
            }

            sender.sendMessage(roleComponent);
        }

        sender.sendMessage(Component.text("Use '/perm roleinfo <role>' for detailed information about a specific role", NamedTextColor.GRAY));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for this command
    }

    @Override
    public String getPermission() {
        return "furious.permission.listroles";
    }
}