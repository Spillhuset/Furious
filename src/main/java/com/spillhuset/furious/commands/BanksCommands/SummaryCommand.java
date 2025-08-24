package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SummaryCommand implements SubCommandInterface {
    private final Furious plugin;
    public SummaryCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 2) {
            if (sender.hasPermission(getPermission() + ".others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(p.getName());
                }
            }
        }
        return list;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        UUID targetId;
        String targetName;
        if (args.length >= 2) {
            if (!SubCommandInterface.super.can(sender, true, true)) return true;
            OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
            if (off == null || (off.getUniqueId() == null)) {
                Components.sendErrorMessage(sender, "Player not found.");
                return true;
            }
            targetId = off.getUniqueId();
            targetName = off.getName() != null ? off.getName() : targetId.toString();
        } else {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Usage: /banks summary <player>");
                return true;
            }
            targetId = p.getUniqueId();
            targetName = p.getName();
        }
        Map<String, Double> balances = plugin.banksService.getAccountsBalances(targetId);
        int accounts = balances.size();
        double total = 0d;
        for (double v : balances.values()) total += v;
        Components.sendInfo(sender,
                Components.t("Bank summary for "), Components.valueComp(targetName),
                Components.t(": accounts="), Components.valueComp(String.valueOf(accounts)),
                Components.t(", total="), Components.valueComp(plugin.walletService.formatAmount(total))
        );
        return true;
    }

    @Override
    public String getName() { return "summary"; }

    @Override
    public String getPermission() { return "furious.banks.summary"; }
}
