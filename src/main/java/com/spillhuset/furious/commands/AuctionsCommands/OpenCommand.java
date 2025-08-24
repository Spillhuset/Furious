package com.spillhuset.furious.commands.AuctionsCommands;

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
        if (args.length == 2) {
            for (String s : new String[]{"true","false"}) if (s.startsWith(args[1].toLowerCase())) list.add(s);
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /auctions open <true|false>");
            return true;
        }
        boolean open = Boolean.parseBoolean(args[1]);
        plugin.auctionsService.setOpen(sender, open);
        return true;
    }

    @Override
    public String getName() { return "open"; }

    @Override
    public String getPermission() { return "furious.auctions.open"; }
}
