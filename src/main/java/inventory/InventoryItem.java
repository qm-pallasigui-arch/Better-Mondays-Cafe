package inventory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InventoryItem {

    private String name;
    private double quantity;
    private String unit;
    private double alertLevel;
    private String storageLocation;
    private String lastUpdated;

    public InventoryItem(String name, double quantity, String unit, double alertLevel) {
        this(name, quantity, unit, alertLevel, "", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a")));
    }

    public InventoryItem(String name, double quantity, String unit, double alertLevel, String storageLocation, String lastUpdated) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertLevel = alertLevel;
        this.storageLocation = storageLocation;
        this.lastUpdated = lastUpdated;
    }

    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getAlertLevel() { return alertLevel; }
    public String getStorageLocation() { return storageLocation; }
    public String getLastUpdated() { return lastUpdated; }

    public void setName(String name) { this.name = name; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setAlertLevel(double alertLevel) { this.alertLevel = alertLevel; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public void deduct(double amount) {
        this.quantity -= amount;
        if (this.quantity < 0) this.quantity = 0;
    }

    public boolean isLowStock() {
        return this.quantity <= alertLevel && this.quantity > 0;
    }

    public boolean isOutOfStock() {
        return this.quantity <= 0;
    }
}
