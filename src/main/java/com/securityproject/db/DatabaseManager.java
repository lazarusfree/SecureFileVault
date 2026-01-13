package com.securityproject.db;

import com.securityproject.utils.SecurityUtils;

import java.sql.*;

public class DatabaseManager {
    // vault name
    private static final String DB_URL = "jdbc:sqlite:securevault.db";

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
                "password_hash TEXT NOT NULL" +
                ");";

        // files table (ownership)
        String sqlFiles = "CREATE TABLE IF NOT EXISTS files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "file_path TEXT NOT NULL, " +
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
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    // register new usr
    public static boolean registeruser(String username, String plainPassword) {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";

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
        String sql = "INSERT INTO files(user_id, file_path) VALUES (?, ?)";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, filePath);
            pstmt.executeUpdate();
            System.out.println("File ownership recorded: " + filePath);
        } catch (SQLException e) {
            System.out.println("Error adding file ownership: " + e.getMessage());
        }
    }

    public static boolean checkFileAccess(int userID, String filePath) {
        String sql = "SELECT id FROM files WHERE user_id = ? AND file_path = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, filePath);
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
        String sql = "SELECT id, username, password_hash FROM users";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("username"),
                        rs.getString("password_hash")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error getting all users: " + e.getMessage());
        }
        return users;
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