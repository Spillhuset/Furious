package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BidCommand implements SubCommandInterface {
    private final Furious plugin;

    public BidCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.auctionsService.getAuctionNames()) {
                if (name.toLowerCase().startsWith(prefix)) list.add(name);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Components.sendErrorMessage(sender, "Only players can do this.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /auctions bid <name> <offer>");
            return true;
        }
        String name = args[1];
        double offer;
        try { offer = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid offer.");
            return true;
        }
        plugin.auctionsService.bid(p, name, offer);
        return true;
    }

    @Override
    public String getName() { return "bid"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
