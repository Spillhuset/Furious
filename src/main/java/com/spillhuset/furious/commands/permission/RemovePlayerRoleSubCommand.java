package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Role;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Subcommand for removing a role from a player.
 */
public class RemovePlayerRoleSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new RemovePlayerRoleSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RemovePlayerRoleSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "removeplayerrole";
    }

    @Override
    public String getDescription() {
        return "Remove a role from a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm removeplayerrole <player> <role>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a role from the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm rpr <player> <role>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing player name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Error: Missing role name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String playerName = args[1];
        String roleName = args[2];

        // Get the player
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            // Try to get an offline player
            UUID playerId = null;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equalsIgnoreCase(playerName)) {
                    playerId = onlinePlayer.getUniqueId();
                    break;
                }
            }

            if (playerId == null) {
                // Try to get from offline players
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
                if (offlinePlayer != null) {
                    playerId = offlinePlayer.getUniqueId();
                }
            }

            if (playerId == null) {
                sender.sendMessage(Component.text("Error: Player '" + playerName + "' not found", NamedTextColor.RED));
                return false;
            }

            // Check if the role exists
            Role role = permissionManager.getRoleByName(roleName);
            if (role == null) {
                sender.sendMessage(Component.text("Error: No role with the name '" + roleName + "' exists", NamedTextColor.RED));
                return false;
            }

            // Remove the role from the player
            boolean removed = permissionManager.removeRoleFromPlayer(playerId, role.getId());

            if (removed) {
                sender.sendMessage(Component.text("Role '" + roleName + "' removed from player '" + playerName + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + playerName + "' does not have the role '" + roleName + "'", NamedTextColor.YELLOW));
            }
        } else {
            // Check if the role exists
            Role role = permissionManager.getRoleByName(roleName);
            if (role == null) {
                sender.sendMessage(Component.text("Error: No role with the name '" + roleName + "' exists", NamedTextColor.RED));
                return false;
            }

            // Remove the role from the player
            boolean removed = permissionManager.removeRoleFromPlayer(player.getUniqueId(), role.getId());

            if (removed) {
                sender.sendMessage(Component.text("Role '" + roleName + "' removed from player '" + player.getName() + "'", NamedTextColor.GREEN));
                player.sendMessage(Component.text("The role '" + roleName + "' has been removed from you", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Player '" + player.getName() + "' does not have the role '" + roleName + "'", NamedTextColor.YELLOW));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partialPlayerName = args[1].toLowerCase();

            // Add online player names that match the partial input
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(partialPlayerName)) {
                    completions.add(playerName);
                }
            }
        } else if (args.length == 3) {
            String playerName = args[1];
            String partialRoleName = args[2].toLowerCase();

            // Get the player
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                // Get the player's roles
                Set<Role> playerRoles = permissionManager.getPlayerRoles(player.getUniqueId());

                // Add role names that match the partial input
                for (Role role : playerRoles) {
                    String roleName = role.getName();
                    if (roleName.toLowerCase().startsWith(partialRoleName)) {
                        completions.add(roleName);
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.removeplayerrole";
    }
}
