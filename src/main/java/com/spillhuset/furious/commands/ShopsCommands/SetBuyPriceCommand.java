package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class SetBuyPriceCommand implements SubCommandInterface {
    private final Furious plugin;
    public SetBuyPriceCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 3) {
            list.add("-");
            list.add("0");
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /shops setbuyprice <shopName> <price|'-'|0>");
            return true;
        }
        String shopName = args[1];
        String priceStr = args[2];
        plugin.shopsService.setBuyPrice(sender, shopName, priceStr);
        return true;
    }

    @Override
    public String getName() { return "setbuyprice"; }

    @Override
    public String getPermission() { return "furious.shops.setbuyprice"; }
}
