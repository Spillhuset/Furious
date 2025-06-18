package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for editing a player's balance in a bank.
 */
public class EditBalanceSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new EditBalanceSubCommand.
     *
     * @param plugin The plugin instance
     */
    public EditBalanceSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "editbalance";
    }

    @Override
    public String getDescription() {
        return "Edit a player's balance in a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank editbalance <bankName> <playerName> <newBalance>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set the balance of the specified player's account in the specified bank", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];
        String playerName = args[2];
        double newBalance;

        // Parse new balance
        try {
            newBalance = Double.parseDouble(args[3]);
            if (newBalance < 0) {
                sender.sendMessage(Component.text("Balance must be non-negative.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid balance: " + args[3], NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            sender.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Get the current balance
        double oldBalance = bank.getBalance(playerId);

        // Set the new balance
        if (bankManager.setBalance(bankName, playerId, newBalance)) {
            sender.sendMessage(Component.text("Balance for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" updated from ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(oldBalance), NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(newBalance), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to update balance.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest bank names for the first argument
            String partialBankName = args[1].toLowerCase();

            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        } else if (args.length == 3) {
            // Suggest player names for the second argument
            String partialPlayerName = args[2].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialPlayerName)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 4) {
            // Suggest some common balances
            String partialBalance = args[3].toLowerCase();
            List<String> balances = List.of("0", "100", "500", "1000", "5000", "10000");

            for (String balance : balances) {
                if (balance.startsWith(partialBalance)) {
                    completions.add(balance);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.editbalance";
    }
}