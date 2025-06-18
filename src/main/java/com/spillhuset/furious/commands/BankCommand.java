package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.bank.*;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command handler for bank-related operations.
 */
public class BankCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;
    private final Map<String, SubCommand> subCommands;
    private final Map<String, String> aliases;

    public BankCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
        this.subCommands = new HashMap<>();
        this.aliases = new HashMap<>();

        registerSubCommands();
    }

    /**
     * Registers all bank subcommands.
     */
    private void registerSubCommands() {
        // Register main subcommands
        registerSubCommand(new BalanceSubCommand(plugin), "b");
        registerSubCommand(new DepositSubCommand(plugin), "d");
        registerSubCommand(new WithdrawSubCommand(plugin), "w");
        registerSubCommand(new TransferSubCommand(plugin), "t");
        registerSubCommand(new ClaimSubCommand(plugin), "c");
        registerSubCommand(new UnclaimSubCommand(plugin), "u");
        registerSubCommand(new InfoSubCommand(plugin), "i");

        // Register administrative subcommands
        registerSubCommand(new CreateBankSubCommand(plugin), null);
        registerSubCommand(new RenameBankSubCommand(plugin), null);
        registerSubCommand(new DeleteBankSubCommand(plugin), null);
        registerSubCommand(new CreateAccountSubCommand(plugin), null);
        registerSubCommand(new DeleteAccountSubCommand(plugin), null);
        registerSubCommand(new EditBalanceSubCommand(plugin), null);
        registerSubCommand(new EditInterestSubCommand(plugin), null);

        // Register help subcommand last so it has access to all other subcommands
        registerSubCommand(new HelpSubCommand(plugin, subCommands), "h");
    }

    /**
     * Registers a subcommand with an optional alias.
     *
     * @param subCommand The subcommand to register
     * @param alias The alias for the subcommand (optional)
     */
    private void registerSubCommand(SubCommand subCommand, String alias) {
        String name = subCommand.getName();
        subCommands.put(name, subCommand);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Get the help subcommand
            SubCommand helpCommand = subCommands.get("help");
            return helpCommand.execute(sender, args);
        }

        String subCommandName = args[0].toLowerCase();

        // Check if the subcommand is an alias
        if (aliases.containsKey(subCommandName)) {
            subCommandName = aliases.get(subCommandName);
        }

        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown bank command. Use /bank help for assistance.", NamedTextColor.RED));
            return false;
        }

        if (!subCommand.checkPermission(sender)) {
            // Message already sent by checkPermission
            return true;
        }

        return subCommand.execute(sender, args);
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            String partialArg = args[0].toLowerCase();

            // Add main subcommands
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                if (subCommand.checkPermission(sender, false) && entry.getKey().startsWith(partialArg)) {
                    completions.add(entry.getKey());
                }
            }

            // Add aliases
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                String aliasKey = entry.getKey();
                SubCommand subCommand = subCommands.get(entry.getValue());
                if (subCommand != null && subCommand.checkPermission(sender, false) && aliasKey.startsWith(partialArg)) {
                    completions.add(aliasKey);
                }
            }
        } else if (args.length > 1) {
            // Get the subcommand
            String subCommandName = args[0].toLowerCase();

            // Check if the subcommand is an alias
            if (aliases.containsKey(subCommandName)) {
                subCommandName = aliases.get(subCommandName);
            }

            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }
}
