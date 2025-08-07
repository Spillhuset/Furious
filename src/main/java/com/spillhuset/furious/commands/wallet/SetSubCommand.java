package com.spillhuset.furious.commands.wallet;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for setting a player's wallet balance.
 */
public class SetSubCommand implements SubCommand {
    private final Furious plugin;
    private final WalletManager walletManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new SetSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SetSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Set a player's wallet balance";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));

        if (!(sender instanceof Player) ||
            sender.hasPermission("furious.wallet.set") ||
            sender.isOp()) {
            sender.sendMessage(Component.text("/wallet set <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Set a player's wallet balance", NamedTextColor.WHITE)));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(Component.text("Amount cannot be negative.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.setBalance(target, amount)) {
            sender.sendMessage(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(target.getName() + "'s", NamedTextColor.YELLOW))
                    .append(Component.text(" balance to ", NamedTextColor.GREEN))
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD)));

            target.sendMessage(Component.text("An admin set your wallet balance to ", NamedTextColor.YELLOW)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD)));

            auditLogger.logSensitiveOperation(sender, "wallet set", "Set " + target.getName() + "'s balance to " + amount);
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to set wallet balance.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.wallet.set";
    }

    /**
     * Checks if the sender has permission to use this command.
     *
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    @Override
    public boolean checkPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) ||
               sender.isOp();
    }
}