package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class RemoveItemCommand implements SubCommandInterface {
    private final Furious plugin;
    public RemoveItemCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /shops removeitem <shop> <material>");
            return true;
        }
        plugin.shopsService.removeItem(sender, args[1], args[2]);
        return true;
    }

    @Override
    public String getName() { return "removeitem"; }

    @Override
    public String getPermission() { return "furious.shops.removeitem"; }
}
