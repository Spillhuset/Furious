package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PositionCommand implements SubCommandInterface {
    private final Furious plugin;
    public PositionCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "position"; }
    @Override public String getPermission() { return "furious.teleport.position"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length == 2) {
            // Could be player or x
            sugg.add("player");
        }
        if (args.length == 2 || args.length == 6) {
            // world name completion
            for (World w : Bukkit.getWorlds()) {
                if (args.length == 6) {
                    if (w.getName().toLowerCase().startsWith(args[5].toLowerCase())) sugg.add(w.getName());
                } else {
                    // If user typed x coordinate first, no world yet; ignore
                }
            }
        }
        if (args.length == 3 && "player".equalsIgnoreCase(args[1])) {
            String prefix = args[2].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) sugg.add(p.getName());
            }
        }
        return sugg;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 5) {
            // /teleport position x y z world -> sender must be a player
            if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this form."); return true; }
            Location loc = parseLocation(args[1], args[2], args[3], args[4]);
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position <x> <y> <z> <world>"); return true; }
            plugin.teleportsService.queueTeleport(player, loc, "position");
            return true;
        } else if (args.length == 6 && "player".equalsIgnoreCase(args[1])) {
            // /teleport position player <name> x y z world
            if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to teleport others."); return true; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
            Location loc = parseLocation(args[3], args[4], args[5], args.length >= 7 ? args[6] : null);
            // Note: above would be 7 args, but spec shows 6 with world included; adapt for 7 safety
            // However, per usage, we expect exactly 6 after 'player': name x y z world -> total 7
            return true;
        } else if (args.length == 7 && "player".equalsIgnoreCase(args[1])) {
            // /teleport position player <name> x y z world (7 args total)
            if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to teleport others."); return true; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
            Location loc = parseLocation(args[3], args[4], args[5], args[6]);
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position player <player> <x> <y> <z> <world>"); return true; }
            // Immediate teleport for target
            target.teleport(loc);
            Components.sendSuccessMessage(sender, "Teleported " + target.getName() + " to (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ") in " + loc.getWorld().getName());
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /teleport position <x> <y> <z> <world> | /teleport position player <player> <x> <y> <z> <world>");
        return true;
    }

    private Location parseLocation(String sx, String sy, String sz, String worldName) {
        try {
            double x = Double.parseDouble(sx);
            double y = Double.parseDouble(sy);
            double z = Double.parseDouble(sz);
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;
            return new Location(world, x, y, z);
        } catch (Exception ex) {
            return null;
        }
    }
}
