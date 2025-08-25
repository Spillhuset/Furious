package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class RestockCommand implements SubCommandInterface {
    private final Furious plugin;
    public RestockCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        if (args.length == 3) {
            String p = args[2].toUpperCase();
            com.spillhuset.furious.utils.Shop shop = plugin.shopsService.getShopByName(args[1]);
            if (shop != null && shop.getType() == com.spillhuset.furious.utils.ShopType.GUILD) {
                for (com.spillhuset.furious.utils.ShopGuildItem gi : com.spillhuset.furious.utils.ShopGuildItem.values()) {
                    if (gi.name().startsWith(p)) list.add(gi.name());
                }
            } else {
                for (Material m : Material.values()) {
                    if (m.isItem() && m.name().startsWith(p)) list.add(m.name());
                }
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Components.sendInfoMessage(sender, "Usage: /shops restock <shop> <material> <amount>");
            return true;
        }
        String shop = args[1];
        String mat = args[2];
        int amount;
        try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { Components.sendErrorMessage(sender, "Invalid amount."); return true; }
        plugin.shopsService.restock(sender, shop, mat, amount);
        return true;
    }

    @Override
    public String getName() { return "restock"; }

    @Override
    public String getPermission() { return "furious.shops.restock"; }
}
