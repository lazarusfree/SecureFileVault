package com.securityproject;

import com.securityproject.ui.LoginScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LOG.info("Secure File Vault v2.0 starting...");

        AppContext.init();

        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception ex) {
            LOG.warn("Failed to initialize FlatLaf: {}", ex.getMessage());
        }

        SwingUtilities.invokeLater(LoginScreen::new);
    }
}
