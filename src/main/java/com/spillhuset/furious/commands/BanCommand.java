package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import java.util.*;

@SuppressWarnings("deprecation")
public class BanCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public BanCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /ban <player> <message...> OR /ban <player> <time> <message...>");
            return true;
        }

        String targetName = args[0];
        String timeToken = (args.length >= 3 ? args[1] : null); // only treat as duration when there is room for a reason after it

        Long durationMs = null;
        int reasonStartIndex = 1;
        if (timeToken != null) {
            durationMs = parseDurationMillis(timeToken);
            if (durationMs != null) {
                reasonStartIndex = 2; // time token consumed
            }
        }

        // Build reason from remaining args
        String reason;
        if (reasonStartIndex >= args.length) {
            // No explicit reason remaining; use a default
            reason = "Banned by an operator.";
        } else {
            reason = String.join(" ", Arrays.copyOfRange(args, reasonStartIndex, args.length));
        }

        Long expiresAt = null;
        if (durationMs != null) {
            expiresAt = System.currentTimeMillis() + durationMs;
        }
        // Use custom ban service
        plugin.banService.banByName(targetName, reason, expiresAt, sender.getName());

        // Kick if online
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null && online.isOnline()) {
            try {
                online.kick(Component.text(reason));
            } catch (Throwable t) {
                try {
                    // Legacy fallback if needed
                    online.kickPlayer(reason);
                } catch (Throwable ignored) {}
            }
        }

        if (expiresAt != null) {
            Components.sendSuccess(sender, Components.t("Banned "), Components.playerComp(targetName), Components.t(" until "), Components.valueComp(new java.util.Date(expiresAt).toString()), Components.t(". Reason: "), Components.valueComp(reason));
        } else {
            Components.sendSuccess(sender, Components.t("Permanently banned "), Components.playerComp(targetName), Components.t(". Reason: "), Components.valueComp(reason));
        }
        return true;
    }

    private Long parseDurationMillis(String token) {
        if (token == null) return null;
        token = token.trim().toLowerCase(Locale.ROOT);
        if (token.equals("perm") || token.equals("permanent") || token.equals("forever")) {
            return null; // explicit permanent
        }
        // Expect format: <number><unit>
        long multiplier;
        if (token.endsWith("ms")) multiplier = 1L; // milliseconds
        else if (token.endsWith("s")) multiplier = 1000L;
        else if (token.endsWith("m")) multiplier = 60_000L;
        else if (token.endsWith("h")) multiplier = 3_600_000L;
        else if (token.endsWith("d")) multiplier = 86_400_000L;
        else if (token.endsWith("w")) multiplier = 604_800_000L;
        else if (token.endsWith("y")) multiplier = 31_536_000_000L; // 365 days
        else return null; // not a duration token

        try {
            String num = token.replaceAll("[^0-9]", "");
            if (num.isEmpty()) return null;
            long value = Long.parseLong(num);
            return value * multiplier;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission(getPermission())) return list;

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) list.add(name);
            }
        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String s : Arrays.asList("10m","30m","1h","12h","1d","3d","1w","30d","perm")) {
                if (s.startsWith(prefix)) list.add(s);
            }
        }
        return list;
    }

    @Override
    public String getName() { return "ban"; }

    @Override
    public String getPermission() { return "furious.ban"; }
}
