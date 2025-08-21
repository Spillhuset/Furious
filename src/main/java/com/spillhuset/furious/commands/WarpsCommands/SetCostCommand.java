package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class SetCostCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetCostCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.warpsService.getWarpNames()) {
                if (name.startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            Components.sendErrorMessage(sender, "Only operators can set warp costs.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /warps setcost <name> <amount>");
            return true;
        }
        Double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            Components.sendErrorMessage(sender, "Invalid amount.");
            return true;
        }
        plugin.warpsService.setCost(sender, args[1], amount);
        return true;
    }

    @Override
    public String getName() { return "setcost"; }

    @Override
    public String getPermission() { return "furious.warps.setcost"; }
}
