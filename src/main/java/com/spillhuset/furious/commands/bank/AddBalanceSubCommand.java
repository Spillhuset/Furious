package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for adding to a player's balance in a bank.
 */
public class AddBalanceSubCommand extends BaseBankCommand {

    /**
     * Creates a new AddBalanceSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AddBalanceSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getDescription() {
        return "Add to a player's balance in a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank add <playerName> <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add the specified amount to the player's account in this bank", NamedTextColor.WHITE)));
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(player);
            return true;
        }

        String playerName = args[0];
        double amount;

        // Check if player is editing another player's balance
        if (!playerName.equalsIgnoreCase(player.getName()) && !player.hasPermission("furious.bank.editbalance.others")) {
            player.sendMessage(Component.text("You don't have permission to edit other players' balances.", NamedTextColor.RED));
            return true;
        }

        // Parse amount
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount: " + args[1], NamedTextColor.RED));
            return true;
        }

        // Get the bank from the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();
        Bank bank = bankManager.getBankByChunk(chunk);

        if (bank == null) {
            player.sendMessage(Component.text("No bank found in this chunk.", NamedTextColor.RED));
            return true;
        }

        String bankName = bank.getName();

        // Get the target player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            player.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            player.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Get the current balance
        double oldBalance = bank.getBalance(playerId);
        double newBalance = oldBalance + amount;

        // Set the new balance
        if (bankManager.setBalance(bankName, playerId, newBalance)) {
            player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(plugin.getWalletManager().formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to player ", NamedTextColor.GREEN))
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text("'s account in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(". New balance: ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(newBalance), NamedTextColor.GOLD)));
        } else {
            player.sendMessage(Component.text("Failed to update balance.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String playerName = args[0];
        double amount;

        // Parse amount
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[1], NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the target player
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
        double newBalance = oldBalance + amount;

        // Set the new balance
        if (bankManager.setBalance(bankName, playerId, newBalance)) {
            sender.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(plugin.getWalletManager().formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to player ", NamedTextColor.GREEN))
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text("'s account in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(". New balance: ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(newBalance), NamedTextColor.GOLD)));
        } else {
            sender.sendMessage(Component.text("Failed to update balance.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(player);
            return true;
        }

        String playerName = args[0];
        double amount;

        // Check if player is editing another player's balance
        if (!playerName.equalsIgnoreCase(player.getName()) && !player.hasPermission("furious.bank.editbalance.others")) {
            player.sendMessage(Component.text("You don't have permission to edit other players' balances.", NamedTextColor.RED));
            return true;
        }

        // Parse amount
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount: " + args[1], NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the target player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            player.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            player.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Get the current balance
        double oldBalance = bank.getBalance(playerId);
        double newBalance = oldBalance + amount;

        // Set the new balance
        if (bankManager.setBalance(bankName, playerId, newBalance)) {
            player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(plugin.getWalletManager().formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to player ", NamedTextColor.GREEN))
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text("'s account in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(". New balance: ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(newBalance), NamedTextColor.GOLD)));
        } else {
            player.sendMessage(Component.text("Failed to update balance.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && !(sender instanceof Player)) {
            // Suggest bank names for console
            String partialBankName = args[0].toLowerCase();
            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        } else if ((args.length == 1 && sender instanceof Player) ||
                  (args.length == 2 && !(sender instanceof Player))) {
            // Suggest player names
            String partialPlayerName = args[args.length - 1].toLowerCase();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(partialPlayerName)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        } else if ((args.length == 2 && sender instanceof Player) ||
                  (args.length == 3 && !(sender instanceof Player))) {
            // Suggest some common amounts
            String partialAmount = args[args.length - 1].toLowerCase();
            List<String> amounts = List.of("10", "50", "100", "500", "1000");

            for (String amount : amounts) {
                if (amount.startsWith(partialAmount)) {
                    completions.add(amount);
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