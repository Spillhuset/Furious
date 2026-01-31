package com.spillhuset.furious.db;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * A very small JDBC connection pool for MySQL.
 * It is intentionally minimal and only supports the subset our plugin needs.
 * <p>
 * Features:
 * - Max pool size with blocking acquire (fair semaphore)
 * - Minimum idle warmup
 * - Idle max lifetime and simple retire/refresh on checkout
 * - Validation query on checkout (optional)
 * <p>
 * Limitations: not production-grade; prefer HikariCP in serious deployments.
 */
public class SimpleConnectionPool implements DataSource, AutoCloseable {
    private final String jdbcUrl;
    private final String user;
    private final String pass;

    private final int maxPoolSize;
    private final int minIdle;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final String validationQuery;

    private final Semaphore permits;
    private final Queue<Pooled> idle = new ArrayDeque<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile PrintWriter logWriter;

    public SimpleConnectionPool(String jdbcUrl, String user, String pass,
                                int maxPoolSize, int minIdle,
                                long connectionTimeoutMs, long idleTimeoutMs, long maxLifetimeMs,
                                String validationQuery) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.user = user;
        this.pass = pass;
        this.maxPoolSize = Math.max(1, maxPoolSize);
        this.minIdle = Math.max(0, Math.min(this.maxPoolSize, minIdle));
        this.connectionTimeoutMs = Math.max(1000, connectionTimeoutMs);
        this.idleTimeoutMs = Math.max(30_000, idleTimeoutMs);
        this.maxLifetimeMs = Math.max(60_000, maxLifetimeMs);
        this.validationQuery = validationQuery;
        this.permits = new Semaphore(this.maxPoolSize, true);

