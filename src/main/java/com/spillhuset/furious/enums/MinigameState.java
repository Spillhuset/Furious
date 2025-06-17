package com.spillhuset.furious.enums;

/**
 * Enum representing different states of a minigame
 */
public enum MinigameState {
    DISABLED("Disabled", "Disabled (Noone can join)"),
    READY("Ready", "Game is ready to be joined"),
    QUEUE("Queue", "Someone has joined the queue, waiting to reach minimum players"),
    COUNTDOWN("Countdown", "Minimum number of players reached, final countdown till game starts"),
    STARTED("Started", "Game has started, players are playing"),
    FINAL("Final", "Game is in its final stage");

    private final String name;
    private final String description;

    MinigameState(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the name of the state
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the description of the state
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
}