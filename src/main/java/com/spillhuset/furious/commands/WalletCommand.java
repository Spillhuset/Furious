package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.WalletCommands.*;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WalletCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public WalletCommand(Furious instance) {
        this.plugin = instance;

        subCommands.addAll(Arrays.asList(
                new AddCommand(plugin),
                new BalanceCommand(plugin),
                new LogCommand(plugin),
                new PayCommand(plugin),
                new SetCommand(plugin),
                new SubCommand(plugin)
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!can(sender, true)) {
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        for (SubCommandInterface subCommandInterface : subCommands) {
            if (subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                if (subCommandInterface.can(sender, true)) {
                    subCommandInterface.execute(sender, args);
                    return true;
                }
            }
        }

        sendUsage(sender);
        return true;
    }

    public void sendUsage(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        for (SubCommandInterface subCommandInterface : subCommands) {
            if (subCommandInterface.can(sender, false)) {
                commands.add(subCommandInterface.getName());
            }
        }
        sendUsage(sender, commands);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length >= 1) {
            for (SubCommandInterface subCommandInterface : subCommands) {
                if (subCommandInterface.can(sender, false)) {
                    if (subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                        return subCommandInterface.tabComplete(sender, args);
                    } else if (subCommandInterface.getName().startsWith(args[0])) {
                        suggestions.add(subCommandInterface.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public String getName() {
        return "wallet";
    }

    @Override
    public String getPermission() {
        return "furious.wallet";
    }
}
