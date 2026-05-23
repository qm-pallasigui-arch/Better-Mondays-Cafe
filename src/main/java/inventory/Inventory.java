package inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import persistence.sqlite.SQLiteInventoryRepository;
import util.StringUtil;

public class Inventory {

    private static Inventory instance;
    private final SQLiteInventoryRepository repository;
    private final Map<String, InventoryItem> inventoryItems;
    private static final Logger LOGGER = Logger.getLogger(Inventory.class.getName());

    private Inventory() {
        repository = new SQLiteInventoryRepository();
        inventoryItems = new LinkedHashMap<>();
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
                inventoryItems.put(StringUtil.normalizeName(item.getName()), item);
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
        addDefaultItem("Cup", new InventoryItem("Cup", 21, "pcs", 20));
        addDefaultItem("Lid", new InventoryItem("Lid", 21, "pcs", 20));
        addDefaultItem("Straw", new InventoryItem("Straw", 31, "pcs", 30));
        addDefaultItem("Cup Holder", new InventoryItem("Cup Holder", 11, "pcs", 10));
        addDefaultItem("Coffee Beans", new InventoryItem("Coffee Beans", 501, "g", 500));
        addDefaultItem("Milk", new InventoryItem("Milk", 1001, "mL", 1000));
        addDefaultItem("Caramel Syrup", new InventoryItem("Caramel Syrup", 301, "mL", 300));
        addDefaultItem("Dark Chocolate Syrup", new InventoryItem("Dark Chocolate Syrup", 301, "mL", 300));
        addDefaultItem("White Chocolate Syrup", new InventoryItem("White Chocolate Syrup", 301, "mL", 300));
        addDefaultItem("Condensed Milk", new InventoryItem("Condensed Milk", 301, "mL", 300));
        addDefaultItem("Strawberry Syrup", new InventoryItem("Strawberry Syrup", 301, "mL", 300));
        addDefaultItem("Mango Syrup", new InventoryItem("Mango Syrup", 301, "mL", 300));
        addDefaultItem("Peach Syrup", new InventoryItem("Peach Syrup", 301, "mL", 300));
        addDefaultItem("Passion Fruit Syrup", new InventoryItem("Passion Fruit Syrup", 301, "mL", 300));
        addDefaultItem("Matcha Powder", new InventoryItem("Matcha Powder", 101, "g", 100));
        addDefaultItem("Ube Flavoring", new InventoryItem("Ube Flavoring", 101, "mL", 100));
        addDefaultItem("Green Tea", new InventoryItem("Green Tea", 501, "mL", 500));
        addDefaultItem("Peppermint Tea Bag", new InventoryItem("Peppermint Tea Bag", 51, "pcs", 50));
        addDefaultItem("Chamomile Tea Bag", new InventoryItem("Chamomile Tea Bag", 51, "pcs", 50));
        addDefaultItem("Earl Grey Tea Bag", new InventoryItem("Earl Grey Tea Bag", 51, "pcs", 50));
        addDefaultItem("Cinnamon Tea Bag", new InventoryItem("Cinnamon Tea Bag", 51, "pcs", 50));

        // Espresso & Coffee
        addDefaultItem("Fine Ground Coffee Beans", new InventoryItem("Fine Ground Coffee Beans", 500, "g", 200));
        addDefaultItem("Ground Coffee Beans", new InventoryItem("Ground Coffee Beans", 500, "g", 200));
        addDefaultItem("Heavy Cream", new InventoryItem("Heavy Cream", 500, "mL", 200));
        addDefaultItem("Sea Salt", new InventoryItem("Sea Salt", 200, "g", 100));
        addDefaultItem("Vanilla Syrup", new InventoryItem("Vanilla Syrup", 500, "mL", 200));

        // Specialty Drinks
        addDefaultItem("Cookie Butter Spread", new InventoryItem("Cookie Butter Spread", 500, "g", 200));
        addDefaultItem("Crushed Biscoff Cookie Crumbs", new InventoryItem("Crushed Biscoff Cookie Crumbs", 500, "g", 200));
        addDefaultItem("Dark Roast Coffee Grounds", new InventoryItem("Dark Roast Coffee Grounds", 500, "g", 200));
        addDefaultItem("Evaporated Milk", new InventoryItem("Evaporated Milk", 500, "mL", 200));
        addDefaultItem("Pumpkin Pie Spice Powder", new InventoryItem("Pumpkin Pie Spice Powder", 200, "g", 100));
        addDefaultItem("Pumpkin Spice Sauce", new InventoryItem("Pumpkin Spice Sauce", 500, "mL", 200));

        // Tea Latte
        addDefaultItem("Chai Tea Spice Concentrate", new InventoryItem("Chai Tea Spice Concentrate", 500, "mL", 200));
        addDefaultItem("Hojicha Powder", new InventoryItem("Hojicha Powder", 200, "g", 100));
        addDefaultItem("Honey", new InventoryItem("Honey", 500, "mL", 200));
        addDefaultItem("Simple Syrup", new InventoryItem("Simple Syrup", 500, "mL", 200));

        // Non-Coffee
        addDefaultItem("Coconut Milk", new InventoryItem("Coconut Milk", 500, "mL", 200));
        addDefaultItem("Dragon Fruit Puree", new InventoryItem("Dragon Fruit Puree", 500, "mL", 200));
        addDefaultItem("Mango Puree", new InventoryItem("Mango Puree", 500, "mL", 200));
        addDefaultItem("Strawberry Puree", new InventoryItem("Strawberry Puree", 500, "mL", 200));

        // Fruit Tea
        addDefaultItem("Jasmine Green Tea Leaves", new InventoryItem("Jasmine Green Tea Leaves", 500, "g", 200));
        addDefaultItem("Sugar Syrup", new InventoryItem("Sugar Syrup", 500, "mL", 200));

        // Herbal Tea
        addDefaultItem("Cinnamon Herbal Blend", new InventoryItem("Cinnamon Herbal Blend", 200, "g", 100));
        addDefaultItem("Cinnamon Stick", new InventoryItem("Cinnamon Stick", 50, "pcs", 20));
        addDefaultItem("Dried Chamomile Flowers", new InventoryItem("Dried Chamomile Flowers", 200, "g", 100));
        addDefaultItem("Dried Peppermint Leaves", new InventoryItem("Dried Peppermint Leaves", 200, "g", 100));
        addDefaultItem("Earl Grey Tea Leaves", new InventoryItem("Earl Grey Tea Leaves", 200, "g", 100));

        // Sandwiches
        addDefaultItem("Basil Pesto Sauce", new InventoryItem("Basil Pesto Sauce", 500, "mL", 200));
        addDefaultItem("Bread Slices", new InventoryItem("Bread Slices", 50, "pcs", 20));
        addDefaultItem("Butter", new InventoryItem("Butter", 500, "g", 200));
        addDefaultItem("Cheddar Cheese", new InventoryItem("Cheddar Cheese", 500, "g", 200));
        addDefaultItem("Cheddar-Mozzarella Blend", new InventoryItem("Cheddar-Mozzarella Blend", 500, "g", 200));
        addDefaultItem("Mayonnaise", new InventoryItem("Mayonnaise", 500, "g", 200));
        addDefaultItem("Mozzarella Cheese", new InventoryItem("Mozzarella Cheese", 500, "g", 200));
        addDefaultItem("Premium Ham Slice", new InventoryItem("Premium Ham Slice", 50, "pcs", 20));

        // Pandesal Pairs
        addDefaultItem("Pandesal Rolls", new InventoryItem("Pandesal Rolls", 50, "pcs", 20));
        addDefaultItem("Quick-Melt Cheese", new InventoryItem("Quick-Melt Cheese", 500, "g", 200));
        addDefaultItem("Sliced Ham", new InventoryItem("Sliced Ham", 50, "pcs", 20));
        addDefaultItem("Spam Slices", new InventoryItem("Spam Slices", 50, "pcs", 20));

        // Pastries
        addDefaultItem("Blueberry Compote Topping", new InventoryItem("Blueberry Compote Topping", 500, "g", 200));
        addDefaultItem("Brown Sugar", new InventoryItem("Brown Sugar", 500, "g", 200));
        addDefaultItem("White Sugar", new InventoryItem("White Sugar", 500, "g", 200));
        addDefaultItem("Chocolate Chips", new InventoryItem("Chocolate Chips", 500, "g", 200));
        addDefaultItem("Chocolate Milk Bath", new InventoryItem("Chocolate Milk Bath", 500, "mL", 200));
        addDefaultItem("Chopped Spinach", new InventoryItem("Chopped Spinach", 500, "g", 200));
        addDefaultItem("Cocoa Powder", new InventoryItem("Cocoa Powder", 500, "g", 200));
        addDefaultItem("Cream Cheese & Heavy Cream Mix", new InventoryItem("Cream Cheese & Heavy Cream Mix", 500, "g", 200));
        addDefaultItem("Egg", new InventoryItem("Egg", 50, "pcs", 20));
        addDefaultItem("Flour", new InventoryItem("Flour", 500, "g", 200));
        addDefaultItem("Graham Cracker Crust Base", new InventoryItem("Graham Cracker Crust Base", 500, "g", 200));
        addDefaultItem("Ladyfinger Biscuits", new InventoryItem("Ladyfinger Biscuits", 50, "pcs", 20));
        addDefaultItem("Mashed Banana", new InventoryItem("Mashed Banana", 500, "g", 200));
        addDefaultItem("Mascarpone & Heavy Cream Mixture", new InventoryItem("Mascarpone & Heavy Cream Mixture", 500, "g", 200));
        addDefaultItem("Matcha Tea Bath", new InventoryItem("Matcha Tea Bath", 500, "mL", 200));
        addDefaultItem("Parmesan & Garlic Seasoning", new InventoryItem("Parmesan & Garlic Seasoning", 200, "g", 100));
        addDefaultItem("Powdered Sugar Coating", new InventoryItem("Powdered Sugar Coating", 500, "g", 200));
        addDefaultItem("Puff Pastry Dough Sheet", new InventoryItem("Puff Pastry Dough Sheet", 50, "pcs", 20));
        addDefaultItem("Sugar", new InventoryItem("Sugar", 500, "g", 200));
        addDefaultItem("Vegetable Oil", new InventoryItem("Vegetable Oil", 500, "mL", 200));
    }

