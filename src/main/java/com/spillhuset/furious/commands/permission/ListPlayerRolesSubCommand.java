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
 * Subcommand for listing the roles assigned to a player.
 */
public class ListPlayerRolesSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new ListPlayerRolesSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListPlayerRolesSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "listplayerroles";
    }

    @Override
    public String getDescription() {
        return "List the roles assigned to a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm listplayerroles <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - List the roles assigned to the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm lpr <player>", NamedTextColor.GRAY));
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
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
                if (offlinePlayer != null) {
                    playerId = offlinePlayer.getUniqueId();
                }
            }

            if (playerId == null) {
                sender.sendMessage(Component.text("Error: Player '" + playerName + "' not found", NamedTextColor.RED));
                return false;
            }
        } else {
            playerId = player.getUniqueId();
            playerName = player.getName(); // Use correct case
        }

        // Get the player's roles
        Set<Role> playerRoles = permissionManager.getPlayerRoles(playerId);

        sender.sendMessage(Component.text("=== Roles for " + playerName + " ===", NamedTextColor.GOLD));

        if (playerRoles.isEmpty()) {
            sender.sendMessage(Component.text("No roles assigned", NamedTextColor.YELLOW));
        } else {
            for (Role role : playerRoles) {
                Component roleComponent = Component.text("- ", NamedTextColor.GRAY)
                        .append(Component.text(role.getName(), NamedTextColor.YELLOW));

                if (!role.getDescription().isEmpty()) {
                    roleComponent = roleComponent.append(Component.text(": ", NamedTextColor.GRAY))
                            .append(Component.text(role.getDescription(), NamedTextColor.WHITE));
                }

                sender.sendMessage(roleComponent);
            }
        }

        // Check if the default role applies
        if (playerRoles.isEmpty()) {
            Role defaultRole = permissionManager.getRoleByName("default");
            if (defaultRole != null) {
                sender.sendMessage(Component.text("Default role applies: ", NamedTextColor.GRAY)
                        .append(Component.text(defaultRole.getName(), NamedTextColor.YELLOW)));
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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.permission.listplayerroles";
    }
}
