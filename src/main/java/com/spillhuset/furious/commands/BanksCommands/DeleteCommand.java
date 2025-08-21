package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class DeleteCommand implements SubCommandInterface {
    private final Furious plugin;
    public DeleteCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.banksService.suggestBankNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /banks delete <name>");
            return true;
        }
        plugin.banksService.deleteBank(sender, args[1]);
        return true;
    }

    @Override
    public String getName() { return "delete"; }

    @Override
    public String getPermission() { return "furious.banks.delete"; }
}
