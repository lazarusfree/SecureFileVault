package com.securityproject.ui;

import com.securityproject.AppContext;
import com.securityproject.model.AuditLog;
import com.securityproject.model.User;
import com.securityproject.model.VaultFile;
import com.securityproject.repository.AuditRepository;
import com.securityproject.repository.FileRepository;
import com.securityproject.repository.UserRepository;
import com.securityproject.util.Cowsay;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminDashboard extends JFrame {
    private final int adminUserId;
    private final UserRepository userRepo;
    private final FileRepository fileRepo;
    private final AuditRepository auditRepo;

    public AdminDashboard(int adminUserId) {
        this.adminUserId = adminUserId;
        AppContext ctx = AppContext.getInstance();
        this.userRepo = ctx.users();
        this.fileRepo = ctx.files();
        this.auditRepo = ctx.audit();

        setTitle("Secure File Vault - Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        JLabel headerLabel = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Users tab
        String[] userColumns = {"ID", "Username", "Role"};
        DefaultTableModel userModel = new DefaultTableModel(userColumns, 0);
        JTable userTable = new JTable(userModel);
        tabbedPane.addTab("Users", new JScrollPane(userTable));

        // Files tab
        String[] fileColumns = {"ID", "User ID", "User", "Original Path", "Encrypted Path",
                "Algorithm", "SHA-256", "Created"};
        DefaultTableModel fileModel = new DefaultTableModel(fileColumns, 0);
        JTable fileTable = new JTable(fileModel);
        tabbedPane.addTab("Encrypted Files", new JScrollPane(fileTable));

        // Audit tab
        String[] auditColumns = {"ID", "User ID", "User", "Action", "Timestamp"};
        DefaultTableModel auditModel = new DefaultTableModel(auditColumns, 0);
        JTable auditTable = new JTable(auditModel);
        tabbedPane.addTab("Audit Logs", new JScrollPane(auditTable));

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        loadData(userModel, fileModel, auditModel);

        JPanel southPanel = new JPanel(new BorderLayout());
        JTextArea cowArea = new JTextArea();
        cowArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cowArea.setEditable(false);
        cowArea.setText(Cowsay.say("Welcome, Admin! I'm keeping an eye on things."));
        southPanel.add(cowArea, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> {
            loadData(userModel, fileModel, auditModel);
            cowArea.setText(Cowsay.say("Data refreshed."));
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            auditRepo.log(adminUserId, "Admin logged out.");
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

    private void loadData(DefaultTableModel userModel, DefaultTableModel fileModel, DefaultTableModel auditModel) {
        userModel.setRowCount(0);
        fileModel.setRowCount(0);
        auditModel.setRowCount(0);

        for (User u : userRepo.findAll()) {
            userModel.addRow(new Object[]{u.id(), u.username(), u.role()});
        }

        for (VaultFile f : fileRepo.findAll()) {
            fileModel.addRow(new Object[]{
                    f.id(), f.userId(), f.username(),
                    f.originalPath(), f.encryptedPath(),
                    f.algorithm(), f.integrityHash(), f.createdAt()
            });
        }

        for (AuditLog a : auditRepo.findAll()) {
            auditModel.addRow(new Object[]{
                    a.id(), a.userId(), a.username(), a.action(), a.timestamp()
            });
        }
    }
}
