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

public class SetCommand implements SubCommandInterface {
    private final Furious instance;

    public SetCommand(Furious plugin) {
        this.instance = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "set";
    }


    @Override
    public String getPermission() {
        return "furious.wallet.set";
    }


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 3 && can(sender, true, false)) {
            // Find player
            OfflinePlayer target = (sender instanceof Player self) ? Utility.findPlayer(args[1],self.getUniqueId()) : Utility.findPlayer(args[1]);
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
            instance.walletService.setBalance(target.getUniqueId(), amount, "admin set by " + sender.getName());

            // Save accounts and transactions
            //instance.walletService.accountsConfig.set(target.getUniqueId().toString(), instance.walletService.getBalance(target.getUniqueId()));
            //instance.walletService.saveAccounts();
            //instance.walletService.saveTransactions();

            double newBalTarget = instance.walletService.getBalance(target.getUniqueId());

            // Feedback
            Components.sendSuccess(sender, Components.t("Updated "), Components.playerComp(Objects.requireNonNullElse(target.getName(), args[1])), Components.t(" to "), Components.amountComp(newBalTarget, instance.walletService));
            if (target.isOnline()) {
                Player tp = target.getPlayer();
                if (tp != null) {
                    Components.sendSuccess(tp, Components.t("Your balance was set to "), Components.amountComp(amount, instance.walletService), Components.t(" by "), Components.playerComp(sender.getName()));
                }
            }
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /wallet set <player> <value>");
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
