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
 * Subcommand for withdrawing money from bank.
 */
public class WithdrawSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new WithdrawSubCommand.
     *
     * @param plugin The plugin instance
     */
    public WithdrawSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "withdraw";
    }

    @Override
    public String getDescription() {
        return "Withdraw from bank to wallet";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank withdraw <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Withdraw the specified amount from your bank account to your wallet", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank w <amount>", NamedTextColor.GRAY));
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

            // Check if player has enough in bank
            if (!bankManager.getBank("RubberBank").hasAmount(player.getUniqueId(), amount)) {
                player.sendMessage(Component.text("You don't have enough in your bank account.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from bank
            if (!bankManager.withdraw(player, amount)) {
                player.sendMessage(Component.text("Failed to withdraw from bank.", NamedTextColor.RED));
                return true;
            }

            // Deposit to wallet
            if (!walletManager.deposit(player, amount)) {
                // Rollback bank withdrawal if wallet deposit fails
                bankManager.deposit(player, amount);
                player.sendMessage(Component.text("Failed to deposit to wallet.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully withdrew ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your bank account.", NamedTextColor.GREEN)));

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
        return "furious.bank.withdraw";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can withdraw from their bank account
    }
}