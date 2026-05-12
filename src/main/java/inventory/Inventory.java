package inventory;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

public class Inventory {
    private static Inventory instance;
    private Map<String, InventoryItem> inventoryItems;

    private Inventory() {
        inventoryItems = new HashMap<>();
        initializeInventory();
    }

    public static Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }

    private void initializeInventory() {
        inventoryItems.put("Cup", new InventoryItem("Cup", 21, "pcs", 20));
        inventoryItems.put("Lid", new InventoryItem("Lid", 21, "pcs", 20));
        inventoryItems.put("Straw", new InventoryItem("Straw", 31, "pcs", 30));
        inventoryItems.put("Cup Holder", new InventoryItem("Cup Holder", 11, "pcs", 10));
        inventoryItems.put("Coffee Beans", new InventoryItem("Coffee Beans", 501, "g", 500));
        inventoryItems.put("Milk", new InventoryItem("Milk", 1001, "mL", 1000));
        inventoryItems.put("Caramel Syrup", new InventoryItem("Caramel Syrup", 301, "mL", 300));
        inventoryItems.put("Dark Chocolate Syrup", new InventoryItem("Dark Chocolate Syrup", 301, "mL", 300));
        inventoryItems.put("White Chocolate Syrup", new InventoryItem("White Chocolate Syrup", 301, "mL", 300));
        inventoryItems.put("Condensed Milk", new InventoryItem("Condensed Milk", 301, "mL", 300));
        inventoryItems.put("Strawberry Syrup", new InventoryItem("Strawberry Syrup", 301, "mL", 300));
        inventoryItems.put("Mango Syrup", new InventoryItem("Mango Syrup", 301, "mL", 300));
        inventoryItems.put("Peach Syrup", new InventoryItem("Peach Syrup", 301, "mL", 300));
        inventoryItems.put("Passion Fruit Syrup", new InventoryItem("Passion Fruit Syrup", 301, "mL", 300));
        inventoryItems.put("Matcha Powder", new InventoryItem("Matcha Powder", 101, "g", 100));
        inventoryItems.put("Ube Flavoring", new InventoryItem("Ube Flavoring", 101, "mL", 100));
        inventoryItems.put("Green Tea", new InventoryItem("Green Tea", 501, "mL", 500));
        inventoryItems.put("Peppermint Tea Bag", new InventoryItem("Peppermint Tea Bag", 51, "pcs", 50));
        inventoryItems.put("Chamomile Tea Bag", new InventoryItem("Chamomile Tea Bag", 51, "pcs", 50));
        inventoryItems.put("Earl Grey Tea Bag", new InventoryItem("Earl Grey Tea Bag", 51, "pcs", 50));
        inventoryItems.put("Cinnamon Tea Bag", new InventoryItem("Cinnamon Tea Bag", 51, "pcs", 50));
    }

   
    public void addItem(InventoryItem item) {
        if (item == null || item.getName() == null || item.getName().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cannot add item: Name is empty or invalid.");
            return;
        }
        String name = item.getName().trim();
        if (inventoryItems.containsKey(name)) {
            JOptionPane.showMessageDialog(null, "Item '" + name + "' already exists in inventory.");
            return;
        }
        inventoryItems.put(name, item);
    }

    public void deductIngredient(String name, double amount) {
        InventoryItem item = inventoryItems.get(name);
        if (item != null) {
            item.deduct(amount);
            if (item.isLowStock()) {
                
            }
        }
    }

    public InventoryItem getItem(String name) {
        return inventoryItems.get(name);
    }

    public Map<String, InventoryItem> getAllItems() {
        return inventoryItems;
    }
}