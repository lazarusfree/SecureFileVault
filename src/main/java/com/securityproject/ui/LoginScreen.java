package com.securityproject.ui;

import com.securityproject.AppContext;
import com.securityproject.exception.AuthenticationException;
import com.securityproject.model.User;
import com.securityproject.service.AuthService;
import com.securityproject.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;

public class LoginScreen extends JFrame {
    private final AuthService authService;
    private JTextField userField;
    private JPasswordField passField;

    public LoginScreen() {
        this.authService = AppContext.getInstance().auth();

        setTitle("Secure File Vault - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        userField = new JTextField();
        mainPanel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        passField = new JPasswordField();
        mainPanel.add(passField, gbc);

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

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());

        setVisible(true);
    }

    private void handleLogin() {
        String username = userField.getText();
        String password = new String(passField.getPassword());

        try {
            User user = authService.authenticate(username, password);
            dispose();

            if (user.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Welcome, Admin!", "Admin Login",
                        JOptionPane.INFORMATION_MESSAGE);
                new AdminDashboard(user.id());
            } else {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                new Dashboard(user.id());
            }
        } catch (AuthenticationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String username = userField.getText();
        String password = new String(passField.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty.");
            return;
        }

        if (username.length() < 3) {
            JOptionPane.showMessageDialog(this, "Username must be at least 3 characters.");
            return;
        }

        PasswordUtil.ValidationResult result = PasswordUtil.validate(password);
        if (!result.isValid()) {
            JOptionPane.showMessageDialog(this, result.message(), "Invalid Password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            authService.register(username, password);
            JOptionPane.showMessageDialog(this, "User successfully registered.");
            userField.setText("");
            passField.setText("");
        } catch (AuthenticationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
