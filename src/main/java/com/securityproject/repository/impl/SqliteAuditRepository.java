package com.securityproject.repository.impl;

import com.securityproject.model.AuditLog;
import com.securityproject.repository.AuditRepository;
import com.securityproject.repository.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteAuditRepository implements AuditRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SqliteAuditRepository.class);
    private final ConnectionProvider cp;

    public SqliteAuditRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public void log(int userId, String action) {
        String sql = "INSERT INTO audit_logs(user_id, action) VALUES (?, ?)";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.executeUpdate();
            LOG.debug("Audit log: user={} action={}", userId, action);
        } catch (SQLException e) {
            LOG.error("Failed to write audit log", e);
        }
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT a.id, a.user_id, u.username, a.action, a.timestamp "
                + "FROM audit_logs a JOIN users u ON a.user_id = u.id "
                + "ORDER BY a.timestamp DESC";
        try (Connection conn = cp.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new AuditLog(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            LOG.error("Failed to fetch audit logs", e);
        }
        return logs;
    }
}
