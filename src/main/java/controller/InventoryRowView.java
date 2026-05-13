package controller;

public class InventoryRowView {

    private final String name;
    private final double quantity;
    private final String unit;
    private final double alertLevel;
    private final String status;

    public InventoryRowView(String name, double quantity, String unit, double alertLevel, String status) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertLevel = alertLevel;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public double getAlertLevel() {
        return alertLevel;
    }

    public String getStatus() {
        return status;
    }
}
