package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Subcommand for managing homes world settings
 *
 * Permission structure:
 * - "furious.homes" - Required for /homes world list
 * - "furious.homes.world" - Required for /homes world enable [world] and /homes world disable [world]
 */
public class WorldSubCommand implements SubCommand {
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
        return "Manage homes world settings";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/homes world <list|enable|disable> [world]", NamedTextColor.YELLOW));
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
                if (!sender.hasPermission("furious.homes")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
                listWorlds(sender);
                break;
            case "disable":
                if (!sender.hasPermission("furious.homes.world")) {
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
                if (!sender.hasPermission("furious.homes.world")) {
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
        sender.sendMessage(Component.text("Homes World Settings:", NamedTextColor.YELLOW));
        Map<String, Boolean> worldsStatus = plugin.getHomesManager().getWorldsStatus();

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

        if (plugin.getHomesManager().disableWorld(world)) {
            sender.sendMessage(Component.text("Homes disabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Homes are already disabled in world: " + worldName, NamedTextColor.YELLOW));
        }
    }

    private void enableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        if (plugin.getHomesManager().enableWorld(world)) {
            sender.sendMessage(Component.text("Homes enabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Homes are already enabled in world: " + worldName, NamedTextColor.YELLOW));
        }
    }

    /**
     * Shows help information based on user's permissions
     * Only displays commands that the user has permission to use
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Homes World Commands:", NamedTextColor.YELLOW));

        // Show list command if user has furious.homes permission
        if (sender.hasPermission("furious.homes")) {
            sender.sendMessage(Component.text("/homes world list - Show all worlds and their homes settings", NamedTextColor.GOLD));
        }

        // Show enable and disable commands if user has furious.homes.world permission
        if (sender.hasPermission("furious.homes.world")) {
            sender.sendMessage(Component.text("/homes world disable <world> - Disable homes in a world", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/homes world enable <world> - Enable homes in a world", NamedTextColor.GOLD));
        }

        // If user has no permissions, show a message
        if (!sender.hasPermission("furious.homes") && !sender.hasPermission("furious.homes.world")) {
            sender.sendMessage(Component.text("You don't have permission to use any homes world commands.", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();

            // Add "list" option if user has "furious.homes" permission
            if (sender.hasPermission("furious.homes")) {
                options.add("list");
            }

            // Add "enable" and "disable" options if user has "furious.homes.world" permission
            if (sender.hasPermission("furious.homes.world")) {
                options.add("disable");
                options.add("enable");
            }

            return options;
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("enable"))) {
            // Only show world options if user has the correct permission
            if (sender.hasPermission("furious.homes.world")) {
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
     * - "furious.homes" for list subcommand
     * - "furious.homes.world" for enable/disable subcommands
     *
     * @see #checkPermission(CommandSender, String)
     */
    @Override
    public String getPermission() {
        return "furious.homes.world";
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
    public boolean checkPermission(CommandSender sender) {
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
    public boolean checkPermission(CommandSender sender, boolean sendMessage) {
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
            return sender.hasPermission("furious.homes");
        } else if (subCommand.equalsIgnoreCase("enable") || subCommand.equalsIgnoreCase("disable")) {
            return sender.hasPermission("furious.homes.world");
        }
        return false;
    }
}
