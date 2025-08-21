package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ListItemCommand implements SubCommandInterface {
    private final Furious plugin;
    public ListItemCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Restrict non-ops to shop-claimed chunks if executed by a player
        if (sender instanceof Player player) {
            if (!player.isOp() && plugin.shopsService.getShopAt(player.getLocation()) == null) {
                Components.sendErrorMessage(player, "You must be in a shop area.");
                return true;
            }
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /shops listitem <shop>");
            return true;
        }
        plugin.shopsService.listItems(sender, args[1]);
        return true;
    }

    @Override
    public String getName() { return "listitem"; }

    @Override
    public String getPermission() { return "furious.shops.listitem"; }
}