        // Warm up minIdle connections
        for (int i = 0; i < this.minIdle; i++) {
            try {
                idle.add(new Pooled(newPhysicalConnection()));
                permits.acquireUninterruptibly();
            } catch (SQLException e) {
                break;
            }
        }
    }

    private Connection newPhysicalConnection() throws SQLException {
        return (user == null) ? DriverManager.getConnection(jdbcUrl)
                : DriverManager.getConnection(jdbcUrl, user, pass);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(user, pass);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (closed.get()) throw new SQLException("Pool is closed");

        long deadline = System.currentTimeMillis() + connectionTimeoutMs;
        Pooled pooled = null;
        while (pooled == null) {
            synchronized (idle) {
                Pooled p = idle.poll();
                if (p != null) pooled = p;
            }
            if (pooled == null) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) throw new SQLException("Timeout waiting for connection");
                try {
                    if (!permits.tryAcquire(Math.min(remaining, 50), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for connection", e);
                }
                // Create a new physical connection
                Connection conn = null;
                try {
                    conn = newPhysicalConnection();
                    pooled = new Pooled(conn);
                } catch (SQLException e) {
                    permits.release();
                    throw e;
                }
            }

            // Validate and check lifetime
            try {
                if (pooled.isExpired(maxLifetimeMs) || pooled.isIdleTooLong(idleTimeoutMs) || (validationQuery != null && !validationQuery.isBlank() && !pooled.isValid(validationQuery))) {
                    pooled.reallyClose();
                    // replace it with a fresh one
                    Connection conn = newPhysicalConnection();
                    pooled = new Pooled(conn);
                }
            } catch (SQLException e) {
                // Try to recover by retrying once within timeout
                pooled = null;
                permits.release();
                if (System.currentTimeMillis() >= deadline) throw e;
            }
        }
        pooled.checkout();
        return new ProxyConnection(pooled);
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        synchronized (idle) {
            for (Pooled p : idle) {
                try {
                    p.reallyClose();
                } catch (SQLException ignored) {
                }
            }
            idle.clear();
        }
        // Drain permits to zero so no outstanding logical connections remain
        // Note: actual physical connections are closed on return.
    }

    public boolean isClosed() {
        return closed.get();
    }

    void returnConnection(Pooled pooled) {
        if (closed.get()) {
            try {
                pooled.reallyClose();
            } catch (SQLException ignored) {
            }
            permits.release();
            return;
        }
        pooled.checkin();
        synchronized (idle) {
            idle.offer(pooled);
        }
    }

    private static class Pooled {
        private final Connection delegate;
        private final Instant createdAt = Instant.now();
        private Instant lastUsed = Instant.now();
        private final AtomicBoolean inUse = new AtomicBoolean(false);

        Pooled(Connection delegate) {
            this.delegate = delegate;
        }

        boolean isExpired(long maxLifeMs) {
            return Duration.between(createdAt, Instant.now()).toMillis() > maxLifeMs;
        }

        boolean isIdleTooLong(long idleMs) {
            return Duration.between(lastUsed, Instant.now()).toMillis() > idleMs;
        }

        boolean isValid(String validationQuery) {
            try (var stmt = delegate.createStatement()) {
                stmt.setQueryTimeout(5);
                stmt.execute(validationQuery);
                return true;
            } catch (SQLException e) {
                return false;
            }
        }

        void checkout() {
            inUse.set(true);
            lastUsed = Instant.now();
        }

        void checkin() {
            inUse.set(false);
            lastUsed = Instant.now();
        }

        void reallyClose() throws SQLException {
            delegate.close();
        }
    }

    // Lightweight proxy that returns to pool on close()
    private class ProxyConnection implements Connection {
        private Pooled pooled;

        private Connection d() throws SQLException {
            if (pooled == null) throw new SQLException("Connection closed");
            return pooled.delegate;
        }

        ProxyConnection(Pooled pooled) {
            this.pooled = pooled;
        }

        private void ensureOpen() throws SQLException {
            if (pooled == null) throw new SQLException("Connection closed");
        }

        @Override
        public void close() throws SQLException {
            if (pooled != null) {
                Pooled p = pooled;
                pooled = null;
                returnConnection(p);
                permits.release();
            }
        }

        @Override
        public boolean isClosed() throws SQLException {
            return pooled == null || d().isClosed();
        }

        // Delegate all other methods
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return d().unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            try {
                return d().isWrapperFor(iface);
            } catch (SQLException e) {
                return false;
            }
        }

        @Override
        public java.sql.Statement createStatement() throws SQLException {
            return d().createStatement();
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
            return d().prepareStatement(sql);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
            return d().prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return d().nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            d().setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return d().getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            d().commit();
        }

        @Override
        public void rollback() throws SQLException {
            d().rollback();
        }

        @Override
        public java.sql.DatabaseMetaData getMetaData() throws SQLException {
            return d().getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            d().setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return d().isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            d().setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return d().getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            d().setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return d().getTransactionIsolation();
        }

        @Override
        public java.sql.SQLWarning getWarnings() throws SQLException {
            return d().getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            d().clearWarnings();
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return d().createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return d().prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return d().prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
            return d().getTypeMap();
        }

        @Override
        public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
            d().setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            d().setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return d().getHoldability();
        }

        @Override
        public java.sql.Savepoint setSavepoint() throws SQLException {
            return d().setSavepoint();
        }

        @Override
        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            return d().setSavepoint(name);
        }

        @Override
        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
            d().rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
            d().releaseSavepoint(savepoint);
        }

        @Override
        public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return d().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return d().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return d().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return d().prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return d().prepareStatement(sql, columnIndexes);
        }

        @Override
        public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return d().prepareStatement(sql, columnNames);
        }

        @Override
        public java.sql.Clob createClob() throws SQLException {
            return d().createClob();
        }

        @Override
        public java.sql.Blob createBlob() throws SQLException {
            return d().createBlob();
        }

        @Override
        public java.sql.NClob createNClob() throws SQLException {
            return d().createNClob();
        }

        @Override
        public java.sql.SQLXML createSQLXML() throws SQLException {
            return d().createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return d().isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {
            try {
                d().setClientInfo(name, value);
            } catch (SQLException e) {
                throw new java.sql.SQLClientInfoException(e.getMessage(), null, e);
            }
        }

        @Override
        public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException {
            try {
                d().setClientInfo(properties);
            } catch (SQLException e) {
                throw new java.sql.SQLClientInfoException(e.getMessage(), null, e);
            }
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return d().getClientInfo(name);
        }

        @Override
        public java.util.Properties getClientInfo() throws SQLException {
            return d().getClientInfo();
        }

        @Override
        public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return d().createArrayOf(typeName, elements);
        }

        @Override
        public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return d().createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            d().setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return d().getSchema();
        }

        @Override
        public void abort(java.util.concurrent.Executor executor) throws SQLException {
            d().abort(executor);
        }

        @Override
        public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
            d().setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return d().getNetworkTimeout();
        }
    }

    // DataSource misc
    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) { /* not supported */ }

    @Override
    public int getLoginTimeout() {
        return (int) (connectionTimeoutMs / 1000);
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger("SimpleConnectionPool");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
