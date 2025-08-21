package com.spillhuset.furious.commands.HomesCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BuyCommand implements SubCommandInterface {
    private final Furious plugin;
    public BuyCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /homes buy [confirm]
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            if ("confirm".startsWith(args[1].toLowerCase())) {
                list.add("confirm");
            }
            return list;
        }
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use /homes buy.");
            return true;
        }

        if (!can(sender, true)) {
            return true;
        }

        if (args.length == 1) {
            double cost = plugin.homesService.getNextBuyCost(player.getUniqueId());
            Components.sendSuccess(player,
                    Components.t("Next home slot costs "),
                    Components.amountComp(cost, plugin.walletService),
                    Components.t(". Use: "),
                    Components.valueComp("/homes buy confirm"),
                    Components.t(" to purchase."));
        } else if (args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
            double cost = plugin.homesService.getNextBuyCost(player.getUniqueId());
            if (!plugin.walletService.subBalance(player.getUniqueId(), cost, "Purchased home slot by command")) {
                Components.sendErrorMessage(player, "Not enough money to purchase a home slot.");
                return true;
            }
            plugin.homesService.purchaseSlot(player.getUniqueId());
            plugin.homesService.save();
            Components.sendSuccess(player, Components.t("Purchased an extra home slot for "), Components.amountComp(cost, plugin.walletService), Components.t("."));
        } else {
            Components.sendInfoMessage(player, "Usage: /homes buy [confirm]");
        }
        return true;
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getPermission() {
        return "furious.homes.buy";
    }
}
