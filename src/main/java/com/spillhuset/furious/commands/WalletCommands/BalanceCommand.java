package com.spillhuset.furious.commands.WalletCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BalanceCommand implements SubCommandInterface {
    private final Furious instance;

    public BalanceCommand(Furious plugin) {
        this.instance = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "balance";
    }


    @Override
    public String getPermission() {
        return "furious.wallet.balance";
    }


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 1 && can(sender, true, false)) {
            // Check if player
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can check their own balance.");
                Components.sendInfo(sender, Components.t("Usage: /wallet balance <player>"));
                return true;
            }

            // Find balance
            double balance = instance.walletService.getBalance(player.getUniqueId());

            // Feedback
            Components.sendInfo(sender, Components.t("Your balance is: "), Components.amountComp(balance, instance.walletService));
            return true;
        }
        // Check permissions
        else if (args.length == 2 && can(sender, true, true)) {
            // Find player
            OfflinePlayer target = (sender instanceof Player self) ? Utility.findPlayer(args[1],self.getUniqueId()) : Utility.findPlayer(args[1]);
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }

            // Find balance
            double balance = instance.walletService.getBalance(target.getUniqueId());

            // Feedback
            Components.sendInfo(sender, Components.playerComp(Objects.requireNonNullElse(target.getName(), args[1])), Components.t(" has: "), Components.amountComp(balance, instance.walletService));
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /wallet balance"+(can(sender,false,true)?" [player]":""));
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 2) {
            UUID uuid = null;
            if (sender instanceof Player self) uuid = self.getUniqueId();
            return new ArrayList<>(suggestPlayers(instance.walletService.getAccountNames(), args, 1,uuid));
        }
        return suggestions;
    }
}
