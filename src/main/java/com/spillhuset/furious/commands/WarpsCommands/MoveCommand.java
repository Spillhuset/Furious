package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MoveCommand implements SubCommandInterface {
    private final Furious plugin;

    public MoveCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.warpsService.getWarpNames()) {
                if (name.startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            Components.sendErrorMessage(sender, "Only operators can move warps.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can move warps.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /warps move <name>");
            return true;
        }
        plugin.warpsService.moveWarp(player, args[1], player.getLocation());
        return true;
    }

    @Override
    public String getName() { return "move"; }

    @Override
    public String getPermission() { return "furious.warps.move"; }
}
