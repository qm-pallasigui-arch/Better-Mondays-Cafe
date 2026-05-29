package monitoring;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import inventory.Inventory;
import inventory.InventoryItem;
import persistence.sqlite.SQLiteInventoryRepository;

public class Monitoring {

    private JTable jTableMonitoring;
    private DefaultTableModel monitoringModel;

    private JTable jTableSales;
    private DefaultTableModel salesModel;

    public Monitoring(JTable monitoringTable, JTable salesTable) {
        this.jTableMonitoring = monitoringTable;
        this.jTableSales = salesTable;
        setupMonitoringTable();
        setupSalesTable();
    }

    private void setupMonitoringTable() {
        monitoringModel = new DefaultTableModel(
                new String[]{"Ingredient", "Quantity", "Unit", "Alert Level", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTableMonitoring.setModel(monitoringModel);
    }

    private void setupSalesTable() {
        salesModel = new DefaultTableModel(
                new String[]{"Product", "Quantity", "Price per Unit", "Total"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTableSales.setModel(salesModel);
    }

    public void loadLowStockIngredients() {
        monitoringModel.setRowCount(0);
        try {
            SQLiteInventoryRepository repository = new SQLiteInventoryRepository();
            boolean foundAlert = false;
            for (InventoryItem item : repository.findAll()) {
                if (item.isLowStock() || item.isOutOfStock()) {
                    foundAlert = true;
                    String status = item.isOutOfStock() ? "Out of Stock" : "Low Stock";
                    monitoringModel.addRow(new Object[]{
                        item.getName(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getAlertLevel(),
                        status
                    });
                }
            }
            if (!foundAlert) {
                monitoringModel.addRow(new Object[]{"No alerts", "-", "-", "-", "All inventory above alert level"});
            }
        } catch (Exception e) {
            Inventory inventory = Inventory.getInstance();
            boolean foundAlert = false;
            for (InventoryItem item : inventory.getAllItems().values()) {
                if (item.isLowStock() || item.isOutOfStock()) {
                    foundAlert = true;
                    String status = item.isOutOfStock() ? "Out of Stock" : "Low Stock";
                    monitoringModel.addRow(new Object[]{
                        item.getName(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getAlertLevel(),
                        status
                    });
                }
            }
            if (!foundAlert) {
                monitoringModel.addRow(new Object[]{"No alerts", "-", "-", "-", "Good"});
            }
        }
    }

    public void addMultipleSales(List<SalesRecord> salesList) {
        for (SalesRecord sale : salesList) {
            salesModel.addRow(new Object[]{
                sale.getProductName(),
                sale.getQuantity(),
                sale.getPrice(),
                sale.getTotal()
            });
        }
    }
}
