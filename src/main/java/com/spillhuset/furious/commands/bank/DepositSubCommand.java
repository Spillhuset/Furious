package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Subcommand for depositing money to bank.
 */
public class DepositSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new DepositSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DepositSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "deposit";
    }

    @Override
    public String getDescription() {
        return "Deposit from wallet to bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank deposit <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Deposit the specified amount from your wallet to your bank account", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank d <amount>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            // Check if player has enough in wallet
            if (!walletManager.has(player, amount)) {
                player.sendMessage(Component.text("You don't have enough in your wallet.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from wallet
            if (!walletManager.withdraw(player, amount)) {
                player.sendMessage(Component.text("Failed to withdraw from wallet.", NamedTextColor.RED));
                return true;
            }

            // Deposit to bank
            if (!bankManager.deposit(player, amount)) {
                // Rollback wallet withdrawal if bank deposit fails
                walletManager.deposit(player, amount);
                player.sendMessage(Component.text("Failed to deposit to bank.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully deposited ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to your bank account.", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest some common amounts
            String partialAmount = args[1].toLowerCase();
            List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000");
            List<String> completions = new ArrayList<>();

            for (String amount : amounts) {
                if (amount.startsWith(partialAmount)) {
                    completions.add(amount);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.bank.deposit";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can deposit to their bank account
    }
}