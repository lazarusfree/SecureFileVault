package com.securityproject.repository.impl;

import com.securityproject.model.VaultFile;
import com.securityproject.repository.ConnectionProvider;
import com.securityproject.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteFileRepository implements FileRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SqliteFileRepository.class);
    private final ConnectionProvider cp;

    public SqliteFileRepository(ConnectionProvider cp) {
        this.cp = cp;
    }

    @Override
    public void save(int userId, String originalPath, String encryptedPath, String algorithm,
                     String integrityHash, String authenticatedMetadata) {
        String sql = "INSERT INTO files(user_id, file_path, original_path, encrypted_path, algorithm, integrity_hash, authenticated_metadata) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, encryptedPath);
            ps.setString(3, originalPath);
            ps.setString(4, encryptedPath);
            ps.setString(5, algorithm);
            ps.setString(6, integrityHash);
            ps.setString(7, authenticatedMetadata);
            ps.executeUpdate();
            LOG.debug("File record saved: {}", encryptedPath);
        } catch (SQLException e) {
            LOG.error("Failed to save file record", e);
        }
    }

    @Override
    public boolean isOwner(int userId, String filePath) {
        String sql = "SELECT id FROM files WHERE user_id = ? AND file_path = ?";
        try (Connection conn = cp.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.error("Failed to check file ownership", e);
            return false;
        }
    }

    @Override
    public List<VaultFile> findAll() {
        List<VaultFile> files = new ArrayList<>();
        String sql = "SELECT f.id, f.user_id, u.username, f.original_path, f.encrypted_path, "
                + "f.algorithm, f.integrity_hash, f.created_at "
                + "FROM files f JOIN users u ON f.user_id = u.id "
                + "ORDER BY f.created_at DESC";
        try (Connection conn = cp.get();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                files.add(mapFile(rs));
            }
        } catch (SQLException e) {
            LOG.error("Failed to fetch files", e);
        }
        return files;
    }

    private static VaultFile mapFile(ResultSet rs) throws SQLException {
        return new VaultFile(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("original_path"),
                rs.getString("encrypted_path"),
                rs.getString("algorithm"),
                rs.getString("integrity_hash"),
                rs.getString("created_at")
        );
    }
}
