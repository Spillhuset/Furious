package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OP-only: Sets a bank's open/closed state.
 * Usage: /banks open <bankName> <true|false>
 */
public class OpenCommand implements SubCommandInterface {
    private final Furious plugin;
    public OpenCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            list.addAll(plugin.banksService.suggestBankNames(args[1]));
        } else if (args.length == 3) {
            for (String s : Arrays.asList("true", "false")) {
                if (s.startsWith(args[2].toLowerCase())) list.add(s);
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendErrorMessage(sender, "Usage: /banks open <bankName> <true|false>");
            return true;
        }
        String bankName = args[1];
        String flag = args[2].toLowerCase();
        Boolean open = null;
        if (flag.equals("true") || flag.equals("yes") || flag.equals("on")) open = true;
        else if (flag.equals("false") || flag.equals("no") || flag.equals("off")) open = false;
        if (open == null) {
            Components.sendErrorMessage(sender, "Invalid flag. Use true or false.");
            return true;
        }
        plugin.banksService.setOpen(sender, bankName, open);
        return true;
    }

    @Override
    public String getName() { return "open"; }

    @Override
    public String getPermission() { return "furious.banks.open"; }
}
