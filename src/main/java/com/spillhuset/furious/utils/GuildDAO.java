package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.enums.GuildType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Data Access Object for Guild entities.
 * Handles database operations for guilds.
 */
public class GuildDAO {
    private final Furious plugin;
    private final DatabaseManager databaseManager;

    /**
     * Creates a new GuildDAO.
     *
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public GuildDAO(Furious plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Saves a guild to the database.
     *
     * @param guild The guild to save
     * @return true if the guild was saved successfully, false otherwise
     */
    public boolean saveGuild(Guild guild) {
        if (databaseManager.isUsingYaml()) {
            return false; // YAML storage is handled by GuildManager
        }

        try (Connection connection = databaseManager.getConnection()) {
            // Begin transaction
            connection.setAutoCommit(false);

            try {
                // Save guild
                saveGuildData(connection, guild);

                // Save members
                saveGuildMembers(connection, guild);

                // Save invites
                saveGuildInvites(connection, guild);

                // Save join requests
                saveGuildJoinRequests(connection, guild);

                // Save claimed chunks
                saveGuildClaimedChunks(connection, guild);

                // Commit transaction
                connection.commit();
                return true;
            } catch (SQLException e) {
                // Rollback transaction on error
                connection.rollback();
                plugin.getLogger().log(Level.SEVERE, "Failed to save guild: " + e.getMessage(), e);
                return false;
            } finally {
                // Restore auto-commit
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get database connection: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Saves guild data to the database.
     *
     * @param connection The database connection
     * @param guild The guild to save
     * @throws SQLException If an SQL error occurs
     */
    private void saveGuildData(Connection connection, Guild guild) throws SQLException {
        String sql = "INSERT INTO guilds (id, name, owner, description, type, mob_spawning_enabled, open) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "name = ?, owner = ?, description = ?, type = ?, mob_spawning_enabled = ?, open = ?";

        // For SQLite, use different syntax for upsert
        if (databaseManager.getStorageType() == DatabaseManager.StorageType.SQLITE) {
            sql = "INSERT OR REPLACE INTO guilds (id, name, owner, description, type, mob_spawning_enabled, open) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // Set parameters for insert
            statement.setString(1, guild.getId().toString());
            statement.setString(2, guild.getName());
            statement.setString(3, guild.getOwner().toString());
            statement.setString(4, guild.getDescription());
            statement.setString(5, guild.getType().name());
            statement.setBoolean(6, guild.isMobSpawningEnabled());
            statement.setBoolean(7, guild.isOpen());

            // For MySQL/MariaDB, set parameters for update
            if (databaseManager.getStorageType() != DatabaseManager.StorageType.SQLITE) {
                statement.setString(8, guild.getName());
                statement.setString(9, guild.getOwner().toString());
                statement.setString(10, guild.getDescription());
                statement.setString(11, guild.getType().name());
                statement.setBoolean(12, guild.isMobSpawningEnabled());
                statement.setBoolean(13, guild.isOpen());
            }

            statement.executeUpdate();
        }
    }

    /**
     * Saves guild members to the database.
     *
     * @param connection The database connection
     * @param guild The guild to save members for
     * @throws SQLException If an SQL error occurs
     */
    private void saveGuildMembers(Connection connection, Guild guild) throws SQLException {
        // First, delete existing members
        String deleteSql = "DELETE FROM guild_members WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setString(1, guild.getId().toString());
            statement.executeUpdate();
        }

        // Then, insert current members
        String insertSql = "INSERT INTO guild_members (guild_id, player_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (UUID memberId : guild.getMembers()) {
                GuildRole role = guild.getMemberRole(memberId);
                statement.setString(1, guild.getId().toString());
                statement.setString(2, memberId.toString());
                statement.setString(3, role.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Saves guild invites to the database.
     *
     * @param connection The database connection
     * @param guild The guild to save invites for
     * @throws SQLException If an SQL error occurs
     */
    private void saveGuildInvites(Connection connection, Guild guild) throws SQLException {
        // First, delete existing invites
        String deleteSql = "DELETE FROM guild_invites WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setString(1, guild.getId().toString());
            statement.executeUpdate();
        }

        // Then, insert current invites
        String insertSql = "INSERT INTO guild_invites (guild_id, player_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (UUID inviteId : guild.getInvites()) {
                statement.setString(1, guild.getId().toString());
                statement.setString(2, inviteId.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Saves guild join requests to the database.
     *
     * @param connection The database connection
     * @param guild The guild to save join requests for
     * @throws SQLException If an SQL error occurs
     */
    private void saveGuildJoinRequests(Connection connection, Guild guild) throws SQLException {
        // First, delete existing join requests
        String deleteSql = "DELETE FROM guild_join_requests WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setString(1, guild.getId().toString());
            statement.executeUpdate();
        }

        // Then, insert current join requests
        String insertSql = "INSERT INTO guild_join_requests (guild_id, player_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (UUID requestId : guild.getJoinRequests()) {
                statement.setString(1, guild.getId().toString());
                statement.setString(2, requestId.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Saves guild claimed chunks to the database.
     *
     * @param connection The database connection
     * @param guild The guild to save claimed chunks for
     * @throws SQLException If an SQL error occurs
     */
    private void saveGuildClaimedChunks(Connection connection, Guild guild) throws SQLException {
        // First, delete existing claimed chunks
        String deleteSql = "DELETE FROM guild_claimed_chunks WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            statement.setString(1, guild.getId().toString());
            statement.executeUpdate();
        }

        // Then, insert current claimed chunks
        String insertSql = "INSERT INTO guild_claimed_chunks (guild_id, chunk_key) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (String chunkKey : guild.getClaimedChunks()) {
                statement.setString(1, guild.getId().toString());
                statement.setString(2, chunkKey);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * Loads all guilds from the database.
     *
     * @return A map of guild IDs to Guild objects
     */
    public Map<UUID, Guild> loadAllGuilds() {
        if (databaseManager.isUsingYaml()) {
            return new HashMap<>(); // YAML storage is handled by GuildManager
        }

        Map<UUID, Guild> guilds = new HashMap<>();

        try (Connection connection = databaseManager.getConnection()) {
            // Load guilds
            String sql = "SELECT * FROM guilds";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    UUID guildId = UUID.fromString(resultSet.getString("id"));
                    String name = resultSet.getString("name");
                    UUID owner = UUID.fromString(resultSet.getString("owner"));
                    String description = resultSet.getString("description");
                    GuildType type = GuildType.valueOf(resultSet.getString("type"));
                    boolean mobSpawningEnabled = resultSet.getBoolean("mob_spawning_enabled");
                    boolean open = resultSet.getBoolean("open");

                    // Create guild
                    Guild guild = new Guild(name, owner, type);
                    guild.setDescription(description);
                    guild.setMobSpawningEnabled(mobSpawningEnabled);
                    guild.setOpen(open);

                    guilds.put(guildId, guild);
                }
            }

            // Load members
            for (Map.Entry<UUID, Guild> entry : guilds.entrySet()) {
                UUID guildId = entry.getKey();
                Guild guild = entry.getValue();

                loadGuildMembers(connection, guildId, guild);
                loadGuildInvites(connection, guildId, guild);
                loadGuildJoinRequests(connection, guildId, guild);
                loadGuildClaimedChunks(connection, guildId, guild);
            }

            return guilds;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load guilds: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Loads guild members from the database.
     *
     * @param connection The database connection
     * @param guildId The ID of the guild
     * @param guild The guild to load members for
     * @throws SQLException If an SQL error occurs
     */
    private void loadGuildMembers(Connection connection, UUID guildId, Guild guild) throws SQLException {
        String sql = "SELECT player_id, role FROM guild_members WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                    GuildRole role = GuildRole.valueOf(resultSet.getString("role"));

                    // Skip owner as they're already added in the constructor
                    if (!playerId.equals(guild.getOwner())) {
                        guild.addMember(playerId, role);
                    }
                }
            }
        }
    }

    /**
     * Loads guild invites from the database.
     *
     * @param connection The database connection
     * @param guildId The ID of the guild
     * @param guild The guild to load invites for
     * @throws SQLException If an SQL error occurs
     */
    private void loadGuildInvites(Connection connection, UUID guildId, Guild guild) throws SQLException {
        String sql = "SELECT player_id FROM guild_invites WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                    guild.invite(playerId);
                }
            }
        }
    }

