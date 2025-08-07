package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Manages database connections and operations for the plugin.
 * Supports MySQL, MariaDB, and SQLite databases.
 */
public class DatabaseManager {
    private final Furious plugin;
    private HikariDataSource dataSource;
    private StorageType storageType;

    /**
     * Enum representing the different storage types supported by the plugin.
     */
    public enum StorageType {
        YAML,
        MYSQL,
        MARIADB,
        SQLITE
    }

    /**
     * Creates a new DatabaseManager.
     *
     * @param plugin The plugin instance
     */
    public DatabaseManager(Furious plugin) {
        this.plugin = plugin;
        initialize();
    }

    /**
     * Initializes the database connection based on the configuration.
     */
    private void initialize() {
        // Get storage type from config
        String storageTypeStr = plugin.getConfig().getString("database.storage-type", "YAML");
        try {
            storageType = StorageType.valueOf(storageTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type: " + storageTypeStr + ". Defaulting to YAML.");
            storageType = StorageType.YAML;
        }

        // If using YAML, no need to set up database connection
        if (storageType == StorageType.YAML) {
            plugin.getLogger().info("Using YAML for data storage.");
            return;
        }

        // Set up database connection
        try {
            setupDataSource();
            plugin.getLogger().info("Successfully connected to the database using " + storageType.name() + ".");

            // Initialize database schema
            initializeSchema();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection: " + e.getMessage(), e);
            plugin.getLogger().warning("Falling back to YAML storage.");
            storageType = StorageType.YAML;
        }
    }

    /**
     * Sets up the HikariCP data source based on the storage type.
     */
    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        ConfigurationSection poolConfig = plugin.getConfig().getConfigurationSection("database.connection-pool");

        // Set connection pool properties
        if (poolConfig != null) {
            config.setMaximumPoolSize(poolConfig.getInt("maximum-pool-size", 10));
            config.setMinimumIdle(poolConfig.getInt("minimum-idle", 5));
            config.setMaxLifetime(poolConfig.getLong("maximum-lifetime", 1800000));
            config.setConnectionTimeout(poolConfig.getLong("connection-timeout", 5000));
            config.setIdleTimeout(poolConfig.getLong("idle-timeout", 600000));
        }

        // Configure based on storage type
        switch (storageType) {
            case MYSQL:
            case MARIADB:
                setupMySQLDataSource(config);
                break;
            case SQLITE:
                setupSQLiteDataSource(config);
                break;
            default:
                throw new IllegalStateException("Unsupported storage type: " + storageType);
        }

        // Create the data source
        dataSource = new HikariDataSource(config);
    }

    /**
     * Sets up the HikariCP data source for MySQL/MariaDB.
     *
     * @param config The HikariCP configuration
     */
    private void setupMySQLDataSource(HikariConfig config) {
        ConfigurationSection mysqlConfig = plugin.getConfig().getConfigurationSection("database.mysql");
        if (mysqlConfig == null) {
            throw new IllegalStateException("MySQL configuration section not found in config.yml");
        }

        String host = mysqlConfig.getString("host", "localhost");
        int port = mysqlConfig.getInt("port", 3306);
        String database = mysqlConfig.getString("database", "furious");
        String username = mysqlConfig.getString("username", "furious");
        String password = mysqlConfig.getString("password", "password");
        boolean useSSL = mysqlConfig.getBoolean("use-ssl", false);

        // Set driver class name based on storage type
        if (storageType == StorageType.MYSQL) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
        } else {
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
        }

        config.setUsername(username);
        config.setPassword(password);

        // MySQL/MariaDB specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    /**
     * Sets up the HikariCP data source for SQLite.
     *
     * @param config The HikariCP configuration
     */
    private void setupSQLiteDataSource(HikariConfig config) {
        ConfigurationSection sqliteConfig = plugin.getConfig().getConfigurationSection("database.sqlite");
        if (sqliteConfig == null) {
            throw new IllegalStateException("SQLite configuration section not found in config.yml");
        }

        String fileName = sqliteConfig.getString("file", "database.db");
        File databaseFile = new File(plugin.getDataFolder(), fileName);

        // Ensure the parent directory exists
        if (!databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }

        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());

