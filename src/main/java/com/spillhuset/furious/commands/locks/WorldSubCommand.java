package com.spillhuset.furious.commands.locks;

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
 * Subcommand for managing locks world settings
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
        return "Manage locks world settings";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/locks world <list|enable|disable> [world]", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkPermission(sender)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "list":
                listWorlds(sender);
                break;
            case "disable":
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
        sender.sendMessage(Component.text("Locks World Settings:", NamedTextColor.YELLOW));
        Map<String, Boolean> worldsStatus = plugin.getLocksManager().getWorldsStatus();

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

        if (plugin.getLocksManager().disableWorld(world)) {
            sender.sendMessage(Component.text("Locks disabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to disable locks in world: " + worldName, NamedTextColor.RED));
        }
    }

    private void enableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        if (plugin.getLocksManager().enableWorld(world)) {
            sender.sendMessage(Component.text("Locks enabled in world: " + worldName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to enable locks in world: " + worldName, NamedTextColor.RED));
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Locks World Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/locks world list - Show all worlds and their locks settings", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks world disable <world> - Disable locks in a world", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks world enable <world> - Enable locks in a world", NamedTextColor.GOLD));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkPermission(sender)) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return Arrays.asList("list", "disable", "enable");
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("disable") || args[1].equalsIgnoreCase("enable"))) {
            return plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> !name.equals(plugin.getWorldManager().getGameWorldName()) &&
                                   !name.equals(plugin.getWorldManager().getGameBackupName()) &&
                                   !name.startsWith("minigame_"))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.locks.world";
    }

    @Override
    public boolean checkPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    @Override
    public boolean checkPermission(CommandSender sender, boolean sendMessage) {
        boolean hasPermission = sender.hasPermission(getPermission());
        if (!hasPermission && sendMessage) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
        }
        return hasPermission;
    }
}