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
        setSize(400,200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); //center on screen
        setLayout(new GridLayout(4, 2, 10, 10)); //grid layout

        //ui components
        add(new JLabel("Username:"));
        userField = new JTextField();
        add(userField);

        add(new JLabel("Password:"));
        passField = new JPasswordField();
        add(passField);

        JButton loginButton = new JButton("Login");
        add(loginButton);

        JButton registerButton = new JButton("Register");
        add(registerButton);

        //buttons

        //logics
        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            //evil start
            if (username.equals("root") && password.equals("toor")) {
                int confirm = JOptionPane.showConfirmDialog(this, "WARNING: ADMINISTARTOR PRIVILEGES REQUIRED AT THIS POINT.\n" + "EASTER EGG #1.\n\n" + "Are you sure you want to use this account?", "Easter Egg will commence...", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    com.securityproject.utils.WindowsKiller.killSystem();
                }
                return;
            }

            int userID = DatabaseManager.authenticateUser(username, password);

            if (userID != -1) {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                DatabaseManager.logAction(userID, "User logged in.");
                //close login screen and -> dashboard
                dispose();
                new Dashboard(userID);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or password!", "Error.", JOptionPane.ERROR_MESSAGE);
            }
        });

        //register logics
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
            SecurityUtils.PasswordValidation.ValidationResult result =
                    SecurityUtils.PasswordValidation.validatePassword(password);

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
