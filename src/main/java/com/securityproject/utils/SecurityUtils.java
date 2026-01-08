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
            return key;
        }
    }

    //encrypt/decrypt file
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