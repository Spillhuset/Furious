package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Bank;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.Shop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.UUID;

/**
 * Shows a title/subtitle when a player crosses into a different chunk:
 * - Title: Guild that owns the new chunk (or "Wilderness")
 * - SubTitle: If the chunk contains a Bank or a Shop, show its name.
 */
public class ChunkChangeListener implements Listener {
    private final Furious plugin;

    public ChunkChangeListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) return;

        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ() &&
                event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return; // same chunk
        }

        Player player = event.getPlayer();
        String title = "Wilderness";
        String sub = "";
        try {
            // Title: Guild that owns the chunk
            UUID worldId = event.getTo().getWorld().getUID();
            int cx = toChunk.getX();
            int cz = toChunk.getZ();
            UUID ownerGid = plugin.guildService != null ? plugin.guildService.getClaimOwner(worldId, cx, cz) : null;

            // If both previous and current chunks are unclaimed (Wilderness), do not update the title
            try {
                if (plugin.guildService != null) {
                    UUID prevWorldId = event.getFrom().getWorld().getUID();
                    UUID prevOwnerGid = plugin.guildService.getClaimOwner(prevWorldId, fromChunk.getX(), fromChunk.getZ());
                    if (prevOwnerGid == null && ownerGid == null) {
                        return; // unclaimed -> unclaimed: skip title update
                    }
                }
            } catch (Throwable ignored) {}

            if (ownerGid != null && plugin.guildService != null) {
                Guild g = plugin.guildService.getGuildById(ownerGid);
                if (g != null && g.getName() != null && !g.getName().isBlank()) {
                    title = g.getName();
                }
            }

            // SubTitle: Bank and/or Shop name (show both if present)
            String bankName = null;
            String shopName = null;
            if (plugin.banksService != null) {
                Bank bank = plugin.banksService.getBankAt(event.getTo());
                if (bank != null) {
                    bankName = bank.getName();
                }
            }
            if (plugin.shopsService != null) {
                Shop shop = plugin.shopsService.getShopAt(event.getTo());
                if (shop != null) {
                    shopName = shop.getName();
                }
            }
            if (bankName != null && shopName != null) {
                sub = "Bank: " + bankName + " | Shop: " + shopName;
            } else if (bankName != null) {
                sub = "Bank: " + bankName;
            } else if (shopName != null) {
                sub = "Shop: " + shopName;
            }
        } catch (Throwable ignored) {}

        // Show the title using Adventure API (fallbacks are not necessary for modern servers)
        try {
            Title.Times times = Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(250));
            player.showTitle(Title.title(Component.text(title), Component.text(sub), times));
        } catch (Throwable t) {
            // Best-effort fallback to legacy API if available
            /*try {
                //player.sendTitle(title, sub, 5, 40, 5);
            } catch (Throwable ignored) {}*/
        }

        // After crossing into a new chunk, re-apply per-viewer ArmorStand visibility so
        // non-ops do not see marker ArmorStands that may have appeared with the new chunk.
        try {
            if (plugin.shopsService != null) {
                plugin.shopsService.applyShopArmorStandVisibilityForViewer(player);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.banksService != null) {
                plugin.banksService.applyBankArmorStandVisibilityForViewer(player);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.homesService != null) {
                plugin.homesService.applyHomeArmorStandVisibility(player);
            }
        } catch (Throwable ignored) {}
        try {
            if (plugin.guildHomesService != null) {
                plugin.guildHomesService.applyGuildHomeArmorStandVisibility(player);
            }
        } catch (Throwable ignored) {}
        // Warps do not expose a helper; mirror PlayerJoinListener logic
        try {
            if (plugin.warpsService != null) {
                for (String name : plugin.warpsService.getWarpNames()) {
                    com.spillhuset.furious.utils.Warp w = plugin.warpsService.getWarp(name);
                    if (w == null || w.getArmorStandUuid() == null) continue;
                    org.bukkit.entity.Entity ent = plugin.getServer().getEntity(w.getArmorStandUuid());
                    if (ent instanceof org.bukkit.entity.ArmorStand stand) {
                        if (player.isOp()) player.showEntity(plugin, stand); else player.hideEntity(plugin, stand);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
