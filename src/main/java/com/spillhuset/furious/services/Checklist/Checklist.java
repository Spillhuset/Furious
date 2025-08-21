package com.spillhuset.furious.services.Checklist;

import java.util.UUID;

/**
 * Minimal contract for checklist-like trackers that grant rewards.
 * Implemented by ChecklistService; concrete services provide their own state.
 */
public interface Checklist {
    /**
     * Load persistent state from backing storage.
     */
    void load();

    /**
     * Save persistent state to backing storage.
     */
    void save();

    /**
     * Returns true if the completion reward has already been granted to the player.
     */
    boolean isCompletionRewarded(UUID playerId);

    /**
     * Mark the player as having received the completion reward and persist.
     */
    void markCompletionRewarded(UUID playerId);

    /**
     * Set a unique paid marker once. Returns true if it was newly marked, false if it already existed.
     */
    boolean markPaidOnce(String marker);

    /**
     * Grants the completion reward to the player and persists the rewarded flag.
     * The amount and checklist name are passed by the caller.
     */
    void grantCompletionReward(UUID playerId, double amount, String checklistName);

    /**
     * Clears any server/year/month "first" tracking data.
     * Default is no-op; checklist services that track "firsts" should override.
     */
    default void clearAllFirsts() {
        // default no-op
    }
}
