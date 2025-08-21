package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SellCommand implements SubCommandInterface {
    private final Furious plugin;
    public SellCommand(Furious plugin) { this.plugin = plugin; }

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
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can sell.");
            return true;
        }
        // Restrict non-ops to shop-claimed chunks
        if (!player.isOp() && plugin.shopsService.getShopAt(player.getLocation()) == null) {
            Components.sendErrorMessage(player, "You must be in a shop area.");
            return true;
        }
        if (args.length < 4) {
            Components.sendInfoMessage(sender, "Usage: /shops sell <shop> <material> <amount>");
            return true;
        }
        String shop = args[1];
        String mat = args[2];
        int amount;
        try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { Components.sendErrorMessage(sender, "Invalid amount."); return true; }
        plugin.shopsService.sell(player, shop, mat, amount);
        return true;
    }

    @Override
    public String getName() { return "sell"; }

    @Override
    public String getPermission() { return "furious.shops.sell"; }
}
