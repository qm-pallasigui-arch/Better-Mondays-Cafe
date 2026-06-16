package pos;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import persistence.sqlite.SQLiteMenuRepository;
import util.StringUtil;
import java.util.ArrayList;
import java.util.List;

public class Menu {

    private static Menu instance;
    private final SQLiteMenuRepository repository;
    private final Map<String, MenuItem> menuItems;

    private Menu() {
        repository = new SQLiteMenuRepository();
        menuItems = new LinkedHashMap<>();
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
                menuItems.put(StringUtil.normalizeName(item.getName()), item);
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
        menuItems.put(StringUtil.normalizeName("Americano"), new CoffeeItem("Americano", 125, 140, 155));
        menuItems.put(StringUtil.normalizeName("Latte"), new CoffeeItem("Latte", 145, 160, 175));
        menuItems.put(StringUtil.normalizeName("Cappuccino"), new CoffeeItem("Cappuccino", 145, 160, 175));
        menuItems.put(StringUtil.normalizeName("Salted Cream Latte"),
                new CoffeeItem("Salted Cream Latte", 160, 175, 190));
        menuItems.put(StringUtil.normalizeName("Spanish Latte"), new CoffeeItem("Spanish Latte", 175, 190, 205));
        menuItems.put(StringUtil.normalizeName("Dark Mocha"), new CoffeeItem("Dark Mocha", 175, 190, 205));
        menuItems.put(StringUtil.normalizeName("White Mocha"), new CoffeeItem("White Mocha", 175, 190, 205));
        menuItems.put(StringUtil.normalizeName("Caramel Macchiato"),
                new CoffeeItem("Caramel Macchiato", 175, 190, 205));
        menuItems.put(StringUtil.normalizeName("Brewed Coffee"), new CoffeeItem("Brewed Coffee", 115, 0, 0));

        // Specialty Drinks (Iced Regular only)
        menuItems.put(StringUtil.normalizeName("Vietnamese Coffee"), new CoffeeItem("Vietnamese Coffee", 0, 190, 0));
        menuItems.put(StringUtil.normalizeName("Ube Espresso"), new CoffeeItem("Ube Espresso", 0, 210, 0));
        menuItems.put(StringUtil.normalizeName("Pumpkin Spice Latte"),
                new CoffeeItem("Pumpkin Spice Latte", 0, 220, 0));
        menuItems.put(StringUtil.normalizeName("Spiced Cookie Latte"),
                new CoffeeItem("Spiced Cookie Latte", 0, 225, 0));

        // Tea Latte (Hot + Iced Regular only)
        menuItems.put(StringUtil.normalizeName("Matcha Latte"), new NonCoffeeItem("Matcha Latte", 205, 220, 0));
        menuItems.put(StringUtil.normalizeName("Chocolate Matcha"), new NonCoffeeItem("Chocolate Matcha", 215, 230, 0));
        menuItems.put(StringUtil.normalizeName("Matcha Espresso"), new CoffeeItem("Matcha Espresso", 230, 245, 0));
        menuItems.put(StringUtil.normalizeName("Hojicha Latte"), new NonCoffeeItem("Hojicha Latte", 200, 215, 0));
        menuItems.put(StringUtil.normalizeName("Chai Latte"), new NonCoffeeItem("Chai Latte", 190, 205, 0));

        // Non-Coffee
        menuItems.put(StringUtil.normalizeName("Chocolate Latte"), new NonCoffeeItem("Chocolate Latte", 165, 180, 195));
        menuItems.put(StringUtil.normalizeName("Strawberry Latte"), new NonCoffeeItem("Strawberry Latte", 0, 190, 205));
        menuItems.put(StringUtil.normalizeName("Mango Latte"), new NonCoffeeItem("Mango Latte", 0, 190, 205));
        menuItems.put(StringUtil.normalizeName("Ube Latte"), new NonCoffeeItem("Ube Latte", 0, 200, 215));

        // Fruit Tea (Iced Regular + Iced Large)
        menuItems.put(StringUtil.normalizeName("Strawberry Green Tea"),
                new FruitTeaItem("Strawberry Green Tea", 0, 180, 195));
        menuItems.put(StringUtil.normalizeName("Mango Green Tea"), new FruitTeaItem("Mango Green Tea", 0, 180, 195));
        menuItems.put(StringUtil.normalizeName("Peach Green Tea"), new FruitTeaItem("Peach Green Tea", 0, 180, 195));
        menuItems.put(StringUtil.normalizeName("Passion Fruit Green Tea"),
                new FruitTeaItem("Passion Fruit Green Tea", 0, 180, 195));

        // Herbal Tea (Hot only)
        menuItems.put(StringUtil.normalizeName("Peppermint"), new HerbalTeaItem("Peppermint", 120, 0, 0));
        menuItems.put(StringUtil.normalizeName("Chamomile"), new HerbalTeaItem("Chamomile", 120, 0, 0));
        menuItems.put(StringUtil.normalizeName("Earl Grey"), new HerbalTeaItem("Earl Grey", 135, 0, 0));
        menuItems.put(StringUtil.normalizeName("Cinnamon"), new HerbalTeaItem("Cinnamon", 135, 0, 0));
    }

