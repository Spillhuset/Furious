package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class SelectionListener implements Listener {
    private final Furious plugin;

    public SelectionListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only consider main hand interactions with a wooden axe
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_AXE) return;
        if (event.getClickedBlock() == null) return;

        Action action = event.getAction();
        UUID uuid = player.getUniqueId();
        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                plugin.guildService.setSelectionPos1(uuid, event.getClickedBlock().getLocation());
                Components.sendGreyMessage(player, "Set point A at " + locStr(event.getClickedBlock().getLocation()));
                event.setCancelled(true);
            }
            case RIGHT_CLICK_BLOCK -> {
                plugin.guildService.setSelectionPos2(uuid, event.getClickedBlock().getLocation());
                Components.sendGreyMessage(player, "Set point B at " + locStr(event.getClickedBlock().getLocation()));
                event.setCancelled(true);
            }
            default -> {}
        }
    }

    private String locStr(org.bukkit.Location loc) {
        return String.format("%s (%d,%d,%d)", loc.getWorld() != null ? loc.getWorld().getName() : "world", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
