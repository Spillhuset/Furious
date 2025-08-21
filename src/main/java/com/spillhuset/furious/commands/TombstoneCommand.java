package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TombstoneCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public TombstoneCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) {
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender, List.of("clear"));
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            if (plugin.tombstoneService == null) {
                Components.sendErrorMessage(sender, "Tombstone service is not available.");
                return true;
            }
            int cleared = plugin.tombstoneService.clearAll();
            Components.sendSuccess(sender, Components.t("Cleared "+cleared+" tombstone(s)."));
            return true;
        }
        sendUsage(sender, List.of("clear"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!can(sender, false)) return list;
        if (args.length == 1) {
            if ("clear".startsWith(args[0].toLowerCase())) list.add("clear");
        }
        return list;
    }

    @Override
    public String getName() {
        return "tombstones";
    }

    @Override
    public String getPermission() {
        return "furious.tombstones.clear";
    }
}
