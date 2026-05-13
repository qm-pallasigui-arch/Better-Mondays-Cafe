package ui;

import inventory.InventoryBatch;
import persistence.InventoryRepository;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import inventory.validation.InventoryBatchValidator;

public class InventoryRegistrationPanel extends JPanel {

    private final InventoryRepository inventoryRepository;

    private JTextField itemNameField = new JTextField(20);
    private JTextField skuField = new JTextField(12);
    private JTextField qtyField = new JTextField(8);
    private JTextField expiryField = new JTextField(12); // ISO date or empty
    private JButton addButton = new JButton("Add Batch");
    private JButton refreshButton = new JButton("Refresh Batches");
    private JTable batchesTable = new JTable();
    private DefaultTableModel batchesModel = new DefaultTableModel(
            new String[]{"Batch ID", "SKU", "Quantity", "Expiry", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public InventoryRegistrationPanel(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridLayout(6,2,6,6));
        form.add(new JLabel("Item name")); form.add(itemNameField);
        form.add(new JLabel("SKU (optional)")); form.add(skuField);
        form.add(new JLabel("Quantity")); form.add(qtyField);
        form.add(new JLabel("Expiry (YYYY-MM-DD, optional)")); form.add(expiryField);
        form.add(new JLabel()); form.add(addButton);
        form.add(new JLabel()); form.add(refreshButton);
        add(form, BorderLayout.NORTH);

        batchesTable.setModel(batchesModel);
        add(new JScrollPane(batchesTable), BorderLayout.CENTER);

        addButton.addActionListener(this::onAddBatch);
        refreshButton.addActionListener(this::onRefreshBatches);
        AppTheme.applyToComponent(this);
    }

    private void onAddBatch(ActionEvent e) {
        String name = itemNameField.getText().trim();
        String sku = skuField.getText().trim();
        String qtys = qtyField.getText().trim();
        String expiry = expiryField.getText().trim();
        if (name.isEmpty() || qtys.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter item name and quantity", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double qty;
        String normalizedExpiry;
        try {
            qty = InventoryBatchValidator.parseQuantity(qtys);
            normalizedExpiry = InventoryBatchValidator.normalizeExpiry(expiry);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String finalSku = sku.isEmpty() ? autoGenerateSku(name) : sku;
            InventoryBatch batch = new InventoryBatch(finalSku, qty, normalizedExpiry);
            inventoryRepository.addBatch(name, batch);
            JOptionPane.showMessageDialog(this, "Batch added", "Success", JOptionPane.INFORMATION_MESSAGE);
            itemNameField.setText(""); skuField.setText(""); qtyField.setText(""); expiryField.setText("");
            refreshBatches(name);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to add batch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRefreshBatches(ActionEvent e) {
        String item = itemNameField.getText().trim();
        if (item.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an item name to list batches", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        refreshBatches(item);
    }

    private void refreshBatches(String itemName) {
        batchesModel.setRowCount(0);
        try {
            List<InventoryBatch> batches = inventoryRepository.findBatchesForItem(itemName);
            for (InventoryBatch b : batches) {
                String status = "OK";
                if (b.getExpiryDate() != null && !b.getExpiryDate().isBlank()) {
                    LocalDate d = LocalDate.parse(b.getExpiryDate());
                    if (d.isBefore(LocalDate.now())) {
                        status = "EXPIRED";
                    } else if (!d.isAfter(LocalDate.now().plusDays(7))) {
                        status = "EXPIRING<7D";
                    }
                }
                batchesModel.addRow(new Object[]{b.getId(), b.getSku(), b.getQuantity(), b.getExpiryDate(), status});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to load batches: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String autoGenerateSku(String itemName) {
        String compact = itemName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (compact.length() > 8) {
            compact = compact.substring(0, 8);
        }
        return compact + "-" + System.currentTimeMillis() % 100000;
    }
}
