package com.securityproject.util;

import com.securityproject.config.VaultConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Set;

public final class PasswordUtil {
    private static final String[] COMMON_PASSWORDS = {
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "letmein", "trustno1", "dragon", "baseball",
            "iloveyou", "master", "welcome", "shadow", "ashley",
            "football", "jesus", "michael", "ninja", "mustang"
    };

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(VaultConfig.BCRYPT_LOG_ROUNDS));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    public record ValidationResult(boolean isValid, String message) {}

    public static ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty.");
        }

        if (password.length() < VaultConfig.PASSWORD_MIN_LENGTH) {
            return new ValidationResult(false,
                    "Password must be at least " + VaultConfig.PASSWORD_MIN_LENGTH + " characters.");
        }

        if (password.length() > VaultConfig.PASSWORD_MAX_LENGTH) {
            return new ValidationResult(false,
                    "Password must be at most " + VaultConfig.PASSWORD_MAX_LENGTH + " characters.");
        }

        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        int score = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (score < 3) {
            return new ValidationResult(false,
                    "Password must contain at least 3 of: uppercase, lowercase, numbers, special characters.");
        }

        String lower = password.toLowerCase();
        for (String common : COMMON_PASSWORDS) {
            if (lower.equals(common)) {
                return new ValidationResult(false,
                        "Password is too common. Please choose a stronger password.");
            }
        }

        return new ValidationResult(true, "Password is valid.");
    }

    public static String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
