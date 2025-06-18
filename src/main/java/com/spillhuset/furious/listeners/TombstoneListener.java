package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Tombstone;
import com.spillhuset.furious.managers.TombstoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listener for tombstone-related events.
 */
public class TombstoneListener implements Listener {
    private final Furious plugin;
    private final TombstoneManager tombstoneManager;

    /**
     * Creates a new TombstoneListener.
     *
     * @param plugin The plugin instance
     */
    public TombstoneListener(Furious plugin) {
        this.plugin = plugin;
        this.tombstoneManager = plugin.getTombstoneManager();
    }

    /**
     * Handles player death events to create tombstones.
     *
     * @param event The player death event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check if player is an op
        if (player.isOp()) {
            // Ops don't have tombstones or drops
            event.getDrops().clear();
            return;
        }

        // Create a tombstone at the player's death location
        Tombstone tombstone = tombstoneManager.createTombstone(player, player.getLocation());

        // If tombstone was created successfully, prevent item drops
        if (tombstone != null) {
            event.getDrops().clear();
        }
    }

    /**
     * Handles player interaction with tombstones.
     *
     * @param event The player interact at entity event
     */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Check if the entity is an armor stand
        if (entity instanceof ArmorStand armorStand) {
            // Check if the armor stand is a tombstone
            Tombstone tombstone = tombstoneManager.getTombstoneByArmorStand(armorStand);
            if (tombstone != null) {
                // Cancel the event to prevent default interaction
                event.setCancelled(true);

                // Open the tombstone inventory for the player
                Player player = event.getPlayer();
                tombstoneManager.openTombstone(player, tombstone);
            }
        }
    }

    /**
     * Handles inventory close events to check if tombstones should be removed.
     *
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        // Check all tombstones to see if this inventory belongs to one of them
        for (Tombstone tombstone : tombstoneManager.getAllTombstones()) {
            if (tombstone.getInventory().equals(inventory)) {
                // Check if the tombstone is empty
                if (tombstoneManager.isTombstoneEmpty(tombstone)) {
                    // Remove the tombstone
                    tombstoneManager.removeTombstone(tombstone.getId());

                    // Notify the player
                    if (event.getPlayer() instanceof Player player) {
                        player.sendMessage(Component.text("The tombstone has been removed.", NamedTextColor.YELLOW));
                    }
                }
                break;
            }
        }
    }

    /**
     * Handles entity damage events to make tombstones indestructible except by player punches.
     *
     * @param event The entity damage by entity event
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // Check if the entity is an armor stand
        if (entity instanceof ArmorStand armorStand) {
            // Check if the armor stand is a tombstone
            Tombstone tombstone = tombstoneManager.getTombstoneByArmorStand(armorStand);
            if (tombstone != null) {
                // If the damager is not a player, cancel the event
                if (!(event.getDamager() instanceof Player)) {
                    event.setCancelled(true);
                    return;
                }

                // If the damager is a player, allow the damage and remove the tombstone
                Player player = (Player) event.getDamager();

                // Notify the player
                player.sendMessage(Component.text("You have destroyed a tombstone.", NamedTextColor.YELLOW));

                // Remove the tombstone
                tombstoneManager.removeTombstone(tombstone.getId());

                // Cancel the event to prevent default damage
                event.setCancelled(true);
            }
        }
    }
}
