package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;

    public ListCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String filter = (args.length >= 2) ? args[1] : null;
        plugin.auctionsService.listAuctions(sender, filter);
        return true;
    }

    @Override
    public String getName() { return "list"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
