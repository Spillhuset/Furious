package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class WorldConfigSubCommand implements SubCommand {
    private final Furious plugin;

    public WorldConfigSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public String getDescription() {
        return "Changing the settings of teleportation between worlds.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(Component.text("/teleport world <list|disable|enable> <world>",NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport world <list|disable|enable> [world]",NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Component.text("You don't have permission to use this command!",
                    NamedTextColor.RED));
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
        sender.sendMessage(Component.text("World UUIDs:", NamedTextColor.YELLOW));
        for (Map.Entry<String, UUID> entry : plugin.getTeleportManager().getWorldUUIDs().entrySet()) {
            boolean disabled = plugin.getTeleportManager().isWorldDisabled(entry.getValue());
            NamedTextColor color = disabled ? NamedTextColor.RED : NamedTextColor.GREEN;
            sender.sendMessage(Component.text("- " + entry.getKey() + (disabled ? " [DISABLED]" : ""), color));
            sender.sendMessage(Component.text(" (UUID: " + entry.getValue() + ")", color));
        }
    }

    private void disableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        plugin.getTeleportManager().addDisabledWorld(world);
        sender.sendMessage(Component.text("Teleportation disabled in world: " + worldName,
                NamedTextColor.GREEN));
    }

    private void enableWorld(CommandSender sender, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Component.text("World not found: " + worldName, NamedTextColor.RED));
            return;
        }

        plugin.getTeleportManager().removeDisabledWorld(world);
        sender.sendMessage(Component.text("Teleportation enabled in world: " + worldName,
                NamedTextColor.GREEN));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("World Configuration Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/teleport world list - Show all worlds and their UUIDs",
                NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/teleport world disable <world> - Disable teleportation in a world",
                NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/teleport world enable <world> - Enable teleportation in a world",
                NamedTextColor.GOLD));
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(getPermission())) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return Arrays.asList("list", "disable", "enable");
        }

        if (args.length == 3 && (args[1].equalsIgnoreCase("disable") ||
                args[1].equalsIgnoreCase("enable"))) {
            return plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.teleport.worldconfig";
    }
}