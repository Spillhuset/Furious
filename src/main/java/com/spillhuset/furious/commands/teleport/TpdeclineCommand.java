package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Command handler for /tpdecline alias
 * Delegates to DeclineSubCommand
 */
public class TpdeclineCommand implements CommandExecutor, TabCompleter {
    private final DeclineSubCommand declineSubCommand;

    public TpdeclineCommand(Furious plugin) {
        this.declineSubCommand = new DeclineSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Create a new args array with "decline" as the first argument
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "decline";
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return declineSubCommand.execute(sender, newArgs);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Create a new args array with "decline" as the first argument
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "decline";
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return declineSubCommand.tabComplete(sender, newArgs);
    }
}