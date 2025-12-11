package com.securityproject.utils;

import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;

public class SecurityUtils {

    private static final String ALGORITHM = "AES";
    private static final String KEY_FILE = "vault.key"; // Where we store the master key

    // ===========================
    // 1. PASSWORD HANDLING (BCrypt)
    // ===========================

    public static String hashPassword(String plainPassword) {
        // Log rounds (12) determines complexity. Higher is slower but safer.
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    // ===========================
    // 2. AES KEY MANAGEMENT
    // ===========================

    // Load the key from disk, or create it if it doesn't exist
    public static SecretKey loadOrGenerateKey() throws Exception {
        File file = new File(KEY_FILE);
        if (file.exists()) {
            byte[] keyBytes = Files.readAllBytes(Paths.get(KEY_FILE));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            // Generate a new 256-bit AES key
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();

            // Save it to disk so we can decrypt files later
            try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
                fos.write(key.getEncoded());
            }
            return key;
        }
    }

    // ===========================
    // 3. FILE ENCRYPTION / DECRYPTION
    // ===========================

    public static void encryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        doCrypto(Cipher.ENCRYPT_MODE, inputFile, outputFile, key);
    }

    public static void decryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        doCrypto(Cipher.DECRYPT_MODE, inputFile, outputFile, key);
    }

    private static void doCrypto(int cipherMode, File inputFile, File outputFile, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(cipherMode, key);

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            outputStream.write(outputBytes);
        }
    }
}