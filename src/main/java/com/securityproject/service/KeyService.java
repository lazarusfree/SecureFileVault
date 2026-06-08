package com.securityproject.service;

import com.securityproject.config.VaultConfig;
import com.securityproject.exception.CryptoOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Set;

public class KeyService {
    private static final Logger LOG = LoggerFactory.getLogger(KeyService.class);

    // SECURITY: The master key is stored in plaintext on disk.
    // A production system should use a hardware security module or encrypt
    // the key with a user-provided passphrase.

    private final String keyFilePath;
    private SecretKey cachedKey;

    public KeyService() {
        this(VaultConfig.KEY_FILE);
    }

    public KeyService(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    public SecretKey loadOrGenerate() {
        if (cachedKey != null) {
            return cachedKey;
        }

        File file = new File(keyFilePath);
        try {
            if (file.exists()) {
                byte[] keyBytes = Files.readAllBytes(Paths.get(keyFilePath));
                cachedKey = new SecretKeySpec(keyBytes, "AES");
                LOG.debug("Master key loaded from {}.", keyFilePath);
                return cachedKey;
            }
        } catch (Exception e) {
            throw new CryptoOperationException("Failed to load master key.", e);
        }

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            cachedKey = keyGen.generateKey();

            try (FileOutputStream fos = new FileOutputStream(keyFilePath)) {
                fos.write(cachedKey.getEncoded());
            }
            protectKeyFile(file);
            LOG.info("New master key generated and saved to {}.", keyFilePath);
            return cachedKey;
        } catch (Exception e) {
            throw new CryptoOperationException("Failed to generate master key.", e);
        }
    }

    private void protectKeyFile(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (Exception ex) {
            LOG.debug("Key file permission hardening skipped (non-POSIX filesystem): {}", ex.getMessage());
        }
    }
}
