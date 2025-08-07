package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for showing information about the bank at the player's location.
 */
public class InfoSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new InfoSubCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Show information about the bank at your location";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank info [bankName]", NamedTextColor.YELLOW)
                .append(Component.text(" - Show information about a bank", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("If bankName is not provided, shows information about the bank at your current location.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Shorthand: /bank i [bankName]", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Handle console commands
        if (!(sender instanceof Player)) {
            // Console needs to specify a bank name
            if (args.length < 1) {
                sender.sendMessage(Component.text("When executing from console, you must specify a bank name.", NamedTextColor.RED));
                return true;
            }

            String bankName = args[0];
            Bank bank = bankManager.getBank(bankName);

            if (bank == null) {
                sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                return true;
            }

            // Show bank information
            sender.sendMessage(Component.text("=== Bank Information ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("Name: ", NamedTextColor.YELLOW)
                    .append(Component.text(bank.getName(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Claimed Chunks: ", NamedTextColor.YELLOW)
                    .append(Component.text(bank.getClaimedChunkCount(), NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Interest Rate: ", NamedTextColor.YELLOW)
                    .append(Component.text(String.format("%.2f%%", bank.getInterestRate() * 100), NamedTextColor.WHITE)));

            return true;
        }

        // Handle player commands
        Player player = (Player) sender;
        Bank bank = null;

        // If a bank name is provided, use that bank
        if (args.length > 0) {
            String bankName = args[0];
            bank = bankManager.getBank(bankName);

            if (bank == null) {
                player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                return true;
            }
        } else {
            // Otherwise, get the bank from the player's current chunk
            Chunk chunk = player.getLocation().getChunk();
            bank = bankManager.getBankByChunk(chunk);

            if (bank == null) {
                player.sendMessage(Component.text("You are not in a bank's territory. Please specify a bank name.", NamedTextColor.RED));
                return true;
            }
        }

        // Show bank information
        player.sendMessage(Component.text("=== Bank Information ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Name: ", NamedTextColor.YELLOW)
                .append(Component.text(bank.getName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Claimed Chunks: ", NamedTextColor.YELLOW)
                .append(Component.text(bank.getClaimedChunkCount(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Interest Rate: ", NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.2f%%", bank.getInterestRate() * 100), NamedTextColor.WHITE)));

        // Show player's account in this bank if they have one
        if (bank.hasAccount(player.getUniqueId())) {
            double balance = bank.getBalance(player.getUniqueId());
            player.sendMessage(Component.text("Your Balance: ", NamedTextColor.YELLOW)
                    .append(Component.text(walletManager.formatAmount(balance), NamedTextColor.WHITE)));
        } else {
            player.sendMessage(Component.text("You don't have an account in this bank.", NamedTextColor.YELLOW));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest bank names for both console and players
            String partialBankName = args[0].toLowerCase();
            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.info";
    }

    @Override
    public boolean denyNonPlayer() {
        return false; // Allow console execution with bank name parameter
    }
}