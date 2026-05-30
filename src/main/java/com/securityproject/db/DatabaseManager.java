package com.securityproject.db;

import com.securityproject.utils.SecurityUtils;

import java.sql.*;

public class DatabaseManager {
    // vault name
    private static final String DB_URL = "jdbc:sqlite:securevault.db";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    // connecting to database.
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // init database
    public static void initialize() {
        // user table
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "role TEXT NOT NULL DEFAULT 'USER'" +
                ");";

        // files table (ownership)
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

        // audit log table
        String sqlAudit = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            // execute command
            stmt.execute(sqlUsers);
            stmt.execute(sqlFiles);
            stmt.execute(sqlAudit);
            migrateSchema(conn);
            ensureDefaultAdmin(conn);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
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

    private static void ensureDefaultAdmin(Connection conn) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, DEFAULT_ADMIN_USERNAME);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                try (PreparedStatement update = conn.prepareStatement("UPDATE users SET role = 'ADMIN' WHERE username = ?")) {
                    update.setString(1, DEFAULT_ADMIN_USERNAME);
                    update.executeUpdate();
                }
                return;
            }
        }

        String insertSql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, 'ADMIN')";
        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setString(1, DEFAULT_ADMIN_USERNAME);
            insert.setString(2, SecurityUtils.hashPassword(DEFAULT_ADMIN_PASSWORD));
            insert.executeUpdate();
        }
    }

    // register new usr
    public static boolean registeruser(String username, String plainPassword) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, 'USER')";

        // 1.hash pwd before -> database
        String hashedPassword = SecurityUtils.hashPassword(plainPassword);

        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error registering user (Username might be taken): " + e.getMessage());
            return false;
        }
    }

    public static String getUserRole(int userID) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.out.println("Role lookup error: " + e.getMessage());
        }
        return "USER";
    }

    // trying to login. will return uid if successful, or -1 if failed
    public static int authenticateUser(String username, String plainPassword) {
        String sql = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); // only set usr params
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { // check if user was found
                int userID = rs.getInt("id");
                String storedHash = rs.getString("password_hash");

                // use securityutils to verify if pwd matches hash CRITICALLLLL
                // USE BCRYPT TO COMPAREEEEE PLAIN PWD AGAINST HASH FROM DB
                if (SecurityUtils.checkPassword(plainPassword, storedHash)) {
                    return userID; // success
                }
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
        return -1; // for failed login
    }

    // logs action to audit table
    public static void logAction(int userID, String action) {
        String sql = "INSERT INTO audit_logs(user_id, action) VALUES (?, ?)";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
            System.out.println("action logged: " + action);
        } catch (SQLException e) {
            System.out.println("Error logging action: " + e.getMessage());
        }
    }

    // function for file ownership

    public static void addFile(int userID, String filePath) {
        addFile(userID, filePath, filePath, "AES legacy", null, null);
    }

    public static void addFile(int userID, String originalPath, String encryptedPath, String algorithm,
                               String integrityHash, String authenticatedMetadata) {
        String sql = "INSERT INTO files(user_id, file_path, original_path, encrypted_path, algorithm, integrity_hash, authenticated_metadata) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, encryptedPath);
            pstmt.setString(3, originalPath);
            pstmt.setString(4, encryptedPath);
            pstmt.setString(5, algorithm);
            pstmt.setString(6, integrityHash);
            pstmt.setString(7, authenticatedMetadata);
            pstmt.executeUpdate();
            System.out.println("File ownership recorded: " + encryptedPath);
        } catch (SQLException e) {
            System.out.println("Error adding file ownership: " + e.getMessage());
        }
    }

    public static boolean checkFileAccess(int userID, String filePath) {
        String sql = "SELECT id FROM files WHERE user_id = ? AND (file_path = ? OR encrypted_path = ?)";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, filePath);
            pstmt.setString(3, filePath);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // True if a record exists
        } catch (SQLException e) {
            System.out.println("Error checking file access: " + e.getMessage());
            return false;
        }
    }

    // functionality for admin account

    public static java.util.List<String[]> getAllUsers() {
        java.util.List<String[]> users = new java.util.ArrayList<>();
        String sql = "SELECT id, username, role, password_hash FROM users";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("password_hash")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public static java.util.List<String[]> getAllFiles() {
        java.util.List<String[]> files = new java.util.ArrayList<>();
        String sql = "SELECT files.id, files.user_id, users.username, files.original_path, files.encrypted_path, " +
                "files.algorithm, files.integrity_hash, files.created_at " +
                "FROM files " +
                "JOIN users ON files.user_id = users.id " +
                "ORDER BY files.created_at DESC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                files.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("username"),
                        rs.getString("original_path"),
                        rs.getString("encrypted_path"),
                        rs.getString("algorithm"),
                        rs.getString("integrity_hash"),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error getting files: " + e.getMessage());
        }
        return files;
    }

    public static java.util.List<String[]> getAllAuditLogs() {
        java.util.List<String[]> logs = new java.util.ArrayList<>();
        String sql = "SELECT audit_logs.id, audit_logs.user_id, users.username, audit_logs.action, audit_logs.timestamp "
                +
                "FROM audit_logs " +
                "JOIN users ON audit_logs.user_id = users.id " +
                "ORDER BY audit_logs.timestamp DESC";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        String.valueOf(rs.getInt("user_id")), // Added User ID
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("timestamp")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error getting audit logs: " + e.getMessage());
        }
        return logs;
    }
}
