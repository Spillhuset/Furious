package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CancelCommand implements SubCommandInterface {
    private final Furious plugin;

    public CancelCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2 && sender instanceof Player p) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.auctionsService.getAuctionNamesOwnedBy(p.getUniqueId())) {
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
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /auctions cancel <name>");
            return true;
        }
        plugin.auctionsService.cancel(p, args[1]);
        return true;
    }

    @Override
    public String getName() { return "cancel"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
