package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class BalancesCommand implements SubCommandInterface {
    private final Furious plugin;
    public BalancesCommand(Furious plugin) { this.plugin = plugin; }

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
        UUID targetId = null;
        String targetName = null;
        if (args.length >= 2) {
            // others check
            if (!SubCommandInterface.super.can(sender, true, true)) return true;
            OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]);
            if (off == null || (off.getUniqueId() == null)) {
                Components.sendErrorMessage(sender, "Player not found.");
                return true;
            }
            targetId = off.getUniqueId();
            targetName = off.getName() != null ? off.getName() : targetId.toString();
        } else {
            if (sender instanceof Player p) {
                targetId = p.getUniqueId();
                targetName = p.getName();
            } else {
                Components.sendErrorMessage(sender, "Usage: /banks balances <player>");
                return true;
            }
        }
        Map<String, Double> map = plugin.banksService.getAccountsBalances(targetId);
        if (map.isEmpty()) {
            Components.sendInfo(sender, Components.t("Bank balances for "), Components.valueComp(targetName), Components.t(": none"));
            return true;
        }
        Components.sendInfo(sender, Components.t("Bank balances for "), Components.valueComp(targetName), Components.t(":"));
        double total = 0d;
        for (Map.Entry<String, Double> e : map.entrySet()) {
            total += e.getValue();
            Components.sendInfo(sender,
                    Components.t(" - "), Components.t(e.getKey(), NamedTextColor.AQUA),
                    Components.t(": "), Components.valueComp(plugin.walletService.formatAmount(e.getValue()))
            );
        }
        Components.sendInfo(sender,
                Components.t("Total: "), Components.valueComp(plugin.walletService.formatAmount(total))
        );
        return true;
    }

    @Override
    public String getName() { return "balances"; }

    @Override
    public String getPermission() { return "furious.banks.balances"; }
}
