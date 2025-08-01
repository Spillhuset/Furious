package com.spillhuset.furious.managers;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.enums.GuildType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages guilds in the game.
 */
public class GuildManager {
    private final Furious plugin;
    private final Map<UUID, Guild> guilds;
    private final Map<UUID, UUID> playerGuilds; // Player UUID -> Guild UUID
    private final File configFile;
    private FileConfiguration config;
    private final int MAX_GUILD_NAME_LENGTH;
    private final int MIN_GUILD_NAME_LENGTH;
    private final int MAX_PLOTS_PER_GUILD;

    // Store references to the unmanned guilds for easy access
    private Guild safeGuild;
    private Guild warGuild;
    private Guild wildGuild;

    // Store UUIDs of worlds where guilds are disabled
    private final Set<UUID> disabledWorlds;

    /**
     * Creates a new GuildManager.
     *
     * @param plugin The plugin instance
     */
    public GuildManager(Furious plugin) {
        this.plugin = plugin;
        this.guilds = new HashMap<>();
        this.playerGuilds = new HashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "guilds.yml");
        this.disabledWorlds = new HashSet<>();

        // Load configuration values
        this.MAX_GUILD_NAME_LENGTH = plugin.getConfig().getInt("guilds.max-name-length", 16);
        this.MIN_GUILD_NAME_LENGTH = plugin.getConfig().getInt("guilds.min-name-length", 3);
        this.MAX_PLOTS_PER_GUILD = plugin.getConfig().getInt("guilds.max-plots-per-guild", 16);

        // Load guild data from the appropriate storage
        if (plugin.getDatabaseManager().isUsingYaml()) {
            // Use YAML storage
            loadConfiguration();
        } else {
            // Use database storage
            loadFromDatabase();

            // Load disabled worlds from YAML (still stored in YAML for simplicity)
            loadDisabledWorldsFromYaml();
        }

