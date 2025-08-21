package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CreateCommand implements SubCommandInterface {
    private final Furious plugin;

    public CreateCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendErrorMessage(sender, "Usage: /banks create <name>");
            return true;
        }
        plugin.banksService.createBank(sender, args[1]);
        return true;
    }

    @Override
    public String getName() { return "create"; }

    @Override
    public String getPermission() { return "furious.banks.create"; }
}
