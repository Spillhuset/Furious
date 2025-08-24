package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.BankType;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
        // Determine guilds the bank is placed in across all claims
        String guildsStr = "Unclaimed";
        if (bank.isClaimed()) {
            Set<String> parts = new LinkedHashSet<>();
            Set<UUID> seen = new LinkedHashSet<>();
            for (Bank.Claim c : bank.getClaims()) {
                UUID owner = plugin.guildService.getClaimOwner(c.worldId, c.chunkX, c.chunkZ);
                if (owner == null) {
                    parts.add("Not in guild territory");
                    continue;
                }
                if (seen.contains(owner)) continue;
                seen.add(owner);
                Guild g = plugin.guildService.getGuildById(owner);
                if (g != null && g.getName() != null) {
                    GuildType gt = g.getType();
                    String typeInfo = gt != null ? " (" + gt.name().toLowerCase() + ")" : "";
                    parts.add(g.getName() + typeInfo);
                } else {
                    parts.add(owner.toString());
                }
            }
            if (!parts.isEmpty()) {
                guildsStr = String.join(", ", parts);
            }
        }
        String openStr = bank.isOpen() ? "open" : "closed";
        String interestStr = plugin.walletService.formatAmount(bank.getInterest()) + "%";
        Components.sendInfo(sender,
                Components.t("Bank "), Components.valueComp(bank.getName()),
                Components.t(" | type: "), Components.t(typeStr, NamedTextColor.AQUA),
                Components.t(" | guilds: "), Components.t(guildsStr, NamedTextColor.GOLD),
                Components.t(" | interest: "), Components.t(interestStr, NamedTextColor.GREEN),
                Components.t(" | status: "), Components.t(openStr, bank.isOpen() ? NamedTextColor.GREEN : NamedTextColor.RED)
        );
    }

    @Override
    public String getName() { return "list"; }

    @Override
    public String getPermission() { return "furious.banks.list"; }
}
