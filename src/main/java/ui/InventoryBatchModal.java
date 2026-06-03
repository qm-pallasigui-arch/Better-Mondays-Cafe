package ui;

import controller.InventoryController;
import inventory.InventoryBatch;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

public class InventoryBatchModal extends JDialog {

    private static final Color OK_FG       = new Color(40,  167, 69);
    private static final Color EXPIRING_FG = new Color(200, 160, 40);
    private static final Color EXPIRED_FG  = new Color(200, 50,  50);
    private static final Color ARCHIVED_FG = new Color(180, 180, 180);

    private static final String[] COLS = {"Batch ID", "SKU", "Quantity", "Expiry", "Status", "Actions"};

    private final String itemName;
    private final InventoryController controller;
    private final Runnable onInventoryRefresh;
    private DefaultTableModel tableModel;
    private JTable table;
    private List<InventoryBatch> currentBatches = new java.util.ArrayList<>();

    public InventoryBatchModal(Window owner, String itemName,
                               InventoryController controller, Runnable onInventoryRefresh) {
        super(owner, "Batches — " + itemName, ModalityType.APPLICATION_MODAL);
        this.itemName = itemName;
        this.controller = controller;
        this.onInventoryRefresh = onInventoryRefresh;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(660, 380));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ── Header ──────────────────────────────────────────────────
        JLabel header = new JLabel("Batches — " + itemName);
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(AppTheme.FG_PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        root.add(header, BorderLayout.NORTH);

        // ── Table ────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        AppTheme.applyTableDefaults(table);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setRowMargin(4);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(32);

        int[] widths = {105, 130, 70, 105, 90, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < COLS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(center);

        table.getColumnModel().getColumn(4).setCellRenderer(new BatchStatusRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new ActionsDotRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (col == 5 && row >= 0) showActionsMenu(e, row);
            }
        });

        populateTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new RoundedLineBorder(AppTheme.BORDER, AppTheme.BORDER_THICKNESS, AppTheme.BORDER_RADIUS));
        scroll.getViewport().setBackground(AppTheme.BG_SURFACE);
        root.add(scroll, BorderLayout.CENTER);

        // ── Close button ─────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.setBackground(AppTheme.BG_SURFACE);
        closeBtn.setForeground(AppTheme.FG_PRIMARY);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedLineBorder(AppTheme.BORDER, AppTheme.BORDER_THICKNESS, AppTheme.BORDER_RADIUS),
            BorderFactory.createEmptyBorder(7, 18, 7, 18)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        btnRow.add(closeBtn);
        root.add(btnRow, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    // ── Table population ──────────────────────────────────────────────
    private void populateTable() {
        tableModel.setRowCount(0);
        currentBatches = controller.getBatchesForItem(itemName);
        List<InventoryBatch> batches = currentBatches;
        if (batches.isEmpty()) {
            tableModel.addRow(new Object[]{"", "", "", "", "—", "No batches"});
            return;
        }
        for (InventoryBatch b : batches) {
            String batchId = b.getId() > 0 ? String.format("INV-%06d", b.getId()) : "—";
            String sku     = b.getSku() != null ? b.getSku() : "—";
            String qty     = String.valueOf(b.getQuantity());
            String expiry  = (b.getExpiryDate() != null && !b.getExpiryDate().isBlank())
                             ? b.getExpiryDate() : "—";
            String status  = b.isArchived() ? "ARCHIVED" : computeStatus(b.getExpiryDate());
            tableModel.addRow(new Object[]{batchId, sku, qty, expiry, status, "•••"});
        }
    }

    // ── Actions popup ─────────────────────────────────────────────────
    private void showActionsMenu(MouseEvent e, int row) {
        if (row >= currentBatches.size()) return;
        InventoryBatch batch = currentBatches.get(row);
        boolean archived = batch.isArchived();

        JPopupMenu menu = new JPopupMenu();

        if (!archived) {
            JMenuItem useItem = new JMenuItem("Use (deduct quantity)");
            useItem.addActionListener(ae -> handleUse(batch));
            menu.add(useItem);

            JMenuItem archiveItem = new JMenuItem("Archive batch");
            archiveItem.addActionListener(ae -> handleArchive(batch));
            menu.add(archiveItem);

            menu.addSeparator();
        }

        JMenuItem deleteItem = new JMenuItem("Delete batch");
        deleteItem.setForeground(AppTheme.DANGER);
        deleteItem.addActionListener(ae -> handleDelete(batch));
        menu.add(deleteItem);

        menu.show(table, e.getX(), e.getY());
    }

    private void handleUse(InventoryBatch batch) {
        String input = JOptionPane.showInputDialog(this,
                String.format("<html>Enter quantity to deduct from batch <b>INV-%06d</b><br>"
                        + "Available: <b>%s</b></html>",
                        batch.getId(), batch.getQuantity()),
                "Use Batch", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        double amount;
        try {
            amount = Double.parseDouble(input.trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive number.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (amount > batch.getQuantity()) {
            JOptionPane.showMessageDialog(this,
                    "Amount exceeds available quantity (" + batch.getQuantity() + ").",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            controller.deductFromBatch(batch.getId(), amount, itemName);
            onInventoryRefresh.run();
            populateTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleArchive(InventoryBatch batch) {
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("<html>Archive batch <b>INV-%06d</b>?<br>"
                        + "It will be removed from active inventory but kept for records.</html>",
                        batch.getId()),
                "Archive Batch", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            controller.archiveBatch(batch.getId(), itemName);
            onInventoryRefresh.run();
            populateTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete(InventoryBatch batch) {
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("<html>Permanently delete batch <b>INV-%06d</b>?<br>"
                        + "This cannot be undone.</html>", batch.getId()),
                "Delete Batch", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            controller.deleteBatch(batch.getId(), itemName);
            onInventoryRefresh.run();
            populateTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private static String computeStatus(String expiryDate) {
        if (expiryDate == null || expiryDate.isBlank()) return "OK";
        try {
            LocalDate exp = LocalDate.parse(expiryDate);
            LocalDate today = LocalDate.now();
            if (!exp.isAfter(today))                 return "EXPIRED";
            if (!exp.isAfter(today.plusDays(7)))     return "EXPIRING";
        } catch (Exception ignored) {}
        return "OK";
    }

    // ── Renderers ─────────────────────────────────────────────────────
    private static class BatchStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                String text = value == null ? "" : value.toString();
                Color fg;
                switch (text) {
                    case "OK"       -> fg = OK_FG;
                    case "EXPIRING" -> fg = EXPIRING_FG;
                    case "EXPIRED"  -> fg = EXPIRED_FG;
                    case "ARCHIVED" -> fg = ARCHIVED_FG;
                    default         -> fg = AppTheme.FG_MUTED;
                }
                if (!sel) label.setForeground(fg);
            }
            return c;
        }
    }

    private static class ActionsDotRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            JLabel label = new JLabel("•••", SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setForeground(sel ? Color.WHITE : AppTheme.FG_MUTED);
            label.setBackground(sel ? t.getSelectionBackground() : t.getBackground());
            label.setOpaque(true);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return label;
        }
    }
}
