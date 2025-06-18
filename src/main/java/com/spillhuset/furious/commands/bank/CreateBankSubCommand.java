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
 * Subcommand for creating a bank.
 */
public class CreateBankSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new CreateBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateBankSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a new bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank create <bankName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a new bank with the specified name", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];

        // Check if the bank name is valid (alphanumeric)
        if (!bankName.matches("^[a-zA-Z0-9]+$")) {
            sender.sendMessage(Component.text("Bank name must be alphanumeric.", NamedTextColor.RED));
            return true;
        }

        // Create the bank
        if (bankManager.createBank(bankName)) {
            sender.sendMessage(Component.text("Bank ", NamedTextColor.GREEN)
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(" created successfully.", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to create bank. A bank with that name may already exist.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for create command
    }

    @Override
    public String getPermission() {
        return "furious.bank.create";
    }
}