    private void ensureExtraItems() {
        Map<String, MenuItem> extra = new LinkedHashMap<>();
        boolean changed = false;

        if (!menuItems.containsKey("Manila Latte")) {
            extra.put(StringUtil.normalizeName("Manila Latte"), new CoffeeItem("Manila Latte", 0, 215, 0));
        }
        if (!menuItems.containsKey("Pumpkin Spice Latte")) {
            extra.put(StringUtil.normalizeName("Pumpkin Spice Latte"),
                    new CoffeeItem("Pumpkin Spice Latte", 0, 220, 0));
        }
        if (!menuItems.containsKey("Spiced Cookie Latte")) {
            extra.put(StringUtil.normalizeName("Spiced Cookie Latte"),
                    new CoffeeItem("Spiced Cookie Latte", 0, 225, 0));
        }
        if (!menuItems.containsKey("Dragon Fruit Coconut Latte")) {
            extra.put(StringUtil.normalizeName("Dragon Fruit Coconut Latte"),
                    new NonCoffeeItem("Dragon Fruit Coconut Latte", 0, 190, 210));
        }
        if (!menuItems.containsKey("Pumpkin Spice Latte")) {
            extra.put(StringUtil.normalizeName("Pumpkin Spice Latte"),
                    new CoffeeItem("Pumpkin Spice Latte", 0, 220, 0));
        }
        if (!menuItems.containsKey("Spiced Cookie Latte")) {
            extra.put(StringUtil.normalizeName("Spiced Cookie Latte"),
                    new CoffeeItem("Spiced Cookie Latte", 0, 225, 0));
        }
        if (!menuItems.containsKey("Hojicha Latte")) {
            extra.put(StringUtil.normalizeName("Hojicha Latte"), new NonCoffeeItem("Hojicha Latte", 200, 215, 0));
        }
        if (!menuItems.containsKey("Chai Latte")) {
            extra.put(StringUtil.normalizeName("Chai Latte"), new NonCoffeeItem("Chai Latte", 190, 205, 0));
        }
        if (!menuItems.containsKey("Earl Grey") && menuItems.containsKey("Early Grey")) {
            MenuItem old = menuItems.remove("Early Grey");
            if (old != null) {
                menuItems.put(StringUtil.normalizeName("Earl Grey"),
                        new HerbalTeaItem("Earl Grey", old.getHotPrice(), 0, 0));
                changed = true;
            }
        }

        // Sandwiches
        if (!menuItems.containsKey("Signature Ham & Cheese")) {
            extra.put(StringUtil.normalizeName("Signature Ham & Cheese"),
                    new FoodItem("Signature Ham & Cheese", "Food", 290));
        }
        if (!menuItems.containsKey("Classic Grilled Cheese")) {
            extra.put(StringUtil.normalizeName("Classic Grilled Cheese"),
                    new FoodItem("Classic Grilled Cheese", "Food", 280));
        }
        if (!menuItems.containsKey("Homestyle Pesto & Cheese")) {
            extra.put(StringUtil.normalizeName("Homestyle Pesto & Cheese"),
                    new FoodItem("Homestyle Pesto & Cheese", "Food", 270));
        }

        // Pandesal Pairs
        if (!menuItems.containsKey("Ham & Cheese")) {
            extra.put(StringUtil.normalizeName("Ham & Cheese"), new FoodItem("Ham & Cheese", "Food", 255));
        }
        if (!menuItems.containsKey("Cheesy Pesto")) {
            extra.put(StringUtil.normalizeName("Cheesy Pesto"), new FoodItem("Cheesy Pesto", "Food", 225));
        }
        if (!menuItems.containsKey("Spam & Cheese")) {
            extra.put(StringUtil.normalizeName("Spam & Cheese"), new FoodItem("Spam & Cheese", "Food", 240));
        }

        // Pastries
        if (!menuItems.containsKey("Chocolate Crinkles")) {
            extra.put(StringUtil.normalizeName("Chocolate Crinkles"), new FoodItem("Chocolate Crinkles", "Food", 60));
        }
        if (!menuItems.containsKey("Chocolate Cookies")) {
            extra.put(StringUtil.normalizeName("Chocolate Cookies"), new FoodItem("Chocolate Cookies", "Food", 65));
        }
        if (!menuItems.containsKey("S'mores Cookie")) {
            extra.put(StringUtil.normalizeName("S'mores Cookie"), new FoodItem("S'mores Cookie", "Food", 75));
        }
        if (!menuItems.containsKey("Red Velvet Cream Cheese Cookie")) {
            extra.put(StringUtil.normalizeName("Red Velvet Cream Cheese Cookie"),
                    new FoodItem("Red Velvet Cream Cheese Cookie", "Food", 85));
        }
        if (!menuItems.containsKey("Brownies")) {
            extra.put(StringUtil.normalizeName("Brownies"), new FoodItem("Brownies", "Food", 80));
        }
        if (!menuItems.containsKey("Banana Bread")) {
            extra.put(StringUtil.normalizeName("Banana Bread"), new FoodItem("Banana Bread", "Food", 75));
        }
        if (!menuItems.containsKey("Chocolate Tiramisu")) {
            extra.put(StringUtil.normalizeName("Chocolate Tiramisu"), new FoodItem("Chocolate Tiramisu", "Food", 130));
        }
        if (!menuItems.containsKey("Matcha Tiramisu")) {
            extra.put(StringUtil.normalizeName("Matcha Tiramisu"), new FoodItem("Matcha Tiramisu", "Food", 140));
        }
        if (!menuItems.containsKey("Creamy Spinach")) {
            extra.put(StringUtil.normalizeName("Creamy Spinach"), new FoodItem("Creamy Spinach", "Food", 150));
        }
        if (!menuItems.containsKey("Blueberry Cheesecake")) {
            extra.put(StringUtil.normalizeName("Blueberry Cheesecake"),
                    new FoodItem("Blueberry Cheesecake", "Food", 145));
        }

        if (!extra.isEmpty()) {
            for (MenuItem item : extra.values()) {
                menuItems.put(StringUtil.normalizeName(item.getName()), item);
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
        return menuItems.get(StringUtil.normalizeName(name));
    }

    public Map<String, MenuItem> getAllItems() {
        return new LinkedHashMap<>(menuItems);
    }

    /** Reload the menu items from the repository and notify listeners. */
    public void reloadFromRepository() {
        try {
            menuItems.clear();
            for (MenuItem item : repository.findAll()) {
                menuItems.put(StringUtil.normalizeName(item.getName()), item);
            }
            notifyChangeListeners();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to reload menu from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void saveItem(MenuItem item) {
        String name = StringUtil.normalizeName(item.getName());
        item.name = name;
        try {
            repository.save(item);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to save menu item to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
            throw new IllegalStateException("Unable to save menu item to database", e);
        }
        menuItems.put(name, item);
        notifyChangeListeners();
    }

    public void removeItem(String name) {
        String n = StringUtil.normalizeName(name);
        try {
            repository.delete(n);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Unable to remove menu item from database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
            throw new IllegalStateException("Unable to remove menu item from database", e);
        }
        menuItems.remove(n);
        notifyChangeListeners();
    }

    // --- Change listener support for UI components that need to refresh on menu
    // changes ---
    private final List<Runnable> changeListeners = new ArrayList<>();

    public void addChangeListener(Runnable r) {
        if (r == null)
            return;
        changeListeners.add(r);
    }

    private void notifyChangeListeners() {
        for (Runnable r : new ArrayList<>(changeListeners)) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    public double getPrice(String name, String variant) {
        MenuItem item = menuItems.get(StringUtil.normalizeName(name));
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
            JOptionPane.showMessageDialog(null, "This option is not available for " + name, "Option Unavailable",
                    JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        return price;
    }
}
