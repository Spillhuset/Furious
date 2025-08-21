package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ConnectPortalCommand implements SubCommandInterface {
    private final Furious plugin;

    public ConnectPortalCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2 || args.length == 3) {
            String prefix = args[args.length-1].toLowerCase();
            for (String name : plugin.warpsService.getWarpNames()) {
                if (name.startsWith(prefix)) out.add(name);
            }
            if (args.length == 3 && "clear".startsWith(prefix)) out.add("clear");
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            Components.sendErrorMessage(sender, "Only operators can connect portals.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /warps connectportal <name> [target|clear]");
            return true;
        }
        String name = args[1];
        String target = null;
        if (args.length >= 3 && !args[2].equalsIgnoreCase("clear")) target = args[2];
        plugin.warpsService.connectPortal(sender, name, target);
        return true;
    }

    @Override
    public String getName() { return "connectportal"; }

    @Override
    public String getPermission() { return "furious.warps.connectportal"; }
}
