package com.spillhuset.furious.commands.AuctionsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand implements SubCommandInterface {
    private final Furious plugin;

    public SpawnCommand(Furious plugin) { this.plugin = plugin; }

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
        plugin.auctionsService.spawnAnchor(sender, p.getLocation());
        return true;
    }

    @Override
    public String getName() { return "spawn"; }

    @Override
    public String getPermission() { return "furious.auctions.spawn"; }
}
