package pos;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

public class Menu {

    private static Menu instance;
    private Map<String, MenuItem> MenuItems;

    private Menu() {
        MenuItems = new HashMap<>();
        initializeMenu();
    }

    public static Menu getInstance() {
        if (instance == null) {
            instance = new Menu();
        }
        return instance;
    }

    private void initializeMenu() {
        MenuItems.put("Americano", new CoffeeItem("Americano", 115, 130, 145));
        MenuItems.put("Latte", new CoffeeItem("Latte", 135, 150, 165));
        MenuItems.put("Cappuccino", new CoffeeItem("Cappuccino", 135, 150, 165));
        MenuItems.put("Salted Cream Latte", new CoffeeItem("Salted Cream Latte", 155, 170, 185));
        MenuItems.put("Dark Mocha", new CoffeeItem("Dark Mocha", 165, 180, 195));
        MenuItems.put("White Mocha", new CoffeeItem("White Mocha", 165, 180, 195));
        MenuItems.put("Caramel Macchiato", new CoffeeItem("Caramel Macchiato", 165, 180, 195));
        MenuItems.put("Spanish Latte", new CoffeeItem("Spanish Latte", 165, 180, 195));
        MenuItems.put("Vietnamese Coffee", new CoffeeItem("Vietnamese Coffee", 165, 180, 195));
        MenuItems.put("Ube Espresso", new CoffeeItem("Ube Espresso", 170, 185, 200));
        MenuItems.put("Matcha Espresso", new CoffeeItem("Matcha Espresso", 160, 175, 190));
        MenuItems.put("Brewed Coffee", new CoffeeItem("Brewed Coffee", 100, 0, 0));

        MenuItems.put("Chocolate Latte", new NonCoffeeItem("Chocolate Latte", 160, 175, 190));
        MenuItems.put("Matcha Latte", new NonCoffeeItem("Matcha Latte", 160, 175, 190));
        MenuItems.put("Chocolate Matcha", new NonCoffeeItem("Chocolate Matcha", 140, 155, 170));
        MenuItems.put("Strawberry Latte", new NonCoffeeItem("Strawberry Latte", 0, 185, 200));
        MenuItems.put("Mango Latte", new NonCoffeeItem("Mango Latte", 0, 185, 200));
        MenuItems.put("Ube Latte", new NonCoffeeItem("Ube Latte", 0, 195, 210));

        MenuItems.put("Strawberry Green Tea", new FruitTeaItem("Strawberry Green Tea", 0, 175, 190));
        MenuItems.put("Mango Green Tea", new FruitTeaItem("Mango Green Tea", 0, 175, 190));
        MenuItems.put("Peach Green Tea", new FruitTeaItem("Peach Green Tea", 0, 175, 190));
        MenuItems.put("Passion Fruit Green Tea", new FruitTeaItem("Passion Fruit Green Tea", 0, 175, 190));

        MenuItems.put("Peppermint", new HerbalTeaItem("Peppermint", 115, 0, 0));
        MenuItems.put("Chamomile", new HerbalTeaItem("Chamomile", 115, 0, 0));
        MenuItems.put("Early Grey", new HerbalTeaItem("Early Grey", 130, 0, 0));
        MenuItems.put("Cinnamon", new HerbalTeaItem("Cinnamon", 130, 0, 0));
    }

    public MenuItem getMenuItem(String name) {
        return MenuItems.get(name);
    }

    public double getPrice(String name, String variant) {
        MenuItem item = MenuItems.get(name);
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
