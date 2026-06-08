package com.securityproject.repository;

import com.securityproject.model.VaultFile;
import java.util.List;

public interface FileRepository {
    void save(int userId, String originalPath, String encryptedPath, String algorithm,
              String integrityHash, String authenticatedMetadata);

    boolean isOwner(int userId, String filePath);

    List<VaultFile> findAll();
}
