package com.securityproject.model;

public record User(int id, String username, String passwordHash, String role) {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
