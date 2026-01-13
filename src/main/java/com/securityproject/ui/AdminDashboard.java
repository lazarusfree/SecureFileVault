package com.securityproject.ui;

import com.securityproject.db.DatabaseManager;
import com.securityproject.utils.Cowsay;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminDashboard extends JFrame {

    public AdminDashboard() {
        setTitle("Secure File Vault - Admin Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        // Header
        JLabel headerLabel = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Tabbed Pane for Data
        JTabbedPane tabbedPane = new JTabbedPane();

        // Users Tab
        JPanel usersPanel = new JPanel(new BorderLayout());
        String[] userColumns = { "ID", "Username", "Password Hash" };
        DefaultTableModel userModel = new DefaultTableModel(userColumns, 0);
        JTable userTable = new JTable(userModel);
        usersPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        tabbedPane.addTab("Users", usersPanel);

        // Audit Logs Tab
        JPanel auditPanel = new JPanel(new BorderLayout());
        String[] auditColumns = { "ID", "User ID", "User", "Action", "Timestamp" };
        DefaultTableModel auditModel = new DefaultTableModel(auditColumns, 0);
        JTable auditTable = new JTable(auditModel);
        auditPanel.add(new JScrollPane(auditTable), BorderLayout.CENTER);
        tabbedPane.addTab("Audit Logs", auditPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Load Data
        loadData(userModel, auditModel);

        // Cowsay Panel (South)
        JPanel southPanel = new JPanel(new BorderLayout());
        JTextArea cowArea = new JTextArea();
        cowArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cowArea.setEditable(false);
        cowArea.setText(Cowsay.say("Welcome, Admin! I'm keeping an eye on things."));
        southPanel.add(cowArea, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> {
            loadData(userModel, auditModel);
            cowArea.setText(Cowsay.say("Data refreshed! Moo!"));
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginScreen();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshBtn);
        buttonPanel.add(logoutBtn);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadData(DefaultTableModel userModel, DefaultTableModel auditModel) {
        // Clear existing data
        userModel.setRowCount(0);
        auditModel.setRowCount(0);

        // Load Users
        List<String[]> users = DatabaseManager.getAllUsers();
        for (String[] user : users) {
            userModel.addRow(user);
        }

        // Load Audit Logs
        List<String[]> logs = DatabaseManager.getAllAuditLogs();
        for (String[] log : logs) {
            auditModel.addRow(log);
        }
    }
}
