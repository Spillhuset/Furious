package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for creating a player's account in a bank.
 */
public class CreateAccountSubCommand extends BaseBankCommand {

    /**
     * Creates a new CreateAccountSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateAccountSubCommand(Furious plugin) {
        super(plugin, false); // Does not require bank chunk
    }

    @Override
    public String getName() {
        return "createaccount";
    }

    @Override
    public String getDescription() {
        return "Create a player's account in a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/banks createaccount <playerName> [initialBalance]", NamedTextColor.YELLOW)
                .append(Component.text(" - Create an account for the specified player in this bank", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("If initialBalance is not specified, it defaults to 0.", NamedTextColor.GRAY));
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        String playerName = args[0];
        double initialBalance = 0.0;

        // Check if player is creating an account for someone else
        if (!playerName.equalsIgnoreCase(player.getName()) && !player.hasPermission("furious.bank.createaccount.others")) {
            player.sendMessage(Component.text("You don't have permission to create accounts for other players.", NamedTextColor.RED));
            return true;
        }

        // Parse initial balance if provided
        if (args.length > 1) {
            try {
                initialBalance = Double.parseDouble(args[1]);
                if (initialBalance < 0) {
                    player.sendMessage(Component.text("Initial balance must be non-negative.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid initial balance: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        // Check if player is in a bank chunk
        Chunk chunk = player.getLocation().getChunk();
        Bank bank = bankManager.getBankByChunk(chunk);
        String bankName;

        if (bank == null) {
            // If not in a bank chunk, use the default bank
            bank = bankManager.getDefaultBank();
            if (bank == null) {
                player.sendMessage(Component.text("No default bank found. Please specify a bank name or stand in a bank chunk.", NamedTextColor.RED));
                return true;
            }
        }

        bankName = bank.getName();

        // Get the target player
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found or not online: " + playerName, NamedTextColor.RED));
            return true;
        }

        // Check if the player already has an account in the bank
        if (bank.hasAccount(targetPlayer.getUniqueId())) {
            player.sendMessage(Component.text("Player " + playerName + " already has an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Create the account
        if (bankManager.createAccount(targetPlayer, bankName, initialBalance)) {
            player.sendMessage(Component.text("Account created for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" with initial balance ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(initialBalance), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to create account.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(sender);
            return true;
        }

        String playerName = args[0];
        double initialBalance = 0.0;

        // Parse initial balance if provided
        if (args.length > 1) {
            try {
                initialBalance = Double.parseDouble(args[1]);
                if (initialBalance < 0) {
                    sender.sendMessage(Component.text("Initial balance must be non-negative.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid initial balance: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the target player
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player not found or not online: " + playerName, NamedTextColor.RED));
            return true;
        }

        // Check if the player already has an account in the bank
        if (bank.hasAccount(targetPlayer.getUniqueId())) {
            sender.sendMessage(Component.text("Player " + playerName + " already has an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Create the account
        if (bankManager.createAccount(targetPlayer, bankName, initialBalance)) {
            sender.sendMessage(Component.text("Account created for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" with initial balance ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(initialBalance), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to create account.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        String playerName = args[0];
        double initialBalance = 0.0;

        // Parse initial balance if provided
        if (args.length > 1) {
            try {
                initialBalance = Double.parseDouble(args[1]);
                if (initialBalance < 0) {
                    player.sendMessage(Component.text("Initial balance must be non-negative.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid initial balance: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the target player
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found or not online: " + playerName, NamedTextColor.RED));
            return true;
        }

        // Check if the player already has an account in the bank
        if (bank.hasAccount(targetPlayer.getUniqueId())) {
            player.sendMessage(Component.text("Player " + playerName + " already has an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Create the account
        if (bankManager.createAccount(targetPlayer, bankName, initialBalance)) {
            player.sendMessage(Component.text("Account created for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" in bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" with initial balance ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getWalletManager().formatAmount(initialBalance), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to create account.", NamedTextColor.RED));
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
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialPlayerName)) {
                    completions.add(player.getName());
                }
            }
        } else if ((args.length == 2 && sender instanceof Player) ||
                  (args.length == 3 && !(sender instanceof Player))) {
            // Suggest some common initial balances
            String partialBalance = args[args.length - 1].toLowerCase();
            List<String> balances = List.of("0", "100", "500", "1000");

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
        return "furious.bank.createaccount";
    }
}