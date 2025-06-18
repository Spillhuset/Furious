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
 * Subcommand for renaming a bank.
 */
public class RenameBankSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new RenameBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RenameBankSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
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
        sender.sendMessage(Component.text("/bank rename <oldName> <newName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Rename a bank from <oldName> to <newName>", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String oldName = args[1];
        String newName = args[2];

        // Check if the new bank name is valid (alphanumeric)
        if (!newName.matches("^[a-zA-Z0-9]+$")) {
            sender.sendMessage(Component.text("Bank name must be alphanumeric.", NamedTextColor.RED));
            return true;
        }

        // Rename the bank
        if (bankManager.renameBank(oldName, newName)) {
            sender.sendMessage(Component.text("Bank renamed from ", NamedTextColor.GREEN)
                    .append(Component.text(oldName, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(newName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to rename bank. Check that the old bank exists and the new name is not already in use.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest bank names for the first argument
            String partialBankName = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }

            return completions;
        }

        return new ArrayList<>(); // No tab completions for other arguments
    }

    @Override
    public String getPermission() {
        return "furious.bank.rename";
    }
}