package com.securityproject;

import com.securityproject.config.VaultConfig;
import com.securityproject.db.DatabaseInitializer;
import com.securityproject.repository.*;
import com.securityproject.repository.impl.*;
import com.securityproject.service.AuthService;
import com.securityproject.service.EncryptionService;
import com.securityproject.service.KeyService;

public class AppContext {
    private final ConnectionProvider connectionProvider;
    private final UserRepository userRepo;
    private final FileRepository fileRepo;
    private final AuditRepository auditRepo;
    private final KeyService keyService;
    private final AuthService authService;
    private final EncryptionService encryptionService;

    private static AppContext instance;

    public static AppContext init() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public static AppContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppContext not initialized. Call init() first.");
        }
        return instance;
    }

    private AppContext() {
        DatabaseInitializer.initialize();

        this.connectionProvider = ConnectionProvider.fromUrl(VaultConfig.DB_URL);
        this.userRepo = new SqliteUserRepository(connectionProvider);
        this.fileRepo = new SqliteFileRepository(connectionProvider);
        this.auditRepo = new SqliteAuditRepository(connectionProvider);
        this.keyService = new KeyService();
        this.authService = new AuthService(userRepo, auditRepo);
        this.encryptionService = new EncryptionService(keyService, fileRepo, auditRepo);
    }

    public AuthService auth() {
        return authService;
    }

    public EncryptionService crypto() {
        return encryptionService;
    }

    public UserRepository users() {
        return userRepo;
    }

    public FileRepository files() {
        return fileRepo;
    }

    public AuditRepository audit() {
        return auditRepo;
    }

    public KeyService keys() {
        return keyService;
    }
}
