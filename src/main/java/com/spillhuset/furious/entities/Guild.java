package com.spillhuset.furious.entities;

import com.spillhuset.furious.enums.GuildRole;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a guild in the game.
 * A guild is a group of players that can work together.
 */
public class Guild {
    private final UUID id;
    private String name;
    private UUID owner;
    private final Map<UUID, GuildRole> members; // Player UUID -> Role
    private final Set<UUID> invites;
    private String description;
    private final Date creationDate;
    private final Set<String> claimedChunks; // Format: "worldUUID:chunkX:chunkZ"
    private boolean mobSpawningEnabled; // Whether mobs can spawn in claimed chunks

    /**
     * Creates a new guild with the given name and owner.
     *
     * @param name  The name of the guild
     * @param owner The UUID of the guild owner
     */
    public Guild(String name, UUID owner) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.owner = owner;
        this.members = new HashMap<>();
        this.invites = new HashSet<>();
        this.description = "";
        this.creationDate = new Date();
        this.claimedChunks = new HashSet<>();
        this.mobSpawningEnabled = false; // Default: mobs cannot spawn in claimed chunks

        // Add the owner as a member with OWNER role
        this.members.put(owner, GuildRole.OWNER);
    }

    /**
     * Gets the unique ID of the guild.
     *
     * @return The guild's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of the guild.
     *
     * @return The guild's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the guild.
     *
     * @param name The new name for the guild
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the UUID of the guild owner.
     *
     * @return The owner's UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Sets a new owner for the guild.
     *
     * @param owner The UUID of the new owner
     */
    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * Gets the set of member UUIDs in the guild.
     *
     * @return A set of member UUIDs
     */
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members.keySet());
    }

    /**
     * Adds a player to the guild with the USER role.
     *
     * @param playerId The UUID of the player to add
     * @return true if the player was added, false if they were already a member
     */
    public boolean addMember(UUID playerId) {
        // Remove from invites if they were invited
        invites.remove(playerId);
        if (members.containsKey(playerId)) {
            return false;
        }
        members.put(playerId, GuildRole.USER);
        return true;
    }

    /**
     * Adds a player to the guild with the specified role.
     *
     * @param playerId The UUID of the player to add
     * @param role The role to assign to the player
     * @return true if the player was added, false if they were already a member
     */
    public boolean addMember(UUID playerId, GuildRole role) {
        // Remove from invites if they were invited
        invites.remove(playerId);
        if (members.containsKey(playerId)) {
            return false;
        }
        members.put(playerId, role);
        return true;
    }

    /**
     * Removes a player from the guild.
     *
     * @param playerId The UUID of the player to remove
     * @return true if the player was removed, false if they weren't a member
     */
    public boolean removeMember(UUID playerId) {
        // Can't remove the owner
        if (playerId.equals(owner)) {
            return false;
        }
        return members.remove(playerId) != null;
    }

    /**
     * Checks if a player is a member of the guild.
     *
     * @param playerId The UUID of the player to check
     * @return true if the player is a member, false otherwise
     */
    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    /**
     * Gets the role of a guild member.
     *
     * @param playerId The UUID of the player to check
     * @return The player's role, or null if they are not a member
     */
    public GuildRole getMemberRole(UUID playerId) {
        return members.get(playerId);
    }

    /**
     * Sets the role of a guild member.
     *
     * @param playerId The UUID of the player to update
     * @param role The new role for the player
     * @return true if the role was updated, false if the player is not a member or is the owner
     */
    public boolean setMemberRole(UUID playerId, GuildRole role) {
        // Can't change the owner's role
        if (playerId.equals(owner)) {
            return false;
        }
        if (!members.containsKey(playerId)) {
            return false;
        }
        members.put(playerId, role);
        return true;
    }

    /**
     * Checks if a player has a specific role or higher in the guild.
     *
     * @param playerId The UUID of the player to check
     * @param role The minimum role required
     * @return true if the player has the required role or higher, false otherwise
     */
    public boolean hasRole(UUID playerId, GuildRole role) {
        GuildRole memberRole = getMemberRole(playerId);
        if (memberRole == null) {
            return false;
        }

        // Owner has all permissions
        if (memberRole == GuildRole.OWNER) {
            return true;
        }

        // Check role hierarchy
        switch (role) {
            case USER:
                return true; // All members are at least users
            case MOD:
                return memberRole == GuildRole.MOD || memberRole == GuildRole.ADMIN;
            case ADMIN:
                return memberRole == GuildRole.ADMIN;
            default:
                return false;
        }
    }

    /**
     * Gets the set of invited player UUIDs.
     *
     * @return A set of invited player UUIDs
     */
    public Set<UUID> getInvites() {
        return Collections.unmodifiableSet(invites);
    }

    /**
     * Invites a player to the guild.
     *
     * @param playerId The UUID of the player to invite
     * @return true if the invitation was added, false if they were already invited
     */
    public boolean invite(UUID playerId) {
        // Don't invite if they're already a member
        if (members.containsKey(playerId)) {
            return false;
        }
        return invites.add(playerId);
    }

    /**
     * Removes an invitation for a player.
     *
     * @param playerId The UUID of the player whose invitation to remove
     * @return true if the invitation was removed, false if they weren't invited
     */
    public boolean removeInvite(UUID playerId) {
        return invites.remove(playerId);
    }

    /**
     * Checks if a player is invited to the guild.
     *
     * @param playerId The UUID of the player to check
     * @return true if the player is invited, false otherwise
     */
    public boolean isInvited(UUID playerId) {
        return invites.contains(playerId);
    }

    /**
     * Gets the guild's description.
     *
     * @return The guild's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the guild's description.
     *
     * @param description The new description for the guild
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the date when the guild was created.
     *
     * @return The creation date
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Gets a list of online members in the guild.
     *
     * @return A list of online Player objects who are guild members
     */
    public List<Player> getOnlineMembers() {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID memberId : members.keySet()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                onlineMembers.add(player);
            }
        }
        return onlineMembers;
    }

    /**
     * Gets the number of members in the guild.
     *
     * @return The member count
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * Formats a chunk as a string for storage.
     *
     * @param chunk The chunk to format
     * @return A string representation of the chunk
     */
    private String formatChunk(Chunk chunk) {
        return chunk.getWorld().getUID().toString() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Claims a chunk for the guild.
     *
     * @param chunk The chunk to claim
     * @return true if the chunk was claimed, false if it was already claimed by this guild
     */
    public boolean claimChunk(Chunk chunk) {
        return claimedChunks.add(formatChunk(chunk));
    }

    /**
     * Unclaims a chunk from the guild.
     *
     * @param chunk The chunk to unclaim
     * @return true if the chunk was unclaimed, false if it wasn't claimed by this guild
     */
    public boolean unclaimChunk(Chunk chunk) {
        return claimedChunks.remove(formatChunk(chunk));
    }

    /**
     * Checks if a chunk is claimed by this guild.
     *
     * @param chunk The chunk to check
     * @return true if the chunk is claimed by this guild, false otherwise
     */
    public boolean isChunkClaimed(Chunk chunk) {
        return claimedChunks.contains(formatChunk(chunk));
    }

    /**
     * Gets all chunks claimed by this guild.
     *
     * @return A set of strings representing the claimed chunks
     */
    public Set<String> getClaimedChunks() {
        return Collections.unmodifiableSet(claimedChunks);
    }

    /**
     * Gets the number of chunks claimed by this guild.
     *
     * @return The number of claimed chunks
     */
    public int getClaimedChunkCount() {
        return claimedChunks.size();
    }

    /**
     * Checks if mob spawning is enabled in this guild's claimed chunks.
     *
     * @return true if mob spawning is enabled, false otherwise
     */
    public boolean isMobSpawningEnabled() {
        return mobSpawningEnabled;
    }

    /**
     * Sets whether mob spawning is enabled in this guild's claimed chunks.
     *
     * @param enabled true to enable mob spawning, false to disable it
     */
    public void setMobSpawningEnabled(boolean enabled) {
        this.mobSpawningEnabled = enabled;
    }

    /**
     * Toggles the mob spawning setting for this guild's claimed chunks.
     *
     * @return the new state of mob spawning (true if enabled, false if disabled)
     */
    public boolean toggleMobSpawning() {
        this.mobSpawningEnabled = !this.mobSpawningEnabled;
        return this.mobSpawningEnabled;
    }
}
