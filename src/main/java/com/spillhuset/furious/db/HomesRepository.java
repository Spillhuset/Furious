package com.spillhuset.furious.db;

import com.spillhuset.furious.utils.Home;
import org.bukkit.Location;
import org.bukkit.Server;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * SQL repository for player Homes data. Mirrors YAML layout used by HomesService
 * but stored in two tables: homes and home_purchases.
 */
public class HomesRepository {
    private final DataSource dataSource;
    private final Server server; // to resolve worlds when building Locations

    public HomesRepository(DataSource dataSource, Server server) {
        this.dataSource = dataSource;
        this.server = server;
    }

    public void initSchema() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS homes (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "player_id VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(128) NOT NULL, " +
                    "world VARCHAR(36) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "armor_stand VARCHAR(36) NULL, " +
                    "UNIQUE (player_id, name)" +
                    ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS home_purchases (" +
                    "player_id VARCHAR(36) PRIMARY KEY, " +
                    "purchased INT NOT NULL" +
                    ")");
        }
    }

    public void loadAll(Map<UUID, Set<UUID>> players,
                        Map<UUID, Home> homes,
                        Map<Location, UUID> locations,
                        Map<UUID, Integer> purchasedSlots) throws Exception {
        players.clear();
        homes.clear();
        locations.clear();
        purchasedSlots.clear();
        try (Connection conn = dataSource.getConnection()) {
            // purchases
            try (PreparedStatement ps = conn.prepareStatement("SELECT player_id, purchased FROM home_purchases")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID pid = UUID.fromString(rs.getString(1));
                        int purchased = rs.getInt(2);
                        purchasedSlots.put(pid, purchased);
                    }
                }
            }
            // homes
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, player_id, name, world, x, y, z, yaw, pitch, armor_stand FROM homes")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString(1));
                        UUID pid = UUID.fromString(rs.getString(2));
                        String name = rs.getString(3);
                        UUID world = UUID.fromString(rs.getString(4));
                        double x = rs.getDouble(5);
                        double y = rs.getDouble(6);
                        double z = rs.getDouble(7);
                        float yaw = rs.getFloat(8);
                        float pitch = rs.getFloat(9);
                        String armor = rs.getString(10);
                        Location loc = new Location(server.getWorld(world), x, y, z, yaw, pitch);
                        Home home = new Home(id, name, loc, pid);
                        if (armor != null && !armor.isBlank()) {
                            try { home.setArmorStandUuid(UUID.fromString(armor)); } catch (IllegalArgumentException ignored) {}
                        }
                        homes.put(id, home);
                        players.computeIfAbsent(pid, k -> new HashSet<>()).add(id);
                        locations.put(loc, id);
                    }
                }
            }
        }
    }

    public void saveAll(Map<UUID, Set<UUID>> players,
                        Map<UUID, Home> homes,
                        Map<UUID, Integer> purchasedSlots) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM homes");
                st.executeUpdate("DELETE FROM home_purchases");
            }
            // purchases
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO home_purchases(player_id, purchased) VALUES(?,?)")) {
                for (Map.Entry<UUID, Integer> e : purchasedSlots.entrySet()) {
                    ps.setString(1, e.getKey().toString());
                    ps.setInt(2, Math.max(0, e.getValue() == null ? 0 : e.getValue()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            // homes
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO homes(id, player_id, name, world, x, y, z, yaw, pitch, armor_stand) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                for (UUID playerId : players.keySet()) {
                    Set<UUID> ids = players.get(playerId);
                    if (ids == null) continue;
                    for (UUID id : ids) {
                        Home h = homes.get(id);
                        if (h == null) continue;
                        ps.setString(1, id.toString());
                        ps.setString(2, playerId.toString());
                        ps.setString(3, h.getName());
                        ps.setString(4, h.getWorld().toString());
                        ps.setDouble(5, h.getX());
                        ps.setDouble(6, h.getY());
                        ps.setDouble(7, h.getZ());
                        ps.setFloat(8, h.getYaw());
                        ps.setFloat(9, h.getPitch());
                        ps.setString(10, h.getArmorStandUuid() == null ? null : h.getArmorStandUuid().toString());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
        }
    }
}
