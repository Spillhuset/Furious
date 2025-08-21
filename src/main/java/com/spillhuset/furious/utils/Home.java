package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class Home {
    private final UUID uuid;
    private String name;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private UUID world;
    private final UUID player;
    // Optional ArmorStand holder entity UUID used as the canonical location
    private UUID armorStandUuid;

    public Home(UUID uuid, String name, Location location, UUID player) {
        this.uuid = uuid;
        this.name = name;
        this.setLocation(location);
        this.player = player;
    }

    public void setLocation(Location location) {
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        if (location.getWorld() != null) {
            this.world = location.getWorld().getUID();
        }
    }

    public Location getLocation(Furious plugin) {
        // Prefer resolving via ArmorStand UUID if present
        if (armorStandUuid != null) {
            Entity ent = plugin.getServer().getEntity(armorStandUuid);
            if (ent != null) {
                return ent.getLocation();
            }
        }
        return new Location(plugin.getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getPlayer() {
        return player;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public UUID getWorld() {
        return world;
    }

    public void setWorld(UUID world) {
        this.world = world;
    }

    public UUID getArmorStandUuid() {
        return armorStandUuid;
    }

    public void setArmorStandUuid(UUID armorStandUuid) {
        this.armorStandUuid = armorStandUuid;
    }
}
