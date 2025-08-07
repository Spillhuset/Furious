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
 * Subcommand for renaming a bank.
 */
public class RenameBankSubCommand extends BaseBankCommand {

    /**
     * Creates a new RenameBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RenameBankSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Rename a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank rename <newName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Rename the bank in this chunk to <newName>", NamedTextColor.WHITE)));
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        String newName = args[0];

        // Check if the new bank name is valid (alphanumeric)
        if (!newName.matches("^[a-zA-Z0-9]+$")) {
            player.sendMessage(Component.text("Bank name must be alphanumeric.", NamedTextColor.RED));
            return true;
        }

        // Get the bank from the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();
        Bank bank = bankManager.getBankByChunk(chunk);

        if (bank == null) {
            player.sendMessage(Component.text("No bank found in this chunk.", NamedTextColor.RED));
            return true;
        }

        String oldName = bank.getName();

        // Rename the bank
        if (bankManager.renameBank(oldName, newName)) {
            player.sendMessage(Component.text("Bank renamed from ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to rename bank. Check that the new name is not already in use.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(sender);
            return true;
        }

        String newName = args[0];

        // Check if the new bank name is valid (alphanumeric)
        if (!newName.matches("^[a-zA-Z0-9]+$")) {
            sender.sendMessage(Component.text("Bank name must be alphanumeric.", NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        String oldName = bank.getName();

        // Rename the bank
        if (bankManager.renameBank(oldName, newName)) {
            sender.sendMessage(Component.text("Bank renamed from ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to rename bank. Check that the new name is not already in use.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        String newName = args[0];

        // Check if the new bank name is valid (alphanumeric)
        if (!newName.matches("^[a-zA-Z0-9]+$")) {
            player.sendMessage(Component.text("Bank name must be alphanumeric.", NamedTextColor.RED));
            return true;
        }

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        String oldName = bank.getName();

        // Rename the bank
        if (bankManager.renameBank(oldName, newName)) {
            player.sendMessage(Component.text("Bank renamed from ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to rename bank. Check that the new name is not already in use.", NamedTextColor.RED));
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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.rename";
    }
}