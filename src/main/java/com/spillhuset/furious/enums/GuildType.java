package com.spillhuset.furious.enums;

/**
 * Represents the different types of guilds in the game.
 */
public enum GuildType {
    /**
     * SAFE: A safe place where normal players cannot create or destroy.
     * This is an unmanned guild type.
     */
    SAFE,

    /**
     * WAR: An unsafe place where normal players cannot create or destroy.
     * This is an unmanned guild type.
     */
    WAR,

    /**
     * WILD: Represents areas with no zone.
     * This is an unmanned guild type.
     */
    WILD,

    /**
     * GUILD: A safe place for members of the corresponding guild.
     * This is a player-owned guild type.
     */
    GUILD;

    /**
     * Checks if this guild type is an unmanned guild.
     *
     * @return true if this is an unmanned guild type (SAFE, WAR, WILD)
     */
    public boolean isUnmanned() {
        return this == SAFE || this == WAR || this == WILD;
    }

    /**
     * Checks if this guild type is a safe zone.
     *
     * @return true if this is a safe zone (SAFE or GUILD)
     */
    public boolean isSafe() {
        return this == SAFE || this == GUILD;
    }

    /**
     * Checks if this guild type allows normal players to create or destroy.
     *
     * @return true if normal players can create or destroy in this zone
     */
    public boolean allowsNormalPlayerBuild() {
        return this == WILD;
    }
}