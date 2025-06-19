package com.spillhuset.furious.entities;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a role in the permission system.
 * A role has a name, a description, and a set of permissions.
 */
public class Role {
    private final UUID id;
    private String name;
    private String description;
    private final Set<Permission> permissions;

    /**
     * Creates a new role with the given name.
     *
     * @param name The name of the role
     */
    public Role(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = "";
        this.permissions = new HashSet<>();
    }

    /**
     * Creates a new role with the given name and description.
     *
     * @param name The name of the role
     * @param description The description of the role
     */
    public Role(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    /**
     * Gets the unique ID of this role.
     *
     * @return The unique ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of this role.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this role.
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of this role.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this role.
     *
     * @param description The new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the permissions of this role.
     *
     * @return The permissions
     */
    public Set<Permission> getPermissions() {
        return new HashSet<>(permissions);
    }

    /**
     * Adds a permission to this role.
     *
     * @param permission The permission to add
     * @return True if the permission was added, false if it was already present
     */
    public boolean addPermission(Permission permission) {
        return permissions.add(permission);
    }

    /**
     * Adds a permission to this role.
     *
     * @param permissionNode The permission node to add
     * @return True if the permission was added, false if it was already present
     */
    public boolean addPermission(String permissionNode) {
        return permissions.add(new Permission(permissionNode));
    }

    /**
     * Removes a permission from this role.
     *
     * @param permission The permission to remove
     * @return True if the permission was removed, false if it was not present
     */
    public boolean removePermission(Permission permission) {
        return permissions.remove(permission);
    }

    /**
     * Removes a permission from this role.
     *
     * @param permissionNode The permission node to remove
     * @return True if the permission was removed, false if it was not present
     */
    public boolean removePermission(String permissionNode) {
        for (Permission permission : permissions) {
            if (permission.getNode().equals(permissionNode)) {
                return permissions.remove(permission);
            }
        }
        return false;
    }

    /**
     * Checks if this role has the given permission.
     * Takes into account wildcards and negated permissions.
     *
     * @param permissionNode The permission node to check
     * @return True if this role has the permission, false otherwise
     */
    public boolean hasPermission(String permissionNode) {
        boolean hasPermission = false;

        // First, check for exact matches and wildcards
        for (Permission permission : permissions) {
            if (permission.matches(permissionNode)) {
                // If the permission is negated, deny access
                if (permission.isNegated()) {
                    return false;
                }
                // Otherwise, mark that we found a matching permission
                hasPermission = true;
            }
        }

        return hasPermission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}