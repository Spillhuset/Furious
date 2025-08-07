package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Abstract base class for bank commands that need to be executed within bank chunks.
 */
public abstract class BaseBankCommand implements SubCommand {
    protected final Furious plugin;
    protected final BankManager bankManager;
    private final boolean requiresBankChunk;

    /**
     * Creates a new BaseBankCommand.
     *
     * @param plugin The plugin instance
     * @param requiresBankChunk Whether this command requires the player to be in a bank chunk
     */
    public BaseBankCommand(Furious plugin, boolean requiresBankChunk) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.requiresBankChunk = requiresBankChunk;
    }

    /**
     * Executes the command with bank chunk validation.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Handle console commands
        if (!(sender instanceof Player player)) {
            // Console needs to specify a bank name
            if (args.length == 0) {
                sender.sendMessage(Component.text("When executing from console, you must specify a bank name as the first argument.", NamedTextColor.RED));
                return true;
            }

            String bankName = args[0];

            // Check if the bank exists
            if (bankManager.getBank(bankName) == null) {
                sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                return true;
            }

            // Remove the bank name from args for the command implementation
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);

            // Execute the command implementation for console
            return executeConsoleCommand(sender, bankName, newArgs);
        }

        // Handle player commands

        // Check if the player needs to be in a bank chunk
        if (requiresBankChunk && !bankManager.isPlayerInBankChunk(player)) {
            // Allow players to specify a bank name as well
            if (args.length > 0) {
                String bankName = args[0];

                // Check if the bank exists
                if (bankManager.getBank(bankName) == null) {
                    player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
                    return true;
                }

                // Remove the bank name from args for the command implementation
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);

                // Execute the command implementation with specified bank
                return executePlayerCommandWithBank(player, bankName, newArgs);
            } else {
                player.sendMessage(Component.text("This command can only be used within a bank's claimed chunk or by specifying a bank name.", NamedTextColor.RED));
                return true;
            }
        }

        // Execute the command implementation in bank chunk
        return executeCommand(player, args);
    }

    /**
     * Executes the command implementation.
     * This method should be implemented by subclasses.
     *
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    protected abstract boolean executeCommand(Player player, @NotNull String[] args);

    /**
     * Executes the command implementation with a specified bank name.
     * This method should be implemented by subclasses.
     *
     * @param player The player executing the command
     * @param bankName The name of the bank
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        // Default implementation delegates to console command
        return executeConsoleCommand(player, bankName, args);
    }

    /**
     * Executes the command implementation from console with a specified bank name.
     * This method should be implemented by subclasses.
     *
     * @param sender The command sender
     * @param bankName The name of the bank
     * @param args The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        sender.sendMessage(Component.text("This command cannot be executed from console.", NamedTextColor.RED));
        return true;
    }

    /**
     * Tab completes the command.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public abstract List<String> tabComplete(CommandSender sender, @NotNull String[] args);

    /**
     * Gets the permission required to use this command.
     *
     * @return The permission required to use this command
     */
    @Override
    public abstract String getPermission();

    /**
     * Checks if this command denies non-player senders.
     *
     * @return true if non-player senders are denied, false otherwise
     */
    @Override
    public boolean denyNonPlayer() {
        return false; // Allow console execution with bank name parameter
    }
}