package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WithdrawCommand implements SubCommandInterface {
    private final Furious plugin;
    public WithdrawCommand(Furious plugin) { this.plugin = plugin; }

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
        // Player form: /banks withdraw <amount> -> infer bank from current claimed chunk
        if (args.length == 2) {
            try {
                double amount = Double.parseDouble(args[1]);
                var bank = plugin.banksService.getBankAt(player.getLocation());
                if (bank == null) {
                    Components.sendErrorMessage(sender, "You must be inside a bank-claimed chunk to withdraw.");
                    return true;
                }
                plugin.banksService.withdraw(player, bank.getName(), amount);
            } catch (NumberFormatException e) {
                Components.sendErrorMessage(sender, "Usage: /banks withdraw <bankName> <amount> OR /banks withdraw <amount>");
            }
            return true;
        }
        // Existing form: /banks withdraw <bankName> <amount>
        if (args.length >= 3) {
            try {
                double amount = Double.parseDouble(args[2]);
                plugin.banksService.withdraw(player, args[1], amount);
            } catch (NumberFormatException e) {
                Components.sendErrorMessage(sender, "Invalid number.");
            }
            return true;
        }
        Components.sendErrorMessage(sender, "Usage: /banks withdraw <bankName> <amount> OR /banks withdraw <amount>");
        return true;
    }

    @Override
    public String getName() { return "withdraw"; }

    @Override
    public String getPermission() { return "furious.banks.withdraw"; }
}
