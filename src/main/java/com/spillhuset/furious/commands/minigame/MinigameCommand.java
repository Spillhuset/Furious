package com.spillhuset.furious.commands.minigame;

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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command handler for minigame-related commands.
 */
public class MinigameCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new MinigameCommand.
     *
     * @param plugin The plugin instance
     */
    public MinigameCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Registers all minigame subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("join", new JoinSubCommand(plugin));
        subCommands.put("leave", new LeaveSubCommand(plugin));
        subCommands.put("list", new ListSubCommand(plugin));
        subCommands.put("info", new InfoSubCommand(plugin));

        // New commands for configurable minigames
        subCommands.put("create", new CreateSubCommand(plugin));
        subCommands.put("disable", new DisableSubCommand(plugin));
        subCommands.put("enable", new EnableSubCommand(plugin));
        subCommands.put("start", new StartSubCommand(plugin));
        subCommands.put("stop", new StopSubCommand(plugin));
        subCommands.put("edit", new EditSubCommand(plugin));
        subCommands.put("spawn", new SpawnSubCommand(plugin));
        subCommands.put("save", new SaveSubCommand(plugin));

        // New commands for minigame creation
        subCommands.put("setLobby", new SetLobbySubCommand(plugin));
        subCommands.put("exit", new ExitSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            showHelp(sender);
            return true;
        }

        if (!subCommand.checkPermission(sender)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    /**
     * Shows help information for minigame commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Minigame Commands:", NamedTextColor.GOLD));

        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.checkPermission(sender, false)) {
                sender.sendMessage(Component.text("/minigame " + subCommand.getName() + " - " + subCommand.getDescription(), NamedTextColor.YELLOW));
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (SubCommand subCmd : subCommands.values()) {
                if (subCmd.getName().startsWith(partial) && subCmd.checkPermission(sender, false)) {
                    completions.add(subCmd.getName());
                }
            }
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }
}
