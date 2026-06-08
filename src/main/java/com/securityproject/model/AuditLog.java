package com.securityproject.model;

public record AuditLog(int id, int userId, String username, String action, String timestamp) {
}
