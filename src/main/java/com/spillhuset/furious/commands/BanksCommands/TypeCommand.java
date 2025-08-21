package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.BankType;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class TypeCommand implements SubCommandInterface {
    private final Furious plugin;
    public TypeCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            list.addAll(plugin.banksService.suggestBankNames(args[1]));
        } else if (args.length == 3) {
            if ("player".startsWith(args[2].toLowerCase())) list.add("player");
            if ("guild".startsWith(args[2].toLowerCase())) list.add("guild");
        }
        return list;

    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Components.sendErrorMessage(sender, "Usage: /banks type <bankName> <player|guild>");
            return true;
        }
        String bankName = args[1];
        String typeArg = args[2].toLowerCase();
        BankType type;
        if (typeArg.equals("player")) type = BankType.PLAYER; else if (typeArg.equals("guild")) type = BankType.GUILD; else type = null;
        plugin.banksService.setType(sender, bankName, type);
        return true;
    }

    @Override
    public String getName() { return "type"; }

    @Override
    public String getPermission() { return "furious.banks.type"; }
}