        // SQLite specific settings
        config.setMaximumPoolSize(1); // SQLite only supports one connection at a time
    }

    /**
     * Initializes the database schema.
     */
    private void initializeSchema() {
        try (Connection connection = getConnection()) {
            // Create tables if they don't exist
            createGuildsTable(connection);
            createGuildMembersTable(connection);
            createGuildInvitesTable(connection);
            createGuildJoinRequestsTable(connection);
            createGuildClaimedChunksTable(connection);
            createWalletsTable(connection);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database schema: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Creates the guilds table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createGuildsTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS guilds (" +
                  "id TEXT PRIMARY KEY, " +
                  "name TEXT NOT NULL, " +
                  "owner TEXT NOT NULL, " +
                  "description TEXT, " +
                  "type TEXT NOT NULL, " +
                  "mob_spawning_enabled INTEGER NOT NULL DEFAULT 1, " +
                  "open INTEGER NOT NULL DEFAULT 0" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS guilds (" +
                  "id VARCHAR(36) PRIMARY KEY, " +
                  "name VARCHAR(32) NOT NULL, " +
                  "owner VARCHAR(36) NOT NULL, " +
                  "description TEXT, " +
                  "type VARCHAR(16) NOT NULL, " +
                  "mob_spawning_enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                  "open BOOLEAN NOT NULL DEFAULT FALSE" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Creates the guild_members table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createGuildMembersTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS guild_members (" +
                  "guild_id TEXT NOT NULL, " +
                  "player_id TEXT NOT NULL, " +
                  "role TEXT NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS guild_members (" +
                  "guild_id VARCHAR(36) NOT NULL, " +
                  "player_id VARCHAR(36) NOT NULL, " +
                  "role VARCHAR(16) NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Creates the guild_invites table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createGuildInvitesTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS guild_invites (" +
                  "guild_id TEXT NOT NULL, " +
                  "player_id TEXT NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS guild_invites (" +
                  "guild_id VARCHAR(36) NOT NULL, " +
                  "player_id VARCHAR(36) NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Creates the guild_join_requests table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createGuildJoinRequestsTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS guild_join_requests (" +
                  "guild_id TEXT NOT NULL, " +
                  "player_id TEXT NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS guild_join_requests (" +
                  "guild_id VARCHAR(36) NOT NULL, " +
                  "player_id VARCHAR(36) NOT NULL, " +
                  "PRIMARY KEY (guild_id, player_id), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Creates the guild_claimed_chunks table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createGuildClaimedChunksTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS guild_claimed_chunks (" +
                  "guild_id TEXT NOT NULL, " +
                  "chunk_key TEXT NOT NULL, " +
                  "PRIMARY KEY (chunk_key), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS guild_claimed_chunks (" +
                  "guild_id VARCHAR(36) NOT NULL, " +
                  "chunk_key VARCHAR(100) NOT NULL, " +
                  "PRIMARY KEY (chunk_key), " +
                  "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Creates the wallets table if it doesn't exist.
     *
     * @param connection The database connection
     * @throws SQLException If an SQL error occurs
     */
    private void createWalletsTable(Connection connection) throws SQLException {
        String sql;

        if (storageType == StorageType.SQLITE) {
            sql = "CREATE TABLE IF NOT EXISTS wallets (" +
                  "player_id TEXT PRIMARY KEY, " +
                  "balance REAL NOT NULL DEFAULT 0.0" +
                  ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS wallets (" +
                  "player_id VARCHAR(36) PRIMARY KEY, " +
                  "balance DOUBLE NOT NULL DEFAULT 0.0" +
                  ")";
        }

        connection.createStatement().execute(sql);
    }

    /**
     * Gets a connection from the connection pool.
     *
     * @return A database connection
     * @throws SQLException If a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Gets the current storage type.
     *
     * @return The storage type
     */
    public StorageType getStorageType() {
        return storageType;
    }

    /**
     * Checks if the database connection is using YAML storage.
     *
     * @return true if using YAML storage, false otherwise
     */
    public boolean isUsingYaml() {
        return storageType == StorageType.YAML;
    }

    /**
     * Closes the database connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}