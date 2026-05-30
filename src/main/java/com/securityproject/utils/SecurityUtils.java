package com.securityproject.utils;

import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

public class SecurityUtils {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final byte[] FILE_MAGIC = new byte[] { 'S', 'F', 'V', '2' };
    private static final byte FILE_VERSION = 2;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int SALT_BYTES = 16;
    private static final int FILE_KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final String KEY_FILE = "vault.key"; // Where we store the master key
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    //bcrypt hashing password
    public static String hashPassword(String plainPassword) {
        //log rounds determine complexity of hashing
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    //aes
    //load key from disk or create if no keys
    public static SecretKey loadOrGenerateKey() throws Exception {
        File file = new File(KEY_FILE);
        if (file.exists()) {
            byte[] keyBytes = Files.readAllBytes(Paths.get(KEY_FILE));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            //generate 256-bit key
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();

            //save to disk
            try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
                fos.write(key.getEncoded());
            }
            protectKeyFile(file);
            return key;
        }
    }

    private static void protectKeyFile(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (Exception ignored) {
            // Non-POSIX filesystems are still supported; this is a best-effort hardening step.
        }
    }

    //encrypt/decrypt file
    public static void encryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        encryptFile(inputFile, outputFile, key, -1);
    }

    public static void decryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        decryptFile(inputFile, outputFile, key, -1);
    }

    public static EncryptionResult encryptFile(File inputFile, File outputFile, SecretKey masterKey, int ownerUserId)
            throws Exception {
        byte[] inputBytes = Files.readAllBytes(inputFile.toPath());
        byte[] salt = randomBytes(SALT_BYTES);
        byte[] iv = randomBytes(GCM_IV_BYTES);
        SecretKey fileKey = deriveFileKey(masterKey, salt);
        String aad = createAuthenticatedMetadata(ownerUserId, inputFile.getName(), inputBytes.length);
        byte[] aadBytes = aad.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, fileKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        cipher.updateAAD(aadBytes);
        byte[] ciphertext = cipher.doFinal(inputBytes);

        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(FILE_MAGIC);
            outputStream.write(FILE_VERSION);
            outputStream.write(salt.length);
            outputStream.write(iv.length);
            outputStream.write(ByteBuffer.allocate(Integer.BYTES).putInt(aadBytes.length).array());
            outputStream.write(salt);
            outputStream.write(iv);
            outputStream.write(aadBytes);
            outputStream.write(ciphertext);
        }

        return new EncryptionResult(
                "AES-256-GCM",
                sha256Hex(ciphertext),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(iv),
                aad
        );
    }

    public static DecryptionResult decryptFile(File inputFile, File outputFile, SecretKey masterKey, int expectedOwnerUserId)
            throws Exception {
        byte[] fileBytes = Files.readAllBytes(inputFile.toPath());
        if (!hasCurrentFormat(fileBytes)) {
            legacyDecryptFile(inputFile, outputFile, masterKey);
            return new DecryptionResult("AES-ECB legacy", "Legacy file did not include authenticated metadata.");
        }

        ByteBuffer header = ByteBuffer.wrap(fileBytes);
        byte[] magic = new byte[FILE_MAGIC.length];
        header.get(magic);
        byte version = header.get();
        int saltLength = Byte.toUnsignedInt(header.get());
        int ivLength = Byte.toUnsignedInt(header.get());
        int aadLength = header.getInt();

        if (version != FILE_VERSION || saltLength <= 0 || ivLength <= 0 || aadLength <= 0) {
            throw new SecurityException("Unsupported or damaged SecureFileVault file header.");
        }

        byte[] salt = new byte[saltLength];
        byte[] iv = new byte[ivLength];
        byte[] aadBytes = new byte[aadLength];
        header.get(salt);
        header.get(iv);
        header.get(aadBytes);
        byte[] ciphertext = Arrays.copyOfRange(fileBytes, header.position(), fileBytes.length);
        String aad = new String(aadBytes, StandardCharsets.UTF_8);

        int fileOwnerId = extractOwnerId(aad);
        if (expectedOwnerUserId >= 0 && fileOwnerId != expectedOwnerUserId) {
            throw new SecurityException("Authenticated file owner does not match the logged-in user.");
        }

        SecretKey fileKey = deriveFileKey(masterKey, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, fileKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        cipher.updateAAD(aadBytes);
        byte[] plaintext = cipher.doFinal(ciphertext);
        Files.write(outputFile.toPath(), plaintext);

        return new DecryptionResult("AES-256-GCM", aad);
    }

    public static String sha256Hex(File file) throws Exception {
        return sha256Hex(Files.readAllBytes(file.toPath()));
    }

    private static SecretKey deriveFileKey(SecretKey masterKey, byte[] salt) throws Exception {
        char[] keyMaterial = Base64.getEncoder().encodeToString(masterKey.getEncoded()).toCharArray();
        PBEKeySpec spec = new PBEKeySpec(keyMaterial, salt, PBKDF2_ITERATIONS, FILE_KEY_BITS);
        byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String createAuthenticatedMetadata(int ownerUserId, String originalFileName, long originalLength) {
        return "owner=" + ownerUserId +
                ";name=" + Base64.getEncoder().encodeToString(originalFileName.getBytes(StandardCharsets.UTF_8)) +
                ";bytes=" + originalLength +
                ";created=" + Instant.now();
    }

    private static int extractOwnerId(String aad) {
        for (String part : aad.split(";")) {
            if (part.startsWith("owner=")) {
                return Integer.parseInt(part.substring("owner=".length()));
            }
        }
        throw new SecurityException("Authenticated metadata is missing the owner field.");
    }

    private static boolean hasCurrentFormat(byte[] fileBytes) {
        return fileBytes.length > FILE_MAGIC.length && Arrays.equals(Arrays.copyOf(fileBytes, FILE_MAGIC.length), FILE_MAGIC);
    }

    private static void legacyDecryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] outputBytes = cipher.doFinal(Files.readAllBytes(inputFile.toPath()));
        Files.write(outputFile.toPath(), outputBytes);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    public record EncryptionResult(String algorithm, String ciphertextSha256, String saltBase64, String ivBase64,
                                   String authenticatedMetadata) {
    }

    public record DecryptionResult(String algorithm, String authenticatedMetadata) {
    }

    public static class PasswordValidation {
        public static final int MIN_LENGTH = 8;
        public static final int MAX_LENGTH = 64;

        public static ValidationResult validatePassword(String password) {
            // Check empty
            if (password == null || password.isEmpty()) {
                return new ValidationResult(false, "Password cannot be empty!");
            }

            // Check minimum length
            if (password.length() < MIN_LENGTH) {
                return new ValidationResult(false,
                        "Password must be at least " + MIN_LENGTH + " characters long!");
            }

            // Check maximum length
            if (password.length() > MAX_LENGTH) {
                return new ValidationResult(false,
                        "Password must be less than " + MAX_LENGTH + " characters!");
            }

            // Check complexity
            boolean hasUpper = false;
            boolean hasLower = false;
            boolean hasDigit = false;
            boolean hasSpecial = false;

            for (char c : password.toCharArray()) {
                if (Character.isUpperCase(c)) hasUpper = true;
                else if (Character.isLowerCase(c)) hasLower = true;
                else if (Character.isDigit(c)) hasDigit = true;
                else hasSpecial = true;
            }

            int complexity = 0;
            if (hasUpper) complexity++;
            if (hasLower) complexity++;
            if (hasDigit) complexity++;
            if (hasSpecial) complexity++;

            if (complexity < 3) {
                return new ValidationResult(false,
                        "Password must contain at least 3 of: uppercase, lowercase, numbers, special characters!");
            }

            // Check common passwords
            String[] commonPasswords = {
                    "password", "123456", "12345678", "qwerty", "abc123",
                    "monkey", "letmein", "trustno1", "dragon", "baseball"
            };

            for (String common : commonPasswords) {
                if (password.toLowerCase().contains(common.toLowerCase())) {
                    return new ValidationResult(false,
                            "Password is too common! Please choose a stronger password.");
                }
            }

            return new ValidationResult(true, "Password is valid!");
        }

        // Helper class to return validation result
        public static class ValidationResult {
            public final boolean isValid;
            public final String message;

            public ValidationResult(boolean isValid, String message) {
                this.isValid = isValid;
                this.message = message;
            }
        }
    }
}
