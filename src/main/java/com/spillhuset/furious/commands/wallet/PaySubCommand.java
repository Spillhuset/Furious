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
 * Subcommand for paying scraps to another player.
 */
public class PaySubCommand implements SubCommand {
    private final Furious plugin;
    private final WalletManager walletManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new PaySubCommand.
     *
     * @param plugin The plugin instance
     */
    public PaySubCommand(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public String getDescription() {
        return "Pay scraps to another player";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));

        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/wallet pay <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Pay scraps to another player", NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("Shorthand: /wallet p", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player from)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet pay <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player to = Bukkit.getPlayer(args[1]);

        if (to == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        if (to.equals(from)) {
            sender.sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
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

        if (!walletManager.has(from, amount)) {
            sender.sendMessage(Component.text("You don't have enough scraps.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.transfer(from, to, amount)) {
            sender.sendMessage(Component.text("You paid ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to " + to.getName() + ".", NamedTextColor.GREEN)));

            to.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from " + from.getName() + ".", NamedTextColor.GREEN)));

            auditLogger.logSensitiveOperation(from, "wallet pay", "Paid " + amount + " to " + to.getName());
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to transfer scraps.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial) && !player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.wallet.pay";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can pay other players
    }
}