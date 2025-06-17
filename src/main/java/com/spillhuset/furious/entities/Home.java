package com.spillhuset.furious.entities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a home location in the game.
 * A home can belong to either a player or a guild.
 */
public class Home {
    private final UUID id;
    private String name;
    private final UUID ownerId; // Player UUID or Guild UUID
    private final boolean isGuildHome; // true if this is a guild home, false if it's a player home
    private UUID worldId;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    /**
     * Creates a new home with the given name and owner at the specified location.
     *
     * @param name       The name of the home
     * @param ownerId    The UUID of the owner (player or guild)
     * @param isGuildHome Whether this is a guild home
     * @param location   The location of the home
     */
    public Home(String name, UUID ownerId, boolean isGuildHome, Location location) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.ownerId = ownerId;
        this.isGuildHome = isGuildHome;
        setLocation(location);
    }

    /**
     * Creates a new home from saved data.
     *
     * @param id         The UUID of the home
     * @param name       The name of the home
     * @param ownerId    The UUID of the owner (player or guild)
     * @param isGuildHome Whether this is a guild home
     * @param worldId    The UUID of the world
     * @param x          The x coordinate
     * @param y          The y coordinate
     * @param z          The z coordinate
     * @param yaw        The yaw angle
     * @param pitch      The pitch angle
     */
    public Home(UUID id, String name, UUID ownerId, boolean isGuildHome, UUID worldId, double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.isGuildHome = isGuildHome;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Gets the unique ID of the home.
     *
     * @return The home's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of the home.
     *
     * @return The home's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the home.
     *
     * @param name The new name for the home
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the UUID of the owner (player or guild).
     *
     * @return The owner's UUID
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Checks if this is a guild home.
     *
     * @return true if this is a guild home, false if it's a player home
     */
    public boolean isGuildHome() {
        return isGuildHome;
    }

    /**
     * Gets the location of the home.
     *
     * @return The home's location, or null if the world doesn't exist
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Sets the location of the home.
     *
     * @param location The new location for the home
     */
    public void setLocation(Location location) {
        this.worldId = location.getWorld().getUID();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    /**
     * Gets the UUID of the world.
     *
     * @return The world's UUID
     */
    public UUID getWorldId() {
        return worldId;
    }

    /**
     * Gets the x coordinate.
     *
     * @return The x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Gets the y coordinate.
     *
     * @return The y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Gets the z coordinate.
     *
     * @return The z coordinate
     */
    public double getZ() {
        return z;
    }

    /**
     * Gets the yaw angle.
     *
     * @return The yaw angle
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Gets the pitch angle.
     *
     * @return The pitch angle
     */
    public float getPitch() {
        return pitch;
    }
}