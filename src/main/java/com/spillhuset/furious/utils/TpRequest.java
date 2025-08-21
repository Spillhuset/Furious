package com.spillhuset.furious.utils;

import java.util.UUID;

/**
 * Data holder representing a teleport request from one player to another,
 * with an absolute expiration timestamp in milliseconds.
 */
public class TpRequest {
    public final UUID sender;  // playerA who wants to teleport
    public final UUID target;  // playerB who will receive sender
    public final long expireAtMs;

    public TpRequest(UUID sender, UUID target, long expireAtMs) {
        this.sender = sender;
        this.target = target;
        this.expireAtMs = expireAtMs;
    }
}
