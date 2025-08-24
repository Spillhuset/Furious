package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AllCommand implements SubCommandInterface {
    private final Furious plugin;
    public AllCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "all"; }
    @Override public String getPermission() { return "furious.teleport.all"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length == 2) {
            sugg.add("player");
        }
        if (args.length == 2 || args.length == 5) {
            for (World w : Bukkit.getWorlds()) {
                if (args.length == 5 && w.getName().toLowerCase().startsWith(args[4].toLowerCase())) sugg.add(w.getName());
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
        if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to teleport all players."); return true; }
        if (args.length == 5) {
            // /teleport all x y z world
            Location loc = parseLocation(args[1], args[2], args[3], args[4]);
            if (loc == null) { Components.sendInfoMessage(sender, "Usage: /teleport all <x> <y> <z> <world>"); return true; }
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp()) continue; // only non-op players
                p.teleport(loc);
                count++;
            }
            Components.sendSuccessMessage(sender, "Teleported " + count + " non-op players to the specified location.");
            return true;
        } else if (args.length == 3 && "player".equalsIgnoreCase(args[1])) {
            // /teleport all player <name>
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
            Location loc = target.getLocation();
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp()) continue;
                if (p.getUniqueId().equals(target.getUniqueId())) continue;
                p.teleport(loc);
                count++;
            }
            Components.sendSuccessMessage(sender, "Teleported " + count + " non-op players to " + target.getName() + ".");
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /teleport all <x> <y> <z> <world> | /teleport all player <player>");
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
