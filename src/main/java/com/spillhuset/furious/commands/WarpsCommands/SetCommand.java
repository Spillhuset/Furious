package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            Components.sendErrorMessage(sender, "Only operators can set warps.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can set warps.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /warps set <name>");
            return true;
        }
        String name = args[1];
        plugin.warpsService.setWarp(player, name, player.getLocation());
        return true;
    }

    @Override
    public String getName() { return "set"; }

    @Override
    public String getPermission() { return "furious.warps.set"; }
}
