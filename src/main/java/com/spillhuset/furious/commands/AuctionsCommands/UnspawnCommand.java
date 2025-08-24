package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class UnspawnCommand implements SubCommandInterface {
    private final Furious plugin;
    public UnspawnCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.auctionsService.removeSpawnAnchor(sender);
        return true;
    }

    @Override
    public String getName() { return "unspawn"; }

    @Override
    public String getPermission() { return "furious.auctions.spawn"; }
}
