package com.securityproject.repository;

import com.securityproject.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    Optional<User> findById(int id);

    List<User> findAll();

    boolean create(String username, String passwordHash);

    boolean createAdmin(String username, String passwordHash);

    String getRole(int userId);
}
