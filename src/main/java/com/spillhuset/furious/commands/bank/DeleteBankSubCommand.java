package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for deleting a bank.
 */
public class DeleteBankSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new DeleteBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteBankSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Delete a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank delete <bankName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete the specified bank", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Note: The default bank (RubberBank) cannot be deleted.", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];

        // Check if trying to delete the default bank
        if (bankName.equals("RubberBank")) {
            sender.sendMessage(Component.text("The default bank (RubberBank) cannot be deleted.", NamedTextColor.RED));
            return true;
        }

        // Delete the bank
        if (bankManager.deleteBank(bankName)) {
            sender.sendMessage(Component.text("Bank ", NamedTextColor.GREEN)
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted successfully.", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to delete bank. Check that the bank exists and is not the default bank.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest bank names for the first argument, excluding the default bank
            String partialBankName = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            for (String bankName : bankManager.getBanks().keySet()) {
                if (!bankName.equals("RubberBank") && bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }

            return completions;
        }

        return new ArrayList<>(); // No tab completions for other arguments
    }

    @Override
    public String getPermission() {
        return "furious.bank.delete";
    }
}