package com.securityproject.ui;

import com.securityproject.db.DatabaseManager;
import com.securityproject.utils.SecurityUtils;
import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;

public class LoginScreen extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginScreen() {
        setTitle("Secure File Vault - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen

        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        userField = new JTextField();
        mainPanel.add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        passField = new JPasswordField();
        mainPanel.add(passField, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);

        // Logic
        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            // Admin Login Check
            if (username.equals("admin") && password.equals("admin123")) {
                JOptionPane.showMessageDialog(this, "Welcome, Admin!", "Admin Login", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                new AdminDashboard();
                return;
            }

            int userID = DatabaseManager.authenticateUser(username, password);

            if (userID != -1) {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                DatabaseManager.logAction(userID, "User logged in.");
                // close login screen and -> dashboard
                dispose();
                new Dashboard(userID);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password!", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Register Logic
        registerButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            // Validate username
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be empty!");
                return;
            }

            if (username.length() < 3) {
                JOptionPane.showMessageDialog(this, "Username must be at least 3 characters!");
                return;
            }

            // Validate password
            SecurityUtils.PasswordValidation.ValidationResult result = SecurityUtils.PasswordValidation
                    .validatePassword(password);

            if (!result.isValid) {
                JOptionPane.showMessageDialog(this, result.message, "Invalid Password",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Register user
            if (DatabaseManager.registeruser(username, password)) {
                JOptionPane.showMessageDialog(this, "User successfully registered!");
                // Clear fields after successful registration
                userField.setText("");
                passField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Username already exists!",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
    }
}
