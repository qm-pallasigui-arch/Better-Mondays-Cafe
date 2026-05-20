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

        // Espresso & Coffee
        inventoryItems.put("Fine Ground Coffee Beans", new InventoryItem("Fine Ground Coffee Beans", 500, "g", 200));
        inventoryItems.put("Ground Coffee Beans", new InventoryItem("Ground Coffee Beans", 500, "g", 200));
        inventoryItems.put("Heavy Cream", new InventoryItem("Heavy Cream", 500, "mL", 200));
        inventoryItems.put("Sea Salt", new InventoryItem("Sea Salt", 200, "g", 100));
        inventoryItems.put("Vanilla Syrup", new InventoryItem("Vanilla Syrup", 500, "mL", 200));

        // Specialty Drinks
        inventoryItems.put("Cookie Butter Spread", new InventoryItem("Cookie Butter Spread", 500, "g", 200));
        inventoryItems.put("Crushed Biscoff Cookie Crumbs", new InventoryItem("Crushed Biscoff Cookie Crumbs", 500, "g", 200));
        inventoryItems.put("Dark Roast Coffee Grounds", new InventoryItem("Dark Roast Coffee Grounds", 500, "g", 200));
        inventoryItems.put("Evaporated Milk", new InventoryItem("Evaporated Milk", 500, "mL", 200));
        inventoryItems.put("Pumpkin Pie Spice Powder", new InventoryItem("Pumpkin Pie Spice Powder", 200, "g", 100));
        inventoryItems.put("Pumpkin Spice Sauce", new InventoryItem("Pumpkin Spice Sauce", 500, "mL", 200));

        // Tea Latte
        inventoryItems.put("Chai Tea Spice Concentrate", new InventoryItem("Chai Tea Spice Concentrate", 500, "mL", 200));
        inventoryItems.put("Hojicha Powder", new InventoryItem("Hojicha Powder", 200, "g", 100));
        inventoryItems.put("Honey", new InventoryItem("Honey", 500, "mL", 200));
        inventoryItems.put("Simple Syrup", new InventoryItem("Simple Syrup", 500, "mL", 200));

        // Non-Coffee
        inventoryItems.put("Coconut Milk", new InventoryItem("Coconut Milk", 500, "mL", 200));
        inventoryItems.put("Dragon Fruit Puree", new InventoryItem("Dragon Fruit Puree", 500, "mL", 200));
        inventoryItems.put("Mango Puree", new InventoryItem("Mango Puree", 500, "mL", 200));
        inventoryItems.put("Strawberry Puree", new InventoryItem("Strawberry Puree", 500, "mL", 200));

        // Fruit Tea
        inventoryItems.put("Jasmine Green Tea Leaves", new InventoryItem("Jasmine Green Tea Leaves", 500, "g", 200));
        inventoryItems.put("Sugar Syrup", new InventoryItem("Sugar Syrup", 500, "mL", 200));

        // Herbal Tea
        inventoryItems.put("Cinnamon Herbal Blend", new InventoryItem("Cinnamon Herbal Blend", 200, "g", 100));
        inventoryItems.put("Cinnamon Stick", new InventoryItem("Cinnamon Stick", 50, "pcs", 20));
        inventoryItems.put("Dried Chamomile Flowers", new InventoryItem("Dried Chamomile Flowers", 200, "g", 100));
        inventoryItems.put("Dried Peppermint Leaves", new InventoryItem("Dried Peppermint Leaves", 200, "g", 100));
        inventoryItems.put("Earl Grey Tea Leaves", new InventoryItem("Earl Grey Tea Leaves", 200, "g", 100));

        // Sandwiches
        inventoryItems.put("Basil Pesto Sauce", new InventoryItem("Basil Pesto Sauce", 500, "mL", 200));
        inventoryItems.put("Bread Slices", new InventoryItem("Bread Slices", 50, "pcs", 20));
        inventoryItems.put("Butter", new InventoryItem("Butter", 500, "g", 200));
        inventoryItems.put("Cheddar Cheese", new InventoryItem("Cheddar Cheese", 500, "g", 200));
        inventoryItems.put("Cheddar-Mozzarella Blend", new InventoryItem("Cheddar-Mozzarella Blend", 500, "g", 200));
        inventoryItems.put("Mayonnaise", new InventoryItem("Mayonnaise", 500, "g", 200));
        inventoryItems.put("Mozzarella Cheese", new InventoryItem("Mozzarella Cheese", 500, "g", 200));
        inventoryItems.put("Premium Ham Slice", new InventoryItem("Premium Ham Slice", 50, "pcs", 20));

        // Pandesal Pairs
        inventoryItems.put("Pandesal Rolls", new InventoryItem("Pandesal Rolls", 50, "pcs", 20));
        inventoryItems.put("Quick-Melt Cheese", new InventoryItem("Quick-Melt Cheese", 500, "g", 200));
        inventoryItems.put("Sliced Ham", new InventoryItem("Sliced Ham", 50, "pcs", 20));
        inventoryItems.put("Spam Slices", new InventoryItem("Spam Slices", 50, "pcs", 20));

        // Pastries
        inventoryItems.put("Blueberry Compote Topping", new InventoryItem("Blueberry Compote Topping", 500, "g", 200));
        inventoryItems.put("Brown Sugar", new InventoryItem("Brown Sugar", 500, "g", 200));
        inventoryItems.put("White Sugar", new InventoryItem("White Sugar", 500, "g", 200));
        inventoryItems.put("Chocolate Chips", new InventoryItem("Chocolate Chips", 500, "g", 200));
        inventoryItems.put("Chocolate Milk Bath", new InventoryItem("Chocolate Milk Bath", 500, "mL", 200));
        inventoryItems.put("Chopped Spinach", new InventoryItem("Chopped Spinach", 500, "g", 200));
        inventoryItems.put("Cocoa Powder", new InventoryItem("Cocoa Powder", 500, "g", 200));
        inventoryItems.put("Cream Cheese & Heavy Cream Mix", new InventoryItem("Cream Cheese & Heavy Cream Mix", 500, "g", 200));
        inventoryItems.put("Egg", new InventoryItem("Egg", 50, "pcs", 20));
        inventoryItems.put("Flour", new InventoryItem("Flour", 500, "g", 200));
        inventoryItems.put("Graham Cracker Crust Base", new InventoryItem("Graham Cracker Crust Base", 500, "g", 200));
        inventoryItems.put("Ladyfinger Biscuits", new InventoryItem("Ladyfinger Biscuits", 50, "pcs", 20));
        inventoryItems.put("Mashed Banana", new InventoryItem("Mashed Banana", 500, "g", 200));
        inventoryItems.put("Mascarpone & Heavy Cream Mixture", new InventoryItem("Mascarpone & Heavy Cream Mixture", 500, "g", 200));
        inventoryItems.put("Matcha Tea Bath", new InventoryItem("Matcha Tea Bath", 500, "mL", 200));
        inventoryItems.put("Parmesan & Garlic Seasoning", new InventoryItem("Parmesan & Garlic Seasoning", 200, "g", 100));
        inventoryItems.put("Powdered Sugar Coating", new InventoryItem("Powdered Sugar Coating", 500, "g", 200));
        inventoryItems.put("Puff Pastry Dough Sheet", new InventoryItem("Puff Pastry Dough Sheet", 50, "pcs", 20));
        inventoryItems.put("Sugar", new InventoryItem("Sugar", 500, "g", 200));
        inventoryItems.put("Vegetable Oil", new InventoryItem("Vegetable Oil", 500, "mL", 200));
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
            try {
                // Prefer FEFO deduction when batches exist; falls back internally to aggregate-only flow.
                repository.deductFEFO(name, amount);
                repository.findByName(name).ifPresent(updated -> {
                    item.setQuantity(updated.getQuantity());
                    item.setUnit(updated.getUnit());
                    item.setAlertLevel(updated.getAlertLevel());
                });
            } catch (Exception e) {
                item.deduct(amount);
                try {
                    repository.save(item);
                } catch (Exception ignored) {
                }
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