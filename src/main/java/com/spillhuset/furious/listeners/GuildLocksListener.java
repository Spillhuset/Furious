package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import java.util.UUID;

/**
 * Enforce LocksService-style protections inside owned guild chunks for specific targets:
 * - Leads (leashing/unleashing)
 * - Item frames (including glow item frames)
 * - Armor stands
 * - Signs (interaction/editing and breaking)
 * - Trapdoors (including copper variants)
 */
public class GuildLocksListener implements Listener {
    private final Furious plugin;

    public GuildLocksListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    private boolean isLocksWorldEnabled(UUID worldId) {
        try {
            return plugin.locksService != null && plugin.locksService.isWorldEnabled(worldId);
        } catch (Throwable t) {
            return false;
        }
    }

    private UUID owningGuildId(Chunk chunk) {
        try {
            if (plugin.guildService == null) return null;
            return plugin.guildService.getClaimOwner(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean isMemberOf(UUID playerId, UUID guildId) {
        if (guildId == null) return true; // unclaimed
        try {
            UUID playerGuild = plugin.guildService.getGuildIdForMember(playerId);
            return guildId.equals(playerGuild);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean denyIfInOwnedChunk(Player player, Chunk chunk, String message) {
        if (player == null || chunk == null) return false;
        if (player.isOp()) return false;
        if (!isLocksWorldEnabled(chunk.getWorld().getUID())) return false;
        UUID gid = owningGuildId(chunk);
        if (gid == null) return false;
        if (isMemberOf(player.getUniqueId(), gid)) return false;
        Components.sendErrorMessage(player, message);
        return true;
    }

    private boolean isSignBlock(Block block) {
        if (block == null) return false;
        try {
            if (block.getState() instanceof Sign) return true;
        } catch (Throwable ignored) {}
        Material type = block.getType();
        String n = type.name();
        return n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN");
    }

    private boolean isTrapdoorBlock(Block block) {
        if (block == null) return false;
        return block.getBlockData() instanceof TrapDoor; // includes copper variants too
    }

    // Trapdoors and signs: prevent opening/using by non-members
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        if (isTrapdoorBlock(block) || isSignBlock(block)) {
            if (denyIfInOwnedChunk(player, block.getChunk(), "Locked by the owning guild.")) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent breaking signs and trapdoors by non-members
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        if (!(isTrapdoorBlock(block) || isSignBlock(block))) return;
        Player player = event.getPlayer();
        if (denyIfInOwnedChunk(player, block.getChunk(), "Locked by the owning guild.")) {
            event.setCancelled(true);
        }
    }

    // Prevent editing sign text by non-members
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        if (!isSignBlock(block)) return;
        Player player = event.getPlayer();
        if (denyIfInOwnedChunk(player, block.getChunk(), "Locked by the owning guild.")) {
            event.setCancelled(true);
        }
    }

    // Item frames and glow item frames: place prevention inside owned chunks
    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame)) return;
        Player player = event.getPlayer();
        if (player == null) return;
        if (denyIfInOwnedChunk(player, event.getEntity().getLocation().getChunk(), "Locked by the owning guild.")) {
            event.setCancelled(true);
        }
    }

    // Prevent rotating/removing from frames, armor stand manipulation; also leash knot interactions
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        if (entity == null) return;
        if (entity instanceof ItemFrame || entity instanceof GlowItemFrame || entity instanceof LeashHitch) {
            if (denyIfInOwnedChunk(player, entity.getLocation().getChunk(), "Locked by the owning guild.")) {
                event.setCancelled(true);
            }
        }
    }

    // Armor stand equipment manipulation
    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) return;
        if (denyIfInOwnedChunk(event.getPlayer(), entity.getLocation().getChunk(), "Locked by the owning guild.")) {
            event.setCancelled(true);
        }
    }

    // Armor stand inventory changes
    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();
        if (stand == null) return;
        if (denyIfInOwnedChunk(event.getPlayer(), stand.getLocation().getChunk(), "Locked by the owning guild.")) {
            event.setCancelled(true);
        }
    }

    // Prevent damaging/breaking item frames, glow frames, armor stands, and leash knots inside owned chunks
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        Player player = null;
        if (damager instanceof Player p) player = p;
        else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p2) player = p2;
        if (player == null) return;
        if (victim instanceof ItemFrame || victim instanceof GlowItemFrame || victim instanceof ArmorStand || victim instanceof LeashHitch) {
            if (denyIfInOwnedChunk(player, victim.getLocation().getChunk(), "Locked by the owning guild.")) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent breaking item frames via hanging break
    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        Player player = null;
        if (remover instanceof Player p) player = p;
        else if (remover instanceof Projectile proj && proj.getShooter() instanceof Player p2) player = p2;
        if (player == null) return;
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof GlowItemFrame) {
            if (denyIfInOwnedChunk(player, event.getEntity().getLocation().getChunk(), "Locked by the owning guild.")) {
                event.setCancelled(true);
            }
        }
    }

}
