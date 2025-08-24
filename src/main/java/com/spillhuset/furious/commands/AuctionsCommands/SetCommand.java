package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Components.sendErrorMessage(sender, "Only players can do this.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /auctions set <name> <start> [buyout] [hours]");
            return true;
        }
        String name = args[1];
        Double start;
        try { start = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid start.");
            return true;
        }
        Double buyout = null;
        if (args.length >= 4) {
            try { buyout = Double.parseDouble(args[3]); } catch (NumberFormatException ignored) {}
        }
        Integer hours = null;
        if (args.length >= 5) {
            try { hours = Integer.parseInt(args[4]); } catch (NumberFormatException ignored) {}
        }
        plugin.auctionsService.setAuction(p, name, start, buyout, hours);
        return true;
    }

    @Override
    public String getName() { return "set"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
