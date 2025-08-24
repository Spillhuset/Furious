package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class OpenCommand implements SubCommandInterface {
    private final Furious plugin;
    public OpenCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        // /shops open <shopName> <true|false>
        if (args.length == 2) {
            list.addAll(plugin.shopsService.suggestShopNames(args[1]));
        } else if (args.length == 3) {
            for (String s : new String[]{"true","false","open","close"}) {
                if (s.startsWith(args[2].toLowerCase())) list.add(s);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /shops open <shopName> <true|false|open|close>");
            return true;
        }
        String shopName = args[1];
        String flag = args[2].toLowerCase();
        boolean open;
        if (flag.equals("true") || flag.equals("open")) open = true;
        else if (flag.equals("false") || flag.equals("close") || flag.equals("closed")) open = false;
        else {
            Components.sendErrorMessage(sender, "Value must be true/false or open/close.");
            return true;
        }
        plugin.shopsService.setOpen(sender, shopName, open);
        return true;
    }

    @Override
    public String getName() { return "open"; }

    @Override
    public String getPermission() { return "furious.shops.open"; }
}
