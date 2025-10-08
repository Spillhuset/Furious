package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.GuildCommands.*;
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

public class GuildCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public GuildCommand(Furious furious) {
        this.plugin = furious.getInstance();
        subCommands.addAll(Arrays.asList(
                new CreateCommand(plugin),
                new RenameCommand(plugin),
                new DeleteCommand(plugin),
                new InviteCommand(plugin),
                new AcceptCommand(plugin),
                new DeclineCommand(plugin),
                new KickCommand(plugin),
                new PromoteCommand(plugin),
                new DemoteCommand(plugin),
                new ClaimCommand(plugin),
                new ClaimsCommand(plugin),
                new UnclaimCommand(plugin),
                new UnclaimsCommand(plugin),
                new SetOpenCommand(plugin),
                new JoinCommand(plugin),
                new LeaveCommand(plugin),
                new WorldsCommand(plugin),
                new com.spillhuset.furious.commands.GuildCommands.HomesCommand(plugin),
                new InfoCommand(plugin),
                new ListCommand(plugin),
                new InvitesCommand(plugin),
                new RequestsCommand(plugin),
                new ConnectivityCommand(plugin),
                                new OutpostCommand(plugin)
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

        for (SubCommandInterface subCommandInterface : subCommands) {
            if (subCommandInterface.getName().equalsIgnoreCase(args[0])) {
                if (subCommandInterface.can(sender, true)) {
                    return subCommandInterface.execute(sender, args);
                } else {
                    // Matched subcommand but no permission; avoid printing usage again
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
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        // If no args typed yet, suggest all accessible subcommands
        if (args.length == 0) {
            for (SubCommandInterface subCommandInterface : subCommands) {
                if (subCommandInterface.can(sender, false)) {
                    suggestions.add(subCommandInterface.getName());
                }
            }
            return suggestions;
        }

        // If at least one arg, either delegate to the subcommand or suggest matching names (case-insensitive)
        if (args.length >= 1) {
            String first = args[0];
            for (SubCommandInterface subCommandInterface : subCommands) {
                if (!subCommandInterface.can(sender, false)) continue;

                String name = subCommandInterface.getName();
                if (name.equalsIgnoreCase(first)) {
                    return subCommandInterface.tabComplete(sender, args);
                } else if (name.toLowerCase().startsWith(first.toLowerCase())) {
                    suggestions.add(name);
                }
            }
        }
        return suggestions;
    }

    @Override
    public String getName() {
        return "guild";
    }

    @Override
    public String getPermission() {
        return "furious.guild";
    }
}
