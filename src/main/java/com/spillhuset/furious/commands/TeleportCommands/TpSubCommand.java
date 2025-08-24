package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpSubCommand implements SubCommandInterface {
    private final Furious plugin;
    public TpSubCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "teleport"; }
    @Override public String getPermission() { return "furious.teleport.teleport"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length == 2 || args.length == 3) {
            String prefix = args[args.length - 1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) sugg.add(p.getName());
            }
        }
        return sugg;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) { Components.sendErrorMessage(sender, "You don't have permission to use this."); return true; }
        if (args.length < 3) { Components.sendInfoMessage(sender, "Usage: /teleport teleport <playerA> <playerB>"); return true; }
        Player a = Bukkit.getPlayerExact(args[1]);
        Player b = Bukkit.getPlayerExact(args[2]);
        if (a == null || b == null) { Components.sendErrorMessage(sender, "Both players must be online."); return true; }
        a.teleport(b.getLocation());
        Components.sendSuccessMessage(sender, "Teleported " + a.getName() + " to " + b.getName() + ".");
        return true;
    }
}
