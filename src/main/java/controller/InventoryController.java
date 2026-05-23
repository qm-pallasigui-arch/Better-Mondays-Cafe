package controller;

import inventory.Inventory;
import inventory.InventoryBatch;
import inventory.InventoryItem;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import persistence.InventoryRepository;
import pos.Menu;
import pos.MenuItem;

public class InventoryController {

    private final Inventory inventory;
    private final InventoryRepository repository;

    public InventoryController(Inventory inventory, InventoryRepository repository) {
        this.inventory = inventory;
        this.repository = repository;
    }

    public List<InventoryRowView> buildInventoryRows() {
        List<InventoryRowView> rows = new ArrayList<>();
        Map<String, InventoryItem> items = inventory.getAllItems();
        Map<String, Set<String>> ingredientUsage = buildIngredientUsageMap();
        Map<String, Set<String>> ingredientCategories = buildIngredientCategoryMap();
        Map<String, Set<String>> foodCategories = buildFoodCategoryMap();

        for (Map.Entry<String, Set<String>> entry : foodCategories.entrySet()) {
            ingredientCategories.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>()).addAll(entry.getValue());
        }

        for (InventoryItem item : items.values()) {
            String status;
            if (item.isOutOfStock()) {
                status = "Out of Stock";
            } else if (item.isLowStock()) {
                status = "Low Stock";
            } else {
                status = "Good";
            }

            try {
                List<InventoryBatch> batches = repository.findBatchesForItem(item.getName());
                boolean anyExpired = false;
                for (InventoryBatch b : batches) {
                    String exp = b.getExpiryDate();
                    if (exp == null || exp.isBlank()) continue;
                    LocalDate d = LocalDate.parse(exp);
                    if (d.isBefore(LocalDate.now())) {
                        anyExpired = true;
                        break;
                    }
                }
                if (anyExpired) {
                    status = "Expired";
                }
            } catch (Exception ignored) {}

            rows.add(new InventoryRowView(
                    item.getName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getAlertLevel(),
                    status,
                    item.getStorageLocation(),
                    item.getLastUpdated(),
                    formatUsageSummary(ingredientUsage.get(item.getName())),
                    formatCategorySummary(ingredientCategories.get(item.getName()))
            ));
        }
        return rows;
    }

    private Map<String, Set<String>> buildIngredientUsageMap() {
        Map<String, Set<String>> usage = new LinkedHashMap<>();
        try {
            for (MenuItem menuItem : Menu.getInstance().getAllItems().values()) {
                for (String ingredient : menuItem.getIngredients().keySet()) {
                    usage.computeIfAbsent(ingredient, key -> new LinkedHashSet<>()).add(menuItem.getName());
                }
            }
        } catch (Exception ignored) {
        }
        return usage;
    }

    private String formatUsageSummary(Set<String> usedIn) {
        if (usedIn == null || usedIn.isEmpty()) {
            return "—";
        }
        List<String> values = new ArrayList<>(usedIn);
        if (values.size() <= 2) {
            return String.join(", ", values);
        }
        return values.get(0) + ", " + values.get(1) + " +" + (values.size() - 2) + " more";
    }

    private Map<String, Set<String>> buildIngredientCategoryMap() {
        Map<String, Set<String>> categories = new LinkedHashMap<>();
        try {
            for (MenuItem menuItem : Menu.getInstance().getAllItems().values()) {
                String category = menuItem.getCategory();
                for (String ingredient : menuItem.getIngredients().keySet()) {
                    categories.computeIfAbsent(ingredient, key -> new LinkedHashSet<>()).add(category);
                }
            }
        } catch (Exception ignored) {
        }
        return categories;
    }

    private Map<String, Set<String>> buildFoodCategoryMap() {
        Map<String, Set<String>> categories = new LinkedHashMap<>();

        Set<String> foodIngredients = Set.of(
                "Basil Pesto Sauce",
                "Bread Slices",
                "Butter",
                "Cheddar Cheese",
                "Cheddar-Mozzarella Blend",
                "Mayonnaise",
                "Mozzarella Cheese",
                "Premium Ham Slice",
                "Pandesal Rolls",
                "Quick-Melt Cheese",
                "Sliced Ham",
                "Spam Slices",
                "Blueberry Compote Topping",
                "Brown Sugar",
                "White Sugar",
                "Chocolate Chips",
                "Chocolate Milk Bath",
                "Chopped Spinach",
                "Cocoa Powder",
                "Cream Cheese & Heavy Cream Mix",
                "Egg",
                "Flour",
                "Graham Cracker Crust Base",
                "Ladyfinger Biscuits",
                "Mashed Banana",
                "Mascarpone & Heavy Cream Mixture",
                "Matcha Tea Bath",
                "Parmesan & Garlic Seasoning",
                "Powdered Sugar Coating",
                "Puff Pastry Dough Sheet",
                "Sugar",
                "Vegetable Oil"
        );

        for (String ingredient : foodIngredients) {
            categories.computeIfAbsent(ingredient, key -> new LinkedHashSet<>()).add("Food");
        }

        return categories;
    }

    private String formatCategorySummary(Set<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "—";
        }
        List<String> values = new ArrayList<>(categories);
        if (values.size() <= 2) {
            return String.join(", ", values);
        }
        return values.get(0) + ", " + values.get(1) + " +" + (values.size() - 2) + " more";
    }

    public void addItem(String name, String quantity, String unit, String alertLevel) {
        validateInputs(name, quantity, unit, alertLevel);
        String key = name.trim();
        if (inventory.getItem(key) != null) {
            throw new IllegalArgumentException("Item already exists in inventory");
        }
        double qty = Double.parseDouble(quantity.trim());
        double alert = Double.parseDouble(alertLevel.trim());
        inventory.addItem(new InventoryItem(key, qty, unit.trim(), alert));
    }

    public void updateItem(String originalName, String newName, String quantity, String unit, String alertLevel) {
        validateInputs(newName, quantity, unit, alertLevel);
        InventoryItem existing = inventory.getItem(originalName.trim());
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in inventory");
        }
        existing.setName(newName.trim());
        existing.setQuantity(Double.parseDouble(quantity.trim()));
        existing.setUnit(unit.trim());
        existing.setAlertLevel(Double.parseDouble(alertLevel.trim()));
        inventory.updateItem(originalName.trim(), existing);
    }

    public void removeItem(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name is required");
        }
        inventory.removeItem(name.trim());
    }

    private static void validateInputs(String name, String quantity, String unit, String alertLevel) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit cannot be empty");
        }
        if (quantity == null || quantity.trim().isEmpty() || alertLevel == null || alertLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity and alert level are required");
        }
        double qty = Double.parseDouble(quantity.trim());
        double alert = Double.parseDouble(alertLevel.trim());
        if (qty < 0 || alert < 0) {
            throw new IllegalArgumentException("Quantity and alert level must be non-negative");
        }
    }

}
