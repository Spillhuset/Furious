package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Permission;
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
 * Subcommand for removing a direct permission from a player.
 */
public class RemovePlayerPermissionSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new RemovePlayerPermissionSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RemovePlayerPermissionSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "removeplayerpermission";
    }

    @Override
    public String getDescription() {
        return "Remove a direct permission from a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm removeplayerpermission <player> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a direct permission from the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm rpp <player> <permission>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing player name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Error: Missing permission", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String playerName = args[1];
        String permissionNode = args[2];

        // Get the player
        Player player = Bukkit.getPlayer(playerName);
        UUID playerId;

        if (player == null) {
            // Try to get an offline player
            playerId = null;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equalsIgnoreCase(playerName)) {
                    playerId = onlinePlayer.getUniqueId();
                    playerName = onlinePlayer.getName(); // Use correct case
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

            // Remove the permission from the player
            boolean removed = permissionManager.removeDirectPermissionFromPlayer(playerId, permissionNode);

            if (removed) {
                sender.sendMessage(Component.text("Permission '" + permissionNode + "' removed from player '" + playerName + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + playerName + "' does not have the direct permission '" + permissionNode + "'", NamedTextColor.YELLOW));
            }
        } else {
            // Remove the permission from the player
            boolean removed = permissionManager.removeDirectPermissionFromPlayer(player.getUniqueId(), permissionNode);

            if (removed) {
                sender.sendMessage(Component.text("Permission '" + permissionNode + "' removed from player '" + player.getName() + "'", NamedTextColor.GREEN));
                player.sendMessage(Component.text("The permission '" + permissionNode + "' has been removed from you", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Player '" + player.getName() + "' does not have the direct permission '" + permissionNode + "'", NamedTextColor.YELLOW));
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
            String partialPermission = args[2].toLowerCase();

            // Get the player
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                // Get the player's direct permissions
                Set<Permission> playerPermissions = permissionManager.getPlayerDirectPermissions(player.getUniqueId());

                // Add permission nodes that match the partial input
                for (Permission permission : playerPermissions) {
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
        return "furious.permission.removeplayerpermission";
    }
}
