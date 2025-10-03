package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.commands.TeleportCommands.*;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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
        return switch (label.toLowerCase()) {
            case "tpa" -> "request";
            case "tpaccept" -> "accept";
            case "tpdecline" -> "decline";
            case "tpdeny" -> "deny";
            case "tppos" -> "position";
            case "tpall" -> "all";
            case "tp" -> "teleport";
            default -> null;
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        // Special handling for alias "/tp" to support Vanilla-like syntax:
        // /tp <player> -> teleport sender to <player>
        // /tp <playerA> <playerB> -> teleport <playerA> to <playerB>
        if (label.equalsIgnoreCase("tp")) {
            return handleTpAlias(sender, args);
        }

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

    private boolean handleTpAlias(CommandSender sender, String[] args) {
        // Permission should match the TpSubCommand permission
        final String perm = "furious.teleport.teleport";
        if (!sender.hasPermission(perm)) {
            Components.sendErrorMessage(sender, "You don't have permission to use this.");
            return true;
        }
        if (args.length == 1) {
            // /tp <target> (sender must be a player)
            if (!(sender instanceof Player me)) {
                Components.sendInfoMessage(sender, "Usage: /tp <playerA> <playerB>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found or not online.");
                return true;
            }
            me.teleport(target.getLocation());
            Components.sendSuccessMessage(sender, "Teleported to " + target.getName() + ".");
            return true;
        } else if (args.length == 2) {
            // /tp <playerA> <playerB>
            Player a = Bukkit.getPlayerExact(args[0]);
            Player b = Bukkit.getPlayerExact(args[1]);
            if (a == null || b == null) {
                Components.sendErrorMessage(sender, "Both players must be online.");
                return true;
            }
            a.teleport(b.getLocation());
            Components.sendSuccessMessage(sender, "Teleported " + a.getName() + " to " + b.getName() + ".");
            return true;
        } else {
            Components.sendInfoMessage(sender, "Usage: /tp <player> OR /tp <playerA> <playerB>");
            return true;
        }
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
        // Special tab completion for "/tp" alias: suggest player names for arg1 and arg2
        if (label.equalsIgnoreCase("tp")) {
            if (args.length == 1 || args.length == 2) {
                String prefix = args[args.length - 1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) sugg.add(p.getName());
                }
            }
            return sugg;
        }
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
