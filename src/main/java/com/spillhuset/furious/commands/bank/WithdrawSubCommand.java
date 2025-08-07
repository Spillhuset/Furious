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
 * Subcommand for withdrawing money from bank.
 */
public class WithdrawSubCommand extends BaseBankCommand {
    private final WalletManager walletManager;

    /**
     * Creates a new WithdrawSubCommand.
     *
     * @param plugin The plugin instance
     */
    public WithdrawSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
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

        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/bank withdraw <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Withdraw the specified amount from your bank account to your wallet", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/bank withdraw <bankName> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Withdraw from a specific bank", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Shorthand: /bank w <amount>", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("/bank withdraw <bankName> <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Withdraw the specified amount from a player's bank account to their wallet", NamedTextColor.WHITE)));
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

            // Check if player has enough in bank
            if (!bank.hasAmount(player.getUniqueId(), amount)) {
                player.sendMessage(Component.text("You don't have enough in your bank account.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from bank
            if (!bankManager.withdraw(player, bank.getName(), amount)) {
                player.sendMessage(Component.text("Failed to withdraw from bank.", NamedTextColor.RED));
                return true;
            }

            // Deposit to wallet
            if (!walletManager.deposit(player, amount)) {
                // Rollback bank withdrawal if wallet deposit fails
                bankManager.deposit(player, bank.getName(), amount);
                player.sendMessage(Component.text("Failed to deposit to wallet.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully withdrew ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your account at ", NamedTextColor.GREEN))
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
        return "furious.bank.withdraw";
    }

    @Override
    public boolean denyNonPlayer() {
        return false; // Allow console execution with bank name and player parameters
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /bank withdraw <bankName> <player> <amount>", NamedTextColor.RED));
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

            // Check if player has enough in bank
            if (!bank.hasAmount(targetPlayer.getUniqueId(), amount)) {
                sender.sendMessage(Component.text("Player doesn't have enough in their bank account.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from bank
            if (!bankManager.withdraw(targetPlayer, bank.getName(), amount)) {
                sender.sendMessage(Component.text("Failed to withdraw from bank.", NamedTextColor.RED));
                return true;
            }

            // Deposit to wallet
            if (!walletManager.deposit(targetPlayer, amount)) {
                // Rollback bank withdrawal if wallet deposit fails
                bankManager.deposit(targetPlayer, bank.getName(), amount);
                sender.sendMessage(Component.text("Failed to deposit to wallet.", NamedTextColor.RED));
                return true;
            }

            sender.sendMessage(Component.text("Successfully withdrew ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from " + targetPlayer.getName() + "'s account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

            targetPlayer.sendMessage(Component.text("An admin has withdrawn ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" and added it to your wallet.", NamedTextColor.GREEN)));

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

            // Check if player has enough in bank
            if (!bank.hasAmount(player.getUniqueId(), amount)) {
                player.sendMessage(Component.text("You don't have enough in your bank account.", NamedTextColor.RED));
                return true;
            }

            // Withdraw from bank
            if (!bankManager.withdraw(player, bank.getName(), amount)) {
                player.sendMessage(Component.text("Failed to withdraw from bank.", NamedTextColor.RED));
                return true;
            }

            // Deposit to wallet
            if (!walletManager.deposit(player, amount)) {
                // Rollback bank withdrawal if wallet deposit fails
                bankManager.deposit(player, bank.getName(), amount);
                player.sendMessage(Component.text("Failed to deposit to wallet.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully withdrew ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your account at ", NamedTextColor.GREEN))
                    .append(Component.text(bank.getName(), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }
}