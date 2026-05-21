package pos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import persistence.sqlite.SQLiteMenuRepository;

public class Menu {

    private static Menu instance;
    private final SQLiteMenuRepository repository;
    private final Map<String, MenuItem> menuItems;

    private Menu() {
        repository = new SQLiteMenuRepository();
        menuItems = new HashMap<>();
        loadFromRepositoryOrInitializeDefaults();
        ensureExtraItems();
    }

    public static Menu getInstance() {
        if (instance == null) {
            instance = new Menu();
        }
        return instance;
    }

    private void loadFromRepositoryOrInitializeDefaults() {
        try {
            for (MenuItem item : repository.findAll()) {
                menuItems.put(item.getName(), item);
            }
            if (!menuItems.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to load menu from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        initializeMenu();
        persistAll();
    }

    private void initializeMenu() {
        // Espresso & Coffee
        menuItems.put("Americano", new CoffeeItem("Americano", 125, 140, 155));
        menuItems.put("Latte", new CoffeeItem("Latte", 145, 160, 175));
        menuItems.put("Cappuccino", new CoffeeItem("Cappuccino", 145, 160, 175));
        menuItems.put("Salted Cream Latte", new CoffeeItem("Salted Cream Latte", 160, 175, 190));
        menuItems.put("Spanish Latte", new CoffeeItem("Spanish Latte", 175, 190, 205));
        menuItems.put("Dark Mocha", new CoffeeItem("Dark Mocha", 175, 190, 205));
        menuItems.put("White Mocha", new CoffeeItem("White Mocha", 175, 190, 205));
        menuItems.put("Caramel Macchiato", new CoffeeItem("Caramel Macchiato", 175, 190, 205));
        menuItems.put("Brewed Coffee", new CoffeeItem("Brewed Coffee", 115, 0, 0));

        // Specialty Drinks (Iced Regular only)
        menuItems.put("Vietnamese Coffee", new CoffeeItem("Vietnamese Coffee", 0, 190, 0));
        menuItems.put("Ube Espresso", new CoffeeItem("Ube Espresso", 0, 210, 0));
        menuItems.put("Pumpkin Spice Latte", new CoffeeItem("Pumpkin Spice Latte", 0, 220, 0));
        menuItems.put("Spiced Cookie Latte", new CoffeeItem("Spiced Cookie Latte", 0, 225, 0));

        // Tea Latte (Hot + Iced Regular only)
        menuItems.put("Matcha Latte", new NonCoffeeItem("Matcha Latte", 205, 220, 0));
        menuItems.put("Chocolate Matcha", new NonCoffeeItem("Chocolate Matcha", 215, 230, 0));
        menuItems.put("Matcha Espresso", new CoffeeItem("Matcha Espresso", 230, 245, 0));
        menuItems.put("Hojicha Latte", new NonCoffeeItem("Hojicha Latte", 200, 215, 0));
        menuItems.put("Chai Latte", new NonCoffeeItem("Chai Latte", 190, 205, 0));

        // Non-Coffee
        menuItems.put("Chocolate Latte", new NonCoffeeItem("Chocolate Latte", 165, 180, 195));
        menuItems.put("Strawberry Latte", new NonCoffeeItem("Strawberry Latte", 0, 190, 205));
        menuItems.put("Mango Latte", new NonCoffeeItem("Mango Latte", 0, 190, 205));
        menuItems.put("Ube Latte", new NonCoffeeItem("Ube Latte", 0, 200, 215));

        // Fruit Tea (Iced Regular + Iced Large)
        menuItems.put("Strawberry Green Tea", new FruitTeaItem("Strawberry Green Tea", 0, 180, 195));
        menuItems.put("Mango Green Tea", new FruitTeaItem("Mango Green Tea", 0, 180, 195));
        menuItems.put("Peach Green Tea", new FruitTeaItem("Peach Green Tea", 0, 180, 195));
        menuItems.put("Passion Fruit Green Tea", new FruitTeaItem("Passion Fruit Green Tea", 0, 180, 195));

        // Herbal Tea (Hot only)
        menuItems.put("Peppermint", new HerbalTeaItem("Peppermint", 120, 0, 0));
        menuItems.put("Chamomile", new HerbalTeaItem("Chamomile", 120, 0, 0));
        menuItems.put("Earl Grey", new HerbalTeaItem("Earl Grey", 135, 0, 0));
        menuItems.put("Cinnamon", new HerbalTeaItem("Cinnamon", 135, 0, 0));
    }

    private void ensureExtraItems() {
        Map<String, MenuItem> extra = new HashMap<>();
        boolean changed = false;

        if (!menuItems.containsKey("Manila Latte")) {
            extra.put("Manila Latte", new CoffeeItem("Manila Latte", 0, 215, 0));
        }
        if (!menuItems.containsKey("Dragon Fruit Coconut Latte")) {
            extra.put("Dragon Fruit Coconut Latte", new NonCoffeeItem("Dragon Fruit Coconut Latte", 0, 190, 210));
        }
        if (!menuItems.containsKey("Earl Grey") && menuItems.containsKey("Early Grey")) {
            MenuItem old = menuItems.remove("Early Grey");
            if (old != null) {
                menuItems.put("Earl Grey", new HerbalTeaItem("Earl Grey", old.getHotPrice(), 0, 0));
                changed = true;
            }
        }

        // Sandwiches
        if (!menuItems.containsKey("Signature Ham & Cheese")) {
            extra.put("Signature Ham & Cheese", new FoodItem("Signature Ham & Cheese", "Food", 290));
        }
        if (!menuItems.containsKey("Classic Grilled Cheese")) {
            extra.put("Classic Grilled Cheese", new FoodItem("Classic Grilled Cheese", "Food", 280));
        }
        if (!menuItems.containsKey("Homestyle Pesto & Cheese")) {
            extra.put("Homestyle Pesto & Cheese", new FoodItem("Homestyle Pesto & Cheese", "Food", 270));
        }

        // Pandesal Pairs
        if (!menuItems.containsKey("Ham & Cheese")) {
            extra.put("Ham & Cheese", new FoodItem("Ham & Cheese", "Food", 255));
        }
        if (!menuItems.containsKey("Cheesy Pesto")) {
            extra.put("Cheesy Pesto", new FoodItem("Cheesy Pesto", "Food", 225));
        }
        if (!menuItems.containsKey("Spam & Cheese")) {
            extra.put("Spam & Cheese", new FoodItem("Spam & Cheese", "Food", 240));
        }

        // Pastries
        if (!menuItems.containsKey("Chocolate Crinkles")) {
            extra.put("Chocolate Crinkles", new FoodItem("Chocolate Crinkles", "Food", 60));
        }
        if (!menuItems.containsKey("Chocolate Cookies")) {
            extra.put("Chocolate Cookies", new FoodItem("Chocolate Cookies", "Food", 65));
        }
        if (!menuItems.containsKey("Brownies")) {
            extra.put("Brownies", new FoodItem("Brownies", "Food", 80));
        }
        if (!menuItems.containsKey("Banana Bread")) {
            extra.put("Banana Bread", new FoodItem("Banana Bread", "Food", 75));
        }
        if (!menuItems.containsKey("Chocolate Tiramisu")) {
            extra.put("Chocolate Tiramisu", new FoodItem("Chocolate Tiramisu", "Food", 130));
        }
        if (!menuItems.containsKey("Matcha Tiramisu")) {
            extra.put("Matcha Tiramisu", new FoodItem("Matcha Tiramisu", "Food", 140));
        }
        if (!menuItems.containsKey("Creamy Spinach")) {
            extra.put("Creamy Spinach", new FoodItem("Creamy Spinach", "Food", 150));
        }
        if (!menuItems.containsKey("Blueberry Cheesecake")) {
            extra.put("Blueberry Cheesecake", new FoodItem("Blueberry Cheesecake", "Food", 145));
        }

        if (!extra.isEmpty()) {
            for (MenuItem item : extra.values()) {
                menuItems.put(item.getName(), item);
                try {
                    repository.save(item);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Unable to save extra item '" + item.getName() + "': " + e.getMessage(),
                            "Database", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

        if (changed) {
            try {
                MenuItem earlGrey = menuItems.get("Earl Grey");
                if (earlGrey != null) {
                    repository.save(earlGrey);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Unable to update Earl Grey in database: " + e.getMessage(),
                        "Database", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void persistAll() {
        for (MenuItem item : menuItems.values()) {
            try {
                repository.save(item);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Unable to save menu seed item '" + item.getName() + "': " + e.getMessage(),
                        "Database", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public MenuItem getMenuItem(String name) {
        return menuItems.get(name);
    }

    public Map<String, MenuItem> getAllItems() {
        return new LinkedHashMap<>(menuItems);
    }

    public void saveItem(MenuItem item) {
        menuItems.put(item.getName(), item);
        try {
            repository.save(item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to save menu item to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void removeItem(String name) {
        menuItems.remove(name);
        try {
            repository.delete(name);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to remove menu item from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public double getPrice(String name, String variant) {
        MenuItem item = menuItems.get(name);
        if (item == null) {
            return 0;
        }

        double price = 0;

        switch (variant) {
            case "Hot":
                price = item.getHotPrice();
                break;
            case "Regular Iced":
                price = item.getIcedRegularPrice();
                break;
            case "Large Iced":
                price = item.getIcedLargePrice();
                break;
        }

        if (price <= 0) {
            JOptionPane.showMessageDialog(null, "This option is not available for " + name, "Option Unavailable", JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        return price;
    }
}
