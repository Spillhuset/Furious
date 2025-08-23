package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class MessageThrottle {
    private final Furious plugin;


    public MessageThrottle(Furious plugin) {
    }


    public void sendActionBarThrottled(Player player, Component component) {
        try {
                player.sendActionBar(component);
            }
        } catch (Throwable ignored) {}
    }

    public void broadcastThrottled(Component component) {
        try {
                plugin.getServer().broadcast(component);
            }
        } catch (Throwable ignored) {}
    }
}
