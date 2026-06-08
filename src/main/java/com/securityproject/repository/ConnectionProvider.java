package com.securityproject.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionProvider {
    Connection get() throws SQLException;

    static ConnectionProvider fromUrl(String url) {
        return () -> DriverManager.getConnection(url);
    }
}
