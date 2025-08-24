package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class UnspawnCommand implements SubCommandInterface {
    private final Furious plugin;
    public UnspawnCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.banksService.suggestBankNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /banks unspawn <bankName>");
            return true;
        }
        String bankName = args[1];
        Bank bank = plugin.banksService.getBankByName(bankName);
        if (bank == null) {
            Components.sendErrorMessage(sender, "Bank not found.");
            return true;
        }
        boolean removed = plugin.banksService.removeArmorStandForBank(bank);
        if (removed) {
            Components.sendSuccess(sender, Components.t("Spawn anchor removed for bank "), Components.valueComp(bank.getName()));
        } else {
            Components.sendInfoMessage(sender, "No spawn anchor to remove.");
        }
        return true;
    }

    @Override
    public String getName() { return "unspawn"; }

    @Override
    public String getPermission() { return "furious.banks.spawn"; }
}