        // Create unmanned guilds if they don't exist
        createUnmannedGuilds();
    }

    /**
     * Creates the unmanned guilds (S_A_F_E, WARZONE, WILDLIFE) if they don't already exist.
     * These special guilds cannot have members and are used to control chunk types:
     * - S_A_F_E: all chunks claimed to this guild will be of type SAFE
     * - WARZONE: all chunks claimed to this guild will be of type WAR
     * - WILDLIFE: for all unclaimed chunks
     */
    private void createUnmannedGuilds() {
        // Create a server UUID to use as the owner for unmanned guilds
        UUID serverUUID = new UUID(0, 0); // Special UUID for server-owned guilds

        // Check if S_A_F_E guild exists, create if not
        safeGuild = getGuildByName("S_A_F_E");
        if (safeGuild == null) {
            safeGuild = new Guild("S_A_F_E", serverUUID, GuildType.SAFE);
            guilds.put(safeGuild.getId(), safeGuild);
            plugin.getLogger().info("Created S_A_F_E zone guild");
        } else if (safeGuild.getType() != GuildType.SAFE) {
            // Ensure correct type
            safeGuild.setType(GuildType.SAFE);
        }

        // Check if WARZONE guild exists, create if not
        warGuild = getGuildByName("WARZONE");
        if (warGuild == null) {
            warGuild = new Guild("WARZONE", serverUUID, GuildType.WAR);
            guilds.put(warGuild.getId(), warGuild);
            plugin.getLogger().info("Created WARZONE guild");
        } else if (warGuild.getType() != GuildType.WAR) {
            // Ensure correct type
            warGuild.setType(GuildType.WAR);
        }

        // Check if WILDLIFE guild exists, create if not
        wildGuild = getGuildByName("WILDLIFE");
        if (wildGuild == null) {
            wildGuild = new Guild("WILDLIFE", serverUUID, GuildType.WILD);
            guilds.put(wildGuild.getId(), wildGuild);
            plugin.getLogger().info("Created WILDLIFE guild for unclaimed chunks");
        } else if (wildGuild.getType() != GuildType.WILD) {
            // Ensure correct type
            wildGuild.setType(GuildType.WILD);
        }

        // Save configuration to persist the unmanned guilds
        saveConfiguration();
    }

    /**
     * Loads guild data from the configuration file.
     */
    private void loadConfiguration() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create guilds.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load disabled worlds
        loadDisabledWorldsFromYaml();

        // Load guilds
        ConfigurationSection guildsSection = config.getConfigurationSection("guilds");
        if (guildsSection != null) {
            for (String guildIdStr : guildsSection.getKeys(false)) {
                try {
                    UUID guildId = UUID.fromString(guildIdStr);
                    ConfigurationSection guildSection = guildsSection.getConfigurationSection(guildIdStr);

                    if (guildSection != null) {
                        String name = guildSection.getString("name");
                        String ownerStr = guildSection.getString("owner");
                        if (ownerStr == null) {
                            plugin.getLogger().warning("Owner is null for guild " + guildIdStr);
                            continue;
                        }
                        UUID owner = UUID.fromString(ownerStr);

                        // Get guild type if exists, default to GUILD
                        GuildType type = GuildType.GUILD;
                        if (guildSection.contains("type")) {
                            try {
                                type = GuildType.valueOf(guildSection.getString("type"));
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid guild type in guild " + guildIdStr + ": " + guildSection.getString("type"));
                            }
                        }

                        // Create the guild
                        Guild guild = new Guild(name, owner, type);

                        // Set description if exists
                        if (guildSection.contains("description")) {
                            guild.setDescription(guildSection.getString("description"));
                        }

                        // Load members and their roles
                        ConfigurationSection membersSection = guildSection.getConfigurationSection("members");
                        if (membersSection != null) {
                            for (String memberIdStr : membersSection.getKeys(false)) {
                                try {
                                    UUID memberId = UUID.fromString(memberIdStr);
                                    String roleStr = membersSection.getString(memberIdStr);
                                    GuildRole role = GuildRole.valueOf(roleStr);

                                    if (!memberId.equals(owner)) { // Owner is already added in constructor
                                        guild.addMember(memberId, role);
                                    }
                                    playerGuilds.put(memberId, guildId);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Invalid UUID or role in guild " + guildIdStr + ": " + memberIdStr);
                                }
                            }
                        } else {
                            // Backward compatibility with old format (no roles)
                            List<String> membersList = guildSection.getStringList("members");
                            for (String memberIdStr : membersList) {
                                try {
                                    UUID memberId = UUID.fromString(memberIdStr);
                                    if (!memberId.equals(owner)) { // Owner is already added in constructor
                                        guild.addMember(memberId, GuildRole.USER); // Default to USER role
                                    }
                                    playerGuilds.put(memberId, guildId);
                                } catch (IllegalArgumentException e) {
                                    plugin.getLogger().warning("Invalid UUID in guild " + guildIdStr + ": " + memberIdStr);
                                }
                            }
                        }

                        // Load invites
                        List<String> invitesList = guildSection.getStringList("invites");
                        for (String inviteIdStr : invitesList) {
                            UUID inviteId = UUID.fromString(inviteIdStr);
                            guild.invite(inviteId);
                        }

                        // Load claimed chunks
                        if (guildSection.contains("claimed-chunks")) {
                            List<String> claimedChunksList = guildSection.getStringList("claimed-chunks");
                            for (String chunkStr : claimedChunksList) {
                                guild.claimChunkFromString(chunkStr);
                            }
                        }

                        // Load mob spawning preference
                        if (guildSection.contains("mob-spawning-enabled")) {
                            guild.setMobSpawningEnabled(guildSection.getBoolean("mob-spawning-enabled"));
                        }

                        // Load open status
                        if (guildSection.contains("open")) {
                            guild.setOpen(guildSection.getBoolean("open"));
                        }

                        // Load join requests
                        List<String> joinRequestsList = guildSection.getStringList("join-requests");
                        for (String requestIdStr : joinRequestsList) {
                            try {
                                UUID requestId = UUID.fromString(requestIdStr);
                                guild.addJoinRequest(requestId);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in join requests for guild " + guildIdStr + ": " + requestIdStr);
                            }
                        }

                        // Add guild to map
                        guilds.put(guildId, guild);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in guilds.yml: " + guildIdStr);
                }
            }
        }
    }

    /**
     * Loads disabled worlds from the YAML configuration file.
     */
    private void loadDisabledWorldsFromYaml() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create guilds.yml: " + e.getMessage());
            }
        }

        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        // Load disabled worlds
        disabledWorlds.clear();
        List<String> disabledWorldsList = config.getStringList("disabled-worlds");
        for (String worldUuidStr : disabledWorldsList) {
            try {
                UUID worldUuid = UUID.fromString(worldUuidStr);
                disabledWorlds.add(worldUuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid world UUID in guilds.yml: " + worldUuidStr);
            }
        }
    }

    /**
     * Loads guild data from the database.
     */
    private void loadFromDatabase() {
        // Clear existing data
        guilds.clear();
        playerGuilds.clear();

        // Load guilds from database
        Map<UUID, Guild> loadedGuilds = plugin.getGuildDAO().loadAllGuilds();

        // Add guilds to our maps
        for (Map.Entry<UUID, Guild> entry : loadedGuilds.entrySet()) {
            UUID guildId = entry.getKey();
            Guild guild = entry.getValue();

            // Add guild to guilds map
            guilds.put(guildId, guild);

            // Add player-guild associations
            for (UUID memberId : guild.getMembers()) {
                playerGuilds.put(memberId, guildId);
            }
        }

        plugin.getLogger().info("Loaded " + guilds.size() + " guilds from database.");
    }

    /**
     * Saves guild data to the appropriate storage.
     */
    private void saveConfiguration() {
        if (plugin.getDatabaseManager().isUsingYaml()) {
            // Save to YAML
            saveToYaml();
        } else {
            // Save to database
            saveToDatabase();

            // Still save disabled worlds to YAML (for simplicity)
            saveDisabledWorldsToYaml();
        }
    }

    /**
     * Saves guild data to the YAML configuration file.
     */
    private void saveToYaml() {
        // Clear existing data
        config.set("guilds", null);

        // Save disabled worlds
        List<String> disabledWorldsList = new ArrayList<>();
        for (UUID worldUuid : disabledWorlds) {
            disabledWorldsList.add(worldUuid.toString());
        }
        config.set("disabled-worlds", disabledWorldsList);

        // Save guilds
        for (Map.Entry<UUID, Guild> entry : guilds.entrySet()) {
            UUID guildId = entry.getKey();
            Guild guild = entry.getValue();

            String guildPath = "guilds." + guildId.toString();

            config.set(guildPath + ".name", guild.getName());
            config.set(guildPath + ".owner", guild.getOwner().toString());
            config.set(guildPath + ".description", guild.getDescription());
            config.set(guildPath + ".type", guild.getType().name());

            // Save members with their roles
            ConfigurationSection membersSection = config.createSection(guildPath + ".members");
            for (UUID memberId : guild.getMembers()) {
                GuildRole role = guild.getMemberRole(memberId);
                membersSection.set(memberId.toString(), role.name());
            }

            // Save invites
            List<String> invitesList = new ArrayList<>();
            for (UUID inviteId : guild.getInvites()) {
                invitesList.add(inviteId.toString());
            }
            config.set(guildPath + ".invites", invitesList);

            // Save claimed chunks
            List<String> claimedChunksList = new ArrayList<>(guild.getClaimedChunks());
            config.set(guildPath + ".claimed-chunks", claimedChunksList);

            // Save mob spawning preference
            config.set(guildPath + ".mob-spawning-enabled", guild.isMobSpawningEnabled());

            // Save open status
            config.set(guildPath + ".open", guild.isOpen());

            // Save join requests
            List<String> joinRequestsList = new ArrayList<>();
            for (UUID requestId : guild.getJoinRequests()) {
                joinRequestsList.add(requestId.toString());
            }
            config.set(guildPath + ".join-requests", joinRequestsList);
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save guilds.yml: " + e.getMessage());
        }
    }

    /**
     * Saves only the disabled worlds to the YAML configuration file.
     * Used when the main storage is database but we still want to keep
     * disabled worlds in YAML for simplicity.
     */
    private void saveDisabledWorldsToYaml() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }

        // Save disabled worlds
        List<String> disabledWorldsList = new ArrayList<>();
        for (UUID worldUuid : disabledWorlds) {
            disabledWorldsList.add(worldUuid.toString());
        }
        config.set("disabled-worlds", disabledWorldsList);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save guilds.yml: " + e.getMessage());
        }
    }

    /**
     * Saves guild data to the database.
     */
    private void saveToDatabase() {
        for (Map.Entry<UUID, Guild> entry : guilds.entrySet()) {
            UUID guildId = entry.getKey();
            Guild guild = entry.getValue();

            // Save guild to database
            boolean success = plugin.getGuildDAO().saveGuild(guild);
            if (!success) {
                plugin.getLogger().warning("Failed to save guild " + guild.getName() + " to database.");
            }
        }
    }

    /**
     * Creates a new guild.
     *
     * @param name  The name of the guild
     * @param owner The player who will own the guild
     * @return The created guild, or null if creation failed
     */
    public Guild createGuild(String name, Player owner) {
        // Check if player is an op
        if (owner.isOp()) {
            owner.sendMessage(Component.text("Ops cannot create or be part of guilds!", NamedTextColor.RED));
            return null;
        }

        // Check if name is valid
        if (name.length() < MIN_GUILD_NAME_LENGTH || name.length() > MAX_GUILD_NAME_LENGTH) {
            owner.sendMessage(Component.text("Guild name must be between " + MIN_GUILD_NAME_LENGTH +
                    " and " + MAX_GUILD_NAME_LENGTH + " characters!", NamedTextColor.RED));
            return null;
        }

        // Check if name contains invalid characters
        if (!name.matches("[a-zA-Z0-9_]+")) {
            owner.sendMessage(Component.text("Guild name can only contain letters, numbers, and underscores!",
                    NamedTextColor.RED));
            return null;
        }

        // Check if name is already taken
        for (Guild guild : guilds.values()) {
            if (guild.getName().equalsIgnoreCase(name)) {
                owner.sendMessage(Component.text("A guild with that name already exists!", NamedTextColor.RED));
                return null;
            }
        }

        // Check if player is already in a guild
        if (playerGuilds.containsKey(owner.getUniqueId())) {
            owner.sendMessage(Component.text("You are already in a guild! Leave your current guild first.",
                    NamedTextColor.RED));
            return null;
        }

        // Create the guild (explicitly set type to GUILD for clarity)
        Guild guild = new Guild(name, owner.getUniqueId(), GuildType.GUILD);
        guilds.put(guild.getId(), guild);
        playerGuilds.put(owner.getUniqueId(), guild.getId());

        // Save configuration
        saveConfiguration();

        return guild;
    }

    /**
     * Disbands a guild.
     *
     * @param guild The guild to disband
     */
    public void disbandGuild(Guild guild) {
        // Remove all player associations
        for (UUID memberId : guild.getMembers()) {
            playerGuilds.remove(memberId);
        }

        // Remove the guild
        guilds.remove(guild.getId());

        // Save configuration
        saveConfiguration();
    }

    /**
     * Gets a guild by its ID.
     *
     * @param guildId The ID of the guild
     * @return The guild, or null if not found
     */
    public Guild getGuild(UUID guildId) {
        return guilds.get(guildId);
    }

    /**
     * Gets a guild by its name.
     *
     * @param name The name of the guild
     * @return The guild, or null if not found
     */
    public Guild getGuildByName(String name) {
        for (Guild guild : guilds.values()) {
            if (guild.getName().equalsIgnoreCase(name)) {
                return guild;
            }
        }
        return null;
    }

    /**
     * Gets the guild a player is in.
     *
     * @param playerId The ID of the player
     * @return The guild, or null if the player is not in a guild
     */
    public Guild getPlayerGuild(UUID playerId) {
        // Ops cannot be in guilds
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOp()) {
            return null;
        }

        UUID guildId = playerGuilds.get(playerId);
        if (guildId != null) {
            return guilds.get(guildId);
        }
        return null;
    }

    /**
     * Checks if a player is in a guild.
     *
     * @param playerId The ID of the player
     * @return true if the player is in a guild, false otherwise
     */
    public boolean isInGuild(UUID playerId) {
        // Ops cannot be in guilds
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOp()) {
            return false;
        }

        return playerGuilds.containsKey(playerId);
    }

    /**
     * Adds a player to a guild.
     *
     * @param guild    The guild to add the player to
     * @param playerId The ID of the player to add
     * @return true if the player was added, false otherwise
     */
    public boolean addPlayerToGuild(Guild guild, UUID playerId) {
        // Check if player is an op
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOp()) {
            if (player.isOnline()) {
                player.sendMessage(Component.text("Ops cannot be part of guilds!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is already in a guild
        if (playerGuilds.containsKey(playerId)) {
            return false;
        }

        // Add player to guild
        if (guild.addMember(playerId)) {
            playerGuilds.put(playerId, guild.getId());
            saveConfiguration();
            return true;
        }

        return false;
    }

    /**
     * Removes a player from their guild.
     *
     * @param playerId The ID of the player to remove
     * @return true if the player was removed, false otherwise
     */
    public boolean removePlayerFromGuild(UUID playerId) {
        Guild guild = getPlayerGuild(playerId);
        if (guild == null) {
            return false;
        }

        // Check if player is the owner
        if (guild.getOwner().equals(playerId)) {
            // Can't remove the owner
            return false;
        }

        // Remove player from guild
        if (guild.removeMember(playerId)) {
            playerGuilds.remove(playerId);
            saveConfiguration();
            return true;
        }

        return false;
    }

    /**
     * Invites a player to a guild.
     *
     * @param guild    The guild to invite the player to
     * @param playerId The ID of the player to invite
     * @return true if the invitation was sent, false otherwise
     */
    public boolean invitePlayerToGuild(Guild guild, UUID playerId) {
        // Check if player is an op
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOp()) {
            // Notify the guild member who tried to invite the op
            Player inviter = Bukkit.getPlayer(guild.getOwner());
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(Component.text("Ops cannot be invited to guilds!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is already in a guild
        if (playerGuilds.containsKey(playerId)) {
            return false;
        }

        // Invite player to guild
        if (guild.invite(playerId)) {
            saveConfiguration();
            return true;
        }

        return false;
    }

    /**
     * Removes an invitation for a player.
     *
     * @param guild    The guild to remove the invitation from
     * @param playerId The ID of the player whose invitation to remove
     * @return true if the invitation was removed, false otherwise
     */
    public boolean removeInvitation(Guild guild, UUID playerId) {
        if (guild.removeInvite(playerId)) {
            saveConfiguration();
            return true;
        }

        return false;
    }

    /**
     * Gets all guilds.
     *
     * @return A collection of all guilds
     */
    public Collection<Guild> getAllGuilds() {
        return guilds.values();
    }

    /**
     * Gets the number of guilds.
     *
     * @return The number of guilds
     */
    public int getGuildCount() {
        return guilds.size();
    }

    /**
     * Transfers ownership of a guild to another player.
     * The previous owner will be set to ADMIN role.
     *
     * @param guild    The guild to transfer ownership of
     * @param newOwner The ID of the new owner
     * @return true if ownership was transferred, false otherwise
     */
    public boolean transferOwnership(Guild guild, UUID newOwner) {
        // Check if new owner is a member
        if (!guild.isMember(newOwner)) {
            return false;
        }

        // Get current owner before transfer
        UUID previousOwner = guild.getOwner();

        // Transfer ownership
        guild.setOwner(newOwner);

        // Set previous owner to ADMIN role
        guild.setMemberRole(previousOwner, GuildRole.ADMIN);

        saveConfiguration();
        return true;
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void shutdown() {
        saveConfiguration();
        guilds.clear();
        playerGuilds.clear();
    }

    /**
     * Removes all data for a player.
     *
     * @param playerId The ID of the player to remove data for
     */
    public void removePlayerData(UUID playerId) {
        // Check if player is in a guild
        Guild guild = getPlayerGuild(playerId);
        if (guild != null) {
            // If player is the owner, disband the guild
            if (guild.getOwner().equals(playerId)) {
                disbandGuild(guild);
            } else {
                // Otherwise, just remove the player
                guild.removeMember(playerId);
                playerGuilds.remove(playerId);
            }
        }

        // Remove any invitations
        for (Guild g : guilds.values()) {
            g.removeInvite(playerId);
        }

        saveConfiguration();
    }

    /**
     * Formats a chunk as a string for storage and lookup.
     *
     * @param chunk The chunk to format
     * @return A string representation of the chunk
     */
    private String formatChunk(Chunk chunk) {
        return chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Checks if a chunk is claimed by any guild.
     *
     * @param chunk The chunk to check
     * @return true if the chunk is claimed by any guild, false otherwise
     */
    public boolean isChunkClaimed(Chunk chunk) {
        String chunkStr = formatChunk(chunk);
        for (Guild guild : guilds.values()) {
            if (guild.getClaimedChunks().contains(chunkStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the guild that has claimed a chunk.
     *
     * @param chunk The chunk to check
     * @return The guild that has claimed the chunk, or null if the chunk is not claimed
     */
    public Guild getChunkOwner(Chunk chunk) {
        String chunkStr = formatChunk(chunk);
        for (Guild guild : guilds.values()) {
            if (guild.getClaimedChunks().contains(chunkStr)) {
                return guild;
            }
        }
        return null;
    }

    /**
     * Claims a chunk for a guild.
     *
     * @param guild  The guild claiming the chunk
     * @param chunk  The chunk to claim
     * @param player The player attempting to claim the chunk (for messaging)
     */
    public void claimChunk(Guild guild, Chunk chunk, Player player) {
        // Check if the chunk is already claimed
        if (isChunkClaimed(chunk)) {
            Guild owner = getChunkOwner(chunk);
            if (owner == guild) {
                player.sendMessage(Component.text("This chunk is already claimed by your guild!", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("This chunk is already claimed by " + owner.getName() + "!", NamedTextColor.RED));
            }
            return;
        }

        // Check if the guild has reached the maximum number of plots
        if (guild.getClaimedChunkCount() >= MAX_PLOTS_PER_GUILD) {
            player.sendMessage(Component.text("Your guild has reached the maximum number of plots (" + MAX_PLOTS_PER_GUILD + ")!", NamedTextColor.RED));
            return;
        }

        // Claim the chunk
        if (guild.claimChunk(chunk)) {
            saveConfiguration();
            player.sendMessage(Component.text("Chunk claimed for " + guild.getName() + "!", NamedTextColor.GREEN));
        }

    }

    /**
     * Unclaims a chunk from a guild.
     *
     * @param guild  The guild unclaiming the chunk
     * @param chunk  The chunk to unclaim
     * @param player The player attempting to unclaim the chunk (for messaging)
     */
    public void unclaimChunk(Guild guild, Chunk chunk, Player player) {
        // Check if the chunk is claimed by the guild
        if (guild.isChunkUnClaimed(chunk)) {
            player.sendMessage(Component.text("This chunk is not claimed by your guild!", NamedTextColor.RED));
            return;
        }

        // Unclaim the chunk
        if (guild.unclaimChunk(chunk)) {
            saveConfiguration();
            player.sendMessage(Component.text("Chunk unclaimed from " + guild.getName() + "!", NamedTextColor.GREEN));
        }

    }

    /**
     * Toggles mob spawning for a guild.
     *
     * @param guild  The guild to toggle mob spawning for
     * @param player The player toggling mob spawning (for messaging)
     */
    public void toggleMobSpawning(Guild guild, Player player) {
        boolean newState = guild.toggleMobSpawning();
        saveConfiguration();

        if (newState) {
            player.sendMessage(Component.text("Mob spawning is now ENABLED in " + guild.getName() + "'s claimed chunks!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Mob spawning is now DISABLED in " + guild.getName() + "'s claimed chunks!", NamedTextColor.RED));
        }

    }

    /**
     * Checks if mob spawning is allowed in a specific chunk.
     *
     * @param chunk The chunk to check
     * @return true if mob spawning is allowed, false otherwise
     */
    public boolean isMobSpawningAllowed(Chunk chunk) {
        Guild guild = getChunkOwner(chunk);

        // If the chunk is not claimed by any guild, allow mob spawning
        if (guild == null) {
            return true;
        }

        // Return the guild's mob spawning preference
        return guild.isMobSpawningEnabled();
    }

    /**
     * Gets the SAFE zone guild.
     *
     * @return The SAFE zone guild
     */
    public Guild getSafeGuild() {
        return safeGuild;
    }

    /**
     * Gets the WAR zone guild.
     *
     * @return The WAR zone guild
     */
    public Guild getWarGuild() {
        return warGuild;
    }

    /**
     * Gets the WILD zone guild.
     *
     * @return The WILD zone guild
     */
    public Guild getWildGuild() {
        return wildGuild;
    }

    /**
     * Checks if a guild is an unmanned guild.
     *
     * @param guild The guild to check
     * @return true if the guild is an unmanned guild (SAFE, WAR, WILD), false otherwise
     */
    public boolean isUnmannedGuild(Guild guild) {
        return guild != null && guild.isUnmanned();
    }

    /**
     * Changes a member's role in a guild.
     *
     * @param guild The guild to change the role in
     * @param playerId The UUID of the player whose role to change
     * @param role The new role for the player
     * @param changer The player making the change (for permission checking)
     * @return true if the role was changed, false otherwise
     */
    public boolean changeMemberRole(Guild guild, UUID playerId, GuildRole role, Player changer) {
        // Check if the changer is the guild owner
        if (!guild.getOwner().equals(changer.getUniqueId())) {
            changer.sendMessage(Component.text("Only the guild owner can change member roles!", NamedTextColor.RED));
            return false;
        }

        // Check if the player is a member of the guild
        if (!guild.isMember(playerId)) {
            changer.sendMessage(Component.text("That player is not a member of your guild!", NamedTextColor.RED));
            return false;
        }

        // Check if the player is the owner (can't change owner's role)
        if (playerId.equals(guild.getOwner())) {
            changer.sendMessage(Component.text("You cannot change the owner's role!", NamedTextColor.RED));
            return false;
        }

        // Change the role
        if (guild.setMemberRole(playerId, role)) {
            // Get the player's name
            Player player = Bukkit.getPlayer(playerId);
            String playerName;
            if (player != null) {
                playerName = player.getName();
            } else {
                // Use UUID string representation for offline players to avoid blocking network calls
                playerName = playerId.toString();
            }

            // Notify the changer
            changer.sendMessage(Component.text("Changed " + playerName + "'s role to " + role.name(), NamedTextColor.GREEN));

            // Notify the player if they're online
            Player targetPlayer = Bukkit.getPlayer(playerId);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(Component.text("Your role in " + guild.getName() + " has been changed to " + role.name(), NamedTextColor.GREEN));
            }

            // Save the configuration
            saveConfiguration();
            return true;
        }

        return false;
    }

    /**
     * Checks if guilds are enabled in the specified world.
     *
     * @param world The world to check
     * @return true if guilds are enabled in the world, false otherwise
     */
    public boolean isWorldEnabled(World world) {
        return world != null && !disabledWorlds.contains(world.getUID());
    }

    /**
     * Enables guilds in the specified world.
     *
     * @param world The world to enable guilds in
     * @return true if the operation was successful, false otherwise
     */
    public boolean enableWorld(World world) {
        if (world == null) {
            return false;
        }

        boolean removed = disabledWorlds.remove(world.getUID());
        if (removed) {
            saveConfiguration();
        }
        return true;
    }

    /**
     * Disables guilds in the specified world.
     *
     * @param world The world to disable guilds in
     * @return true if the operation was successful, false otherwise
     */
    public boolean disableWorld(World world) {
        if (world == null) {
            return false;
        }

        boolean added = disabledWorlds.add(world.getUID());
        if (added) {
            saveConfiguration();
        }
        return true;
    }

    /**
     * Gets a list of all worlds and whether guilds are enabled in them.
     *
     * @return A map of world names to boolean values indicating if guilds are enabled
     */
    public Map<String, Boolean> getWorldsStatus() {
        Map<String, Boolean> worldsStatus = new HashMap<>();

        for (World world : plugin.getServer().getWorlds()) {
            // Skip game worlds
            if (world.getName().equals(plugin.getWorldManager().getGameWorldName()) ||
                world.getName().equals(plugin.getWorldManager().getGameBackupName()) ||
                world.getName().startsWith("minigame_")) {
                continue;
            }

            worldsStatus.put(world.getName(), isWorldEnabled(world));
        }

        return worldsStatus;
    }

    /**
     * Sets whether a guild is open for anyone to join without invitation.
     *
     * @param guild  The guild to modify
     * @param open   true to make the guild open, false otherwise
     * @param player The player making the change (for messaging)
     */
    public void setGuildOpen(Guild guild, boolean open, Player player) {
        guild.setOpen(open);
        saveConfiguration();

        if (open) {
            player.sendMessage(Component.text("Guild is now open for anyone to join!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Guild is now closed. Players need an invitation to join.", NamedTextColor.YELLOW));
        }

    }

    /**
     * Adds a join request from a player to a guild.
     *
     * @param guild The guild to request to join
     * @param playerId The UUID of the player requesting to join
     * @return true if the request was added, false otherwise
     */
    public boolean addJoinRequest(Guild guild, UUID playerId) {
        // Check if player is already in a guild
        if (playerGuilds.containsKey(playerId)) {
            return false;
        }

        // Add join request
        if (guild.addJoinRequest(playerId)) {
            saveConfiguration();

            // Notify guild admins and owner
            for (UUID memberId : guild.getMembers()) {
                GuildRole role = guild.getMemberRole(memberId);
                if (role == GuildRole.OWNER || role == GuildRole.ADMIN) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.isOnline()) {
                        Player requestingPlayer = Bukkit.getPlayer(playerId);
                        String playerName;
                        if (requestingPlayer != null) {
                            playerName = requestingPlayer.getName();
                        } else {
                            // Use UUID string representation for offline players to avoid blocking network calls
                            playerName = playerId.toString();
                        }
                        member.sendMessage(Component.text(playerName + " has requested to join your guild. " +
                                "Type /guild accept " + playerName + " to accept or /guild decline " + playerName + " to decline.",
                                NamedTextColor.YELLOW));
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Accepts a join request from a player.
     *
     * @param guild The guild to accept the request for
     * @param playerId The UUID of the player whose request to accept
     * @param acceptor The player accepting the request (for messaging)
     * @return true if the request was accepted, false otherwise
     */
    public boolean acceptJoinRequest(Guild guild, UUID playerId, Player acceptor) {
        // Check if the player has a join request
        if (!guild.hasJoinRequest(playerId)) {
            acceptor.sendMessage(Component.text("That player has not requested to join your guild!", NamedTextColor.RED));
            return false;
        }

        // Check if player is already in a guild
        if (playerGuilds.containsKey(playerId)) {
            guild.removeJoinRequest(playerId);
            saveConfiguration();
            acceptor.sendMessage(Component.text("That player is already in a guild!", NamedTextColor.RED));
            return false;
        }

        // Remove the request and add the player to the guild
        guild.removeJoinRequest(playerId);
        if (guild.addMember(playerId)) {
            playerGuilds.put(playerId, guild.getId());
            saveConfiguration();

            // Get the player's name
            Player joinedPlayer = Bukkit.getPlayer(playerId);
            String playerName;
            if (joinedPlayer != null) {
                playerName = joinedPlayer.getName();
            } else {
                // Use UUID string representation for offline players to avoid blocking network calls
                playerName = playerId.toString();
            }

            // Notify the acceptor
            acceptor.sendMessage(Component.text("You have accepted " + playerName + " into your guild!", NamedTextColor.GREEN));

            // Notify the player if they're online
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("Your request to join " + guild.getName() + " has been accepted!", NamedTextColor.GREEN));
            }

            return true;
        }

        return false;
    }

    /**
     * Declines a join request from a player.
     *
     * @param guild The guild to decline the request for
     * @param playerId The UUID of the player whose request to decline
     * @param decliner The player declining the request (for messaging)
     * @return true if the request was declined, false otherwise
     */
    public boolean declineJoinRequest(Guild guild, UUID playerId, Player decliner) {
        // Check if the player has a join request
        if (!guild.hasJoinRequest(playerId)) {
            decliner.sendMessage(Component.text("That player has not requested to join your guild!", NamedTextColor.RED));
            return false;
        }

        // Remove the request
        guild.removeJoinRequest(playerId);
        saveConfiguration();

        // Get the player's name
        Player declinedPlayer = Bukkit.getPlayer(playerId);
        String playerName;
        if (declinedPlayer != null) {
            playerName = declinedPlayer.getName();
        } else {
            // Use UUID string representation for offline players to avoid blocking network calls
            playerName = playerId.toString();
        }

        // Notify the decliner
        decliner.sendMessage(Component.text("You have declined " + playerName + "'s request to join your guild.", NamedTextColor.YELLOW));

        // Notify the player if they're online
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(Component.text("Your request to join " + guild.getName() + " has been declined.", NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Admin function: Claims a chunk for an unmanned guild.
     *
     * @param guild The unmanned guild to claim for
     * @param chunk The chunk to claim
     * @param admin The admin performing the action
     */
    public void adminClaimChunk(Guild guild, Chunk chunk, Player admin) {
        // Check if the guild is unmanned
        if (!isUnmannedGuild(guild)) {
            admin.sendMessage(Component.text("You can only claim chunks for unmanned guilds with admin permissions!", NamedTextColor.RED));
            return;
        }

        // Check if the chunk is already claimed
        if (isChunkClaimed(chunk)) {
            Guild owner = getChunkOwner(chunk);
            admin.sendMessage(Component.text("This chunk is already claimed by " + owner.getName() + "!", NamedTextColor.RED));
            return;
        }

        // Claim the chunk
        if (guild.claimChunk(chunk)) {
            saveConfiguration();
            admin.sendMessage(Component.text("Chunk claimed for " + guild.getName() + "!", NamedTextColor.GREEN));
        }

    }

    /**
     * Admin function: Unclaims a chunk from an unmanned guild.
     *
     * @param guild The unmanned guild to unclaim from
     * @param chunk The chunk to unclaim
     * @param admin The admin performing the action
     * @return true if the chunk was unclaimed, false otherwise
     */
    public boolean adminUnclaimChunk(Guild guild, Chunk chunk, Player admin) {
        // Check if the guild is unmanned
        if (!isUnmannedGuild(guild)) {
            admin.sendMessage(Component.text("You can only unclaim chunks from unmanned guilds with admin permissions!", NamedTextColor.RED));
            return false;
        }

        // Check if the chunk is claimed by the guild
        if (guild.isChunkUnClaimed(chunk)) {
            admin.sendMessage(Component.text("This chunk is not claimed by " + guild.getName() + "!", NamedTextColor.RED));
            return false;
        }

        // Unclaim the chunk
        if (guild.unclaimChunk(chunk)) {
            saveConfiguration();
            admin.sendMessage(Component.text("Chunk unclaimed from " + guild.getName() + "!", NamedTextColor.GREEN));
            return true;
        }

        return false;
    }

    /**
     * Admin function: Transfers ownership of a guild to another player.
     *
     * @param guild The guild to transfer
     * @param newOwner The UUID of the new owner
     * @param admin The admin performing the action
     * @return true if ownership was transferred, false otherwise
     */
    public boolean adminTransferGuild(Guild guild, UUID newOwner, Player admin) {
        // Check if new owner is already in a guild
        Guild playerGuild = getPlayerGuild(newOwner);
        if (playerGuild != null && !playerGuild.getId().equals(guild.getId())) {
            admin.sendMessage(Component.text("That player is already in another guild!", NamedTextColor.RED));
            return false;
        }

        // If new owner is not in the guild, add them
        if (!guild.isMember(newOwner)) {
            guild.addMember(newOwner, GuildRole.USER);
        }

        // Get the old owner's role
        UUID oldOwner = guild.getOwner();

        // Transfer ownership
        guild.setOwner(newOwner);

        // Set the old owner's role to ADMIN if they're still in the guild
        if (guild.isMember(oldOwner) && !oldOwner.equals(newOwner)) {
            guild.setMemberRole(oldOwner, GuildRole.ADMIN);
        }

        // Save configuration
        saveConfiguration();

        // Get player names
        Player newOwnerPlayer = Bukkit.getPlayer(newOwner);
        String newOwnerName;
        if (newOwnerPlayer != null) {
            newOwnerName = newOwnerPlayer.getName();
        } else {
            // Use UUID string representation for offline players to avoid blocking network calls
            newOwnerName = newOwner.toString();
        }

        // Notify the admin
        admin.sendMessage(Component.text("Transferred ownership of " + guild.getName() + " to " + newOwnerName + "!", NamedTextColor.GREEN));

        // Notify the new owner if they're online
        if (newOwnerPlayer != null && newOwnerPlayer.isOnline()) {
            newOwnerPlayer.sendMessage(Component.text("You are now the owner of " + guild.getName() + "!", NamedTextColor.GREEN));
        }

        return true;
    }

    /**
     * Admin function: Disbands a guild.
     *
     * @param guild The guild to disband
     * @param admin The admin performing the action
     * @return true if the guild was disbanded, false otherwise
     */
    public boolean adminDisbandGuild(Guild guild, Player admin) {
        // Check if the guild is unmanned
        if (isUnmannedGuild(guild)) {
            admin.sendMessage(Component.text("You cannot disband unmanned guilds!", NamedTextColor.RED));
            return false;
        }

        // Notify all online members
        for (UUID memberId : guild.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(Component.text("Your guild " + guild.getName() + " has been disbanded by an admin!", NamedTextColor.RED));
            }
        }

        // Disband the guild
        disbandGuild(guild);

        // Notify the admin
        admin.sendMessage(Component.text("Guild " + guild.getName() + " has been disbanded!", NamedTextColor.GREEN));

        return true;
    }

    /**
     * Declines an invitation to join a guild.
     *
     * @param guild The guild whose invitation to decline
     * @param playerId The UUID of the player declining the invitation
     * @param player The player object for messaging
     * @return true if the invitation was declined, false otherwise
     */
    public boolean declineInvitation(Guild guild, UUID playerId, Player player) {
        // Check if the player was invited
        if (!guild.isInvited(playerId)) {
            player.sendMessage(Component.text("You have not been invited to this guild!", NamedTextColor.RED));
            return false;
        }

        // Remove the invitation
        if (guild.removeInvite(playerId)) {
            saveConfiguration();
            player.sendMessage(Component.text("You have declined the invitation to join " + guild.getName() + ".", NamedTextColor.YELLOW));

            // Notify the guild owner if they're online
            Player owner = Bukkit.getPlayer(guild.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(Component.text(player.getName() + " has declined the invitation to join your guild.", NamedTextColor.YELLOW));
            }

            return true;
        }

        return false;
    }

    /**
     * Sets whether mobs can spawn in a guild's claimed chunks.
     *
     * @param guild   The guild to modify
     * @param enabled true to allow mob spawning, false to deny it
     * @param player  The player making the change (for messaging)
     */
    public void setGuildMobSpawning(Guild guild, boolean enabled, Player player) {
        guild.setMobSpawningEnabled(enabled);
        saveConfiguration();

        if (enabled) {
            player.sendMessage(Component.text("Mob spawning is now ALLOWED in your guild's claimed chunks!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Mob spawning is now DENIED in your guild's claimed chunks!", NamedTextColor.RED));
        }

    }

    /**
     * Admin function: Kicks a player from a guild.
     *
     * @param guild The guild to kick from
     * @param playerId The UUID of the player to kick
     * @param admin The admin performing the action
     * @return true if the player was kicked, false otherwise
     */
    public boolean adminKickPlayer(Guild guild, UUID playerId, Player admin) {
        // Check if the player is in the guild
        if (!guild.isMember(playerId)) {
            admin.sendMessage(Component.text("That player is not a member of " + guild.getName() + "!", NamedTextColor.RED));
            return false;
        }

        // Check if the player is the owner
        if (playerId.equals(guild.getOwner())) {
            admin.sendMessage(Component.text("You cannot kick the guild owner! Transfer ownership first.", NamedTextColor.RED));
            return false;
        }

        // Get the player's name
        Player kickedPlayer = Bukkit.getPlayer(playerId);
        String playerName;
        if (kickedPlayer != null) {
            playerName = kickedPlayer.getName();
        } else {
            // Use UUID string representation for offline players to avoid blocking network calls
            playerName = playerId.toString();
        }

        // Remove the player from the guild
        if (guild.removeMember(playerId)) {
            playerGuilds.remove(playerId);
            saveConfiguration();

            // Notify the admin
            admin.sendMessage(Component.text("Kicked " + playerName + " from " + guild.getName() + "!", NamedTextColor.GREEN));

            // Notify the player if they're online
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("You have been kicked from " + guild.getName() + " by an admin!", NamedTextColor.RED));
            }

            return true;
        }

        return false;
    }
}