    /**
     * Loads guild join requests from the database.
     *
     * @param connection The database connection
     * @param guildId The ID of the guild
     * @param guild The guild to load join requests for
     * @throws SQLException If an SQL error occurs
     */
    private void loadGuildJoinRequests(Connection connection, UUID guildId, Guild guild) throws SQLException {
        String sql = "SELECT player_id FROM guild_join_requests WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerId = UUID.fromString(resultSet.getString("player_id"));
                    guild.addJoinRequest(playerId);
                }
            }
        }
    }

    /**
     * Loads guild claimed chunks from the database.
     *
     * @param connection The database connection
     * @param guildId The ID of the guild
     * @param guild The guild to load claimed chunks for
     * @throws SQLException If an SQL error occurs
     */
    private void loadGuildClaimedChunks(Connection connection, UUID guildId, Guild guild) throws SQLException {
        String sql = "SELECT chunk_key FROM guild_claimed_chunks WHERE guild_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String chunkKey = resultSet.getString("chunk_key");
                    guild.claimChunkFromString(chunkKey);
                }
            }
        }
    }

    /**
     * Deletes a guild from the database.
     *
     * @param guildId The ID of the guild to delete
     * @return true if the guild was deleted successfully, false otherwise
     */
    public boolean deleteGuild(UUID guildId) {
        if (databaseManager.isUsingYaml()) {
            return false; // YAML storage is handled by GuildManager
        }

        try (Connection connection = databaseManager.getConnection()) {
            String sql = "DELETE FROM guilds WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, guildId.toString());
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete guild: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the guild a player is in.
     *
     * @param playerId The ID of the player
     * @return The ID of the guild the player is in, or null if the player is not in a guild
     */
    public UUID getPlayerGuildId(UUID playerId) {
        if (databaseManager.isUsingYaml()) {
            return null; // YAML storage is handled by GuildManager
        }

        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT guild_id FROM guild_members WHERE player_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return UUID.fromString(resultSet.getString("guild_id"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player guild: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Gets the guild that owns a chunk.
     *
     * @param chunk The chunk to check
     * @return The ID of the guild that owns the chunk, or null if the chunk is not claimed
     */
    public UUID getChunkOwner(Chunk chunk) {
        if (databaseManager.isUsingYaml()) {
            return null; // YAML storage is handled by GuildManager
        }

        String chunkKey = chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ();

        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT guild_id FROM guild_claimed_chunks WHERE chunk_key = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, chunkKey);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return UUID.fromString(resultSet.getString("guild_id"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get chunk owner: " + e.getMessage(), e);
        }
        return null;
    }
}