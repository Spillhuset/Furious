package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for editing a bank's interest rate.
 */
public class EditInterestSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new EditInterestSubCommand.
     *
     * @param plugin The plugin instance
     */
    public EditInterestSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "editinterest";
    }

    @Override
    public String getDescription() {
        return "Edit a bank's interest rate";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank editinterest <bankName> <interestRate>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set the interest rate of the specified bank", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Interest rate is a decimal value (e.g., 0.05 for 5%).", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Interest is applied after 2 day-cycles.", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];
        double interestRate;

        // Parse interest rate
        try {
            interestRate = Double.parseDouble(args[2]);
            if (interestRate < 0) {
                sender.sendMessage(Component.text("Interest rate must be non-negative.", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid interest rate: " + args[2], NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the current interest rate
        double oldInterestRate = bank.getInterestRate();

        // Set the new interest rate
        if (bankManager.setInterestRate(bankName, interestRate)) {
            sender.sendMessage(Component.text("Interest rate for bank ", NamedTextColor.GREEN)
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" updated from ", NamedTextColor.GREEN))
                    .append(Component.text(String.format("%.2f%%", oldInterestRate * 100), NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(String.format("%.2f%%", interestRate * 100), NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to update interest rate.", NamedTextColor.RED));
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
            // Suggest some common interest rates
            String partialRate = args[2].toLowerCase();
            List<String> rates = List.of("0", "0.01", "0.02", "0.05", "0.1");

            for (String rate : rates) {
                if (rate.startsWith(partialRate)) {
                    completions.add(rate);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.editinterest";
    }
}