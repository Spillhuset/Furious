package com.spillhuset.furious.commands.WarpsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class SetPasswordCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetPasswordCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

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
            Components.sendErrorMessage(sender, "Only operators can set warp passwords.");
            return true;
        }
        if (args.length < 3) {
            Components.sendInfoMessage(sender, "Usage: /warps setpassword <name> <password|clear>");
            return true;
        }
        String name = args[1];
        String pass = args[2];
        if (pass.equalsIgnoreCase("clear") || pass.equalsIgnoreCase("none")) {
            plugin.warpsService.setPassword(sender, name, null);
        } else {
            plugin.warpsService.setPassword(sender, name, pass);
        }
        return true;
    }

    @Override
    public String getName() { return "setpassword"; }

    @Override
    public String getPermission() { return "furious.warps.setpassword"; }
}
