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
import java.util.UUID;

/**
 * Subcommand for adding a role to a player.
 */
public class AddPlayerRoleSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new AddPlayerRoleSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AddPlayerRoleSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "addplayerrole";
    }

    @Override
    public String getDescription() {
        return "Add a role to a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm addplayerrole <player> <role>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a role to the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm apr <player> <role>", NamedTextColor.GRAY));
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

            // Add the role to the player
            boolean added = permissionManager.addRoleToPlayer(playerId, role.getId());

            if (added) {
                sender.sendMessage(Component.text("Role '" + roleName + "' added to player '" + playerName + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + playerName + "' already has the role '" + roleName + "'", NamedTextColor.YELLOW));
            }
        } else {
            // Check if the role exists
            Role role = permissionManager.getRoleByName(roleName);
            if (role == null) {
                sender.sendMessage(Component.text("Error: No role with the name '" + roleName + "' exists", NamedTextColor.RED));
                return false;
            }

            // Add the role to the player
            boolean added = permissionManager.addRoleToPlayer(player.getUniqueId(), role.getId());

            if (added) {
                sender.sendMessage(Component.text("Role '" + roleName + "' added to player '" + player.getName() + "'", NamedTextColor.GREEN));
                player.sendMessage(Component.text("You have been assigned the role '" + roleName + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + player.getName() + "' already has the role '" + roleName + "'", NamedTextColor.YELLOW));
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
            String partialRoleName = args[2].toLowerCase();

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
        return "furious.permission.addplayerrole";
    }
}
