package inventory;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import persistence.sqlite.SQLiteInventoryRepository;

public class Inventory {

    private static Inventory instance;
    private final SQLiteInventoryRepository repository;
    private final Map<String, InventoryItem> inventoryItems;

    private Inventory() {
        repository = new SQLiteInventoryRepository();
        inventoryItems = new HashMap<>();
        loadFromRepositoryOrInitializeDefaults();
    }

    public static Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }

    private void loadFromRepositoryOrInitializeDefaults() {
        try {
            for (InventoryItem item : repository.findAll()) {
                inventoryItems.put(item.getName(), item);
            }
            if (!inventoryItems.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to load inventory from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        initializeInventory();
        persistAll();
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

    private void persistAll() {
        for (InventoryItem item : inventoryItems.values()) {
            try {
                repository.save(item);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Unable to save inventory seed item '" + item.getName() + "': " + e.getMessage(),
                        "Database", JOptionPane.WARNING_MESSAGE);
            }
        }
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
        try {
            repository.save(item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to save inventory item to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void updateItem(String originalName, InventoryItem item) {
        if (originalName == null || item == null) {
            return;
        }

        inventoryItems.remove(originalName.trim());
        inventoryItems.put(item.getName(), item);
        try {
            repository.delete(originalName.trim());
            repository.save(item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to update inventory item in database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void removeItem(String name) {
        inventoryItems.remove(name);
        try {
            repository.delete(name);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to remove inventory item from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void deductIngredient(String name, double amount) {
        InventoryItem item = inventoryItems.get(name);
        if (item != null) {
            item.deduct(amount);
            try {
                repository.save(item);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Unable to persist inventory deduction: " + e.getMessage(),
                        "Database", JOptionPane.WARNING_MESSAGE);
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