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
import java.util.List;

public class PositionCommand implements SubCommandInterface {
    private final Furious plugin;
    public PositionCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "position"; }
    @Override public String getPermission() { return "furious.teleport.position"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        // Args: ["position", ...]
        // Self: position <x> <y> <z> [world]
        // Other: position player <name> <x> <y> <z> [world]
        if (args.length == 2) {
            // Could be "player" keyword or start of x coordinate
            sugg.add("player");
        }
        // World name completion when optional world is being typed
        if ((args.length == 6 && !"player".equalsIgnoreCase(args[1])) || (args.length == 7 && "player".equalsIgnoreCase(args[1]))) {
            String prefix = args[args.length - 1].toLowerCase();
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().startsWith(prefix)) sugg.add(w.getName());
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
        // Expected forms (including the leading "position" in args[0]):
        // 1) position <x> <y> <z> [world]
        // 2) position player <player> <x> <y> <z> [world]
        if (args.length == 4) {
            // position x y z  (no world) -> sender must be a player; use sender's world
            if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this form."); return true; }
            Location loc = parseLocation(args[1], args[2], args[3], player.getWorld());
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position <x> <y> <z> [world]"); return true; }
            plugin.teleportsService.queueTeleport(player, loc, "position");
            return true;
        } else if (args.length == 5) {
            // position x y z world
            if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this form."); return true; }
            Location loc = parseLocation(args[1], args[2], args[3], Bukkit.getWorld(args[4]));
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position <x> <y> <z> [world]"); return true; }
            plugin.teleportsService.queueTeleport(player, loc, "position");
            return true;
        } else if (args.length == 6 && "player".equalsIgnoreCase(args[1])) {
            // position player <name> x y z  (no world) -> use target's current world
            if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to teleport others."); return true; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
            Location loc = parseLocation(args[3], args[4], args[5], target.getWorld());
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position player <player> <x> <y> <z> [world]"); return true; }
            target.teleport(loc);
            Components.sendSuccessMessage(sender, "Teleported " + target.getName() + " to (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ") in " + loc.getWorld().getName());
            return true;
        } else if (args.length == 7 && "player".equalsIgnoreCase(args[1])) {
            // position player <name> x y z world
            if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to teleport others."); return true; }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
            World world = Bukkit.getWorld(args[6]);
            Location loc = parseLocation(args[3], args[4], args[5], world);
            if (loc == null) { Components.sendErrorMessage(sender, "Usage: /teleport position player <player> <x> <y> <z> [world]"); return true; }
            target.teleport(loc);
            Components.sendSuccessMessage(sender, "Teleported " + target.getName() + " to (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ") in " + loc.getWorld().getName());
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /teleport position <x> <y> <z> [world] | /teleport position player <player> <x> <y> <z> [world]");
        return true;
    }

    private Location parseLocation(String sx, String sy, String sz, World world) {
        try {
            double x = Double.parseDouble(sx);
            double y = Double.parseDouble(sy);
            double z = Double.parseDouble(sz);
            if (world == null) return null;
            return new Location(world, x, y, z);
        } catch (Exception ex) {
            return null;
        }
    }
}
