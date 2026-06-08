package com.securityproject.db;

import com.securityproject.config.VaultConfig;
import com.securityproject.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public final class DatabaseInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {}

    public static void initialize() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'USER'" +
                ");";

        String sqlFiles = "CREATE TABLE IF NOT EXISTS files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "file_path TEXT NOT NULL, " +
                "original_path TEXT, " +
                "encrypted_path TEXT, " +
                "algorithm TEXT, " +
                "integrity_hash TEXT, " +
                "authenticated_metadata TEXT, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        String sqlAudit = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = DriverManager.getConnection(VaultConfig.DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlUsers);
            stmt.execute(sqlFiles);
            stmt.execute(sqlAudit);
            migrateSchema(conn);
            ensureAdmin(conn);
            LOG.info("Database initialized successfully.");

        } catch (SQLException e) {
            LOG.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void migrateSchema(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "users", "role", "TEXT NOT NULL DEFAULT 'USER'");
        addColumnIfMissing(conn, "files", "original_path", "TEXT");
        addColumnIfMissing(conn, "files", "encrypted_path", "TEXT");
        addColumnIfMissing(conn, "files", "algorithm", "TEXT");
        addColumnIfMissing(conn, "files", "integrity_hash", "TEXT");
        addColumnIfMissing(conn, "files", "authenticated_metadata", "TEXT");
        addColumnIfMissing(conn, "files", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE files SET encrypted_path = file_path WHERE encrypted_path IS NULL");
            stmt.executeUpdate("UPDATE files SET algorithm = 'AES legacy' WHERE algorithm IS NULL");
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void ensureAdmin(Connection conn) throws SQLException {
        String select = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, VaultConfig.DEFAULT_ADMIN_USERNAME);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE users SET role = 'ADMIN' WHERE username = ?")) {
                    update.setString(1, VaultConfig.DEFAULT_ADMIN_USERNAME);
                    update.executeUpdate();
                }
                return;
            }
        }

        String insert = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, 'ADMIN')";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, VaultConfig.DEFAULT_ADMIN_USERNAME);
            ps.setString(2, PasswordUtil.hash(VaultConfig.resolveAdminPassword()));
            ps.executeUpdate();
            LOG.info("Default admin account created.");
        }
    }
}
