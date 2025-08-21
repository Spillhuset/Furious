package com.spillhuset.furious.commands.BanksCommands;

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
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.banksService.suggestBankNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this.");
            return true;
        }
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /banks claim <bankName>");
            return true;
        }
        Location loc = player.getLocation();
        plugin.banksService.claimBank(sender, args[1], loc);
        return true;
    }

    @Override
    public String getName() { return "claim"; }

    @Override
    public String getPermission() { return "furious.banks.claim"; }
}
