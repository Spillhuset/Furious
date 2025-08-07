package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command to list all available banks.
 */
public class ListSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new ListSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ListSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all available banks";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /bank list", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Map<String, Bank> banks = bankManager.getBanks();

        if (banks.isEmpty()) {
            sender.sendMessage(Component.text("There are no banks available.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Available Banks ===", NamedTextColor.GOLD));

        boolean isOp = sender.isOp();

        for (Map.Entry<String, Bank> entry : banks.entrySet()) {
            String bankName = entry.getKey();
            Bank bank = entry.getValue();

            // Display bank name and any other relevant information
            Component message = Component.text("• ", NamedTextColor.GOLD)
                    .append(Component.text(bankName, NamedTextColor.YELLOW));

            // Add interest rate information if available
            double interestRate = bankManager.getInterestRate(bankName);
            if (interestRate > 0) {
                message = message.append(Component.text(" (Interest: ", NamedTextColor.GRAY))
                        .append(Component.text(String.format("%.2f%%", interestRate * 100), NamedTextColor.AQUA))
                        .append(Component.text(")", NamedTextColor.GRAY));
            }

            // For op players, show additional information
            if (isOp) {
                // Show number of active accounts
                int accountCount = bank.getAccounts().size();
                message = message.append(Component.text(" | Accounts: ", NamedTextColor.GRAY))
                        .append(Component.text(accountCount, NamedTextColor.AQUA));

                // Show if spawn is set
                boolean hasSpawn = bank.getSpawnLocation() != null;
                message = message.append(Component.text(" | Spawn: ", NamedTextColor.GRAY))
                        .append(Component.text(hasSpawn ? "Set" : "Not set",
                                hasSpawn ? NamedTextColor.GREEN : NamedTextColor.RED));
            }

            sender.sendMessage(message);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.bank.list";
    }
}