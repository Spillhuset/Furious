package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.enums.GuildType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener for guild-related events.
 */
public class GuildListener implements Listener {
    private final Furious plugin;

    /**
     * Creates a new GuildListener.
     *
     * @param plugin The plugin instance
     */
    public GuildListener(Furious plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles creature spawn events to control mob spawning in the guilds claimed chunks.
     *
     * @param event The creature spawn event
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only handle natural spawns and spawner spawns
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL &&
                reason != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        // Ignore non-monster entities
        EntityType entityType = event.getEntityType();
        if (!isHostileMob(entityType)) {
            return;
        }

        // Get the chunk where the entity is spawning
        Chunk chunk = event.getLocation().getChunk();

        // Check if the chunk is claimed by a guild and if mob spawning is allowed
        if (!plugin.getGuildManager().isMobSpawningAllowed(chunk)) {
            // Cancel the spawn event if mob spawning is not allowed
            event.setCancelled(true);
        }
    }

    /**
     * Checks if an entity type is a hostile mob.
     *
     * @param entityType The entity type to check
     * @return true if the entity is a hostile mob, false otherwise
     */
    private boolean isHostileMob(EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, CAVE_SPIDER, ENDERMAN, WITCH, BLAZE, GHAST, MAGMA_CUBE, SLIME,
                 PHANTOM, DROWNED, HUSK, STRAY, WITHER_SKELETON, RAVAGER, PILLAGER, VINDICATOR, EVOKER, VEX, GUARDIAN,
                 ELDER_GUARDIAN, SHULKER, ENDERMITE, SILVERFISH, HOGLIN, PIGLIN, PIGLIN_BRUTE, ZOGLIN, WARDEN -> true;
            default -> false;
        };
    }

    /**
     * Handles liquid flow events to prevent unwanted lava or water from flowing into claimed plots.
     * A member of the guild placing lava or water should be allowed to flow.
     *
     * @param event The block from-to event
     */
    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        // Check if this is a liquid flow event
        Block fromBlock = event.getBlock();
        Material fromType = fromBlock.getType();

        if (fromType != Material.WATER && fromType != Material.LAVA) {
            return;
        }

        // Get the chunks for the source and destination blocks
        Chunk fromChunk = fromBlock.getChunk();
        Chunk toChunk = event.getToBlock().getChunk();

        // If both chunks are in the same guild's territory, allow the flow
        Guild fromGuild = plugin.getGuildManager().getChunkOwner(fromChunk);
        Guild toGuild = plugin.getGuildManager().getChunkOwner(toChunk);

        // If the destination chunk is not claimed, allow the flow
        if (toGuild == null) {
            return;
        }

        // If the source chunk is not claimed, check if the flow is into a claimed chunk
        if (fromGuild == null) {
            // Block flow from unclaimed chunks into claimed chunks
            event.setCancelled(true);
            return;
        }

        // If both chunks are claimed by the same guild, allow the flow
        if (fromGuild.getId().equals(toGuild.getId())) {
            return;
        }

        // If the chunks are claimed by different guilds, block the flow
        event.setCancelled(true);
    }

    /**
     * Handles player interaction with blocks to protect chests and doors in claimed chunks.
     * Only guild members can access chests and doors in their guild's claimed chunks.
     * Also prevents players from using flint and steel in guild-claimed chunks.
     *
     * @param event The player interact event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click interactions with blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Material blockType = block.getType();
        Player player = event.getPlayer();
        Material itemInHand = player.getInventory().getItemInMainHand().getType();

        // Get the chunk where the block is located
        Chunk chunk = block.getChunk();

        // Check if the chunk is claimed by a guild
        Guild guild = plugin.getGuildManager().getChunkOwner(chunk);
        if (guild == null) {
            // If the chunk is not claimed, allow the interaction
            return;
        }

        // Check if the player is an operator (opped)
        if (player.isOp()) {
            // Opped players are treated as having MOD role, which can access chests and doors
            return;
        }

        // Handle based on guild type for flint and steel
        if (itemInHand == Material.FLINT_AND_STEEL) {
            if (guild.isUnmanned()) {
                // For unmanned guilds (SAFE, WAR, WILD)
                if (guild.getType() == GuildType.WILD) {
                    // WILD zones allow normal players to use flint and steel
                    return;
                } else {
                    // SAFE and WAR zones don't allow normal players to use flint and steel
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot use flint and steel in " +
                                    guild.getName() + " zone.",
                            NamedTextColor.RED));
                    return;
                }
            }

            // For player guilds (GUILD)
            // Check if the player is a member of the guild
            if (!guild.isMember(player.getUniqueId())) {
                // If the player is not a member, cancel the interaction
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot use flint and steel in " +
                        guild.getName() + "'s territory.", NamedTextColor.RED));
                return;
            }

            // If the player is a member, check if they have the appropriate role to build/destroy
            GuildRole role = guild.getMemberRole(player.getUniqueId());
            if (role == null || !role.canBuild()) {
                // If the player doesn't have the appropriate role, cancel the interaction
                event.setCancelled(true);
                player.sendMessage(Component.text("You don't have permission to use flint and steel in this guild territory.",
                        NamedTextColor.RED));
                return;
            }
        }

        // Check if the block is a chest or door
        if (!isChestOrDoor(blockType)) {
            return;
        }

        // Handle based on guild type for chests and doors
        if (guild.isUnmanned()) {
            // For unmanned guilds (SAFE, WAR, WILD)
            if (guild.getType() == GuildType.WILD) {
                // WILD zones allow normal players to access chests and doors
                return;
            } else if (guild.getType() == GuildType.SAFE) {
                // SAFE zones allow access to doors but not chests
                if (isChest(blockType)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Chests in SAFE zones are protected.",
                            NamedTextColor.RED));
                }
                return;
            } else {
                // WAR zones don't allow normal players to access chests or doors
                event.setCancelled(true);
                player.sendMessage(Component.text("This " +
                                (isChest(blockType) ? "chest" : "door") +
                                " is in a WAR zone and is protected.",
                        NamedTextColor.RED));
                return;
            }
        }

        // For player guilds (GUILD)
        // Check if the player is a member of the guild
        if (!guild.isMember(player.getUniqueId())) {
            // If the player is not a member, cancel the interaction
            event.setCancelled(true);
            player.sendMessage(Component.text("This " +
                            (isChest(blockType) ? "chest" : "door") +
                            " is protected by the guild " + guild.getName() + ".",
                    NamedTextColor.RED));
            return;
        }

        // If the player is a member, check if they have the appropriate role to access chests/doors
        GuildRole role = guild.getMemberRole(player.getUniqueId());
        if (role == null || !role.canAccess()) {
            // If the player doesn't have the appropriate role, cancel the interaction
            event.setCancelled(true);
            player.sendMessage(Component.text("You don't have permission to access " +
                            (isChest(blockType) ? "chests" : "doors") +
                            " in this guild territory.",
                    NamedTextColor.RED));
        }
    }

    /**
     * Handles bucket empty events to prevent griefing in guild claimed chunks.
     * Only guild members with appropriate roles can use buckets in their guild's claimed chunks.
     *
     * @param event The player bucket empty event
     */
    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        // Check if the chunk is claimed by a guild
        Guild guild = plugin.getGuildManager().getChunkOwner(chunk);
        if (guild == null) {
            // If the chunk is not claimed, allow the bucket use
            return;
        }

        // Check if the player is an operator (opped)
        if (player.isOp()) {
            // Opped players are treated as having MOD role, which can build/destroy
            return;
        }

        // Handle based on guild type
        if (guild.isUnmanned()) {
            // For unmanned guilds (SAFE, WAR, WILD)
            if (guild.getType() == GuildType.WILD) {
                // WILD zones allow normal players to use buckets
                return;
            } else {
                // SAFE and WAR zones don't allow normal players to use buckets
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot use buckets in " +
                                guild.getName() + " zone.",
                        NamedTextColor.RED));
                return;
            }
        }

        // For player guilds (GUILD)
        // Check if the player is a member of the guild
        if (!guild.isMember(player.getUniqueId())) {
            // If the player is not a member, cancel the bucket use
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot use buckets in " +
                    guild.getName() + "'s territory.", NamedTextColor.RED));
            return;
        }

        // If the player is a member, check if they have the appropriate role to build/destroy
        GuildRole role = guild.getMemberRole(player.getUniqueId());
        if (role == null || !role.canBuild()) {
            // If the player doesn't have the appropriate role, cancel the bucket use
            event.setCancelled(true);
            player.sendMessage(Component.text("You don't have permission to use buckets in this guild territory.",
                    NamedTextColor.RED));
        }
    }

    /**
     * Checks if a material is a chest.
     *
     * @param material The material to check
     * @return true if the material is a chest, false otherwise
     */
    private boolean isChest(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.ENDER_CHEST;
    }

    /**
     * Checks if a material is a door.
     *
     * @param material The material to check
     * @return true if the material is a door, false otherwise
     */
    private boolean isDoor(Material material) {
        return material.name().contains("_DOOR");
    }

    /**
     * Checks if a material is a chest or door.
     *
     * @param material The material to check
     * @return true if the material is a chest or door, false otherwise
     */
    private boolean isChestOrDoor(Material material) {
        return isChest(material) || isDoor(material);
    }

    /**
     * Handles entity explosion events to prevent griefing in guild claimed chunks.
     * Prevents TNT, creepers, and other explosives from damaging blocks in guild-claimed chunks.
     *
     * @param event The entity explode event
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();

        // Create a copy of the block list to avoid ConcurrentModificationException
        List<Block> blocksToRemove = new ArrayList<>();

        // Check each block in the explosion
        for (Block block : event.blockList()) {
            Chunk chunk = block.getChunk();

            // Check if the chunk is claimed by a guild
            Guild guild = plugin.getGuildManager().getChunkOwner(chunk);
            if (guild != null) {
                // If the chunk is claimed, add the block to the list of blocks to remove from the explosion
                blocksToRemove.add(block);
            }
        }

        // Remove the blocks in claimed chunks from the explosion
        event.blockList().removeAll(blocksToRemove);
    }

    /**
     * Handles block break events to prevent griefing in guild claimed chunks.
     * Only guild members with appropriate roles can break blocks in their guild's claimed chunks.
     *
     * @param event The block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        // Check if the chunk is claimed by a guild
        Guild guild = plugin.getGuildManager().getChunkOwner(chunk);
        if (guild == null) {
            // If the chunk is not claimed, allow the break
            return;
        }

        // Check if the player is an operator (opped)
        if (player.isOp()) {
            // Opped players are treated as having MOD role, which can build/destroy
            return;
        }

        // Handle based on guild type
        if (guild.isUnmanned()) {
            // For unmanned guilds (SAFE, WAR, WILD)
            if (guild.getType() == GuildType.WILD) {
                // WILD zones allow normal players to build/destroy
                return;
            } else {
                // SAFE and WAR zones don't allow normal players to build/destroy
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot break blocks in " +
                                guild.getName() + " zone.",
                        NamedTextColor.RED));
                return;
            }
        }

        // For player guilds (GUILD)
        // Check if the player is a member of the guild
        if (!guild.isMember(player.getUniqueId())) {
            // If the player is not a member, cancel the break
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot break blocks in " +
                            guild.getName() + "'s territory.",
                    NamedTextColor.RED));
            return;
        }

        // If the player is a member, check if they have the appropriate role to build/destroy
        GuildRole role = guild.getMemberRole(player.getUniqueId());
        if (role == null || !role.canBuild()) {
            // If the player doesn't have the appropriate role, cancel the break
            event.setCancelled(true);
            player.sendMessage(Component.text("You don't have permission to break blocks in this guild territory.",
                    NamedTextColor.RED));
        }
    }

    /**
     * Handles block place events to prevent griefing in guild claimed chunks.
     * Only guild members with appropriate roles can place blocks in their guild's claimed chunks.
     *
     * @param event The block place event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        // Check if the chunk is claimed by a guild
        Guild guild = plugin.getGuildManager().getChunkOwner(chunk);
        if (guild == null) {
            // If the chunk is not claimed, allow the place
            return;
        }

        // Check if the player is an operator (opped)
        if (player.isOp()) {
            // Opped players are treated as having MOD role, which can build/destroy
            return;
        }

        // Handle based on guild type
        if (guild.isUnmanned()) {
            // For unmanned guilds (SAFE, WAR, WILD)
            if (guild.getType() == GuildType.WILD) {
                // WILD zones allow normal players to build/destroy
                return;
            } else {
                // SAFE and WAR zones don't allow normal players to build/destroy
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot place blocks in " +
                                guild.getName() + " zone.",
                        NamedTextColor.RED));
                return;
            }
        }

        // For player guilds (GUILD)
        // Check if the player is a member of the guild
        if (!guild.isMember(player.getUniqueId())) {
            // If the player is not a member, cancel the place
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot place blocks in " +
                            guild.getName() + "'s territory.",
                    NamedTextColor.RED));
            return;
        }

        // If the player is a member, check if they have the appropriate role to build/destroy
        GuildRole role = guild.getMemberRole(player.getUniqueId());
        if (role == null || !role.canBuild()) {
            // If the player doesn't have the appropriate role, cancel the place
            event.setCancelled(true);
            player.sendMessage(Component.text("You don't have permission to place blocks in this guild territory.",
                    NamedTextColor.RED));
        }
    }
}
