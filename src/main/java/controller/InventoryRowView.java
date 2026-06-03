package controller;

import inventory.InventoryBatch;
import java.util.Collections;
import java.util.List;

public class InventoryRowView {

    private final String name;
    private final double quantity;
    private final String unit;
    private final double alertLevel;
    private final String status;
    private final String storageLocation;
    private final String lastUpdated;
    private final String usedIn;
    private final String categories;
    private final List<InventoryBatch> batches;

    public InventoryRowView(String name, double quantity, String unit, double alertLevel, String status,
            String storageLocation, String lastUpdated, String usedIn, String categories,
            List<InventoryBatch> batches) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertLevel = alertLevel;
        this.status = status;
        this.storageLocation = storageLocation;
        this.lastUpdated = lastUpdated;
        this.usedIn = usedIn;
        this.categories = categories;
        this.batches = batches != null ? batches : Collections.emptyList();
    }

    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getAlertLevel() { return alertLevel; }
    public String getStatus() { return status; }
    public String getStorageLocation() { return storageLocation; }
    public String getLastUpdated() { return lastUpdated; }
    public String getUsedIn() { return usedIn; }
    public String getCategories() { return categories; }
    public List<InventoryBatch> getBatches() { return batches; }
}
