package com.spillhuset.furious.entities;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * Represents a warp location in the game.
 * Warps can be created by ops and used by all players.
 */
public class Warp {
    private final UUID id;
    private String name;
    private final UUID creatorId; // Player UUID who created the warp
    private UUID worldId;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double cost; // Cost to use this warp
    private String password; // Optional password protection
    private boolean hasPortal; // Whether this warp has a linked portal
    private String portalFilling; // The material filling the portal (water, lava, or air)
    private Location portalLocation; // Location of the portal

    /**
     * Creates a new warp with the given name and creator at the specified location.
     *
     * @param name       The name of the warp
     * @param creatorId  The UUID of the creator (op player)
     * @param location   The location of the warp
     * @param cost       The cost to use this warp
     * @param password   The password (can be null)
     */
    public Warp(String name, UUID creatorId, Location location, double cost, String password) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.creatorId = creatorId;
        this.cost = cost;
        this.password = password;
        this.hasPortal = false;
        this.portalFilling = "air";
        setLocation(location);
    }

    /**
     * Creates a new warp from saved data.
     *
     * @param id           The UUID of the warp
     * @param name         The name of the warp
     * @param creatorId    The UUID of the creator
     * @param worldId      The UUID of the world
     * @param x            The x coordinate
     * @param y            The y coordinate
     * @param z            The z coordinate
     * @param yaw          The yaw angle
     * @param pitch        The pitch angle
     * @param cost         The cost to use this warp
     * @param password     The password (can be null)
     * @param hasPortal    Whether this warp has a linked portal
     * @param portalFilling The material filling the portal
     * @param portalWorld  The world of the portal
     * @param portalX      The x coordinate of the portal
     * @param portalY      The y coordinate of the portal
     * @param portalZ      The z coordinate of the portal
     */
    public Warp(UUID id, String name, UUID creatorId, UUID worldId, double x, double y, double z, float yaw, float pitch,
                double cost, String password, boolean hasPortal, String portalFilling,
                UUID portalWorld, double portalX, double portalY, double portalZ) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.cost = cost;
        this.password = password;
        this.hasPortal = hasPortal;
        this.portalFilling = portalFilling;

        if (hasPortal && portalWorld != null) {
            World world = Bukkit.getWorld(portalWorld);
            if (world != null) {
                this.portalLocation = new Location(world, portalX, portalY, portalZ);
            }
        }
    }

    /**
     * Gets the unique ID of the warp.
     *
     * @return The warp's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of the warp.
     *
     * @return The warp's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the warp.
     *
     * @param name The new name for the warp
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the UUID of the creator.
     *
     * @return The creator's UUID
     */
    public UUID getCreatorId() {
        return creatorId;
    }

    /**
     * Gets the location of the warp.
     *
     * @return The warp's location, or null if the world doesn't exist
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Sets the location of the warp.
     *
     * @param location The new location for the warp
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

    /**
     * Gets the cost to use this warp.
     *
     * @return The cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * Sets the cost to use this warp.
     *
     * @param cost The new cost
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * Gets the password for this warp.
     *
     * @return The password, or null if not password-protected
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for this warp.
     *
     * @param password The new password, or null to remove password protection
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Checks if this warp has a password.
     *
     * @return true if password-protected, false otherwise
     */
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    /**
     * Checks if this warp has a linked portal.
     *
     * @return true if has a portal, false otherwise
     */
    public boolean hasPortal() {
        return hasPortal;
    }

    /**
     * Sets whether this warp has a linked portal.
     *
     * @param hasPortal true if has a portal, false otherwise
     */
    public void setHasPortal(boolean hasPortal) {
        this.hasPortal = hasPortal;
    }

    /**
     * Gets the portal filling material.
     *
     * @return The portal filling (water, lava, or air)
     */
    public String getPortalFilling() {
        return portalFilling;
    }

    /**
     * Sets the portal filling material.
     *
     * @param portalFilling The portal filling (water, lava, or air)
     */
    public void setPortalFilling(String portalFilling) {
        this.portalFilling = portalFilling;
    }

    /**
     * Gets the location of the portal.
     *
     * @return The portal's location, or null if no portal or world doesn't exist
     */
    public Location getPortalLocation() {
        return portalLocation;
    }

    /**
     * Sets the location of the portal.
     *
     * @param portalLocation The new location for the portal
     */
    public void setPortalLocation(Location portalLocation) {
        this.portalLocation = portalLocation;
    }
}