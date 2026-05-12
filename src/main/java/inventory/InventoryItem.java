package inventory;

public class InventoryItem {

    private String name;
    private double quantity;
    private String unit;
    private double alertLevel;

    public InventoryItem(String name, double quantity, String unit, double alertLevel) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertLevel = alertLevel;
    }

    
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getAlertLevel() { return alertLevel; }

    
    public void setName(String name) { this.name = name; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setAlertLevel(double alertLevel) { this.alertLevel = alertLevel; }

    
    public void deduct(double amount) {
        this.quantity -= amount;
        if (this.quantity < 0) this.quantity = 0;
    }

    
    public boolean isLowStock() {
        return this.quantity <= alertLevel;
    }
}
