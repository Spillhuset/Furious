package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class UnbanCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public UnbanCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        if (args.length < 1) {
            Components.sendInfoMessage(sender, "Usage: /unban <player>");
            return true;
        }
        String targetName = args[0];

        boolean existed = plugin.banService.unbanByName(targetName);
        if (!existed) {
            Components.sendInfoMessage(sender, "That player is not banned: " + targetName);
            return true;
        }
        Components.sendSuccess(sender, Components.t("Unbanned "), Components.playerComp(targetName), Components.t("."));
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission(getPermission())) return list;
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (com.spillhuset.furious.services.BanService.BanRecord r : plugin.banService.list()) {
                String name = r.name;
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) list.add(name);
            }
        }
        return list;
    }

    @Override
    public String getName() { return "unban"; }

    @Override
    public String getPermission() { return "furious.unban"; }
}
