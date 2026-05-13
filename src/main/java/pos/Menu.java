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
        menuItems.put("Americano", new CoffeeItem("Americano", 115, 130, 145));
        menuItems.put("Latte", new CoffeeItem("Latte", 135, 150, 165));
        menuItems.put("Cappuccino", new CoffeeItem("Cappuccino", 135, 150, 165));
        menuItems.put("Salted Cream Latte", new CoffeeItem("Salted Cream Latte", 155, 170, 185));
        menuItems.put("Dark Mocha", new CoffeeItem("Dark Mocha", 165, 180, 195));
        menuItems.put("White Mocha", new CoffeeItem("White Mocha", 165, 180, 195));
        menuItems.put("Caramel Macchiato", new CoffeeItem("Caramel Macchiato", 165, 180, 195));
        menuItems.put("Spanish Latte", new CoffeeItem("Spanish Latte", 165, 180, 195));
        menuItems.put("Vietnamese Coffee", new CoffeeItem("Vietnamese Coffee", 165, 180, 195));
        menuItems.put("Ube Espresso", new CoffeeItem("Ube Espresso", 170, 185, 200));
        menuItems.put("Matcha Espresso", new CoffeeItem("Matcha Espresso", 160, 175, 190));
        menuItems.put("Brewed Coffee", new CoffeeItem("Brewed Coffee", 100, 0, 0));

        menuItems.put("Chocolate Latte", new NonCoffeeItem("Chocolate Latte", 160, 175, 190));
        menuItems.put("Matcha Latte", new NonCoffeeItem("Matcha Latte", 160, 175, 190));
        menuItems.put("Chocolate Matcha", new NonCoffeeItem("Chocolate Matcha", 140, 155, 170));
        menuItems.put("Strawberry Latte", new NonCoffeeItem("Strawberry Latte", 0, 185, 200));
        menuItems.put("Mango Latte", new NonCoffeeItem("Mango Latte", 0, 185, 200));
        menuItems.put("Ube Latte", new NonCoffeeItem("Ube Latte", 0, 195, 210));

        menuItems.put("Strawberry Green Tea", new FruitTeaItem("Strawberry Green Tea", 0, 175, 190));
        menuItems.put("Mango Green Tea", new FruitTeaItem("Mango Green Tea", 0, 175, 190));
        menuItems.put("Peach Green Tea", new FruitTeaItem("Peach Green Tea", 0, 175, 190));
        menuItems.put("Passion Fruit Green Tea", new FruitTeaItem("Passion Fruit Green Tea", 0, 175, 190));

        menuItems.put("Peppermint", new HerbalTeaItem("Peppermint", 115, 0, 0));
        menuItems.put("Chamomile", new HerbalTeaItem("Chamomile", 115, 0, 0));
        menuItems.put("Early Grey", new HerbalTeaItem("Early Grey", 130, 0, 0));
        menuItems.put("Cinnamon", new HerbalTeaItem("Cinnamon", 130, 0, 0));
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
