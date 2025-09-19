package com.spillhuset.furious.commands.WorldCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SpawnCommand implements SubCommandInterface {
    private final Furious plugin;

    public SpawnCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        // /world spawn <world> [player]
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (World w : Bukkit.getWorlds()) {
                String name = w.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(name);
            }
        } else if (args.length == 3) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /world spawn <world> [player]");
            return true;
        }
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Components.sendErrorMessage(sender, "World not found or not loaded: " + worldName);
            return true;
        }
        Location spawn = world.getSpawnLocation();

        // No player specified -> self (must be a player)
        if (args.length == 2) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Console must specify a player: /world spawn <world> <player>");
                return true;
            }
            p.teleport(spawn);
            Components.sendSuccess(sender, Components.t("Teleported to world spawn for "), Components.valueComp(world.getName()), Components.t("."));
            return true;
        }

        // player specified -> teleport target(s)
        if (!sender.hasPermission(getPermission() + ".others")) {
            Components.sendErrorMessage(sender, "You don't have permission to teleport other players.");
            return true;
        }

        Set<Player> targets = new LinkedHashSet<>();
        for (int i = 2; i < args.length; i++) {
            Player t = Bukkit.getPlayerExact(args[i]);
            if (t != null && t.isOnline()) targets.add(t);
            else Components.sendErrorMessage(sender, "Player not found or not online: " + args[i]);
        }
        if (targets.isEmpty()) {
            Components.sendInfoMessage(sender, "No valid online players to teleport.");
            return true;
        }
        for (Player t : targets) {
            t.teleport(spawn);
            if (!t.equals(sender)) {
                Components.sendInfo(t, Components.t("You have been teleported to world spawn for "), Components.valueComp(world.getName()), Components.t("."));
            }
        }
        if (targets.size() == 1) {
            Player only = targets.iterator().next();
            Components.sendSuccess(sender, Components.t("Teleported "), Components.playerComp(only.getName()), Components.t(" to world spawn for "), Components.valueComp(world.getName()), Components.t("."));
        } else {
            Components.sendSuccess(sender, Components.t("Teleported "), Components.valueComp(String.valueOf(targets.size())), Components.t(" players to world spawn for "), Components.valueComp(world.getName()), Components.t("."));
        }
        return true;
    }

    @Override
    public String getName() { return "spawn"; }

    @Override
    public String getPermission() { return "furious.world.spawn"; }
}
