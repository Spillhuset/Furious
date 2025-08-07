package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command handler for bank-related commands.
 */
public class BankCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    /**
     * Creates a new BankCommand.
     *
     * @param plugin The plugin instance
     */
    public BankCommand(Furious plugin) {
        this.plugin = plugin;

        // Register subcommands
        registerSubCommand(new BalanceSubCommand(plugin));
        registerSubCommand(new ClaimSubCommand(plugin));
        registerSubCommand(new CreateAccountSubCommand(plugin));
        registerSubCommand(new CreateBankSubCommand(plugin));
        registerSubCommand(new DeleteAccountSubCommand(plugin));
        registerSubCommand(new DeleteBankSubCommand(plugin));
        registerSubCommand(new DepositSubCommand(plugin));
        registerSubCommand(new SetBalanceSubCommand(plugin));
        registerSubCommand(new InterestSubCommand(plugin));
        registerSubCommand(new InfoSubCommand(plugin));
        registerSubCommand(new ListSubCommand(plugin));
        registerSubCommand(new RenameBankSubCommand(plugin));
        registerSubCommand(new SpawnBankSubCommand(plugin));
        registerSubCommand(new TeleportBankSubCommand(plugin));
        registerSubCommand(new TransferSubCommand(plugin));
        registerSubCommand(new UnclaimSubCommand(plugin));
        registerSubCommand(new WithdrawSubCommand(plugin));

        // Register help command last so it has access to all other commands
        registerSubCommand(new HelpSubCommand(plugin, subCommands));
    }

    /**
     * Registers a subcommand.
     *
     * @param subCommand The subcommand to register
     */
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    /**
     * Executes the command.
     *
     * @param sender The command sender
     * @param command The command
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Show help if no subcommand is provided
            if (subCommands.containsKey("help")) {
                return subCommands.get("help").execute(sender, new String[0]);
            } else {
                sender.sendMessage(Component.text("Unknown command. Type /bank help for a list of commands.", NamedTextColor.RED));
                return true;
            }
        }

        String subCommandName = args[0].toLowerCase();
        if (subCommands.containsKey(subCommandName)) {
            SubCommand subCommand = subCommands.get(subCommandName);

            // Check if the subcommand denies non-player senders
            if (subCommand.denyNonPlayer() && !(sender instanceof Player)) {
                sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                return true;
            }

            // Check if the sender has permission to use the subcommand
            String permission = subCommand.getPermission();
            if (permission != null && !sender.hasPermission(permission)) {
                sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }

            // Execute the subcommand
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            return subCommand.execute(sender, subArgs);
        } else {
            sender.sendMessage(Component.text("Unknown command. Type /bank help for a list of commands.", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Tab completes the command.
     *
     * @param sender The command sender
     * @param command The command
     * @param alias The command alias
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Tab complete subcommand names
            List<String> completions = new ArrayList<>();
            String partialName = args[0].toLowerCase();

            for (String subCommandName : subCommands.keySet()) {
                if (subCommandName.startsWith(partialName)) {
                    SubCommand subCommand = subCommands.get(subCommandName);
                    String permission = subCommand.getPermission();
                    if (permission == null || sender.hasPermission(permission)) {
                        completions.add(subCommandName);
                    }
                }
            }

            return completions;
        } else if (args.length > 1) {
            // Tab complete subcommand arguments
            String subCommandName = args[0].toLowerCase();
            if (subCommands.containsKey(subCommandName)) {
                SubCommand subCommand = subCommands.get(subCommandName);
                String permission = subCommand.getPermission();
                if (permission == null || sender.hasPermission(permission)) {
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                    return subCommand.tabComplete(sender, subArgs);
                }
            }
        }

        return new ArrayList<>();
    }
}