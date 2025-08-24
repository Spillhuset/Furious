package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildType;
import com.spillhuset.furious.utils.GuildRole;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces protections for SAFE-type guild claims:
 * - No block placing or breaking (players) within SAFE chunks.
 * - No mob spawning within SAFE chunks.
 * - No explosions or entity-caused block changes within SAFE chunks.
 * - Prevents harmful entities like TNT from spawning/activating in SAFE chunks.
 * - Prevents players from being hit by arrows, fireballs, snowballs, or harmful potions in SAFE chunks.
 */
public class ProtectionListener implements Listener {
    private static final String MSG_SAFE = "You cannot do that here (SAFE territory)";
    private static final String MSG_WAR = "You cannot do that here (WAR zone)";
    private static final String MSG_OWNED_OUTSIDER = "You cannot do that here. Claimed land";
    private static final String MSG_OWNED_ROLE = "Only Moderators/Admins of this guild can build here";
    private final Furious plugin;
    private final Map<UUID, Long> lastPurgeAt = new ConcurrentHashMap<>();

    public ProtectionListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    private void notifyDenied(org.bukkit.entity.Player player, String message) {
        if (player == null) return;
        try {
            if (plugin.messageThrottle != null) {
                plugin.messageThrottle.sendActionBarThrottled(player, Component.text(message));
            } else {
                player.sendMessage(message);
            }
        } catch (Throwable ignored) {}
    }

