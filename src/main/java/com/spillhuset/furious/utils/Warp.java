package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    // Multiple portal targets supported; legacy single target methods map to first element
    private final List<String> portalTargets = new ArrayList<>();
    // Optional ArmorStand holder entity UUID used as the canonical location
    private @Nullable UUID armorStandUuid;

    // Optional portal region (axis-aligned box) that triggers teleport to portalTarget
    private @Nullable UUID portalWorld;
    private Integer pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ;

    public Warp(String name, Location loc) {
        this.name = name;
        setLocation(loc);
        this.cost = 0.0d;
        this.password = null;
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
        if (portalTarget != null && !portalTarget.isBlank()) this.portalTargets.add(portalTarget);
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

    // Legacy single-target accessors mapped to first element
    public @Nullable String getPortalTarget() { return portalTargets.isEmpty() ? null : portalTargets.get(0); }
    public void setPortalTarget(@Nullable String portalTarget) {
        portalTargets.clear();
        if (portalTarget != null && !portalTarget.isBlank()) portalTargets.add(portalTarget);
    }

    // New multi-target API
    public List<String> getPortalTargets() { return Collections.unmodifiableList(portalTargets); }
    public void setPortalTargets(List<String> targets) {
        portalTargets.clear();
        if (targets != null) {
            for (String t : targets) {
                if (t != null && !t.isBlank()) portalTargets.add(t);
            }
        }
    }
    public boolean hasPortalTargets() { return !portalTargets.isEmpty(); }

    public @Nullable UUID getArmorStandUuid() { return armorStandUuid; }
    public void setArmorStandUuid(@Nullable UUID armorStandUuid) { this.armorStandUuid = armorStandUuid; }

    public UUID getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    // Portal region API
    public void clearPortalRegion() {
        portalWorld = null;
        pMinX = pMinY = pMinZ = pMaxX = pMaxY = pMaxZ = null;
    }

    public void setPortalRegion(UUID worldId, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.portalWorld = worldId;
        this.pMinX = Math.min(x1, x2);
        this.pMinY = Math.min(y1, y2);
        this.pMinZ = Math.min(z1, z2);
        this.pMaxX = Math.max(x1, x2);
        this.pMaxY = Math.max(y1, y2);
        this.pMaxZ = Math.max(z1, z2);
    }

    public @Nullable UUID getPortalWorld() { return portalWorld; }
    public Integer getpMinX() { return pMinX; }
    public Integer getpMinY() { return pMinY; }
    public Integer getpMinZ() { return pMinZ; }
    public Integer getpMaxX() { return pMaxX; }
    public Integer getpMaxY() { return pMaxY; }
    public Integer getpMaxZ() { return pMaxZ; }

    public boolean hasPortalRegion() {
        return portalWorld != null && pMinX != null && pMinY != null && pMinZ != null && pMaxX != null && pMaxY != null && pMaxZ != null;
    }

    public boolean isInsidePortalRegion(Location loc) {
        if (!hasPortalRegion() || loc.getWorld() == null) return false;
        if (!loc.getWorld().getUID().equals(portalWorld)) return false;
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        return bx >= pMinX && bx <= pMaxX && by >= pMinY && by <= pMaxY && bz >= pMinZ && bz <= pMaxZ;
    }
}
