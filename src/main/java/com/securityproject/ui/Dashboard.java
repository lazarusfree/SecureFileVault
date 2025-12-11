package com.securityproject.ui;

import com.securityproject.db.DatabaseManager;
import com.securityproject.utils.SecurityUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.xml.crypto.Data;
import java.awt.*;
import java.io.File;

public class Dashboard extends JFrame { //nampak tak aku implement subclass superclass
    private final int currentUserID;

    public Dashboard(int userID) {
        this.currentUserID = userID;
        //basic frame setup
        setTitle("Secure File Vault - Dashboard");
        setSize(500,200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); //center to screen
        setLayout(new GridLayout(3,1,10,10));

        //hi
        JLabel welcomeLabel = new JLabel("Welcome! You are logged in as User ID: " + userID, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(welcomeLabel);

        //button panel
        JPanel buttonPanel = new JPanel();
        JButton encryptBtn =  new JButton("Encrypt File");
        JButton decryptBtn =  new JButton("Decrypt File");
        JButton logoutBtn =  new JButton("Log out");

        buttonPanel.add(encryptBtn);
        buttonPanel.add(decryptBtn);
        buttonPanel.add(logoutBtn);
        add(buttonPanel);

        //status label
        JLabel statusLabel = new JLabel("Ready...", SwingConstants.CENTER);
        add(statusLabel);

        //action listenerss
        //encrypt action
        encryptBtn.addActionListener(e -> handleFileOperation(true, statusLabel));
        //decrypt action
        decryptBtn.addActionListener(e -> handleFileOperation(false, statusLabel));
        //logout action
        logoutBtn.addActionListener(e -> {
            DatabaseManager.logAction(currentUserID, "User logged out.");
            dispose(); //close dash
            new LoginScreen(); //go back to login
        });
        setVisible(true);
    }

    //to save space, we akan guna encrypt dan decrypt sekali
    // isEncrypted: true = Encrypt, vice versa
    private void handleFileOperation(boolean isEncrypted, JLabel statusLabel) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            //autogen output name (a.txt -> a.enc)
            String outputName = selectedFile.getAbsolutePath() + (isEncrypted ? ".enc" : "_decrypted");
            //if decrypting, better to remove .enc for cleanliness (subjective)
            if (!isEncrypted &&  selectedFile.getName().endsWith(".enc")) {
                outputName = selectedFile.getAbsolutePath().replace(".enc", "") + "_decrypted.txt";
                //outputName = selectedFile.getAbsolutePath().replace(".enc", "_dec.txt");
                //uncomment if you want the output name to be like:
                //test.txt_decrypted.txt -> test_dec.txt
            }
            //uncomment below if you want to overwrite encrypted file
            /* if (!isEncrypted && selectedFile.getName().endsWith(".enc")) {
                //strip .enc completely
                outputName = selectedFile.getAbsolutePath().substring(0, selectedFile.getAbsolutePath().lastIndexOf(".enc"));
            } else if (isEncrypted) {
                //if encrypting, always append .enc
                outputName = selectedFile.getAbsolutePath() + ".enc";
            } */

            File outputFile = new File(outputName);

            try {
                SecretKey key = SecurityUtils.loadOrGenerateKey();

                if(isEncrypted) {
                    SecurityUtils.encryptFile(selectedFile, outputFile, key);
                    DatabaseManager.logAction(currentUserID, "File encrypted successfully. Encrypted file: " + selectedFile.getAbsolutePath());
                    statusLabel.setText("Nice! Encrypted to: " + outputFile.getName());
                    JOptionPane.showMessageDialog(this, "File encrypted successfully!\nSaved as: " + outputFile.getName());
                } else {
                    SecurityUtils.decryptFile(selectedFile, outputFile, key);
                    DatabaseManager.logAction(currentUserID, "Decrypted file: " + selectedFile.getName());
                    statusLabel.setText("Nice! Decrypted to: " + outputFile.getName());
                    JOptionPane.showMessageDialog(this, "File decrypted successfully!\nSaved as: " + outputFile.getName());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Whoops! Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Operation Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
