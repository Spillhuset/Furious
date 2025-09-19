package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.WorldCommands.InfoCommand;
import com.spillhuset.furious.commands.WorldCommands.SetSpawnCommand;
import com.spillhuset.furious.commands.WorldCommands.SpawnCommand;
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

public class WorldCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public WorldCommand(Furious plugin) {
        this.plugin = plugin;
        subCommands.addAll(Arrays.asList(
                new SetSpawnCommand(plugin),
                new SpawnCommand(plugin),
                new InfoCommand(plugin)
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        for (SubCommandInterface sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(args[0])) {
                if (sub.can(sender, true)) {
                    return sub.execute(sender, args);
                } else {
                    return true;
                }
            }
        }

        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        List<String> available = new ArrayList<>();
        for (SubCommandInterface sub : subCommands) {
            if (sub.can(sender, false)) available.add(sub.getName());
        }
        sendUsage(sender, available);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length >= 1) {
            for (SubCommandInterface sub : subCommands) {
                if (!sub.can(sender, false)) continue;
                if (sub.getName().equalsIgnoreCase(args[0])) {
                    return sub.tabComplete(sender, args);
                }
                if (args.length == 1 && sub.getName().startsWith(args[0].toLowerCase())) {
                    out.add(sub.getName());
                }
            }
        }
        return out;
    }

    @Override
    public String getName() {
        return "world";
    }

    @Override
    public String getPermission() {
        return "furious.world";
    }
}
