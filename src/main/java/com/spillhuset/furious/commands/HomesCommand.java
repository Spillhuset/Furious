package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.HomesCommands.*;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomesCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public HomesCommand(Furious instance) {
        this.plugin = instance;

        subCommands.addAll(Arrays.asList(
                new BuyCommand(plugin),
                new ListCommand(plugin),
                new RemoveCommand(plugin),
                new MoveCommand(plugin),
                new RenameCommand(plugin),
                new SetCommand(plugin),
                new com.spillhuset.furious.commands.HomesCommands.TeleportCommand(plugin),
                new WorldsCommand(plugin)
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) {
            return true;
        }

        if(args.length == 0) {
            sendUsage(sender);
            return true;
        }

        for(SubCommandInterface subCommandInterface : subCommands) {
            if(subCommandInterface.getName().equalsIgnoreCase(args[0])) {
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
        for(SubCommandInterface subCommandInterface : subCommands) {
            if(subCommandInterface.can(sender, false)) {
                commands.add(subCommandInterface.getName());
            }
        }

        sendUsage(sender, commands);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if(args.length >= 1) {
            for(SubCommandInterface subCommandInterface : subCommands) {
                if(subCommandInterface.can(sender, false)) {
                    if(subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                        return subCommandInterface.tabComplete(sender, args);
                    } else if(subCommandInterface.getName().startsWith(args[0])) {
                        suggestions.add(subCommandInterface.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public String getName() {
        return "homes";
    }

    @Override
    public String getPermission() {
        return "furious.homes";
    }
}
