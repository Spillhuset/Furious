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

public class LogCommand implements SubCommandInterface {
    private final Furious instance;

    public LogCommand(Furious plugin) {
        this.instance = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "log";
    }


    @Override
    public String getPermission() {
        return "furious.wallet.log";
    }


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 1 && can(sender, true)) {
            // Get global log
            List<String> lines = instance.walletService.getGlobalLog();
            if (lines.isEmpty()) {
                Components.sendInfoMessage(sender, "No global transactions recorded.");
                return true;
            }

            // Feedback
            Components.sendInfo(sender, Components.t("Global transactions (latest "), Components.valueComp(String.valueOf(lines.size())), Components.t("): "));
            for (String l : lines) Components.sendGreyMessage(sender, l);
            return true;
        } else if (args.length == 2 && can(sender, true, true)) {
            // Find player
            OfflinePlayer target = (sender instanceof Player self) ? Utility.findPlayer(args[1], self.getUniqueId()) : Utility.findPlayer(args[1]);
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }

            // Get log
            List<String> lines = instance.walletService.getLog(target.getUniqueId());
            if (lines.isEmpty()) {
                Components.sendInfo(sender, Components.t("No transactions for "), Components.playerComp(Objects.requireNonNullElse(target.getName(), args[1])));
                return true;
            }

            // Feedback
            Components.sendInfo(sender, Components.t("Transactions for "), Components.playerComp(Objects.requireNonNullElse(target.getName(), args[1])), Components.t(":"));
            for (String l : lines) Components.sendGreyMessage(sender, l);
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /wallet log [player]");
        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length == 2) {
            UUID uuid = null;
            if (sender instanceof Player self) uuid = self.getUniqueId();
            return new ArrayList<>(suggestPlayers(instance.walletService.getAccountNames(), args, 1, uuid));
        } else {
            return List.of();
        }
    }
}