    private void addDefaultItem(String name, InventoryItem item) {
        inventoryItems.putIfAbsent(StringUtil.normalizeName(name), item);
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
        String name = StringUtil.normalizeName(item.getName());
        if (inventoryItems.containsKey(name)) {
            JOptionPane.showMessageDialog(null, "Item '" + name + "' already exists in inventory.");
            return;
        }
        item.setName(name);
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

        String on = StringUtil.normalizeName(originalName);
        String nn = StringUtil.normalizeName(item.getName());
        inventoryItems.remove(on);
        item.setName(nn);
        inventoryItems.put(nn, item);
        try {
            repository.delete(on);
            repository.save(item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to update inventory item in database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void removeItem(String name) {
        String n = StringUtil.normalizeName(name);
        inventoryItems.remove(n);
        try {
            repository.delete(n);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to remove inventory item from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void deductIngredient(String name, double amount) {
        String nn = StringUtil.normalizeName(name);
        InventoryItem item = inventoryItems.get(nn);
        if (item != null) {
            LOGGER.log(Level.INFO, "Deducting {0} from {1}", new Object[]{amount, nn});
            try {
                // Prefer FEFO deduction when batches exist; falls back internally to aggregate-only flow.
                repository.deductFEFO(nn, amount);
                repository.findByName(nn).ifPresent(updated -> {
                    item.setQuantity(updated.getQuantity());
                    item.setUnit(updated.getUnit());
                    item.setAlertLevel(updated.getAlertLevel());
                });
                LOGGER.log(Level.INFO, "After deduction {0} has quantity {1}", new Object[]{nn, item.getQuantity()});
            } catch (Exception e) {
                item.deduct(amount);
                try {
                    repository.save(item);
                } catch (Exception ignored) {
                }
                JOptionPane.showMessageDialog(null,
                        "Unable to persist inventory deduction: " + e.getMessage(),
                        "Database", JOptionPane.WARNING_MESSAGE);
                LOGGER.log(Level.WARNING, "Deduction fallback for {0}: {1}", new Object[]{nn, e.getMessage()});
            }
        } else {
            LOGGER.log(Level.WARNING, "Attempted to deduct from unknown inventory item: {0}", name);
        }
    }

    /** Refreshes a single inventory item from the repository (if present). */
    public void refreshItem(String name) {
        if (name == null || name.isBlank()) return;
        String nn = StringUtil.normalizeName(name);
        try {
            repository.findByName(nn).ifPresent(found -> {
                inventoryItems.put(nn, found);
            });
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "refreshItem failed for {0}: {1}", new Object[]{nn, e.getMessage()});
        }
    }

    public InventoryItem getItem(String name) {
        return inventoryItems.get(StringUtil.normalizeName(name));
    }

    public Map<String, InventoryItem> getAllItems() {
        return inventoryItems;
    }
}