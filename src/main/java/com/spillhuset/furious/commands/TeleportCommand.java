package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.TeleportCommands.*;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TeleportCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final List<SubCommandInterface> subCommands = new ArrayList<>();

    public TeleportCommand(Furious instance) {
        this.plugin = instance;
        subCommands.addAll(Arrays.asList(
                new RequestCommand(plugin),
                new AcceptCommand(plugin),
                new DeclineCommand(plugin),
                new CancelCommand(plugin),
                new DenyCommand(plugin),
                new PositionCommand(plugin),
                new AllCommand(plugin),
                new TpSubCommand(plugin)
        ));
    }

    private String mapAliasToDefaultSub(String label) {
        switch (label.toLowerCase()) {
            case "tpa": return "request";
            case "tpaccept": return "accept";
            case "tpdecline": return "decline";
            case "tpdeny": return "deny";
            case "tppos": return "position";
            case "tpall": return "all";
            case "tp": return "teleport";
            default: return null;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;
        if (args.length == 0) {
            String mapped = mapAliasToDefaultSub(label);
            if (mapped != null) {
                // Execute the mapped subcommand with no additional args
                for (SubCommandInterface sub : subCommands) {
                    if (sub.getName().equalsIgnoreCase(mapped)) {
                        if (sub.can(sender, true)) {
                            return sub.execute(sender, new String[]{mapped});
                        } else {
                            return true;
                        }
                    }
                }
            }
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

    public void sendUsage(CommandSender sender) {
        List<String> cmds = new ArrayList<>();
        for (SubCommandInterface sub : subCommands) {
            if (sub.can(sender, false)) cmds.add(sub.getName());
        }
        sendUsage(sender, cmds);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> sugg = new ArrayList<>();
        if (args.length >= 1) {
            for (SubCommandInterface sub : subCommands) {
                if (sub.can(sender, false)) {
                    if (sub.getName().equalsIgnoreCase(args[0])) {
                        return sub.tabComplete(sender, args);
                    } else if (sub.getName().startsWith(args[0])) {
                        sugg.add(sub.getName());
                    }
                }
            }
        }
        return sugg;
    }

    @Override
    public String getName() { return "teleport"; }

    @Override
    public String getPermission() { return "furious.teleport"; }
}
