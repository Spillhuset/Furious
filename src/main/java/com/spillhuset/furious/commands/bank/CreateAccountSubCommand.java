package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for creating a player's account in a bank.
 */
public class CreateAccountSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new CreateAccountSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateAccountSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
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
        sender.sendMessage(Component.text("/bank createaccount <bankName> <playerName> [initialBalance]", NamedTextColor.YELLOW)
                .append(Component.text(" - Create an account for the specified player in the specified bank", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("If initialBalance is not specified, it defaults to 0.", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];
        String playerName = args[2];
        double initialBalance = 0.0;

        // Parse initial balance if provided
        if (args.length > 3) {
            try {
                initialBalance = Double.parseDouble(args[3]);
                if (initialBalance < 0) {
                    sender.sendMessage(Component.text("Initial balance must be non-negative.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid initial balance: " + args[3], NamedTextColor.RED));
                return true;
            }
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the player
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(Component.text("Player not found or not online: " + playerName, NamedTextColor.RED));
            return true;
        }

        // Check if the player already has an account in the bank
        if (bank.hasAccount(player.getUniqueId())) {
            sender.sendMessage(Component.text("Player " + playerName + " already has an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Create the account
        if (bankManager.createAccount(player, bankName, initialBalance)) {
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
            // Suggest some common initial balances
            String partialBalance = args[3].toLowerCase();
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