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
 * Command handler for /tpa alias
 * Delegates to RequestSubCommand
 */
public class TpaCommand implements CommandExecutor, TabCompleter {
    private final RequestSubCommand requestSubCommand;

    public TpaCommand(Furious plugin) {
        this.requestSubCommand = new RequestSubCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Create a new args array with "request" as the first argument
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "request";
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return requestSubCommand.execute(sender, newArgs);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return requestSubCommand.tabComplete(sender, args);
    }
}