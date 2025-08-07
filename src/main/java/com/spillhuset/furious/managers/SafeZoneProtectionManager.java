package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manager responsible for protecting safe zones from hostile entities.
 * Periodically checks for and removes hostile mobs and projectiles in safe zones.
 */
public class SafeZoneProtectionManager {
    private final Furious plugin;
    private BukkitTask protectionTask;

    // Set of entity types considered hostile
    private static final Set<EntityType> HOSTILE_ENTITY_TYPES = new HashSet<>(Arrays.asList(
        // Hostile mobs
        EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
        EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.WITCH, EntityType.BLAZE,
        EntityType.GHAST, EntityType.MAGMA_CUBE, EntityType.SLIME, EntityType.PHANTOM,
        EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY, EntityType.WITHER_SKELETON,
        EntityType.RAVAGER, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
        EntityType.VEX, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER,
        EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.HOGLIN, EntityType.PIGLIN,
        EntityType.PIGLIN_BRUTE, EntityType.ZOGLIN, EntityType.WARDEN,

        // Projectiles and other harmful entities
        EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.TRIDENT,
        EntityType.FIREBALL, EntityType.SMALL_FIREBALL, EntityType.DRAGON_FIREBALL,
        EntityType.WITHER_SKULL, EntityType.LLAMA_SPIT, EntityType.SHULKER_BULLET,
        EntityType.FALLING_BLOCK, EntityType.ENDER_PEARL, EntityType.TNT
    ));

    /**
     * Creates a new SafeZoneProtectionManager.
     *
     * @param plugin The plugin instance
     */
    public SafeZoneProtectionManager(Furious plugin) {
        this.plugin = plugin;
        startProtectionTask();
    }

    /**
     * Starts the periodic task that checks for and removes hostile entities in safe zones.
     */
    private void startProtectionTask() {
        // Run the task every 5 seconds (100 ticks)
        protectionTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRemoveHostileEntities, 20L, 100L);
        plugin.getLogger().info("Safe zone protection task started");
    }

    /**
     * Checks all loaded chunks in all worlds for hostile entities in safe zones and removes them.
     */
    private void checkAndRemoveHostileEntities() {
        int removedCount = 0;

        // Loop through all worlds
        for (World world : Bukkit.getWorlds()) {
            // Skip worlds where guild functionality is disabled
            if (!plugin.getGuildManager().isWorldEnabled(world)) {
                continue;
            }

            // Check all loaded chunks in this world
            for (Chunk chunk : world.getLoadedChunks()) {
                // Get the guild that owns this chunk
                Guild guild = plugin.getGuildManager().getChunkOwner(chunk);

                // Skip if not in a safe zone
                if (guild == null || !guild.isSafe()) {
                    continue;
                }

                // Check all entities in this chunk
                for (Entity entity : chunk.getEntities()) {
                    if (isHostileEntity(entity)) {
                        // Remove the entity
                        entity.remove();
                        removedCount++;
                    }
                }
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().log(Level.FINE, "Removed {0} hostile entities from safe zones", removedCount);
        }
    }

    /**
     * Checks if an entity is considered hostile and should be removed from safe zones.
     *
     * @param entity The entity to check
     * @return true if the entity is hostile, false otherwise
     */
    private boolean isHostileEntity(Entity entity) {
        // Check if it's in our list of hostile entity types
        if (HOSTILE_ENTITY_TYPES.contains(entity.getType())) {
            return true;
        }

        // Check if it's a projectile
        if (entity instanceof Projectile) {
            return true;
        }

        return false;
    }

    /**
     * Shuts down the protection task.
     */
    public void shutdown() {
        if (protectionTask != null) {
            protectionTask.cancel();
            plugin.getLogger().info("Safe zone protection task stopped");
        }
    }
}