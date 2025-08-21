package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Warp {
    private String name;
    private UUID world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double cost;
    private @Nullable String password;
    private @Nullable String portalTarget; // name of linked portal warp
    // Optional ArmorStand holder entity UUID used as the canonical location
    private @Nullable UUID armorStandUuid;

    public Warp(String name, Location loc) {
        this.name = name;
        setLocation(loc);
        this.cost = 0.0d;
        this.password = null;
        this.portalTarget = null;
    }

    public Warp(String name, UUID world, double x, double y, double z, float yaw, float pitch, double cost, @Nullable String password, @Nullable String portalTarget) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.cost = cost;
        this.password = password;
        this.portalTarget = portalTarget;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Location toLocation(Furious plugin) {
        if (armorStandUuid != null) {
            Entity ent = plugin.getServer().getEntity(armorStandUuid);
            if (ent != null) {
                return ent.getLocation();
            }
        }
        if (world == null) return null;
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void setLocation(Location loc) {
        this.world = loc.getWorld() != null ? loc.getWorld().getUID() : null;
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = Math.max(0, cost); }

    public @Nullable String getPassword() { return password; }
    public void setPassword(@Nullable String password) { this.password = (password == null || password.isBlank()) ? null : password; }

    public @Nullable String getPortalTarget() { return portalTarget; }
    public void setPortalTarget(@Nullable String portalTarget) { this.portalTarget = portalTarget; }

    public @Nullable UUID getArmorStandUuid() { return armorStandUuid; }
    public void setArmorStandUuid(@Nullable UUID armorStandUuid) { this.armorStandUuid = armorStandUuid; }

    public UUID getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