    private String ownedBySuffix(Guild g) {
        try {
            if (g != null && g.getName() != null && !g.getName().isBlank()) {
                return " (" + g.getName() + ")";
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private boolean isInSafeGuild(Location loc) {
        if (loc == null || plugin.guildService == null || loc.getWorld() == null) return false;
        Chunk chunk = loc.getChunk();
        UUID worldId = loc.getWorld().getUID();
        UUID owner = plugin.guildService.getClaimOwner(worldId, chunk.getX(), chunk.getZ());
        if (owner == null) return false;
        Guild g = plugin.guildService.getGuildById(owner);
        return g != null && g.getType() == GuildType.SAFE;
    }

    private boolean isInWarGuild(Location loc) {
        if (loc == null || plugin.guildService == null || loc.getWorld() == null) return false;
        Chunk chunk = loc.getChunk();
        UUID worldId = loc.getWorld().getUID();
        UUID owner = plugin.guildService.getClaimOwner(worldId, chunk.getX(), chunk.getZ());
        if (owner == null) return false;
        Guild g = plugin.guildService.getGuildById(owner);
        return g != null && g.getType() == GuildType.WAR;
    }

    private Guild getOwningGuild(Location loc) {
        if (loc == null || plugin.guildService == null || loc.getWorld() == null) return null;
        Chunk chunk = loc.getChunk();
        UUID worldId = loc.getWorld().getUID();
        UUID owner = plugin.guildService.getClaimOwner(worldId, chunk.getX(), chunk.getZ());
        return owner == null ? null : plugin.guildService.getGuildById(owner);
    }

    private boolean isInOwnedGuild(Location loc) {
        Guild g = getOwningGuild(loc);
        return g != null && g.getType() == GuildType.OWNED;
    }

    // --- Build/break protections ---
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (isInSafeGuild(loc) || isInWarGuild(loc)) {
            if (event.getPlayer().isOp()) return; // allow ops
            event.setCancelled(true);
            notifyDenied(event.getPlayer(), isInSafeGuild(loc) ? MSG_SAFE : MSG_WAR);
            return;
        }
        // OWNED: only MODERATOR or ADMIN of the owning guild may break
        if (isInOwnedGuild(loc) && !event.getPlayer().isOp()) {
            Guild g = getOwningGuild(loc);
            if (g != null) {
                GuildRole role = g.getMembers().get(event.getPlayer().getUniqueId());
                UUID playerGuildId = plugin.guildService.getGuildIdForMember(event.getPlayer().getUniqueId());
                boolean sameGuild = g.getUuid().equals(playerGuildId);
                boolean can = sameGuild && (role == GuildRole.MODERATOR || role == GuildRole.ADMIN);
                if (!can) {
                    event.setCancelled(true);
                    notifyDenied(event.getPlayer(), sameGuild ? MSG_OWNED_ROLE : (MSG_OWNED_OUTSIDER + ownedBySuffix(g)));
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();
        if (isInSafeGuild(loc) || isInWarGuild(loc)) {
            if (event.getPlayer().isOp()) return; // allow ops
            event.setCancelled(true);
            notifyDenied(event.getPlayer(), isInSafeGuild(loc) ? MSG_SAFE : MSG_WAR);
            return;
        }
        // OWNED: only MODERATOR or ADMIN of the owning guild may place
        if (isInOwnedGuild(loc) && !event.getPlayer().isOp()) {
            Guild g = getOwningGuild(loc);
            if (g != null) {
                GuildRole role = g.getMembers().get(event.getPlayer().getUniqueId());
                UUID playerGuildId = plugin.guildService.getGuildIdForMember(event.getPlayer().getUniqueId());
                boolean sameGuild = g.getUuid().equals(playerGuildId);
                boolean can = sameGuild && (role == GuildRole.MODERATOR || role == GuildRole.ADMIN);
                if (!can) {
                    event.setCancelled(true);
                    notifyDenied(event.getPlayer(), sameGuild ? MSG_OWNED_ROLE : (MSG_OWNED_OUTSIDER + ownedBySuffix(g)));
                }
            }
        }
    }

    // --- Entity/environment protections ---

    // Fire ignition/spread/burn handling in OWNED claims
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isInOwnedGuild(loc)) return;
        BlockIgniteEvent.IgniteCause cause = event.getCause();
        switch (cause) {
            case SPREAD -> {
                // Prevent natural fire spread ignition in OWNED territories
                event.setCancelled(true);
            }
            case LAVA -> {
                // Prevent lava-caused ignitions inside OWNED territories
                event.setCancelled(true);
            }
            case LIGHTNING, EXPLOSION, ENDER_CRYSTAL -> {
                // Prevent lightning/explosion-based ignitions inside OWNED territories
                event.setCancelled(true);
            }
            case FLINT_AND_STEEL -> {
                // Allow only owning guild MODERATOR/ADMIN (or ops)
                if (event.getPlayer() == null) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getPlayer().isOp()) return;
                Guild g = getOwningGuild(loc);
                if (g == null) {
                    event.setCancelled(true);
                    return;
                }
                UUID playerId = event.getPlayer().getUniqueId();
                UUID playerGuildId = plugin.guildService.getGuildIdForMember(playerId);
                boolean sameGuild = g.getUuid().equals(playerGuildId);
                GuildRole role = g.getMembers().get(playerId);
                boolean can = sameGuild && (role == GuildRole.MODERATOR || role == GuildRole.ADMIN);
                if (!can) {
                    event.setCancelled(true);
                    notifyDenied(event.getPlayer(), MSG_OWNED_ROLE + ownedBySuffix(g));
                }
            }
            default -> {
                // For other ignite causes in OWNED, be conservative and cancel spread-like ignitions
                // We allow portal/campfire etc. only if player action is allowed via block place rules
            }
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        // Cancel fire block spreading into OWNED territory
        if (event.getSource().getType() == Material.FIRE) {
            if (isInOwnedGuild(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        // Prevent blocks from burning in OWNED territories
        if (isInOwnedGuild(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // Allow mods/admins to place water/lava in OWNED chunks; outsiders blocked
    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        // Only care about water and lava buckets per requirement
        Material bucket = event.getBucket();
        if (bucket != Material.WATER_BUCKET && bucket != Material.LAVA_BUCKET) return;
        Location targetLoc = null;
        try {
            Block b = event.getBlock();
            targetLoc = b.getLocation();
        } catch (Throwable ignored) {
        }
        if (targetLoc == null) {
            try {
                Block clicked = event.getBlockClicked();
                targetLoc = clicked.getRelative(event.getBlockFace()).getLocation();
            } catch (Throwable ignored) {
            }
        }
        if (targetLoc == null) return;
        if (!isInOwnedGuild(targetLoc)) return; // only enforce in OWNED
        if (event.getPlayer().isOp()) return; // ops bypass
        Guild g = getOwningGuild(targetLoc);
        if (g == null) {
            event.setCancelled(true);
            notifyDenied(event.getPlayer(), MSG_OWNED_OUTSIDER);
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        UUID playerGuildId = plugin.guildService.getGuildIdForMember(playerId);
        boolean sameGuild = g.getUuid().equals(playerGuildId);
        GuildRole role = g.getMembers().get(playerId);
        boolean can = sameGuild && (role == GuildRole.MODERATOR || role == GuildRole.ADMIN);
        if (!can) {
            event.setCancelled(true);
            notifyDenied(event.getPlayer(), sameGuild ? MSG_OWNED_ROLE : (MSG_OWNED_OUTSIDER + ownedBySuffix(g)));
        }
    }

    // Prevent griefing water/lava flow into OWNED chunks from differently owned or unclaimed chunks
    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Material type = from.getType();
        if (type != Material.WATER && type != Material.LAVA) return; // only water/lava per requirement
        Block to = event.getToBlock();
        Location toLoc = to.getLocation();
        Guild destGuild = getOwningGuild(toLoc);
        if (destGuild == null || destGuild.getType() != GuildType.OWNED) return; // only protect OWNED
        Guild srcGuild = getOwningGuild(from.getLocation());
        // Allow only if the same owning guild; cancel otherwise (includes unclaimed or different owners)
        if (srcGuild == null || !srcGuild.getUuid().equals(destGuild.getUuid())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (isInSafeGuild(event.getLocation())) {
            // Block all mob spawning in SAFE territory
            event.setCancelled(true);
            return;
        }
        // In OWNED territory, block only harmful mobs (hostiles)
        if (isInOwnedGuild(event.getLocation()) && event.getEntity() instanceof Enemy) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isInSafeGuild(event.getLocation())) {
            // Cancel explosion effects entirely in SAFE
            event.setCancelled(true);
            event.blockList().clear();
        } else if (isInWarGuild(event.getLocation())) {
            // In WAR, prevent terrain damage but allow explosion entity effects
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (isInSafeGuild(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.blockList().clear();
        } else if (isInWarGuild(event.getBlock().getLocation())) {
            // In WAR, prevent terrain damage but allow explosion entity effects
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isInSafeGuild(event.getBlock().getLocation()) || isInWarGuild(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotentiallyHarmfulEntitySpawn(EntitySpawnEvent event) {
        Entity ent = event.getEntity();
        // Only handle non-creature spawns that are dangerous in SAFE; rely on CreatureSpawnEvent for mobs
        if (ent instanceof TNTPrimed && isInSafeGuild(ent.getLocation())) {
            event.setCancelled(true);
        }
    }

    // Remove hostile mobs that teleport into SAFE or OWNED
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        Entity ent = event.getEntity();
        if (!(ent instanceof Enemy)) return;
        Location to = event.getTo();
        if (to == null) return;
        if (isInSafeGuild(to) || isInOwnedGuild(to)) {
            try {
                ent.remove();
            } catch (Throwable ignored) {
            }
            event.setCancelled(true);
        }
    }

    // --- Combat protections inside SAFE ---
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Prevent ANY damage to players while they are in SAFE territory
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        if (isInSafeGuild(entity.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Redundant safety for projectiles inside SAFE; kept to also remove projectiles
        Entity victim = event.getEntity();
        if (!isInSafeGuild(victim.getLocation())) return;

        Entity damager = event.getDamager();
        if (damager instanceof Arrow ||
                damager instanceof Snowball ||
                damager instanceof Fireball) {
            event.setCancelled(true);
            // Optionally remove the projectile to prevent lingering interactions
            Projectile proj = (Projectile) damager;
            try {
                proj.remove();
            } catch (Throwable ignored) {
            }
        }
    }

    private static final Set<PotionEffectType> HARMFUL_EFFECTS = Set.of(
            PotionEffectType.INSTANT_DAMAGE,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.UNLUCK,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.DARKNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.LEVITATION
    );

    private boolean potionHasHarmfulEffects(ThrownPotion potion) {
        try {
            for (PotionEffect effect : potion.getEffects()) {
                PotionEffectType type = effect.getType();
                if (HARMFUL_EFFECTS.contains(type)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!isInSafeGuild(potion.getLocation())) return;
        if (potionHasHarmfulEffects(potion)) {
            event.setCancelled(true);
        }
    }

    private static final Set<PotionType> HARMFUL_POTION_TYPES = Set.of(
            PotionType.HARMING,
            PotionType.POISON,
            PotionType.WEAKNESS,
            PotionType.SLOWNESS
    );

    @EventHandler
    public void onLingeringPotionApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        if (!isInSafeGuild(cloud.getLocation())) return;
        try {
            // Base type check
            try {
                PotionType base = cloud.getBasePotionType();
                if (base != null && HARMFUL_POTION_TYPES.contains(base)) {
                    event.setCancelled(true);
                    return;
                }
            } catch (Throwable ignored) {
            }
            // Custom effects check
            List<PotionEffect> effects = cloud.getCustomEffects();
            for (PotionEffect effect : effects) {
                if (effect != null && HARMFUL_EFFECTS.contains(effect.getType())) {
                    event.setCancelled(true);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // --- Interactions in OWNED claims ---
    // Note: Outsiders are blocked from these; same-guild members can interact
    private static final Set<Material> INTERACTABLES = Set.of(
            Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.MANGROVE_DOOR, Material.CHERRY_DOOR, Material.BAMBOO_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR,
            Material.IRON_DOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.BAMBOO_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR,
            Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.MANGROVE_BUTTON, Material.CHERRY_BUTTON, Material.BAMBOO_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
            Material.STONE_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE, Material.BAMBOO_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE, Material.WARPED_PRESSURE_PLATE,
            Material.NOTE_BLOCK, Material.REPEATER, Material.COMPARATOR, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.CRAFTING_TABLE,
            Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BELL,
            // Utility/workstation UIs
            Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.SMITHING_TABLE, Material.ENCHANTING_TABLE, Material.GRINDSTONE, Material.STONECUTTER,
            Material.LOOM, Material.CARTOGRAPHY_TABLE, Material.LECTERN,
            // Shulker boxes (all colors)
            Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.BLACK_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX, Material.PINK_SHULKER_BOX
    );

    private boolean isInteractable(Material type) {
        return INTERACTABLES.contains(type);
    }

    @EventHandler
    public void onInteractOwned(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // only main hand relevance here
        // Only handle clicks on blocks
        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();
        if (!isInOwnedGuild(loc)) return;
        if (event.getPlayer().isOp()) return; // allow ops bypass
        Guild g = getOwningGuild(loc);
        if (g == null) return;
        UUID playerId = event.getPlayer().getUniqueId();
        UUID playerGuildId = plugin.guildService.getGuildIdForMember(playerId);
        boolean sameGuild = g.getUuid().equals(playerGuildId);
        Material type = event.getClickedBlock().getType();
        if (!sameGuild) {
            // Outsiders: block interaction with interactables to prevent grief
            if (isInteractable(type)) {
                event.setCancelled(true);
                notifyDenied(event.getPlayer(), MSG_OWNED_OUTSIDER + ownedBySuffix(g));
            }
            return;
        }
        // Members: allow interactables regardless of role; other interactions proceed as normal
        // No action needed unless we want to explicitly cancel non-interactables (we don't)
    }

    // Block opening entity containers (storage minecarts, hopper minecarts, chest boats) by outsiders in OWNED claims
    @EventHandler
    public void onInteractEntityOwned(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        Location loc = entity.getLocation();
        if (!isInOwnedGuild(loc)) return;
        if (event.getPlayer().isOp()) return;
        Guild g = getOwningGuild(loc);
        if (g == null) return;
        // Only consider entity containers
        boolean isEntityContainer = (entity instanceof org.bukkit.entity.minecart.StorageMinecart)
                || (entity instanceof org.bukkit.entity.minecart.HopperMinecart)
                || (entity instanceof org.bukkit.entity.ChestBoat);
        if (!isEntityContainer) return;
        UUID playerId = event.getPlayer().getUniqueId();
        UUID playerGuildId = plugin.guildService.getGuildIdForMember(playerId);
        boolean sameGuild = g.getUuid().equals(playerGuildId);
        if (!sameGuild) {
            event.setCancelled(true);
            notifyDenied(event.getPlayer(), MSG_OWNED_OUTSIDER + ownedBySuffix(g));
        }
    }
    @EventHandler
    public void onMove(EntityMoveEvent event) {
        if(!(event.getEntity() instanceof LivingEntity le))  return;
        if (!(le instanceof Enemy)) return;
        Entity entity = event.getEntity();
        if (isInSafeGuild(entity.getLocation()) || isInOwnedGuild(entity.getLocation())) {
            le.remove();
            event.setCancelled(true);
        }
    }

    // --- Remove monsters entering SAFE/OWNED ---
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (!(le instanceof Enemy)) return; // only hostile entities
        Entity target = event.getTarget();
        if (!(target instanceof Player)) return;
        // If attacker or target is in SAFE or OWNED, remove the hostile mob and cancel targeting
        if (isInSafeGuild(le.getLocation()) || isInSafeGuild(target.getLocation())
                || isInOwnedGuild(le.getLocation()) || isInOwnedGuild(target.getLocation())) {
            try {
                le.remove();
            } catch (Throwable ignored) {
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Guard against null 'to' location during certain teleport/move edge-cases
        Location to = event.getTo();
        if (to == null) return;
        // Only act when changing chunks in the same world to reduce checks
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = to.getChunk();
        World fromWorld = event.getFrom().getWorld();
        World toWorld = to.getWorld();
        if (fromWorld != null && fromWorld.equals(toWorld)
                && fromChunk.getX() == toChunk.getX()
                && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }
        // If player is now in SAFE or OWNED, purge nearby hostile mobs that are inside same protected territory
        boolean inSafe = isInSafeGuild(to);
        boolean inOwned = !inSafe && isInOwnedGuild(to);
        if (!inSafe && !inOwned) return;
        if (toWorld == null) return;
        // Cooldown per player to reduce frequent scans on busy servers
        UUID pid = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastPurgeAt.get(pid);
        if (last != null && (now - last) < 1000L) {
            return;
        }
        lastPurgeAt.put(pid, now);
        // Reasonable radius to cover adjacent entries while limiting cost
        for (Entity e : toWorld.getNearbyEntities(to, 32, 16, 32)) {
            if (e instanceof LivingEntity le && e instanceof Enemy) {
                if ((inSafe && isInSafeGuild(le.getLocation())) || (inOwned && isInOwnedGuild(le.getLocation()))) {
                    try {
                        le.remove();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }
}
