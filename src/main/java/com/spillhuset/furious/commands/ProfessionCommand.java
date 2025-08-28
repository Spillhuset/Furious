package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.ProfessionCommands.SetPrimaryCommand;
import com.spillhuset.furious.commands.ProfessionCommands.SetSecondaryCommand;
import com.spillhuset.furious.commands.ProfessionCommands.ShowCommand;
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

public class ProfessionCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public ProfessionCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
        subCommands.addAll(Arrays.asList(
                new ShowCommand(this.plugin),
                new SetPrimaryCommand(this.plugin),
                new SetSecondaryCommand(this.plugin)
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        for (SubCommandInterface sc : subCommands) {
            if (sc.getName().equalsIgnoreCase(args[0])) {
                if (sc.can(sender, true)) {
                    sc.execute(sender, args);
                    return true;
                }
            }
        }
        sendUsage(sender);
        return true;
    }

    public void sendUsage(CommandSender sender) {
        List<String> cmds = new ArrayList<>();
        for (SubCommandInterface sc : subCommands) {
            if (sc.can(sender, false)) cmds.add(sc.getName());
        }
        sendUsage(sender, cmds);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length >= 1) {
            for (SubCommandInterface sc : subCommands) {
                if (sc.can(sender, false)) {
                    if (sc.getName().equalsIgnoreCase(args[0])) {
                        return sc.tabComplete(sender, args);
                    } else if (sc.getName().startsWith(args[0])) {
                        suggestions.add(sc.getName());
                    }
                }
            }
        }
        return suggestions;
    }

    @Override
    public String getName() { return "profession"; }

    @Override
    public String getPermission() { return "furious.profession"; }
}
