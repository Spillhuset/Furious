package com.spillhuset.furious.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Utility {
    public static @Nullable OfflinePlayer findPlayer(String name) {
        return findPlayer(name, null);
    }

    public static @Nullable OfflinePlayer findPlayer(String name, UUID self) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && self != null && online.getUniqueId().equals(self)) return null;
        if (online != null) return online;
        // try offline matching by exact name
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }
}
