package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RequestCommand implements SubCommandInterface {
    private final Furious plugin;
    public RequestCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "request"; }
    @Override public String getPermission() { return "furious.teleport.request"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player sp && p.getUniqueId().equals(sp.getUniqueId())) continue;
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) sugg.add(p.getName());
            }
        }
        return sugg;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this."); return true; }
        if (args.length < 2) { Components.sendInfoMessage(sender, "Usage: /teleport request <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Components.sendErrorMessage(sender, "Player not found or not online."); return true; }
        plugin.teleportsService.requestTeleport(player, target);
        return true;
    }
}
