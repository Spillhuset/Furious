package com.spillhuset.furious.entities;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the roles assigned to a player.
 */
public class PlayerRoles {
    private final UUID playerId;
    private final Set<UUID> roleIds;
    private final Set<Permission> directPermissions;

    /**
     * Creates a new PlayerRoles instance for the given player.
     *
     * @param playerId The UUID of the player
     */
    public PlayerRoles(UUID playerId) {
        this.playerId = playerId;
        this.roleIds = new HashSet<>();
        this.directPermissions = new HashSet<>();
    }

    /**
     * Gets the UUID of the player.
     *
     * @return The player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Gets the IDs of the roles assigned to the player.
     *
     * @return The role IDs
     */
    public Set<UUID> getRoleIds() {
        return new HashSet<>(roleIds);
    }

    /**
     * Gets the direct permissions assigned to the player.
     * These are permissions that are assigned directly to the player, not through roles.
     *
     * @return The direct permissions
     */
    public Set<Permission> getDirectPermissions() {
        return new HashSet<>(directPermissions);
    }

    /**
     * Adds a role to the player.
     *
     * @param roleId The ID of the role to add
     * @return True if the role was added, false if it was already present
     */
    public boolean addRole(UUID roleId) {
        return roleIds.add(roleId);
    }

    /**
     * Removes a role from the player.
     *
     * @param roleId The ID of the role to remove
     * @return True if the role was removed, false if it was not present
     */
    public boolean removeRole(UUID roleId) {
        return roleIds.remove(roleId);
    }

    /**
     * Adds a direct permission to the player.
     *
     * @param permission The permission to add
     * @return True if the permission was added, false if it was already present
     */
    public boolean addDirectPermission(Permission permission) {
        return directPermissions.add(permission);
    }

    /**
     * Adds a direct permission to the player.
     *
     * @param permissionNode The permission node to add
     * @return True if the permission was added, false if it was already present
     */
    public boolean addDirectPermission(String permissionNode) {
        return directPermissions.add(new Permission(permissionNode));
    }

    /**
     * Removes a direct permission from the player.
     *
     * @param permission The permission to remove
     * @return True if the permission was removed, false if it was not present
     */
    public boolean removeDirectPermission(Permission permission) {
        return directPermissions.remove(permission);
    }

    /**
     * Removes a direct permission from the player.
     *
     * @param permissionNode The permission node to remove
     * @return True if the permission was removed, false if it was not present
     */
    public boolean removeDirectPermission(String permissionNode) {
        for (Permission permission : directPermissions) {
            if (permission.getNode().equals(permissionNode)) {
                return directPermissions.remove(permission);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerRoles that = (PlayerRoles) o;
        return Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String toString() {
        return "PlayerRoles{" +
                "playerId=" + playerId +
                ", roleIds=" + roleIds +
                ", directPermissions=" + directPermissions +
                '}';
    }
}