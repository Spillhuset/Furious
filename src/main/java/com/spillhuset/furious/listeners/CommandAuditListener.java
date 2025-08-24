package com.spillhuset.furious.listeners;

import com.spillhuset.furious.utils.AuditLog;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandAuditListener implements Listener {

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // event.getMessage() starts with "/"
        String raw = event.getMessage();
        if (raw != null && raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        AuditLog.logCommand(event.getPlayer(), raw == null ? "" : raw);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        AuditLog.logCommand(sender, event.getCommand() == null ? "" : event.getCommand());
    }
}
