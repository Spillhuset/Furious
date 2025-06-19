package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Permission;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Subcommand for listing the direct permissions assigned to a player.
 */
public class ListPlayerPermissionsSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new ListPlayerPermissionsSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListPlayerPermissionsSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "listplayerpermissions";
    }

    @Override
    public String getDescription() {
        return "List the direct permissions assigned to a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm listplayerpermissions <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - List the direct permissions assigned to the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm lpp <player>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Error: Missing player name", NamedTextColor.RED));
            getUsage(sender);
            return false;
        }

        String playerName = args[1];

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
                playerId = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            }

            if (playerId == null) {
                sender.sendMessage(Component.text("Error: Player '" + playerName + "' not found", NamedTextColor.RED));
                return false;
            }
        } else {
            playerId = player.getUniqueId();
            playerName = player.getName(); // Use correct case
        }

        // Get the player's direct permissions
        Set<Permission> playerPermissions = permissionManager.getPlayerDirectPermissions(playerId);

        sender.sendMessage(Component.text("=== Direct Permissions for " + playerName + " ===", NamedTextColor.GOLD));

        if (playerPermissions.isEmpty()) {
            sender.sendMessage(Component.text("No direct permissions assigned", NamedTextColor.YELLOW));
        } else {
            // Group permissions by their first segment (e.g., "furious.bank.*" -> "furious.bank")
            Map<String, List<Permission>> groupedPermissions = new HashMap<>();

            for (Permission permission : playerPermissions) {
                String node = permission.getNode();
                String group = node.contains(".") ? node.substring(0, node.indexOf(".")) : node;

                groupedPermissions.computeIfAbsent(group, k -> new ArrayList<>()).add(permission);
            }

            // Display grouped permissions
            for (String group : groupedPermissions.keySet()) {
                sender.sendMessage(Component.text(group + ":", NamedTextColor.GRAY));

                for (Permission permission : groupedPermissions.get(group)) {
                    Component permComponent = Component.text("  - ", NamedTextColor.GRAY);

                    if (permission.isNegated()) {
                        permComponent = permComponent.append(Component.text("-", NamedTextColor.RED));
                    }

                    permComponent = permComponent.append(Component.text(permission.getNode(),
                            permission.isNegated() ? NamedTextColor.RED : NamedTextColor.GREEN));

                    sender.sendMessage(permComponent);
                }
            }
        }

        sender.sendMessage(Component.text("Use '/perm listplayerroles " + playerName + "' to see the player's roles and their permissions", NamedTextColor.GRAY));

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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.listplayerpermissions";
    }
}