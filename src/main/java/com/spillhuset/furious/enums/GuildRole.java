package com.spillhuset.furious.enums;

/**
 * Represents the different roles a player can have in a guild.
 */
public enum GuildRole {
    /**
     * Owner: the creator of the guild, can change settings and everything an admin can.
     */
    OWNER,

    /**
     * Admin: can claim plots, invite others.
     */
    ADMIN,

    /**
     * Mod: can build/destroy.
     */
    MOD,

    /**
     * User: can access doors and chests.
     */
    USER;

    /**
     * Checks if this role can change guild settings.
     *
     * @return true if this role can change guild settings
     */
    public boolean canChangeSettings() {
        return this == OWNER;
    }

    /**
     * Checks if this role can claim or unclaim plots.
     *
     * @return true if this role can claim or unclaim plots
     */
    public boolean canManagePlots() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * Checks if this role can invite others to the guild.
     *
     * @return true if this role can invite others
     */
    public boolean canInvite() {
        return this == OWNER || this == ADMIN;
    }

    /**
     * Checks if this role can build or destroy blocks in guild territory.
     *
     * @return true if this role can build or destroy
     */
    public boolean canBuild() {
        return this == OWNER || this == ADMIN || this == MOD;
    }

    /**
     * Checks if this role can access doors and chests in guild territory.
     *
     * @return true if this role can access doors and chests
     */
    public boolean canAccess() {
        return this == OWNER || this == ADMIN || this == MOD || this == USER;
    }

    /**
     * Checks if this role can manage other members' roles.
     * Only the owner can manage roles.
     *
     * @return true if this role can manage roles
     */
    public boolean canManageRoles() {
        return this == OWNER;
    }
}