package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
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
 * Subcommand for deleting a bank.
 */
public class DeleteBankSubCommand extends BaseBankCommand {

    /**
     * Creates a new DeleteBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteBankSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
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
        sender.sendMessage(Component.text("/bank delete", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete the bank in this chunk", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Note: The default bank (RubberBank) cannot be deleted.", NamedTextColor.GRAY));
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        // Get the bank from the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();
        Bank bank = bankManager.getBankByChunk(chunk);

        if (bank == null) {
            player.sendMessage(Component.text("No bank found in this chunk.", NamedTextColor.RED));
            return true;
        }

        String bankName = bank.getName();

        // Check if trying to delete the default bank
        if (bankName.equals("RubberBank")) {
            player.sendMessage(Component.text("The default bank (RubberBank) cannot be deleted.", NamedTextColor.RED));
            return true;
        }

        // Delete the bank
        if (bankManager.deleteBank(bankName)) {
            player.sendMessage(Component.text("Bank ", NamedTextColor.GREEN)
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted successfully.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to delete bank. Check that the bank exists and is not the default bank.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

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
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Check if trying to delete the default bank
        if (bankName.equals("RubberBank")) {
            player.sendMessage(Component.text("The default bank (RubberBank) cannot be deleted.", NamedTextColor.RED));
            return true;
        }

        // Delete the bank
        if (bankManager.deleteBank(bankName)) {
            player.sendMessage(Component.text("Bank ", NamedTextColor.GREEN)
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted successfully.", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to delete bank. Check that the bank exists and is not the default bank.", NamedTextColor.RED));
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
                // Don't suggest RubberBank as it can't be deleted
                if (!bankName.equals("RubberBank") && bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.delete";
    }
}