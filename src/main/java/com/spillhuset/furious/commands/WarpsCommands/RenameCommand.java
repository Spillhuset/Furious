package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class RenameCommand implements SubCommandInterface {
    private final Furious plugin;

    public RenameCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

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
            Components.sendErrorMessage(sender, "Only operators can rename warps.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /warps rename <old> <new>");
            return true;
        }
        plugin.warpsService.renameWarp(sender, args[1], args[2]);
        return true;
    }

    @Override
    public String getName() { return "rename"; }

    @Override
    public String getPermission() { return "furious.warps.rename"; }
}
