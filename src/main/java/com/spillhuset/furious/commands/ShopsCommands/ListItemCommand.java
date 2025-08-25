package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Shop;
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
        // First parameter after subcommand is the shop name; suggest known shop names
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // If player and not op, they must be in a shop-claimed chunk to use this command at all
        Shop here = null;
        if (sender instanceof Player player) {
            here = plugin.shopsService.getShopAt(player.getLocation());
            if (!player.isOp() && here == null) {
                Components.sendErrorMessage(player, "You must be in a shop area.");
                return true;
            }
        }

        // If no shop name provided and we are inside a claimed shop, use that shop
        if (args.length < 2) {
            if (here != null) {
                plugin.shopsService.listItems(sender, here.getName());
                return true;
            }
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
