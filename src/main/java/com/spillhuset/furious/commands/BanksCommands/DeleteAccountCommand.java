package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DeleteAccountCommand implements SubCommandInterface {
    private final Furious plugin;
    public DeleteAccountCommand(Furious plugin) { this.plugin = plugin; }

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
        // Player form: /banks deleteaccount -> infer bank from current claimed chunk
        if (args.length == 1) {
            var bank = plugin.banksService.getBankAt(player.getLocation());
            if (bank == null) {
                Components.sendErrorMessage(sender, "You must be inside a bank-claimed chunk to delete your account.");
                return true;
            }
            plugin.banksService.deleteAccount(player, bank.getName());
            return true;
        }
        // Existing form: /banks deleteaccount <bankName>
        plugin.banksService.deleteAccount(player, args[1]);
        return true;
    }

    @Override
    public String getName() { return "deleteaccount"; }

    @Override
    public String getPermission() { return "furious.banks.deleteaccount"; }
}
