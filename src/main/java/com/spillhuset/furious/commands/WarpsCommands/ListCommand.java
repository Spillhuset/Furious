package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;

    public ListCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /warps list has no further arguments
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;
        // Expected: /warps list
        if (args.length != 1) {
            Components.sendInfoMessage(sender, "Usage: /warps list");
            return true;
        }
        plugin.warpsService.listWarps(sender);
        return true;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return "furious.warps.list";
    }
}
