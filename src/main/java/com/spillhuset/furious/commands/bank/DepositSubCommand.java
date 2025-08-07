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
import java.util.Arrays;
import java.util.List;

/**
 * Subcommand for depositing money to bank.
 */
public class DepositSubCommand extends BaseBankCommand {
    private final WalletManager walletManager;

    /**
     * Creates a new DepositSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DepositSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
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

        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/bank deposit <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Deposit the specified amount from your wallet to your bank account", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/bank deposit <bankName> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Deposit to a specific bank", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Shorthand: /bank d <amount>", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("/bank deposit <bankName> <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Deposit the specified amount from a player's wallet to their bank account", NamedTextColor.WHITE)));
        }
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            // Get the bank from the chunk the player is standing in
            Chunk chunk = player.getLocation().getChunk();
            Bank bank = bankManager.getBankByChunk(chunk);

            if (bank == null) {
                player.sendMessage(Component.text("No bank found in this chunk.", NamedTextColor.RED));
                return true;
            }

            // Check if player has an account in this bank
            if (!bank.hasAccount(player.getUniqueId())) {
                player.sendMessage(Component.text("You don't have an account in this bank.", NamedTextColor.RED));
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
            if (!bankManager.deposit(player, bank.getName(), amount)) {
                // Rollback wallet withdrawal if bank deposit fails
                walletManager.deposit(player, amount);
                player.sendMessage(Component.text("Failed to deposit to bank.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully deposited ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to your account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Different tab completion logic for console vs player
        if (!(sender instanceof Player)) {
            // Console tab completion
            if (args.length == 1) {
                // Suggest bank names
                String partialName = args[0].toLowerCase();
                for (String bankName : bankManager.getBanks().keySet()) {
                    if (bankName.toLowerCase().startsWith(partialName)) {
                        completions.add(bankName);
                    }
                }
            } else if (args.length == 2) {
                // Suggest player names
                String partialName = args[1].toLowerCase();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                // Suggest common amounts
                String partialAmount = args[2].toLowerCase();
                List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000");

                for (String amount : amounts) {
                    if (amount.startsWith(partialAmount)) {
                        completions.add(amount);
                    }
                }
            }
        } else {
            // Player tab completion
            if (args.length == 1) {
                // First check if it could be a bank name
                String partialName = args[0].toLowerCase();
                boolean foundBankMatch = false;

                for (String bankName : bankManager.getBanks().keySet()) {
                    if (bankName.toLowerCase().startsWith(partialName)) {
                        completions.add(bankName);
                        foundBankMatch = true;
                    }
                }

                // If no bank matches or partial is numeric, suggest amounts
                if (!foundBankMatch || partialName.matches("\\d*")) {
                    List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000");

                    for (String amount : amounts) {
                        if (amount.startsWith(partialName)) {
                            completions.add(amount);
                        }
                    }
                }
            } else if (args.length == 2) {
                // If first arg is a bank name, suggest amounts
                if (bankManager.getBank(args[0]) != null) {
                    String partialAmount = args[1].toLowerCase();
                    List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000");

                    for (String amount : amounts) {
                        if (amount.startsWith(partialAmount)) {
                            completions.add(amount);
                        }
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.deposit";
    }

    @Override
    public boolean denyNonPlayer() {
        return false; // Allow console execution with bank name and player parameters
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /bank deposit <bankName> <player> <amount>", NamedTextColor.RED));
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = plugin.getServer().getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            // Get the bank
            Bank bank = bankManager.getBank(bankName);
            if (bank == null) {
                sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                return true;
            }

            // Check if player has an account in this bank
            if (!bank.hasAccount(targetPlayer.getUniqueId())) {
                sender.sendMessage(Component.text("Player doesn't have an account in this bank.", NamedTextColor.RED));
                return true;
            }

            // Check if player has enough in wallet
            if (!walletManager.has(targetPlayer, amount)) {
                sender.sendMessage(Component.text("Player doesn't have enough in their wallet.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from wallet
            if (!walletManager.withdraw(targetPlayer, amount)) {
                sender.sendMessage(Component.text("Failed to withdraw from player's wallet.", NamedTextColor.RED));
                return true;
            }

            // Deposit to bank
            if (!bankManager.deposit(targetPlayer, bank.getName(), amount)) {
                // Rollback wallet withdrawal if bank deposit fails
                walletManager.deposit(targetPlayer, amount);
                sender.sendMessage(Component.text("Failed to deposit to bank.", NamedTextColor.RED));
                return true;
            }

            sender.sendMessage(Component.text("Successfully deposited ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to " + targetPlayer.getName() + "'s account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

            targetPlayer.sendMessage(Component.text("An admin has deposited ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your wallet to your account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            // Get the bank
            Bank bank = bankManager.getBank(bankName);
            if (bank == null) {
                player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                return true;
            }

            // Check if player has an account in this bank
            if (!bank.hasAccount(player.getUniqueId())) {
                player.sendMessage(Component.text("You don't have an account in this bank.", NamedTextColor.RED));
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
            if (!bankManager.deposit(player, bank.getName(), amount)) {
                // Rollback wallet withdrawal if bank deposit fails
                walletManager.deposit(player, amount);
                player.sendMessage(Component.text("Failed to deposit to bank.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully deposited ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to your account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }
}