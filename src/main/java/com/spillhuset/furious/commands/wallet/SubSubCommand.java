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
 * Subcommand for subtracting scraps from a player's wallet.
 */
public class SubSubCommand implements SubCommand {
    private final Furious plugin;
    private final WalletManager walletManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new SubSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SubSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "sub";
    }

    @Override
    public String getDescription() {
        return "Subtract scraps from a player's wallet";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));

        if (!(sender instanceof Player) ||
            sender.hasPermission("furious.wallet.sub") ||
            sender.isOp()) {
            sender.sendMessage(Component.text("/wallet sub <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Subtract scraps from a player's wallet", NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("Shorthand: /wallet s", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet sub <player> <amount>", NamedTextColor.RED));
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

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        if (!walletManager.has(target, amount)) {
            sender.sendMessage(Component.text(target.getName() + " doesn't have enough scraps.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.withdraw(target, amount)) {
            sender.sendMessage(Component.text("Subtracted ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from " + target.getName() + "'s wallet.", NamedTextColor.GREEN)));

            target.sendMessage(Component.text("An admin subtracted ", NamedTextColor.YELLOW)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your wallet.", NamedTextColor.YELLOW)));

            auditLogger.logSensitiveOperation(sender, "wallet sub", "Subtracted " + amount + " from " + target.getName() + "'s wallet");
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to subtract scraps.", NamedTextColor.RED));
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
        return "furious.wallet.sub";
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