package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Shop;
import com.spillhuset.furious.utils.ShopGuildItem;
import com.spillhuset.furious.utils.ShopType;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BuyCommand implements SubCommandInterface {
    private final Furious plugin;

    public BuyCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        // Context-aware completion based on player's current shop
        if (sender instanceof Player p) {
            Shop shop = plugin.shopsService.getShopAt(p.getLocation());
            if (shop != null && shop.getType() == ShopType.GUILD) {
                // /shops buy <GuildItem> [confirm]
                if (args.length == 2) {
                    String pref = args[1].toUpperCase();
                    for (ShopGuildItem gi : ShopGuildItem.values()) {
                        if (gi.name().startsWith(pref)) list.add(gi.name());
                    }
                } else if (args.length == 3) {
                    if ("confirm".startsWith(args[2].toLowerCase())) list.add("confirm");
                }
                return list;
            }
        }
        // Default (player shops): /shops buy <material> <amount>
        if (args.length == 2) {
            String pfx = args[1].toUpperCase();
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().startsWith(pfx)) list.add(m.name());
            }
        } else if (args.length == 3) {
            String p = args[2];
            for (String s : new String[]{"1", "8", "16", "32", "64"}) {
                if (s.startsWith(p)) list.add(s);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can buy.");
            return true;
        }
        // Determine the shop by player's current location
        Shop shop = plugin.shopsService.getShopAt(player.getLocation());
        if (shop == null) {
            Components.sendErrorMessage(player, "You must be inside a shop to buy.");
            return true;
        }
        if (shop.getType() == ShopType.GUILD) {
            // /shops buy <GuildItem> [confirm]
            if (args.length < 2) {
                Components.sendInfoMessage(sender, "Usage: /shops buy <GuildItem> [confirm]");
                return true;
            }
            String item = args[1];
            if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
                plugin.shopsService.buyGuildConfirm(player, shop.getName(), item);
            } else {
                plugin.shopsService.buyGuildRequest(sender, shop.getName(), item);
            }
            return true;
        } else {
            if (args.length < 3) {
                Components.sendInfoMessage(sender, "Usage: /shops buy <material> <amount>");
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
            plugin.shopsService.buy(player, shop.getName(), mat, amount);
            return true;
        }
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getPermission() {
        return "furious.shops.buy";
    }
}
