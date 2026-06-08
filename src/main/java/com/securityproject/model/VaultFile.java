package com.securityproject.model;

public record VaultFile(
        int id,
        int userId,
        String username,
        String originalPath,
        String encryptedPath,
        String algorithm,
        String integrityHash,
        String createdAt) {
}
