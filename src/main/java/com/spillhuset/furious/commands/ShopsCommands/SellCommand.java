package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Shop;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SellCommand implements SubCommandInterface {
    private final Furious plugin;

    public SellCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        // /shops sell <material> <amount>
        if (args.length == 2) {
            String p = args[1].toUpperCase();
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().startsWith(p)) list.add(m.name());
            }
        } else if (args.length == 3) {
            String p = args[2];
            // suggest common stack sizes if they start with provided prefix
            for (String s : new String[]{"1", "8", "16", "32", "64"}) {
                if (s.startsWith(p)) list.add(s);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can sell.");
            return true;
        }
        // Determine the shop by player's current location
        Shop shop = plugin.shopsService.getShopAt(player.getLocation());
        if (shop == null) {
            Components.sendErrorMessage(player, "You must be inside a shop to sell.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /shops sell <material> <amount>");
            return true;
        }
        String mat = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid amount.");
            return true;
        }
        plugin.shopsService.sell(player, shop.getName(), mat, amount);
        return true;
    }

    @Override
    public String getName() {
        return "sell";
    }

    @Override
    public String getPermission() {
        return "furious.shops.sell";
    }
}
