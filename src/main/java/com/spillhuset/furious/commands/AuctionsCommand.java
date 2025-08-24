package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.AuctionsCommands.*;
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

public class AuctionsCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public AuctionsCommand(Furious plugin) {
        this.plugin = plugin;
        subCommands.addAll(Arrays.asList(
                new ClaimCommand(plugin),
                new UnclaimCommand(plugin),
                new OpenCommand(plugin),
                new SpawnCommand(plugin),
                new UnspawnCommand(plugin),
                new com.spillhuset.furious.commands.AuctionsCommands.TeleportCommand(plugin),
                new SetCommand(plugin),
                new ListCommand(plugin),
                new BuyoutCommand(plugin),
                new CancelCommand(plugin),
                new BidCommand(plugin)
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
        for (SubCommandInterface s : subCommands) {
            if (s.getName().equalsIgnoreCase(args[0])) {
                if (s.can(sender, true)) {
                    s.execute(sender, args);
                    return true;
                }
            }
        }
        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        List<String> cmds = new ArrayList<>();
        for (SubCommandInterface s : subCommands) {
            if (s.can(sender, false)) cmds.add(s.getName());
        }
        sendUsage(sender, cmds);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length >= 1) {
            for (SubCommandInterface s : subCommands) {
                if (s.can(sender, false)) {
                    if (s.getName().equalsIgnoreCase(args[0])) {
                        return s.tabComplete(sender, args);
                    } else if (s.getName().startsWith(args[0])) {
                        list.add(s.getName());
                    }
                }
            }
        }
        return list;
    }

    @Override
    public String getName() { return "auctions"; }

    @Override
    public String getPermission() { return "furious.auctions"; }
}
