package com.spillhuset.furious.entities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a portal in the game.
 * Portals can be linked to warps for teleportation.
 */
public class Portal {
    private final UUID id;
    private Location location;
    private Location goldBlock1;
    private Location goldBlock2;
    private String filling;

    /**
     * Creates a new portal at the specified location with the given filling material.
     *
     * @param location The center location of the portal
     * @param filling  The material filling the portal (water, lava, air, etc.)
     */
    public Portal(Location location, String filling) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.filling = filling;
    }

    /**
     * Creates a new portal with the specified gold block locations and filling material.
     *
     * @param location   The center location of the portal
     * @param goldBlock1 The location of the first gold block
     * @param goldBlock2 The location of the second gold block
     * @param filling    The material filling the portal (water, lava, air, etc.)
     */
    public Portal(Location location, Location goldBlock1, Location goldBlock2, String filling) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.goldBlock1 = goldBlock1;
        this.goldBlock2 = goldBlock2;
        this.filling = filling;
    }

    /**
     * Creates a new portal from saved data.
     *
     * @param id          The UUID of the portal
     * @param worldId     The UUID of the world
     * @param x           The x coordinate
     * @param y           The y coordinate
     * @param z           The z coordinate
     * @param filling     The material filling the portal
     */
    public Portal(UUID id, UUID worldId, double x, double y, double z, String filling) {
        this.id = id;
        this.filling = filling;

        World world = Bukkit.getWorld(worldId);
        if (world != null) {
            this.location = new Location(world, x, y, z);
        }
    }

    /**
     * Creates a new portal from saved data including gold block locations.
     *
     * @param id           The UUID of the portal
     * @param worldId      The UUID of the world
     * @param x            The x coordinate of the center
     * @param y            The y coordinate of the center
     * @param z            The z coordinate of the center
     * @param gold1X       The x coordinate of the first gold block
     * @param gold1Y       The y coordinate of the first gold block
     * @param gold1Z       The z coordinate of the first gold block
     * @param gold2X       The x coordinate of the second gold block
     * @param gold2Y       The y coordinate of the second gold block
     * @param gold2Z       The z coordinate of the second gold block
     * @param filling      The material filling the portal
     */
    public Portal(UUID id, UUID worldId, double x, double y, double z, 
                 double gold1X, double gold1Y, double gold1Z,
                 double gold2X, double gold2Y, double gold2Z, String filling) {
        this.id = id;
        this.filling = filling;

        World world = Bukkit.getWorld(worldId);
        if (world != null) {
            this.location = new Location(world, x, y, z);
            this.goldBlock1 = new Location(world, gold1X, gold1Y, gold1Z);
            this.goldBlock2 = new Location(world, gold2X, gold2Y, gold2Z);
        }
    }

    /**
     * Gets the unique ID of the portal.
     *
     * @return The portal's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the center location of the portal.
     *
     * @return The portal's location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the center location of the portal.
     *
     * @param location The new location
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets the world ID of the portal.
     *
     * @return The UUID of the world
     */
    public UUID getWorldId() {
        return location != null && location.getWorld() != null ? location.getWorld().getUID() : null;
    }

    /**
     * Gets the x coordinate of the portal.
     *
     * @return The x coordinate
     */
    public double getX() {
        return location != null ? location.getX() : 0;
    }

    /**
     * Gets the y coordinate of the portal.
     *
     * @return The y coordinate
     */
    public double getY() {
        return location != null ? location.getY() : 0;
    }

    /**
     * Gets the z coordinate of the portal.
     *
     * @return The z coordinate
     */
    public double getZ() {
        return location != null ? location.getZ() : 0;
    }

    /**
     * Gets the material filling the portal.
     *
     * @return The filling material
     */
    public String getFilling() {
        return filling;
    }

    /**
     * Sets the material filling the portal.
     *
     * @param filling The new filling material
     */
    public void setFilling(String filling) {
        this.filling = filling;
    }

    /**
     * Gets the location of the first gold block.
     *
     * @return The first gold block's location
     */
    public Location getGoldBlock1() {
        return goldBlock1;
    }

    /**
     * Sets the location of the first gold block.
     *
     * @param goldBlock1 The new location
     */
    public void setGoldBlock1(Location goldBlock1) {
        this.goldBlock1 = goldBlock1;
    }

    /**
     * Gets the location of the second gold block.
     *
     * @return The second gold block's location
     */
    public Location getGoldBlock2() {
        return goldBlock2;
    }

    /**
     * Sets the location of the second gold block.
     *
     * @param goldBlock2 The new location
     */
    public void setGoldBlock2(Location goldBlock2) {
        this.goldBlock2 = goldBlock2;
    }

    /**
     * Checks if this portal has both gold block locations defined.
     *
     * @return true if both gold block locations are defined, false otherwise
     */
    public boolean hasGoldBlockLocations() {
        return goldBlock1 != null && goldBlock2 != null;
    }
}
