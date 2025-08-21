package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class AddItemCommand implements SubCommandInterface {
    private final Furious plugin;
    public AddItemCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        if (args.length == 3) {
            String p = args[2].toUpperCase();
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().startsWith(p)) list.add(m.name());
            }
        }
        if (args.length == 5) { list.add("-"); list.add("0"); }
        if (args.length == 6) { list.add("-"); list.add("0"); }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 6) {
            Components.sendInfoMessage(sender, "Usage: /shops additem <shop> <material> <stock> <buyPrice|'-'|0> <sellPrice|'-'|0>");
            return true;
        }
        String shop = args[1];
        String mat = args[2];
        int stock;
        try { stock = Integer.parseInt(args[3]); } catch (NumberFormatException e) { Components.sendErrorMessage(sender, "Invalid stock."); return true; }
        String buyStr = args[4];
        String sellStr = args[5];
        plugin.shopsService.addItem(sender, shop, mat, stock, buyStr, sellStr);
        return true;
    }

    @Override
    public String getName() { return "additem"; }

    @Override
    public String getPermission() { return "furious.shops.additem"; }
}
