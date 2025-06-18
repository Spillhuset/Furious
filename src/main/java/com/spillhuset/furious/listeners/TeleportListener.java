package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.TeleportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

public class TeleportListener implements Listener {
    private final Furious plugin;

    public TeleportListener(Furious plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.getTeleportManager().isPlayerTeleporting(player)) {
            plugin.getTeleportManager().cancelTeleportTask(player);
            player.sendMessage(Component.text("Teleport cancelled due to damage!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (plugin.getTeleportManager().isPlayerTeleporting(player)) {
            plugin.getTeleportManager().cancelTeleportTask(player);
            player.sendMessage(Component.text("Teleport cancelled due to interaction!", NamedTextColor.RED));
        }
    }


    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {

        Player leavingPlayer = event.getPlayer();

        TeleportManager teleportManager = plugin.getTeleportManager();

        // Cancelling ongoing requests
        teleportManager.cancelRequest(leavingPlayer);

        // Handle outgoing request
        UUID targetId = teleportManager.getOutgoingRequest(leavingPlayer);
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                target.sendMessage(Component.text(leavingPlayer.getName() + " has left the server. Teleport request cancelled.",
                        NamedTextColor.YELLOW));
            }
        }

        // Handle incoming requests
        Set<UUID> incomingRequests = teleportManager.getIncomingRequests(leavingPlayer);
        for (UUID requesterId : incomingRequests) {
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null) {
                requester.sendMessage(Component.text(leavingPlayer.getName() + " has left the server. Teleport request cancelled.",
                        NamedTextColor.YELLOW));
            }
        }

        // Clean up the player's data
        teleportManager.removePlayerData(leavingPlayer);
    }
}
