package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Listener for lock-related events.
 */
public class LocksListener implements Listener {
    private final Furious plugin;
    private final NamespacedKey lockKey;
    private final NamespacedKey unlockKey;
    private final NamespacedKey infoKey;
    private final NamespacedKey keyKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey blockTypeKey;
    private final NamespacedKey guildKey;

    /**
     * Creates a new LocksListener.
     *
     * @param plugin The plugin instance
     */
    public LocksListener(Furious plugin) {
        this.plugin = plugin;
        this.lockKey = new NamespacedKey(plugin, "lock_item");
        this.unlockKey = new NamespacedKey(plugin, "unlock_item");
        this.infoKey = new NamespacedKey(plugin, "info_tool");
        this.keyKey = new NamespacedKey(plugin, "key_item");
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
        this.blockTypeKey = new NamespacedKey(plugin, "block_type");
        this.guildKey = new NamespacedKey(plugin, "guild_key");
    }

    /**
     * Handles player interactions with blocks using lock items.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        // Only handle right-clicks on blocks with items
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || item == null || block == null) {
            return;
        }

        // Check if the block is locked and handle access control
        // This needs to happen before handling lock items
        if (plugin.getLocksManager().isLocked(block)) {
            // Get the owner of the lock
            UUID lockOwner = plugin.getLocksManager().getOwner(block);

            // Check if the player is the owner or an op
            if (player.getUniqueId().equals(lockOwner) || player.isOp()) {
                // Allow access
                return;
            }

            // Check if the player has a key for this lock in either hand
            // First check main hand
            if (checkKeyForLock(item, lockOwner, block)) {
                // Allow access
                return;
            }

            // Then check off hand
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            if (checkKeyForLock(offHandItem, lockOwner, block)) {
                // Allow access
                return;
            }

            // Check if the block is in a guild territory
            Chunk chunk = block.getChunk();
            Guild guild = plugin.getGuildManager().getChunkOwner(chunk);

            if (guild != null) {
                // Block is in guild territory, use existing guild permission system
                // The GuildListener will handle this
                return;
            }

            // Block is outside guild territory and player doesn't have permission
            event.setCancelled(true);
            player.sendMessage(Component.text("This block is locked by someone else!", NamedTextColor.RED));
            return;
        }

        // Check if the player is in a valid world (not nether, the end, or a game world)
        World world = block.getWorld();
        String worldName = world.getName();

        // Don't allow locks in nether or end worlds
        if (worldName.endsWith("_nether") || worldName.endsWith("_the_end")) {
            player.sendMessage(Component.text("Locks cannot be used in this dimension!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Don't allow locks in game worlds
        if (worldName.equals(plugin.getWorldManager().getGameWorldName()) ||
            worldName.equals(plugin.getWorldManager().getGameBackupName()) ||
            worldName.contains("_playground") ||
            worldName.contains("Map") ||
            worldName.contains("Backup")) {
            player.sendMessage(Component.text("Locks cannot be used in game worlds!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Handle lock item
        if (container.has(lockKey, PersistentDataType.STRING)) {
            event.setCancelled(true); // Prevent normal interaction

            // Check if the block is lockable
            if (!plugin.getLocksManager().isLockable(block)) {
                player.sendMessage(Component.text("This block cannot be locked!", NamedTextColor.RED));
                return;
            }

            // Check if the block is already locked
            if (plugin.getLocksManager().isLocked(block)) {
                player.sendMessage(Component.text("This block is already locked!", NamedTextColor.RED));
                return;
            }

            // Lock the block
            if (plugin.getLocksManager().lockBlock(player, block)) {
                player.sendMessage(Component.text("Block locked successfully!", NamedTextColor.GREEN));

                // Remove one lock item from the player's hand
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            } else {
                player.sendMessage(Component.text("Failed to lock the block!", NamedTextColor.RED));
            }
        }

        // Handle unlock item
        else if (container.has(unlockKey, PersistentDataType.STRING)) {
            event.setCancelled(true); // Prevent normal interaction

            // Check if the block is locked
            if (!plugin.getLocksManager().isLocked(block)) {
                player.sendMessage(Component.text("This block is not locked!", NamedTextColor.RED));
                return;
            }

            // Check if the player is the owner
            UUID owner = plugin.getLocksManager().getOwner(block);
            if (owner == null || !owner.equals(player.getUniqueId())) {
                player.sendMessage(Component.text("You don't own this lock!", NamedTextColor.RED));
                return;
            }

            // Unlock the block
            if (plugin.getLocksManager().unlockBlock(player, block)) {
                player.sendMessage(Component.text("Block unlocked successfully!", NamedTextColor.GREEN));

                // Remove one unlock item from the player's hand
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            } else {
                player.sendMessage(Component.text("Failed to unlock the block!", NamedTextColor.RED));
            }
        }

        // Handle info tool
        else if (container.has(infoKey, PersistentDataType.STRING)) {
            event.setCancelled(true); // Prevent normal interaction

            // Check if the block is locked
            if (!plugin.getLocksManager().isLocked(block)) {
                player.sendMessage(Component.text("This block is not locked!", NamedTextColor.YELLOW));
                return;
            }

            // Get the owner
            UUID ownerUUID = plugin.getLocksManager().getOwner(block);
            String ownerName = "Unknown";

            if (ownerUUID != null) {
                Player owner = Bukkit.getPlayer(ownerUUID);
                if (owner != null) {
                    ownerName = owner.getName();
                } else {
                    // Use UUID string representation for offline players to avoid blocking network calls
                    ownerName = ownerUUID.toString();
                }
            }

            player.sendMessage(Component.text("This block is locked by: ", NamedTextColor.YELLOW)
                    .append(Component.text(ownerName, NamedTextColor.GOLD)));
        }

        // Handle key item
        else if (container.has(keyKey, PersistentDataType.STRING)) {
            event.setCancelled(true); // Prevent normal interaction

            // Check if the block is locked
            if (!plugin.getLocksManager().isLocked(block)) {
                player.sendMessage(Component.text("This block is not locked!", NamedTextColor.RED));
                return;
            }

            // Get the owner of the key
            String keyOwnerStr = container.get(ownerKey, PersistentDataType.STRING);
            if (keyOwnerStr == null) {
                player.sendMessage(Component.text("This key is invalid!", NamedTextColor.RED));
                return;
            }

            UUID keyOwner = UUID.fromString(keyOwnerStr);

            // Check if the key is for a specific block type
            String keyBlockType = container.get(blockTypeKey, PersistentDataType.STRING);
            if (keyBlockType != null && !block.getType().name().equals(keyBlockType)) {
                player.sendMessage(Component.text("This key doesn't work on this type of block!", NamedTextColor.RED));
                return;
            }

            // Get the owner of the lock
            UUID lockOwner = plugin.getLocksManager().getOwner(block);
            if (lockOwner == null) {
                player.sendMessage(Component.text("This lock is invalid!", NamedTextColor.RED));
                return;
            }

            // Check if the key owner matches the lock owner
            if (!keyOwner.equals(lockOwner)) {
                player.sendMessage(Component.text("This key doesn't fit this lock!", NamedTextColor.RED));
                return;
            }

            // Unlock the block
            if (plugin.getLocksManager().unlockBlock(player, block)) {
                player.sendMessage(Component.text("Block unlocked successfully with key!", NamedTextColor.GREEN));

                // Remove one key from the player's hand
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInMainHand(item);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            } else {
                player.sendMessage(Component.text("Failed to unlock the block with key!", NamedTextColor.RED));
            }
        }
    }

    /**
     * Prevents placing lock/key items.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Check if the item is a lock-related item
        if (container.has(lockKey, PersistentDataType.STRING) ||
            container.has(unlockKey, PersistentDataType.STRING) ||
            container.has(infoKey, PersistentDataType.STRING) ||
            container.has(keyKey, PersistentDataType.STRING)) {

            // Prevent placing the item
            event.setCancelled(true);
        }
    }

    /**
     * Prevents dropping the info tool.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (!item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Check if the item is an info tool
        if (container.has(infoKey, PersistentDataType.STRING)) {
            // Prevent dropping the item
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You cannot drop the lock info tool!", NamedTextColor.RED));
        }
    }

    /**
     * Checks if an item is a key for a specific lock.
     *
     * @param item The item to check
     * @param lockOwner The UUID of the lock owner
     * @param block The locked block
     * @return true if the item is a key for the lock, false otherwise
     */
    private boolean checkKeyForLock(ItemStack item, UUID lockOwner, Block block) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(keyKey, PersistentDataType.STRING)) {
            // Check if the key is for a specific block type
            String keyBlockType = container.get(blockTypeKey, PersistentDataType.STRING);
            if (keyBlockType != null && !block.getType().name().equals(keyBlockType)) {
                return false; // Key is for a different block type
            }

            // Check if this is a guild key
            String guildIdStr = container.get(guildKey, PersistentDataType.STRING);
            if (guildIdStr != null) {
                // This is a guild key
                UUID guildId = UUID.fromString(guildIdStr);

                // Get the guild that owns the lock
                Guild lockGuild = null;
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.isMember(lockOwner)) {
                        lockGuild = guild;
                        break;
                    }
                }

                // If the lock owner is not in a guild, check if the key's guild matches
                if (lockGuild == null) {
                    // Get the key's guild
                    Guild keyGuild = plugin.getGuildManager().getGuild(guildId);
                    if (keyGuild != null && keyGuild.isMember(lockOwner)) {
                        return true; // The lock owner is a member of the key's guild
                    }
                } else {
                    // Check if the key's guild matches the lock owner's guild
                    return lockGuild.getId().equals(guildId);
                }
            } else {
                // This is a player key
                // Get the owner of the key
                String keyOwnerStr = container.get(ownerKey, PersistentDataType.STRING);
                if (keyOwnerStr != null) {
                    UUID keyOwner = UUID.fromString(keyOwnerStr);
                    // Check if the key owner matches the lock owner
                    return keyOwner.equals(lockOwner);
                }
            }
        }

        return false;
    }

    /**
     * Prevents transferring the info tool.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Check if the item is an info tool
        if (container.has(infoKey, PersistentDataType.STRING)) {
            // Only allow the owner to move the item within their own inventory
            String ownerStr = container.get(ownerKey, PersistentDataType.STRING);
            if (ownerStr == null) {
                // Invalid item, cancel the event
                event.setCancelled(true);
                return;
            }

            UUID ownerUUID = UUID.fromString(ownerStr);
            Player player = (Player) event.getWhoClicked();

            // If the player is not the owner or trying to move to another inventory
            if (!player.getUniqueId().equals(ownerUUID) || event.getClickedInventory() != player.getInventory()) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot transfer the lock info tool!", NamedTextColor.RED));
            }
        }
    }
}
