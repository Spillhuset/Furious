package com.spillhuset.furious.commands.BanksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin: Audit bank totals per bank and grand total across all accounts.
 */
public class AuditCommand implements SubCommandInterface {
    private final Furious plugin;
    public AuditCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Map<String, Double> totals = plugin.banksService.getTotalsPerBank();
        if (totals.isEmpty()) {
            Components.sendInfoMessage(sender, "No banks or accounts found.");
            return true;
        }
        Components.sendInfoMessage(sender, "Bank audit report:");
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            Components.sendInfo(sender,
                    Components.t(" - "), Components.t(e.getKey(), NamedTextColor.AQUA),
                    Components.t(": "), Components.valueComp(plugin.walletService.formatAmount(e.getValue()))
            );
        }
        double grand = plugin.banksService.getGrandTotal();
        Components.sendInfo(sender,
                Components.t("Grand total: "), Components.valueComp(plugin.walletService.formatAmount(grand))
        );
        return true;
    }

    @Override
    public String getName() { return "audit"; }

    @Override
    public String getPermission() { return "furious.banks.audit"; }
}
