package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public FeedCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Only players can feed themselves from console. Use /feed <player> ...");
                return true;
            }
            feed(p);
            Components.sendSuccess(sender, Components.t("You have been fed."));
            return true;
        }

        if (!sender.hasPermission(getPermission() + ".others")) {
            Components.sendErrorMessage(sender, "You don't have permission to feed others.");
            return true;
        }

        Set<Player> targets = new HashSet<>();
        for (String name : args) {
            Player t = Bukkit.getPlayer(name);
            if (t != null && t.isOnline()) {
                targets.add(t);
            } else {
                Components.sendErrorMessage(sender, "Player not found: " + name);
            }
        }

        if (targets.isEmpty()) {
            Components.sendInfoMessage(sender, "No valid online players to feed.");
            return true;
        }

        for (Player t : targets) {
            feed(t);
            if (!t.equals(sender)) {
                Components.sendInfo(t, Components.t("You have been fed by "), Components.playerComp(sender.getName()), Components.t("."));
            }
        }
        if (targets.size() == 1) {
            Player only = targets.iterator().next();
            Components.sendSuccess(sender, Components.t("Fed "), Components.playerComp(only.getName()), Components.t("."));
        } else {
            Components.sendSuccess(sender, Components.t("Fed "), Components.valueComp(String.valueOf(targets.size())), Components.t(" players."));
        }
        return true;
    }

    private void feed(Player p) {
        try {
            p.setFoodLevel(20);
            p.setSaturation(20f);
            p.setExhaustion(0f);
        } catch (Throwable ignored) {}
    }

    private void sendUsage(CommandSender sender) {
        Components.sendInfo(sender, Components.t("Usage: /" + getName() + " [player ...]", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length >= 1) {
            String prefix = args[args.length - 1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    list.add(name);
                }
            }
        }
        return list;
    }

    @Override
    public String getName() { return "feed"; }

    @Override
    public String getPermission() { return "furious.feed"; }
}
