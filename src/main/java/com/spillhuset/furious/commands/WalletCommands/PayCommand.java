package com.spillhuset.furious.commands.WalletCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PayCommand implements SubCommandInterface {
    private final Furious instance;

    public PayCommand(Furious plugin) {
        this.instance = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public String getPermission() {
        return "furious.wallet.pay";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 3 && can(sender, true, false)) {
            // Check player
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use /wallet pay.");
                return true;
            }

            // Find player
            OfflinePlayer target = Utility.findPlayer(args[1],player.getUniqueId());
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }

            // Parse amount
            Double amount = instance.walletService.parseAmount(args[2]);
            if (amount == null) {
                Components.sendErrorMessage(sender, "Invalid amount.");
                return true;
            }

            // Update balance
            boolean ok = instance.walletService.pay(player.getUniqueId(), target.getUniqueId(), amount);
            if (!ok) {
                Components.sendErrorMessage(sender, "Insufficient funds or invalid amount.");
                return true;
            }

            double newBal = instance.walletService.getBalance(player.getUniqueId());

            // Feedback
            Components.sendSuccess(sender, Components.t("Paid "), Components.playerComp(Objects.requireNonNullElse(target.getName(), args[1])), Components.t(" "), Components.amountComp(amount, instance.walletService), Components.t(". New balance: "), Components.amountComp(newBal, instance.walletService));
            if (target.isOnline()) {
                Player tp = target.getPlayer();
                if (tp != null) {
                    Components.sendSuccess(tp, Components.t("You received "), Components.amountComp(amount, instance.walletService), Components.t(" from "), Components.playerComp(player.getName()));
                }
            }

            // Save accounts and transactions
            //instance.walletService.accountsConfig.set(player.getUniqueId().toString(), instance.walletService.getBalance(player.getUniqueId()));
            //instance.walletService.accountsConfig.set(target.getUniqueId().toString(), instance.walletService.getBalance(target.getUniqueId()));
            //instance.walletService.saveAccounts();
            //instance.walletService.saveTransactions();
            instance.walletService.flushNow();
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /wallet pay <player> <value>");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 3) {
            return List.of("<value>");
        } else if (args.length == 2) {
            UUID uuid = null;
            if (sender instanceof Player self) uuid = self.getUniqueId();
            return new ArrayList<>(suggestPlayers(instance.walletService.getAccountNames(), args, 1,uuid));
        } else {
            return List.of();
        }
    }
}
