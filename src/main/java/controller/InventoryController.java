package controller;

import inventory.Inventory;
import inventory.InventoryBatch;
import inventory.InventoryItem;
import inventory.analytics.InventoryPolicyService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import persistence.InventoryRepository;

public class InventoryController {

    private final Inventory inventory;
    private final InventoryRepository repository;
    private final InventoryPolicyService policyService;

    public InventoryController(Inventory inventory, InventoryRepository repository) {
        this.inventory = inventory;
        this.repository = repository;
        this.policyService = new InventoryPolicyService();
    }

    public List<InventoryRowView> buildInventoryRows() {
        List<InventoryRowView> rows = new ArrayList<>();
        Map<String, InventoryItem> items = inventory.getAllItems();
        Map<String, String> abc = policyService.classifyAbc(items);

        for (InventoryItem item : items.values()) {
            String status = item.isLowStock() ? "LOW STOCK" : "OK";
            try {
                List<InventoryBatch> batches = repository.findBatchesForItem(item.getName());
                boolean expired = false;
                boolean expiringSoon = false;
                for (InventoryBatch b : batches) {
                    String exp = b.getExpiryDate();
                    if (exp == null || exp.isBlank()) {
                        continue;
                    }
                    LocalDate d = LocalDate.parse(exp);
                    if (d.isBefore(LocalDate.now())) {
                        expired = true;
                    } else if (!d.isAfter(LocalDate.now().plusDays(7))) {
                        expiringSoon = true;
                    }
                }
                if (expired) {
                    status = appendStatus(status, "EXPIRED");
                } else if (expiringSoon) {
                    status = appendStatus(status, "EXPIRING<7D");
                }
            } catch (Exception ignored) {
                // keep status without FEFO metadata if repository lookup fails
            }

            double eoq = policyService.computeRecommendedEoq(item);
            status = appendStatus(status, "ABC=" + abc.getOrDefault(item.getName(), "C"));
            status = appendStatus(status, "EOQ~" + String.format("%.0f", eoq));

            rows.add(new InventoryRowView(
                    item.getName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getAlertLevel(),
                    status
            ));
        }
        return rows;
    }

    public void addItem(String name, String quantity, String unit, String alertLevel) {
        validateInputs(name, quantity, unit, alertLevel);
        String key = name.trim();
        if (inventory.getItem(key) != null) {
            throw new IllegalArgumentException("Item already exists in inventory");
        }
        double qty = Double.parseDouble(quantity.trim());
        double alert = Double.parseDouble(alertLevel.trim());
        inventory.addItem(new InventoryItem(key, qty, unit.trim(), alert));
    }

    public void updateItem(String originalName, String newName, String quantity, String unit, String alertLevel) {
        validateInputs(newName, quantity, unit, alertLevel);
        InventoryItem existing = inventory.getItem(originalName.trim());
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in inventory");
        }
        existing.setName(newName.trim());
        existing.setQuantity(Double.parseDouble(quantity.trim()));
        existing.setUnit(unit.trim());
        existing.setAlertLevel(Double.parseDouble(alertLevel.trim()));
        inventory.updateItem(originalName.trim(), existing);
    }

    public void removeItem(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name is required");
        }
        inventory.removeItem(name.trim());
    }

    private static void validateInputs(String name, String quantity, String unit, String alertLevel) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit cannot be empty");
        }
        if (quantity == null || quantity.trim().isEmpty() || alertLevel == null || alertLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity and alert level are required");
        }
        double qty = Double.parseDouble(quantity.trim());
        double alert = Double.parseDouble(alertLevel.trim());
        if (qty < 0 || alert < 0) {
            throw new IllegalArgumentException("Quantity and alert level must be non-negative");
        }
    }

    private static String appendStatus(String base, String value) {
        if (base == null || base.isBlank()) {
            return value;
        }
        if ("OK".equals(base)) {
            return value;
        }
        return base + "; " + value;
    }
}
