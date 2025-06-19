package com.spillhuset.furious.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a permission in the system.
 * Permissions can be positive (granted) or negative (denied).
 * Wildcards are supported using the * character.
 */
public class Permission {
    private final UUID id;
    private final String node;
    private final boolean isNegated;

    /**
     * Creates a new permission with the given node.
     *
     * @param node The permission node (e.g., "furious.locks.create")
     */
    public Permission(String node) {
        this.id = UUID.randomUUID();

        if (node.startsWith("-")) {
            this.node = node.substring(1);
            this.isNegated = true;
        } else {
            this.node = node;
            this.isNegated = false;
        }
    }

    /**
     * Creates a new permission with the given node and negation status.
     *
     * @param node The permission node (e.g., "furious.locks.create")
     * @param isNegated Whether this permission is negated (denied)
     */
    public Permission(String node, boolean isNegated) {
        this.id = UUID.randomUUID();
        this.node = node;
        this.isNegated = isNegated;
    }

    /**
     * Gets the permission node.
     *
     * @return The permission node
     */
    public String getNode() {
        return node;
    }

    /**
     * Checks if this permission is negated (denied).
     *
     * @return True if this permission is negated, false otherwise
     */
    public boolean isNegated() {
        return isNegated;
    }

    /**
     * Gets the unique ID of this permission.
     *
     * @return The unique ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Checks if this permission matches the given permission node.
     * Supports wildcard matching.
     *
     * @param permissionNode The permission node to check against
     * @return True if this permission matches the given node, false otherwise
     */
    public boolean matches(String permissionNode) {
        // If this permission is a wildcard (ends with *)
        if (node.endsWith("*")) {
            String prefix = node.substring(0, node.length() - 1);
            return permissionNode.startsWith(prefix);
        }

        // Otherwise, exact match
        return node.equals(permissionNode);
    }

    /**
     * Returns the string representation of this permission.
     * Includes the negation symbol if this permission is negated.
     *
     * @return The string representation
     */
    @Override
    public String toString() {
        return isNegated ? "-" + node : node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return isNegated == that.isNegated && Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, isNegated);
    }
}