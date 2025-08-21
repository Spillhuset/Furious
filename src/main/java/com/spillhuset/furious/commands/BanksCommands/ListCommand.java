package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.BankType;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;
    public ListCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) list.addAll(plugin.banksService.suggestBankNames(args[1]));
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Usage: /banks list [bankName]
        if (args.length >= 2) {
            String bankName = args[1];
            Bank bank = plugin.banksService.getBankByName(bankName);
            if (bank == null) {
                Components.sendErrorMessage(sender, "Bank not found.");
                return true;
            }
            // Header with total count (1)
            Components.sendInfo(sender, Components.t("Banks total: "), Components.valueComp("1"));
            sendBankInfo(sender, bank);
        } else {
            Collection<Bank> banks = plugin.banksService.getBanks();
            if (banks.isEmpty()) {
                Components.sendInfoMessage(sender, "No banks created.");
                return true;
            }
            // Header with total count
            Components.sendInfo(sender, Components.t("Banks total: "), Components.valueComp(String.valueOf(banks.size())));
            for (Bank bank : banks) {
                sendBankInfo(sender, bank);
            }
        }
        return true;
    }

    private void sendBankInfo(CommandSender sender, Bank bank) {
        String typeStr = bank.getType() == BankType.GUILD ? "guild" : "player";
        // Determine guild(s) the bank is placed in. Current model supports one claim owner.
        String guildsStr = "Unclaimed";
        if (bank.isClaimed()) {
            UUID owner = plugin.guildService.getClaimOwner(bank.getWorldId(), bank.getChunkX(), bank.getChunkZ());
            if (owner != null) {
                Guild g = plugin.guildService.getGuildById(owner);
                if (g != null && g.getName() != null) {
                    guildsStr = g.getName();
                } else {
                    guildsStr = owner.toString();
                }
            } else {
                guildsStr = "Not in guild territory";
            }
        }
        Components.sendInfo(sender,
                Components.t("Bank "), Components.valueComp(bank.getName()),
                Components.t(" | type: "), Components.t(typeStr, NamedTextColor.AQUA),
                Components.t(" | guild(s): "), Components.t(guildsStr, NamedTextColor.GOLD)
        );
    }

    @Override
    public String getName() { return "list"; }

    @Override
    public String getPermission() { return "furious.banks.list"; }
}
