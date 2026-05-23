package controller;

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

    public InventoryRowView(String name, double quantity, String unit, double alertLevel, String status, String storageLocation, String lastUpdated, String usedIn, String categories) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertLevel = alertLevel;
        this.status = status;
        this.storageLocation = storageLocation;
        this.lastUpdated = lastUpdated;
        this.usedIn = usedIn;
        this.categories = categories;
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
}
