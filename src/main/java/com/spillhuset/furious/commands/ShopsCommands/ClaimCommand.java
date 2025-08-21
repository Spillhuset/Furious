package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClaimCommand implements SubCommandInterface {
    private final Furious plugin;
    public ClaimCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // No dynamic shop names registry exposed yet
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this.");
            return true;
        }
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /shops claim <shopName>");
            return true;
        }
        Location loc = player.getLocation();
        plugin.shopsService.claimShop(sender, args[1], loc);
        return true;
    }

    @Override
    public String getName() { return "claim"; }

    @Override
    public String getPermission() { return "furious.shops.claim"; }
}
