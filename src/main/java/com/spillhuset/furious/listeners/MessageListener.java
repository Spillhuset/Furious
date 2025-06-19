package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class MessageListener implements Listener {
    private final Furious plugin;

    public MessageListener(Furious furious) {
        this.plugin = furious;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player is in a guild
        if (plugin.getGuildManager().isInGuild(playerId)) {
            Guild guild = plugin.getGuildManager().getPlayerGuild(playerId);
            // Format: [✔] <player> - <guild>
            event.joinMessage(Component.text("[✔] " + player.getName() + " - " + guild.getName()));
        } else {
            // Format: [✔] <player>
            event.joinMessage(Component.text("[✔] " + player.getName()));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player is in a guild
        if (plugin.getGuildManager().isInGuild(playerId)) {
            Guild guild = plugin.getGuildManager().getPlayerGuild(playerId);
            // Format: [✘] <player> - <guild>
            event.quitMessage(Component.text("[✘] " + player.getName() + " - " + guild.getName()));
        } else {
            // Format: [✘] <player>
            event.quitMessage(Component.text("[✘] " + player.getName()));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Format: [☠] <player>
        event.deathMessage(Component.text("[☠] " + player.getName()));
    }
}
