package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PermissionManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for adding a direct permission to a player.
 */
public class AddPlayerPermissionSubCommand implements SubCommand {
    private final Furious plugin;
    private final PermissionManager permissionManager;

    /**
     * Creates a new AddPlayerPermissionSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AddPlayerPermissionSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public String getName() {
        return "addplayerpermission";
    }

    @Override
    public String getDescription() {
        return "Add a direct permission to a player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm addplayerpermission <player> <permission>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a direct permission to the specified player", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm app <player> <permission>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Examples:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /perm app Steve furious.teleport.force", NamedTextColor.YELLOW)
                .append(Component.text(" - Add the teleport.force permission to Steve", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  /perm app Alex -furious.guild.claim", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a negated permission to Alex", NamedTextColor.WHITE)));
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
                playerId = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            }

            if (playerId == null) {
                sender.sendMessage(Component.text("Error: Player '" + playerName + "' not found", NamedTextColor.RED));
                return false;
            }

            // Add the permission to the player
            boolean added = permissionManager.addDirectPermissionToPlayer(playerId, permissionNode);

            if (added) {
                sender.sendMessage(Component.text("Permission '" + permissionNode + "' added to player '" + playerName + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + playerName + "' already has the permission '" + permissionNode + "'", NamedTextColor.YELLOW));
            }
        } else {
            // Add the permission to the player
            boolean added = permissionManager.addDirectPermissionToPlayer(player.getUniqueId(), permissionNode);

            if (added) {
                sender.sendMessage(Component.text("Permission '" + permissionNode + "' added to player '" + player.getName() + "'", NamedTextColor.GREEN));
                player.sendMessage(Component.text("You have been granted the permission '" + permissionNode + "'", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Player '" + player.getName() + "' already has the permission '" + permissionNode + "'", NamedTextColor.YELLOW));
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
        return "furious.permission.addplayerpermission";
    }
}