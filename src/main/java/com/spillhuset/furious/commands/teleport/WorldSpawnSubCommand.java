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
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        World world;
        if (args.length > 2) {
            world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage(Component.text("World not found!", NamedTextColor.RED));
                return true;
            }
        } else {
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                sender.sendMessage(Component.text("Console must specify a world!", NamedTextColor.RED));
                return true;
            }
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
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase().startsWith(partial)) {
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("/teleport worldspawn <player> [world]", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("/teleport worldspawn <player>", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("/teleport worldspawn <player> <world>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If no world is specified, the player's current world will be used.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("If the player is not online, the teleport will be attempted when they log in.", NamedTextColor.YELLOW));
    }

    @Override
    public String getPermission() {
        return "furious.teleport.worldspawn";
    }
}