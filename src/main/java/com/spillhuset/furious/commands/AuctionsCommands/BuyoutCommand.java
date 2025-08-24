package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BuyoutCommand implements SubCommandInterface {
    private final Furious plugin;

    public BuyoutCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.auctionsService.getAuctionNamesWithBuyout()) {
                if (name.toLowerCase().startsWith(prefix)) list.add(name);
            }
        } else if (args.length == 3) {
            if ("confirm".startsWith(args[2].toLowerCase())) list.add("confirm");
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /auctions buyout <name> [confirm]");
            return true;
        }
        String name = args[1];
        if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Only players can do this.");
                return true;
            }
            plugin.auctionsService.buyoutConfirm(p, name);
        } else {
            plugin.auctionsService.buyoutRequest(sender, name);
        }
        return true;
    }

    @Override
    public String getName() { return "buyout"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
