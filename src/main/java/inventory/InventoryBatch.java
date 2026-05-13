package inventory;

/**
 * Represents a batch/lot of inventory for FEFO/expiration handling.
 */
public class InventoryBatch {

    private long id;
    private String sku;
    private double quantity;
    private String expiryDate; // ISO string, nullable

    public InventoryBatch(long id, String sku, double quantity, String expiryDate) {
        this.id = id;
        this.sku = sku;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public InventoryBatch(String sku, double quantity, String expiryDate) {
        this(-1, sku, quantity, expiryDate);
    }

    public long getId() { return id; }
    public String getSku() { return sku; }
    public double getQuantity() { return quantity; }
    public String getExpiryDate() { return expiryDate; }

    public void setId(long id) { this.id = id; }
    public void setSku(String sku) { this.sku = sku; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}
