package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subcommand for managing guild world settings
 *
 * Permission structure:
 * - "furious.guild" - Required for /guild world list
 * - "furious.guild.world" - Required for /guild world enable [world] and /guild world disable [world]
 */
public class WorldSubCommand implements GuildSubCommand {
    private final Furious plugin;

    public WorldSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public String getDescription() {
        return "Manage guild world settings";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/guild world <list|enable|disable> [world]", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "list":
                if (!sender.hasPermission("furious.guild")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
                listWorlds(sender);
                break;
            case "disable":
                if (!sender.hasPermission("furious.guild.world")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
                if (args.length == 2 && (sender instanceof Player player)) {
                    disableWorld(player, player.getWorld().getName());
                    return true;
                }
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Please specify a world name!", NamedTextColor.RED));
                    return true;
                }
                disableWorld(sender, args[2]);
                break;
            case "enable":
                if (!sender.hasPermission("furious.guild.world")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
                if (args.length == 2 && (sender instanceof Player player)) {
                    enableWorld(player, player.getWorld().getName());
                    return true;
                }
                if (args.length != 3) {
                    sender.sendMessage(Component.text("Please specify a world name!", NamedTextColor.RED));
                    return true;
                }
                enableWorld(sender, args[2]);
                break;
            default:
                showHelp(sender);
        }
        return true;
    }

    private void listWorlds(CommandSender sender) {
        sender.sendMessage(Component.text("Guild World Settings:", NamedTextColor.YELLOW));
        Map<String, Boolean> worldsStatus = plugin.getGuildManager().getWorldsStatus();

        if (worldsStatus.isEmpty()) {
            sender.sendMessage(Component.text("No worlds available.", NamedTextColor.GRAY));
            return;
        }

        for (Map.Entry<String, Boolean> entry : worldsStatus.entrySet()) {
            String worldName = entry.getKey();
            boolean enabled = entry.getValue();
            NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
            sender.sendMessage(Component.text("- " + worldName + (enabled ? " [ENABLED]" : " [DISABLED]"), color));
        }
    }

    private void disableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        if (plugin.getGuildManager().disableWorld(world)) {
            sender.sendMessage(Component.text("Guilds disabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to disable guilds in world: " + worldName, NamedTextColor.RED));
        }
    }

    private void enableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        if (plugin.getGuildManager().enableWorld(world)) {
            sender.sendMessage(Component.text("Guilds enabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to enable guilds in world: " + worldName, NamedTextColor.RED));
        }
    }

    /**
     * Shows help information based on user's permissions
     * Only displays commands that the user has permission to use
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Guild World Commands:", NamedTextColor.YELLOW));

        // Show list command if user has furious.guild permission
        if (sender.hasPermission("furious.guild")) {
            sender.sendMessage(Component.text("/guild world list - Show all worlds and their guild settings", NamedTextColor.GOLD));
        }

        // Show enable and disable commands if user has furious.guild.world permission
        if (sender.hasPermission("furious.guild.world")) {
            sender.sendMessage(Component.text("/guild world disable <world> - Disable guilds in a world", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/guild world enable <world> - Enable guilds in a world", NamedTextColor.GOLD));
        }

        // If user has no permissions, show a message
        if (!sender.hasPermission("furious.guild") && !sender.hasPermission("furious.guild.world")) {
            sender.sendMessage(Component.text("You don't have permission to use any guild world commands.", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();

            // Add "list" option if user has "furious.guild" permission
            if (sender.hasPermission("furious.guild")) {
                options.add("list");
            }

            // Add "enable" and "disable" options if user has "furious.guild.world" permission
            if (sender.hasPermission("furious.guild.world")) {
                options.add("disable");
                options.add("enable");
            }

            return options;
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("enable"))) {
            // Only show world options if user has the correct permission
            if (sender.hasPermission("furious.guild.world")) {
                return plugin.getServer().getWorlds().stream()
                        .map(World::getName)
                        .filter(name -> !name.equals(plugin.getWorldManager().getGameWorldName()) &&
                                !name.equals(plugin.getWorldManager().getGameBackupName()) &&
                                !name.startsWith("minigame_"))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    /**
     * Returns the base permission for this command
     * Note: Different subcommands may require different permissions
     * - "furious.guild" for list subcommand
     * - "furious.guild.world" for enable/disable subcommands
     *
     * @see #checkPermission(CommandSender, String)
     */
    @Override
    public String getPermission() {
        return "furious.guild.world";
    }

    @Override
    public GuildRole getRequiredRole() {
        return null; // No specific guild role required
    }

    /**
     * Checks if the sender has the base permission for this command
     * Note: This method is kept for backward compatibility
     * For specific subcommand permission checks, use hasPermission() directly
     *
     * @param sender The command sender
     * @return true if the sender has the base permission
     */
    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    /**
     * Checks if the sender has the base permission for this command
     * Note: This method is kept for backward compatibility
     * For specific subcommand permission checks, use hasPermission() directly
     *
     * @param sender The command sender
     * @param sendMessage Whether to send a message if permission is denied
     * @return true if the sender has the base permission
     */
    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean sendMessage) {
        boolean hasPermission = sender.hasPermission(getPermission());
        if (!hasPermission && sendMessage) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
        }
        return hasPermission;
    }

    /**
     * Checks if the sender has permission for a specific subcommand
     *
     * @param sender The command sender
     * @param subCommand The subcommand to check permission for
     * @return true if the sender has permission for the subcommand
     */
    private boolean checkPermission(CommandSender sender, String subCommand) {
        if (subCommand.equalsIgnoreCase("list")) {
            return sender.hasPermission("furious.guild");
        } else if (subCommand.equalsIgnoreCase("enable") || subCommand.equalsIgnoreCase("disable")) {
            return sender.hasPermission("furious.guild.world");
        }
        return false;
    }
}
