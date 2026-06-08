package com.securityproject.repository.impl;

import com.securityproject.model.User;
import com.securityproject.repository.ConnectionProvider;
import com.securityproject.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SqliteUserRepository.class);
    private final ConnectionProvider cp;

    public SqliteUserRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to find user by username", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOG.error("Failed to find user by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role FROM users ORDER BY id";
        try (Connection conn = cp.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            LOG.error("Failed to fetch all users", e);
        }
        return users;
    }

    @Override
    public boolean create(String username, String passwordHash) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, 'USER')";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            LOG.info("User '{}' created.", username);
            return true;
        } catch (SQLException e) {
            LOG.error("Failed to create user '{}': {}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createAdmin(String username, String passwordHash) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?, ?, 'ADMIN')";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.error("Failed to create admin user", e);
            return false;
        }
    }

    @Override
    public String getRole(int userId) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            LOG.error("Role lookup failed for user {}", userId, e);
        }
        return "USER";
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role")
        );
    }
}
