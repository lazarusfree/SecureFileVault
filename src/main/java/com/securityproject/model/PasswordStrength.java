package com.securityproject.model;

public record PasswordStrength(boolean isValid, String message) {

    public static PasswordStrength valid() {
        return new PasswordStrength(true, "Password is valid!");
    }

    public static PasswordStrength invalid(String message) {
        return new PasswordStrength(false, message);
    }
}
