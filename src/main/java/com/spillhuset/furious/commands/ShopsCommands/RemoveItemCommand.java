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
        if (args.length == 3) {
            com.spillhuset.furious.utils.Shop shop = plugin.shopsService.getShopByName(args[1]);
            String p = args[2].toUpperCase();
            if (shop != null && shop.getType() == com.spillhuset.furious.utils.ShopType.GUILD) {
                for (com.spillhuset.furious.utils.ShopGuildItem gi : com.spillhuset.furious.utils.ShopGuildItem.values()) {
                    if (gi.name().startsWith(p)) list.add(gi.name());
                }
            } else {
                for (org.bukkit.Material m : org.bukkit.Material.values()) {
                    if (m.isItem() && m.name().startsWith(p)) list.add(m.name());
                }
            }
        }
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
