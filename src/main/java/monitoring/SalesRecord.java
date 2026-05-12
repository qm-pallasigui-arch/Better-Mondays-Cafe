package monitoring;

public class SalesRecord {

    private String productName;
    private int quantity;
    private double price;
    private double total;

    public SalesRecord(String productName, int quantity, double price, double total) {
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.total = total;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getTotal() {
        return total;
    }
}
