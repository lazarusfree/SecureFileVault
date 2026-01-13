package com.securityproject.ui;

import com.securityproject.db.DatabaseManager;
import com.securityproject.utils.SecurityUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;
import java.io.File;

public class Dashboard extends JFrame {
    private final int currentUserID;
    private JTextArea cowArea;

    public Dashboard(int userID) {
        this.currentUserID = userID;
        // Basic frame setup
        setTitle("Secure File Vault - Dashboard");
        setSize(800, 600); // Increased size for the cow
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center to screen

        // Main Panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Welcome Label
        gbc.gridy = 0;
        JLabel welcomeLabel = new JLabel("Welcome! You are logged in as User ID: " + userID, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        mainPanel.add(welcomeLabel, gbc);

        // Status Label
        gbc.gridy = 1;
        JLabel statusLabel = new JLabel("Ready...", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        mainPanel.add(statusLabel, gbc);

        // Cow Area
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        cowArea = new JTextArea();
        cowArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cowArea.setEditable(false);
        cowArea.setText(com.securityproject.utils.Cowsay.say("Hello! I am your secure vault guardian Moonpie!"));
        mainPanel.add(new JScrollPane(cowArea), gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton encryptBtn = new JButton("Encrypt File");
        JButton decryptBtn = new JButton("Decrypt File");
        JButton logoutBtn = new JButton("Log out");

        buttonPanel.add(encryptBtn);
        buttonPanel.add(decryptBtn);
        buttonPanel.add(logoutBtn);

        gbc.gridy = 3;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        mainPanel.add(buttonPanel, gbc);

        // Action Listeners
        encryptBtn.addActionListener(e -> handleFileOperation(true, statusLabel));
        decryptBtn.addActionListener(e -> handleFileOperation(false, statusLabel));
        logoutBtn.addActionListener(e -> {
            DatabaseManager.logAction(currentUserID, "User logged out.");
            dispose(); // Close dashboard
            new LoginScreen(); // Go back to login
        });

        setVisible(true);
    }

    private void handleFileOperation(boolean isEncrypted, JLabel statusLabel) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Auto-generate output name
            String outputName = selectedFile.getAbsolutePath() + (isEncrypted ? ".enc" : "_decrypted");

            // Clean up decryption name if possible
            if (!isEncrypted && selectedFile.getName().endsWith(".enc")) {
                outputName = selectedFile.getAbsolutePath().substring(0,
                        selectedFile.getAbsolutePath().lastIndexOf(".enc"));
            }

            File outputFile = new File(outputName);

            try {
                SecretKey key = SecurityUtils.loadOrGenerateKey();

                if (isEncrypted) {
                    SecurityUtils.encryptFile(selectedFile, outputFile, key);

                    // RECORD OWNERSHIP
                    DatabaseManager.addFile(currentUserID, selectedFile.getAbsolutePath() + ".enc");

                    DatabaseManager.logAction(currentUserID,
                            "File encrypted successfully. Encrypted file: " + selectedFile.getAbsolutePath());
                    statusLabel.setText("Encrypted: " + outputFile.getName());
                    cowArea.setText(com.securityproject.utils.Cowsay.say("File locked! Only you can open it now."));
                    HtmlMessage("File encrypted successfully!<br>Saved as: <b>" + outputFile.getName() + "</b>",
                            "Success");
                } else {
                    // CHECK OWNERSHIP
                    if (!DatabaseManager.checkFileAccess(currentUserID, selectedFile.getAbsolutePath())) {
                        cowArea.setText(com.securityproject.utils.Cowsay.say("MOOO! STOP! You don't own this file!"));
                        JOptionPane.showMessageDialog(this,
                                "Access Denied: You cannot decrypt a file you didn't encrypt.", "Security Alert",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    SecurityUtils.decryptFile(selectedFile, outputFile, key);
                    DatabaseManager.logAction(currentUserID, "Decrypted file: " + selectedFile.getName());
                    statusLabel.setText("Decrypted: " + outputFile.getName());
                    cowArea.setText(com.securityproject.utils.Cowsay.say("File unlocked. Enjoy your secrets!"));
                    HtmlMessage("File decrypted successfully!<br>Saved as: <b>" + outputFile.getName() + "</b>",
                            "Success");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Error: " + ex.getMessage());
                cowArea.setText(com.securityproject.utils.Cowsay.say("Uh oh... something went wrong."));
                JOptionPane.showMessageDialog(this, "Operation Failed: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void HtmlMessage(String msg, String title) {
        JLabel label = new JLabel("<html><body style='width: 300px'>" + msg + "</body></html>");
        JOptionPane.showMessageDialog(this, label, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
