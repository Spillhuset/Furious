package com.spillhuset.furious.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface CommandInterface {
    String getName();
    String getPermission();


    default boolean can(CommandSender sender, boolean feedback) {
        if (!sender.hasPermission(getPermission())) {
            if (feedback) {
                Components.sendErrorMessage(sender, "You don't have permission to use this command.");
            }
            return false;
        }
        return true;
    }

    default void sendUsage(CommandSender sender,List<String> commands) {
        Components.sendInfo(sender,Components.t("Usage: /"+getName()+" <"+String.join(" | ",commands)+">"));
    }

    default List<String> suggestPlayers(List<OfflinePlayer> players, String[] args, int level, UUID self) {
        List<String> suggestions = new ArrayList<>();

        for (OfflinePlayer offlinePlayer : players) {
            if (offlinePlayer.getUniqueId().equals(self)) continue;
            if (Objects.requireNonNull(offlinePlayer.getName()).startsWith(args[level])) {
                suggestions.add(offlinePlayer.getName());
            }
        }

        return suggestions;
    }
}
