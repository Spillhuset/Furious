package com.spillhuset.furious.enums;

/**
 * Enum representing different types of minigames
 */
public enum MinigameType {
    HUNGERGAME("hungergame", "Hunger Games"),
    SPLEEF("spleef", "Spleef"),
    ZOMBIESURVIVAL("zombiesurvival", "Zombie Survival");

    private final String id;
    private final String displayName;

    MinigameType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Gets the ID of the minigame type
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of the minigame type
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets a minigame type by its ID
     *
     * @param id The ID to look for
     * @return The minigame type, or null if not found
     */
    public static MinigameType getById(String id) {
        for (MinigameType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
