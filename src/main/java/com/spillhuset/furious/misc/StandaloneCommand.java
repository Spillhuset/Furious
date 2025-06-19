package com.spillhuset.furious.misc;

import com.spillhuset.furious.Furious;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for standalone commands that provides consistent permission checking.
 * This class implements both CommandExecutor and TabCompleter interfaces for Bukkit commands,
 * as well as the SubCommand interface for consistent permission handling.
 */
public abstract class StandaloneCommand implements CommandExecutor, TabCompleter, SubCommand {
    protected final Furious plugin;

    /**
     * Creates a new StandaloneCommand.
     *
     * @param plugin The plugin instance
     */
    public StandaloneCommand(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the command with permission checking.
     * This method handles the permission check before delegating to the executeCommand method.
     *
     * @param sender The command sender
     * @param command The command being executed
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!checkPermission(sender)) {
            return true;
        }

        return executeCommand(sender, command, label, args);
    }

    /**
     * Override of the checkPermission method from SubCommand.
     * This ensures that StandaloneCommand uses the same permission checking logic as SubCommand.
     *
     * @param sender The command sender
     * @param feedback Whether to send feedback messages
     * @return true if the sender has permission, false otherwise
     */
    @Override
    public boolean checkPermission(@NotNull CommandSender sender, boolean feedback) {
        return SubCommand.super.checkPermission(sender, feedback);
    }

    /**
     * Executes the command after permission check.
     * This method should be implemented by subclasses to handle the command logic.
     *
     * @param sender The command sender
     * @param command The command being executed
     * @param label The command label
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    protected abstract boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args);

    /**
     * Provides tab completions for the command.
     * This method should be implemented by subclasses to provide tab completions.
     *
     * @param sender The command sender
     * @param command The command being tab-completed
     * @param label The command label
     * @param args The command arguments
     * @return A list of tab completions
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return tabComplete(sender, args);
    }

    /**
     * Implementation of SubCommand.execute that delegates to executeCommand.
     * This method is required by the SubCommand interface but is not used directly.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // This method is not used directly, but is required by the SubCommand interface
        return true;
    }

    /**
     * Gets the usage message for this command.
     * This method should be implemented by subclasses to provide a usage message.
     *
     * @param sender The command sender
     */
    @Override
    public abstract void getUsage(CommandSender sender);
}
