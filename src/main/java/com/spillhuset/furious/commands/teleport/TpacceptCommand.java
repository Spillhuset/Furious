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
 * Command handler for /tpaccept alias
 * Delegates to AcceptSubCommand
 */
public class TpacceptCommand implements CommandExecutor, TabCompleter {
    private final AcceptSubCommand acceptSubCommand;

    public TpacceptCommand(Furious plugin) {
        this.acceptSubCommand = new AcceptSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Create a new args array with "accept" as the first argument
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "accept";
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return acceptSubCommand.execute(sender, newArgs);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Create a new args array with "accept" as the first argument
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "accept";
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return acceptSubCommand.tabComplete(sender, newArgs);
    }
}