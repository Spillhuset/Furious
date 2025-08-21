package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CreateAccountCommand implements SubCommandInterface {
    private final Furious plugin;
    public CreateAccountCommand(Furious plugin) { this.plugin = plugin; }

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
        // Player form: /banks createaccount -> infer bank from current claimed chunk
        if (args.length == 1) {
            var bank = plugin.banksService.getBankAt(player.getLocation());
            if (bank == null) {
                Components.sendErrorMessage(sender, "You must be inside a bank-claimed chunk to create an account.");
                return true;
            }
            plugin.banksService.createAccount(player, bank.getName());
            return true;
        }
        // OP form: /banks createaccount <bankName>
        plugin.banksService.createAccount(player, args[1]);
        return true;
    }

    @Override
    public String getName() { return "createaccount"; }

    @Override
    public String getPermission() { return "furious.banks.createaccount"; }
}
