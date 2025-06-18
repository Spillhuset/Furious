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
import java.util.List;

/**
 * Subcommand for checking bank balance.
 */
public class BalanceSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new BalanceSubCommand.
     *
     * @param plugin The plugin instance
     */
    public BalanceSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "View your wallet and bank balances";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank balance", NamedTextColor.YELLOW)
                .append(Component.text(" - View your wallet and bank balances", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank b", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        double walletBalance = walletManager.getBalance(player);
        double bankBalance = bankManager.getBalance(player);

        player.sendMessage(Component.text("=== Your Finances ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Wallet: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(walletBalance), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Bank (RubberBank): ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(bankBalance), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Total: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(walletBalance + bankBalance), NamedTextColor.WHITE)));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for balance command
    }

    @Override
    public String getPermission() {
        return "furious.bank.balance";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can check their balance
    }
}