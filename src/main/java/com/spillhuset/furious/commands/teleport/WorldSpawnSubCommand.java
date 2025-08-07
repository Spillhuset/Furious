package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WorldSpawnSubCommand implements SubCommand {
    private final Furious plugin;

    public WorldSpawnSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Player target;
        World world;

        // If no arguments, teleport the sender to the current world's spawn
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a player and/or world!", NamedTextColor.RED));
                return true;
            }
            target = player;
            world = player.getWorld();
        }
        // If one argument, it could be a player or a world
        else if (args.length == 2) {
            // Check if the argument is a player
            target = Bukkit.getPlayer(args[1]);

            // If not a player, it might be a world
            if (target == null) {
                world = Bukkit.getWorld(args[1]);
                if (world == null) {
                    sender.sendMessage(Component.text("Player or world not found: " + args[1], NamedTextColor.RED));
                    return true;
                }

                // If it's a world, the target must be the sender
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Console must specify a player!", NamedTextColor.RED));
                    return true;
                }
                target = player;
            } else {
                // If it's a player, use their current world
                world = target.getWorld();
            }
        }
        // If two arguments, first is player, second is world
        else if (args.length > 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }

            world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage(Component.text("World not found: " + args[2], NamedTextColor.RED));
                return true;
            }
        } else {
            getUsage(sender);
            return true;
        }

        if (plugin.getTeleportManager().forceTeleport(target, world.getSpawnLocation())) {
            Component message = Component.text("Teleported " + target.getName() + " to " + world.getName() + "'s spawn", NamedTextColor.GREEN);
            sender.sendMessage(message);
            if (sender != target) {
                target.sendMessage(Component.text("You were teleported to " + world.getName() + "'s spawn", NamedTextColor.GREEN));
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            String partial = args[2].toLowerCase();
            String gameBackupName = plugin.getWorldManager().getGameBackupName();

            for (World world : Bukkit.getWorlds()) {
                // Skip GameBackup world
                if (!world.getName().equals(gameBackupName) && world.getName().toLowerCase().startsWith(partial)) {
                    completions.add(world.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getName() {
        return "worldspawn";
    }

    @Override
    public String getDescription() {
        return "";
    }

    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/teleport worldspawn", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport worldspawn <world>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport worldspawn <player>", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport worldspawn <player> [world]", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/teleport worldspawn <player> <world>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no arguments are provided, you will be teleported to your current world's spawn.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If only a world is specified, you will be teleported to that world's spawn.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If only a player is specified, they will be teleported to their current world's spawn.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If both player and world are specified, the player will be teleported to that world's spawn.", NamedTextColor.YELLOW));
    }

    @Override
    public String getPermission() {
        return "furious.teleport.worldspawn";
    }
}
