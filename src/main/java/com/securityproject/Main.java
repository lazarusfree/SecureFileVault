package com.securityproject;

import com.securityproject.db.DatabaseManager;
import com.securityproject.ui.LoginScreen;
//import com.securityproject.utils.SecurityUtils;

//import javax.crypto.SecretKey;
//import java.io.File;
//import java.nio.file.Files;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        /*
         * try { //phase3
         * System.out.println("Starting Secure File Vault...");
         * 
         * //1. init. db
         * DatabaseManager.initialize();
         * //2. test pwd hashing
         * String originalPw = "secret123";
         * String hashed = SecurityUtils.hashPassword(originalPw);
         * System.out.println("Hashed Password: " + hashed);
         * System.out.println("Password valid? " +
         * SecurityUtils.checkPassword(originalPw, hashed));
         * //3. test encrypttion
         * SecretKey key = SecurityUtils.loadOrGenerateKey();
         * 
         * //create dummy file
         * File testFile = new File("secret.txt");
         * Files.writeString(testFile.toPath(), "This is secret data.");
         * File encryptedFile = new File("secret.enc");
         * File decryptedFile = new File("secret_decrypted.txt");
         * 
         * //enc. -> dec.
         * SecurityUtils.encryptFile(testFile, encryptedFile, key);
         * System.out.println("File Encrypted: " + encryptedFile.getAbsolutePath());
         * SecurityUtils.decryptFile(encryptedFile, decryptedFile, key);
         * System.out.println("File Decrypted: " + decryptedFile.getAbsolutePath());
         * 
         * } catch (Exception e) {
         * e.printStackTrace();
         * }
         */

        // init. database
        System.out.println("--- System Startup ---");
        DatabaseManager.initialize();

        // setup flatlaf
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // start gui on event dispatchthread
        SwingUtilities.invokeLater(() -> {
            new LoginScreen();
        });

        /*
         * //register
         * System.out.println("\n[1] Registering user 'rezza'...");
         * boolean registered = DatabaseManager.registeruser("rezza",
         * "superDuperUltraSecretPassword");
         * if (registered) {
         * System.out.println(" -> Successfully registered user 'rezza'");
         * } else {
         * System.out.
         * println(" -> Failed to registered user 'rezza'. User already exists.");
         * }
         * 
         * //login (fail case)
         * System.out.println("\n[2] Attempting login with WRONG password...");
         * int userID = DatabaseManager.authenticateUser("rezza",
         * "superDuperUltraSecretWrongPassword");
         * if (userID == -1) {
         * System.out.println(" -> Failed to authenticate user 'rezza'");
         * } else {
         * System.out.println(" -> ERROR: Somehow we accepted wrong password...");
         * }
         * 
         * //login (success case)
         * System.out.println("\n[3] Attempting login with CORRECT password...");
         * userID = DatabaseManager.authenticateUser("rezza",
         * "superDuperUltraSecretPassword");
         * 
         * if (userID != -1) {
         * System.out.println(" -> Successfully authenticated user '" + userID + "'");
         * //log actionn
         * System.out.println("\n[4] Logging a sensitive action...");
         * DatabaseManager.logAction(userID, "user encrypted a file named secret.txt");
         * System.out.println(" -> Action logged to database.");
         * } else {
         * System.out.println(" -> Login Failed!");
         * }
         */

    }
}