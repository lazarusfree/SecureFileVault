package com.securityproject.ui;

import com.securityproject.db.DatabaseManager;
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

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username or password is empty!");
                return;
            }

            if (DatabaseManager.registeruser(username, password)) {
                JOptionPane.showMessageDialog(this, "User successfully registered!");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or password!", "Error.", JOptionPane.ERROR_MESSAGE);
            }
        });
        setVisible(true);
    }
}
