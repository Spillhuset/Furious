package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WorldSpawnCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public WorldSpawnCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        // No args: teleport executing player to their current world's spawn
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Usage from console: /worldspawn <world> <player...>");
                return true;
            }
            World world = p.getWorld();
            Location spawn = world.getSpawnLocation();
            p.teleport(spawn);
            Components.sendSuccess(sender, Components.t("Teleported to world spawn for "), Components.valueComp(world.getName()), Components.t("."));
            return true;
        }

        // Arg1: world name
        String worldName = args[0];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            Components.sendErrorMessage(sender, "World not found or not loaded: " + worldName);
            return true;
        }
        Location spawn = targetWorld.getSpawnLocation();

        // If only world specified: teleport self (must be a player)
        if (args.length == 1) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Console must specify players: /worldspawn <world> <player...>");
                return true;
            }
            p.teleport(spawn);
            Components.sendSuccess(sender, Components.t("Teleported to world spawn for "), Components.valueComp(targetWorld.getName()), Components.t("."));
            return true;
        }

        // World + players: teleport others; require extra permission
        if (!sender.hasPermission(getPermission() + ".others")) {
            Components.sendErrorMessage(sender, "You don't have permission to teleport other players.");
            return true;
        }

        Set<Player> targets = new LinkedHashSet<>();
        for (int i = 1; i < args.length; i++) {
            String name = args[i];
            Player t = Bukkit.getPlayerExact(name);
            if (t != null && t.isOnline()) {
                targets.add(t);
            } else {
                Components.sendErrorMessage(sender, "Player not found or not online: " + name);
            }
        }

        if (targets.isEmpty()) {
            Components.sendInfoMessage(sender, "No valid online players to teleport.");
            return true;
        }

        for (Player t : targets) {
            t.teleport(spawn);
            if (!t.equals(sender)) {
                Components.sendInfo(t, Components.t("You have been teleported to world spawn for "), Components.valueComp(targetWorld.getName()), Components.t("."));
            }
        }

        if (targets.size() == 1) {
            Player only = targets.iterator().next();
            Components.sendSuccess(sender, Components.t("Teleported "), Components.playerComp(only.getName()), Components.t(" to world spawn for "), Components.valueComp(targetWorld.getName()), Components.t("."));
        } else {
            Components.sendSuccess(sender, Components.t("Teleported "), Components.valueComp(String.valueOf(targets.size())), Components.t(" players to world spawn for "), Components.valueComp(targetWorld.getName()), Components.t("."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission(getPermission())) return list;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (World w : Bukkit.getWorlds()) {
                String name = w.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) list.add(name);
            }
        } else if (args.length >= 2) {
            if (!sender.hasPermission(getPermission() + ".others")) return list;
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) list.add(name);
            }
        }
        return list;
    }

    @Override
    public String getName() { return "worldspawn"; }

    @Override
    public String getPermission() { return "furious.worldspawn.teleport"; }
}
