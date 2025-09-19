package com.spillhuset.furious.db;

import com.spillhuset.furious.Furious;
import org.bukkit.configuration.ConfigurationSection;

import javax.sql.DataSource;
import java.util.logging.Level;

/**
 * Central database manager using a simple built-in pool (no HikariCP).
 * Disabled by default via config. Currently supports MySQL.
 */
public class DatabaseManager {
    private final Furious plugin;
    private SimpleConnectionPool dataSource;

    public DatabaseManager(Furious plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            ConfigurationSection db = plugin.getConfig().getConfigurationSection("database");
            if (db == null) {
                plugin.getLogger().info("Database: disabled (no database section, using YAML storage).");
                return;
            }
            // Accept multiple truthy values for backward compatibility (true, 'true', 'enable', 'enabled', 'on', 'yes', 1)
            boolean enabled;
            Object enabledObj = db.get("enabled");
            if (enabledObj == null) {
                enabled = false;
            } else if (enabledObj instanceof Boolean) {
                enabled = (Boolean) enabledObj;
            } else if (enabledObj instanceof Number) {
                enabled = ((Number) enabledObj).intValue() != 0;
            } else {
                String s = String.valueOf(enabledObj).trim().toLowerCase();
                enabled = s.equals("true") || s.equals("enable") || s.equals("enabled") || s.equals("on") || s.equals("yes") || s.equals("1");
            }
            if (!enabled) {
                plugin.getLogger().info("Database: disabled (using YAML storage).");
                return;
            }
            String type = db.getString("type", "mysql");
            if (!"mysql".equalsIgnoreCase(type)) {
                plugin.getLogger().warning("Database type '" + type + "' not supported yet. Falling back to disabled.");
                return;
            }
            ConfigurationSection mysql = db.getConfigurationSection("mysql");
            if (mysql == null) {
                plugin.getLogger().warning("Database: missing mysql section in config, disabling.");
                return;
            }
            String host = mysql.getString("host", "localhost");
            int port = mysql.getInt("port", 3306);
            String database = mysql.getString("database", "furious");
            String user = mysql.getString("user", "furious");
            String password = mysql.getString("password", "");
            String params = mysql.getString("params", "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + (params == null || params.isBlank() ? "" : ("?" + params));

            ConfigurationSection pool = db.getConfigurationSection("pool");
            int maximumPoolSize = 10;
            int minimumIdle = 2;
            long connectionTimeoutMs = 10_000L;
            long idleTimeoutMs = 600_000L;
            long maxLifetimeMs = 1_800_000L;
            if (pool != null) {
                maximumPoolSize = pool.getInt("maximumPoolSize", maximumPoolSize);
                minimumIdle = pool.getInt("minimumIdle", minimumIdle);
                connectionTimeoutMs = pool.getLong("connectionTimeoutMs", connectionTimeoutMs);
                idleTimeoutMs = pool.getLong("idleTimeoutMs", idleTimeoutMs);
                maxLifetimeMs = pool.getLong("maxLifetimeMs", maxLifetimeMs);
            }

            // Build simple pool
            String validationQuery = "SELECT 1";
            this.dataSource = new SimpleConnectionPool(
                    jdbcUrl, user, password,
                    maximumPoolSize, minimumIdle,
                    connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs,
                    validationQuery
            );
            plugin.getLogger().info("Database: MySQL simple pool initialized.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database pool: " + t.getMessage(), t);
            // ensure no half-open pool remains
            if (dataSource != null) try { dataSource.close(); } catch (Throwable ignored) {}
            dataSource = null;
        }
    }

    public DataSource getDataSource() { return dataSource; }

    public boolean isEnabled() { return dataSource != null && !dataSource.isClosed(); }

    public void shutdown() {
        try {
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
                plugin.getLogger().info("Database: pool closed.");
            }
        } catch (Throwable ignored) { }
    }
}
