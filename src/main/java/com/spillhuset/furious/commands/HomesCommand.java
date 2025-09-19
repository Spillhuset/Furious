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
import org.bukkit.entity.Player;
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
                new WorldsCommand(plugin),
                new com.spillhuset.furious.commands.HomesCommands.MigrateCommand(plugin)
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) {
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // Try to match a subcommand first
        for (SubCommandInterface subCommandInterface : subCommands) {
            if (subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                if (subCommandInterface.can(sender, true)) {
                    subCommandInterface.execute(sender, args);
                    return true;
                }
            }
        }

        // If no subcommand matched and exactly one arg is given, treat it as shorthand for teleporting to own home
        if (args.length == 1 && sender instanceof Player player) {
            if (!sender.hasPermission("furious.homes.teleport")) {
                Components.sendErrorMessage(sender, "You don't have permission to teleport to homes.");
                return true;
            }
            plugin.homesService.teleportHome(player, player.getUniqueId(), args[0]);
            return true;
        }

        // Fallback: show usage
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
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length >= 1) {
            // If first arg matches a subcommand, delegate to it
            for (SubCommandInterface subCommandInterface : subCommands) {
                if (subCommandInterface.can(sender, false)) {
                    if (subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                        return subCommandInterface.tabComplete(sender, args);
                    }
                }
            }
            // Otherwise suggest subcommand names and (if allowed) own home names
            String prefix = args[0].toLowerCase();
            for (SubCommandInterface subCommandInterface : subCommands) {
                if (subCommandInterface.can(sender, false)) {
                    String name = subCommandInterface.getName();
                    if (name.toLowerCase().startsWith(prefix)) {
                        suggestions.add(name);
                    }
                }
            }
            if (sender instanceof Player player && sender.hasPermission("furious.homes.teleport")) {
                for (String home : plugin.homesService.getHomesNames(player.getUniqueId())) {
                    if (home.toLowerCase().startsWith(prefix)) {
                        suggestions.add(home);
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
