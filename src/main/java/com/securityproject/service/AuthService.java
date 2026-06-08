package com.securityproject.service;

import com.securityproject.exception.AuthenticationException;
import com.securityproject.model.User;
import com.securityproject.repository.AuditRepository;
import com.securityproject.repository.UserRepository;
import com.securityproject.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepo;
    private final AuditRepository auditRepo;

    public AuthService(UserRepository userRepo, AuditRepository auditRepo) {
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
    }

    public User authenticate(String username, String plainPassword) {
        Optional<User> userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            LOG.info("Authentication failed: unknown user '{}'", username);
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verify(plainPassword, user.passwordHash())) {
            LOG.info("Authentication failed: wrong password for '{}'", username);
            throw new AuthenticationException("Invalid username or password.");
        }

        auditRepo.log(user.id(), "User logged in.");
        LOG.info("User '{}' authenticated successfully.", username);
        return user;
    }

    public User register(String username, String plainPassword) {
        if (username == null || username.length() < 3) {
            throw new AuthenticationException("Username must be at least 3 characters.");
        }

        PasswordUtil.ValidationResult result = PasswordUtil.validate(plainPassword);
        if (!result.isValid()) {
            throw new AuthenticationException(result.message());
        }

        String hashed = PasswordUtil.hash(plainPassword);
        if (!userRepo.create(username, hashed)) {
            throw new AuthenticationException("Username already exists.");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Registration failed unexpectedly."));
        LOG.info("User '{}' registered.", username);
        return user;
    }

    public boolean isAdmin(int userId) {
        return "ADMIN".equalsIgnoreCase(userRepo.getRole(userId));
    }
}
