package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DepositCommand implements SubCommandInterface {
    private final Furious plugin;
    public DepositCommand(Furious plugin) { this.plugin = plugin; }

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
        // Player form: /banks deposit <amount> -> infer bank from current claimed chunk
        if (args.length == 2) {
            try {
                double amount = Double.parseDouble(args[1]);
                var bank = plugin.banksService.getBankAt(player.getLocation());
                if (bank == null) {
                    Components.sendErrorMessage(sender, "You must be inside a bank-claimed chunk to deposit.");
                    return true;
                }
                plugin.banksService.deposit(player, bank.getName(), amount);
            } catch (NumberFormatException e) {
                Components.sendErrorMessage(sender, "Usage: /banks deposit <bankName> <amount> OR /banks deposit <amount>");
            }
            return true;
        }
        // Existing form: /banks deposit <bankName> <amount>
        if (args.length >= 3) {
            try {
                double amount = Double.parseDouble(args[2]);
                plugin.banksService.deposit(player, args[1], amount);
            } catch (NumberFormatException e) {
                Components.sendErrorMessage(sender, "Invalid number.");
            }
            return true;
        }
        Components.sendErrorMessage(sender, "Usage: /banks deposit <bankName> <amount> OR /banks deposit <amount>");
        return true;
    }

    @Override
    public String getName() { return "deposit"; }

    @Override
    public String getPermission() { return "furious.banks.deposit"; }
}
