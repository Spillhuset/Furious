package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleportCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    public TeleportCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("request", new RequestSubCommand(plugin));
        subCommands.put("accept", new AcceptSubCommand(plugin));
        subCommands.put("decline", new DeclineSubCommand(plugin));
        subCommands.put("list", new ListSubCommand(plugin));
        subCommands.put("abort", new AbortSubCommand(plugin));
        subCommands.put("deny", new DenySubCommand(plugin));
        subCommands.put("world", new WorldConfigSubCommand(plugin));
        subCommands.put("coords", new CoordsSubCommand(plugin));
        subCommands.put("worldspawn", new WorldSpawnSubCommand(plugin));
        subCommands.put("setworldspawn", new SetWorldSpawnSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if the player is in a minigame
        if (sender instanceof Player player && plugin.getMinigameManager().isInGame(player)) {
            player.sendMessage(Component.text("You cannot use teleport commands while in a minigame!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        for (SubCommand subCommand : subCommands.values()) {
            if (args[0].equalsIgnoreCase(subCommand.getName())) {
                if (sender.hasPermission(subCommand.getPermission())) {
                    return subCommand.execute(sender, args);
                }
            }
        }

        // Handle direct player teleport for OPs
        if ((args.length == 1 || args.length == 2)
                && sender.hasPermission("furious.teleport.force")) {
            return new ForceToPlayerSubCommand(plugin).execute(sender, args);
        }


        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            showHelp(sender);
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Teleport Commands:", NamedTextColor.GOLD));
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text("/teleport request <player> - Request to teleport to a player", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport accept [player] - Accept teleport request", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport decline [player] - Decline teleport request", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport list [in|out] - List teleport requests", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport abort - Cancel your outgoing request or countdown", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport deny - Toggle auto-decline of requests", NamedTextColor.YELLOW));
            if (sender.isOp()) {
                sender.sendMessage(Component.text("/teleport <playerA> [playerB] - Teleports yourself to playerA | Teleport playerA to playerB", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("/teleport coords [player] <x> <y> <z> [world] - Teleports you to a given position | Teleports a given player to a specified location", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("/teleport worldspawn <player> [world] - Teleport player to world's spawn location", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("/teleport setworldspawn [world] - Set the spawn location of a world to your current position", NamedTextColor.YELLOW));
            }
        } else {
            sender.sendMessage(Component.text("/teleport deny <player> - Toggle auto-decline of requests by given player", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport coords <player> <x> <y> <z> [world] - Teleports a given player to a specified location", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport world enable <world> - Enables teleportation request to a given world, if same-world is disabled", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport world disable <world> - Disables teleportation requests to a given world, if same-world is disabled", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport world list - Shows a list of worlds where teleportation is disabled", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport <playerA> <playerB> - Teleport playerA to playerB", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport worldspawn <player> <world> - Teleport player to world's spawn location", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/teleport setworldspawn <world> - Set the spawn location of a world", NamedTextColor.YELLOW));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Handle direct player teleport tab completion for OPs
        if (args.length == 1 && sender.hasPermission("furious.teleport.force")) {
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            // Add subcommands to completions as well
            for (String subCmd : subCommands.keySet()) {
                if (subCmd.startsWith(partial) && sender.hasPermission(subCommands.get(subCmd).getPermission())) {
                    completions.add(subCmd);
                }
            }
            return completions;
        }


        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String subCmd : subCommands.keySet()) {
                if (subCmd.startsWith(partial) && sender.hasPermission(subCommands.get(subCmd).getPermission())) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }
}
