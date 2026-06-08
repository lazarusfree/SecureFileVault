package com.securityproject.config;

public final class VaultConfig {
    private VaultConfig() {}

    public static final String DB_URL = "jdbc:sqlite:securevault.db";
    public static final String KEY_FILE = "vault.key";
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final int BCRYPT_LOG_ROUNDS = 12;

    public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    public static final int GCM_TAG_BITS = 128;
    public static final int GCM_IV_BYTES = 12;
    public static final int SALT_BYTES = 16;
    public static final int FILE_KEY_BITS = 256;
    public static final int PBKDF2_ITERATIONS = 120_000;
    public static final byte[] FILE_MAGIC = {'S', 'F', 'V', '2'};
    public static final byte FILE_VERSION = 2;

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 64;
    public static final int USERNAME_MIN_LENGTH = 3;

    public static String resolveAdminPassword() {
        String prop = System.getProperty("sfv.admin.password", System.getenv("SFV_ADMIN_PASSWORD"));
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        System.err.println("WARNING: Using default admin password. Override via -Dsfv.admin.password or SFV_ADMIN_PASSWORD.");
        return "admin123";
    }
}
