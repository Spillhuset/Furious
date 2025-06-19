package com.spillhuset.furious.entities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.Inventory;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Represents a tombstone in the game.
 */
public class Tombstone {
    private final UUID id;
    private final UUID playerId;
    private final String playerName;
    private final Location location;
    private final ArmorStand armorStand;
    private final long creationTime;
    private final Inventory inventory;

    /**
     * Creates a new Tombstone.
     *
     * @param id The unique ID of the tombstone
     * @param playerId The ID of the player who died
     * @param playerName The name of the player who died
     * @param location The location of the tombstone
     * @param armorStand The armor stand entity representing the tombstone
     * @param creationTime The time when the tombstone was created
     */
    public Tombstone(UUID id, UUID playerId, String playerName, Location location, ArmorStand armorStand, long creationTime) {
        this.id = id;
        this.playerId = playerId;
        this.playerName = playerName;
        this.location = location;
        this.armorStand = armorStand;
        this.creationTime = creationTime;
        this.inventory = Bukkit.createInventory(null, 54, Component.text("Tombstone of " + playerName));
    }

    /**
     * Gets the unique ID of the tombstone.
     *
     * @return The tombstone ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the ID of the player who died.
     *
     * @return The player ID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Gets the name of the player who died.
     *
     * @return The player name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Gets the location of the tombstone.
     *
     * @return The location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the armor stand entity representing the tombstone.
     *
     * @return The armor stand
     */
    public ArmorStand getArmorStand() {
        return armorStand;
    }

    /**
     * Gets the time when the tombstone was created.
     *
     * @return The creation time in milliseconds
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Gets the inventory of the tombstone.
     *
     * @return The inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
}
