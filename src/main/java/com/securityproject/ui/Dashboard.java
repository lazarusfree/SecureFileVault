package com.securityproject.ui;

import com.securityproject.AppContext;
import com.securityproject.exception.FileAccessDeniedException;
import com.securityproject.exception.VaultException;
import com.securityproject.service.EncryptionService;
import com.securityproject.util.Cowsay;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Dashboard extends JFrame {
    private final int currentUserId;
    private final EncryptionService crypto;
    private JTextArea cowArea;

    public Dashboard(int userId) {
        this.currentUserId = userId;
        this.crypto = AppContext.getInstance().crypto();

        setTitle("Secure File Vault - Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        JLabel welcomeLabel = new JLabel("Welcome! You are logged in as User ID: " + userId, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(welcomeLabel, gbc);

        gbc.gridy = 1;
        JLabel statusLabel = new JLabel("Ready...", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        mainPanel.add(statusLabel, gbc);

        gbc.gridy = 2;
        JPanel securityPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        securityPanel.add(new JLabel("Confidentiality: AES-256-GCM", SwingConstants.CENTER));
        securityPanel.add(new JLabel("Integrity: GCM tag + SHA-256", SwingConstants.CENTER));
        securityPanel.add(new JLabel("Authenticity: login + owner metadata", SwingConstants.CENTER));
        mainPanel.add(securityPanel, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        cowArea = new JTextArea();
        cowArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cowArea.setEditable(false);
        cowArea.setText(Cowsay.say("Hello! I am your secure vault guardian Moonpie!"));
        mainPanel.add(new JScrollPane(cowArea), gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton encryptBtn = new JButton("Encrypt File");
        JButton decryptBtn = new JButton("Decrypt File");
        JButton logoutBtn = new JButton("Log out");

        buttonPanel.add(encryptBtn);
        buttonPanel.add(decryptBtn);
        buttonPanel.add(logoutBtn);

        gbc.gridy = 4;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        mainPanel.add(buttonPanel, gbc);

        encryptBtn.addActionListener(e -> handleEncrypt(statusLabel));
        decryptBtn.addActionListener(e -> handleDecrypt(statusLabel));
        logoutBtn.addActionListener(e -> {
            AppContext.getInstance().audit().log(currentUserId, "User logged out.");
            dispose();
            new LoginScreen();
        });

        setVisible(true);
    }

    private void handleEncrypt(JLabel statusLabel) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File input = chooser.getSelectedFile();
        File output = new File(input.getAbsolutePath() + ".enc");

        try {
            EncryptionService.EncryptionResult result = crypto.encrypt(input, output, currentUserId);
            statusLabel.setText("Encrypted: " + output.getName());
            cowArea.setText(Cowsay.say("File locked with authenticated encryption."));
            showHtmlMessage("File encrypted successfully!<br>Saved as: <b>" + output.getName()
                            + "</b><br>Algorithm: <b>" + result.algorithm()
                            + "</b><br>SHA-256: <b>" + result.ciphertextSha256() + "</b>",
                    "Success");
        } catch (VaultException ex) {
            AppContext.getInstance().audit().log(currentUserId, "Encryption failed: " + ex.getMessage());
            statusLabel.setText("Error: " + ex.getMessage());
            cowArea.setText(Cowsay.say("Uh oh... something went wrong."));
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDecrypt(JLabel statusLabel) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File input = chooser.getSelectedFile();
        String name = input.getAbsolutePath();
        String outName = name.endsWith(".enc")
                ? name.substring(0, name.length() - ".enc".length())
                : name + "_decrypted";
        File output = new File(outName);

        try {
            EncryptionService.DecryptionResult result = crypto.decrypt(input, output, currentUserId);
            statusLabel.setText("Decrypted: " + output.getName());
            cowArea.setText(Cowsay.say("File unlocked after GCM verification."));
            showHtmlMessage("File decrypted successfully!<br>Saved as: <b>" + output.getName()
                            + "</b><br>Verified: <b>" + result.algorithm() + "</b>",
                    "Success");
        } catch (FileAccessDeniedException ex) {
            cowArea.setText(Cowsay.say("MOOO! STOP! You don't own this file!"));
            JOptionPane.showMessageDialog(this,
                    "Access Denied: " + ex.getMessage(), "Security Alert",
                    JOptionPane.ERROR_MESSAGE);
        } catch (VaultException ex) {
            AppContext.getInstance().audit().log(currentUserId, "Decryption failed: " + ex.getMessage());
            statusLabel.setText("Error: " + ex.getMessage());
            cowArea.setText(Cowsay.say("Uh oh... something went wrong."));
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHtmlMessage(String msg, String title) {
        JLabel label = new JLabel("<html><body style='width: 300px'>" + msg + "</body></html>");
        JOptionPane.showMessageDialog(this, label, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
