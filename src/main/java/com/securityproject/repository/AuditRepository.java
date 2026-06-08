package com.securityproject.repository;

import com.securityproject.model.AuditLog;
import java.util.List;

public interface AuditRepository {
    void log(int userId, String action);

    List<AuditLog> findAll();
}
