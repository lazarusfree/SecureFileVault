package com.securityproject.db;

import com.securityproject.utils.SecurityUtils;

import java.sql.*;

public class DatabaseManager {
    // The name of the file where data will be stored
    private static final String DB_URL = "jdbc:sqlite:securevault.db";

    //Connect to the database. If the file doesn't exist, SQLite creates it automatically.
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

     //Initialize the database tables.
     //Run this once when the application starts.
    public static void initialize() {
        // SQL for creating the Users table
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL" +
                ");";

        // SQL for creating the Audit Log table
        String sqlAudit = "CREATE TABLE IF NOT EXISTS audit_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "action TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Execute the SQL commands
            stmt.execute(sqlUsers);
            stmt.execute(sqlAudit);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    //register new usr
    public static boolean registeruser(String username, String plainPassword) {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";

        //1.hash pwd before -> database
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

    //trying to login. will return uid if successful, or -1 if failed
    public static int authenticateUser(String username, String plainPassword) {
        String sql = "SELECT id, password_hash FROM users WHERE username = ?";
        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); //only set usr params
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { //check if user was found
                int userID = rs.getInt("id");
                String storedHash = rs.getString("password_hash");

                //use securityutils to verify if pwd matches hash CRITICALLLLL
                //USE BCRYPT TO COMPAREEEEE PLAIN PWD AGAINST HASH FROM DB
                if (SecurityUtils.checkPassword(plainPassword, storedHash)) {
                    return userID; //success
                }
            }
        } catch (SQLException e) {
            System.out.println("Login error: " +  e.getMessage());
        }
        return -1; //for failed login
    }

    //logs action to audit table
    public static void logAction(int userID, String action) {
        String sql = "INSERT INTO audit_logs(user_id, action) VALUES (?, ?)";
        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
            System.out.println("action logged: "+ action);
        } catch (SQLException e) {
            System.out.println("Error logging action: " + e.getMessage());
        }
    }
}