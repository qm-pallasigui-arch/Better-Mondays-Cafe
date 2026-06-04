package ui;

import persistence.AppDatabase;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupPanel extends JPanel {

    private static final Path DB_PATH = Paths.get("data", "coffee-cafe.db");
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Session backup history table
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"File Name", "Type", "Size", "Timestamp"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable historyTable = new JTable(historyModel);

    private final JButton backupBtn      = new JButton("Backup Now");
    private final JButton restoreBtn     = new JButton("Restore from File");
    private final JButton resetProdBtn   = new JButton("Reset for Production");
    private final JLabel  statusLabel    = new JLabel(" ");

    public BackupPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        styleButton(backupBtn,    false);
        styleButton(restoreBtn,   true);
        styleButton(resetProdBtn, true);

        backupBtn.addActionListener(e    -> onBackup());
        restoreBtn.addActionListener(e   -> onRestore());
        resetProdBtn.addActionListener(e -> onResetForProduction());

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(buildHistoryPanel(),    BorderLayout.CENTER);
        center.add(buildResetPanel(),      BorderLayout.SOUTH);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(center,          BorderLayout.CENTER);
        add(statusLabel,     BorderLayout.SOUTH);

        AppTheme.applyToComponent(this);
    }

    // ── UI construction ──────────────────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel info = new JPanel(new GridLayout(0, 1, 4, 4));
        info.setBorder(BorderFactory.createTitledBorder(
                AppTheme.inputBorderRegular(), "Database Backup & Restore",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13)));
        info.add(new JLabel("Database file: " + DB_PATH.toAbsolutePath()));
        info.add(new JLabel("Backup creates a full copy of the database (.db)."));
        info.add(new JLabel("Restore replaces the current database — the app will restart automatically."));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttons.add(backupBtn);
        buttons.add(restoreBtn);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(info,    BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildHistoryPanel() {
        AppTheme.applyTableDefaults(historyTable);
        historyTable.setRowHeight(26);
        historyTable.setFillsViewportHeight(true);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(
                AppTheme.inputBorderRegular(), "Session Backup History",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13)));
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void onBackup() {
        if (!Files.exists(DB_PATH)) {
            showError("Database file not found at: " + DB_PATH.toAbsolutePath());
            return;
        }

        String timestamp = LocalDateTime.now().format(FILE_FMT);
        String defaultName = "backup-" + timestamp + ".db";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Backup As");
        chooser.setSelectedFile(new java.io.File(defaultName));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "SQLite Database (*.db)", "db"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path dest = chooser.getSelectedFile().toPath();
        if (!dest.toString().endsWith(".db")) {
            dest = dest.resolveSibling(dest.getFileName() + ".db");
        }

        final Path finalDest = dest;
        backupBtn.setEnabled(false);
        statusLabel.setText("Creating backup…");

        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                Files.copy(DB_PATH, finalDest, StandardCopyOption.REPLACE_EXISTING);
                return Files.size(finalDest);
            }

            @Override
            protected void done() {
                backupBtn.setEnabled(true);
                try {
                    long bytes = get();
                    String size = formatSize(bytes);
                    String ts   = LocalDateTime.now().format(DISPLAY_FMT);
                    historyModel.insertRow(0, new Object[]{
                            finalDest.getFileName().toString(), "Backup", size, ts});
                    statusLabel.setText("Backup saved: " + finalDest.toAbsolutePath());
                    JOptionPane.showMessageDialog(BackupPanel.this,
                            "<html><b>Backup created successfully.</b><br><br>"
                            + "File: " + finalDest.toAbsolutePath() + "<br>"
                            + "Size: " + size + "</html>",
                            "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    statusLabel.setText("Backup failed.");
                    showError("Backup failed:\n" + rootCause(ex));
                }
            }
        }.execute();
    }

    private void onRestore() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Backup File to Restore");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "SQLite Database (*.db)", "db"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path src = chooser.getSelectedFile().toPath();
        if (!Files.exists(src)) {
            showError("Selected file does not exist.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Warning: this will replace the current database.</b><br><br>"
                + "Source: " + src.toAbsolutePath() + "<br>"
                + "All unsaved changes will be lost.<br><br>"
                + "The application will restart automatically after restore.<br><br>"
                + "Continue?</html>",
                "Confirm Restore",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        restoreBtn.setEnabled(false);
        statusLabel.setText("Restoring…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Reset init flag so schema re-runs after the file is replaced
                AppDatabase.resetForRestore();
                Files.copy(src, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    String ts = LocalDateTime.now().format(DISPLAY_FMT);
                    historyModel.insertRow(0, new Object[]{
                            src.getFileName().toString(), "Restore", "—", ts});
                    JOptionPane.showMessageDialog(BackupPanel.this,
                            "<html><b>Restore complete.</b><br><br>"
                            + "The application will now close and must be restarted manually.</html>",
                            "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                } catch (Exception ex) {
                    restoreBtn.setEnabled(true);
                    statusLabel.setText("Restore failed.");
                    showError("Restore failed:\n" + rootCause(ex));
                }
            }
        }.execute();
    }

    // ── Production reset ─────────────────────────────────────────────────────

    private JPanel buildResetPanel() {
        JPanel info = new JPanel(new GridLayout(0, 1, 4, 4));
        info.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xDC2626), 1),
                "⚠  Production Reset",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(0xDC2626)));

        info.add(new JLabel("Wipes all test/operational data. Keeps: menu items, inventory definitions."));
        info.add(new JLabel("Erases: shifts, sales, transactions, inventory batches, all user accounts."));
        info.add(new JLabel("Default admin and staff accounts are re-created automatically after reset."));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        buttons.add(resetProdBtn);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(info,    BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void onResetForProduction() {
        // First confirmation
        int first = JOptionPane.showConfirmDialog(this,
                "<html><b>⚠ Reset for Production?</b><br><br>"
                + "This will permanently erase:<br>"
                + "&nbsp;&nbsp;• All staff shift records<br>"
                + "&nbsp;&nbsp;• All sales and transaction history<br>"
                + "&nbsp;&nbsp;• All inventory batch quantities<br>"
                + "&nbsp;&nbsp;• All user accounts<br>"
                + "&nbsp;&nbsp;• All password reset requests<br><br>"
                + "The following will be <b>kept</b>:<br>"
                + "&nbsp;&nbsp;• Menu items and ingredients<br>"
                + "&nbsp;&nbsp;• Inventory item definitions<br><br>"
                + "Default admin and staff accounts will be re-created.<br><br>"
                + "<b>This cannot be undone. Are you sure?</b></html>",
                "Reset for Production — Step 1 of 2",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (first != JOptionPane.YES_OPTION) return;

        // Second confirmation — type the word to confirm
        String typed = JOptionPane.showInputDialog(this,
                "<html>Type <b>RESET</b> to confirm the production reset:</html>",
                "Reset for Production — Step 2 of 2",
                JOptionPane.WARNING_MESSAGE);
        if (typed == null || !typed.trim().equals("RESET")) {
            JOptionPane.showMessageDialog(this,
                    "Reset cancelled — confirmation word did not match.",
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        resetProdBtn.setEnabled(false);
        statusLabel.setText("Resetting…");

        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    persistence.AppDatabase.resetForProduction();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                resetProdBtn.setEnabled(true);
                if (errorMsg != null) {
                    statusLabel.setText("Reset failed.");
                    showError("Production reset failed:\n" + errorMsg);
                } else {
                    String ts = LocalDateTime.now().format(DISPLAY_FMT);
                    historyModel.insertRow(0, new Object[]{
                            "— production reset —", "Reset", "—", ts});
                    statusLabel.setText("Production reset completed at " + ts);
                    JOptionPane.showMessageDialog(BackupPanel.this,
                            "<html><b>Production reset complete.</b><br><br>"
                            + "All operational data has been erased.<br>"
                            + "Default admin and staff accounts have been re-created.<br><br>"
                            + "The system is ready for live use.</html>",
                            "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private static String rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void styleButton(JButton btn, boolean danger) {
        btn.putClientProperty("appTheme.variant", danger ? "danger" : "primary");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setMinimumSize(new Dimension(160, 38));
        btn.setMargin(new Insets(8, 14, 8, 14));
        btn.setFocusPainted(false);
    }
}
