package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AcceptCommand implements SubCommandInterface {
    private final Furious plugin;
    public AcceptCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "accept"; }
    @Override public String getPermission() { return "furious.teleport.accept"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length == 2 && sender instanceof Player p) {
            // Suggest online players; could be narrowed to sender of pending request
            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op.getName().toLowerCase().startsWith(args[1].toLowerCase())) sugg.add(op.getName());
            }
        }
        return sugg;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this."); return true; }
        if (args.length < 2) { Components.sendInfoMessage(sender, "Usage: /teleport accept <player>"); return true; }
        plugin.teleportsService.accept(player, args[1]);
        return true;
    }
}
