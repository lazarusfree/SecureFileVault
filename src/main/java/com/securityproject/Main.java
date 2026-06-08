package com.securityproject;

import com.securityproject.db.DatabaseManager;
import com.securityproject.ui.LoginScreen;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Secure File Vault Startup ---");
        DatabaseManager.initialize();

        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(LoginScreen::new);
    }
}