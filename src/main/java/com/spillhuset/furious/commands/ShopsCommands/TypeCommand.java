package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.ShopType;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class TypeCommand implements SubCommandInterface {
    private final Furious plugin;
    public TypeCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        } else if (args.length == 3) {
            String s = args[2].toLowerCase();
            if ("player".startsWith(s)) list.add("player");
            if ("guild".startsWith(s)) list.add("guild");
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendErrorMessage(sender, "Usage: /shops type <shopName> <player|guild>");
            return true;
        }
        String shopName = args[1];
        String typeArg = args[2].toLowerCase();
        ShopType type;
        if (typeArg.equals("player")) type = ShopType.PLAYER; else if (typeArg.equals("guild")) type = ShopType.GUILD; else type = null;
        plugin.shopsService.setType(sender, shopName, type);
        return true;
    }

    @Override
    public String getName() { return "type"; }

    @Override
    public String getPermission() { return "furious.shops.type"; }
}
