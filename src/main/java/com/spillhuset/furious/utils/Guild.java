package com.spillhuset.furious.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Guild {
    private final UUID uuid;
    private String name;
    private GuildType type;
    private UUID owner;
    private boolean open = false; // invitedOnly by default (open=false)
    private final Map<UUID, GuildRole> members = new HashMap<>();

    public Guild(UUID uuid, String name, GuildType type, UUID owner) {
        this.uuid = uuid;
        this.name = name;
        this.type = type;
        this.owner = owner;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GuildType getType() {
        return type;
    }

    public void setType(GuildType type) {
        this.type = type;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Map<UUID, GuildRole> getMembers() {
        return members;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
