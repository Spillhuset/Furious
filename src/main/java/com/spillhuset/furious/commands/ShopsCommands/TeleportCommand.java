package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand implements SubCommandInterface {
    private final Furious plugin;
    public TeleportCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can teleport.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /shops teleport <name>");
            return true;
        }
        plugin.shopsService.teleport(player, args[1]);
        return true;
    }

    @Override
    public String getName() { return "teleport"; }

    @Override
    public String getPermission() { return "furious.shops.teleport"; }
}
