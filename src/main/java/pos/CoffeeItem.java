package pos;

import java.util.HashMap;
import java.util.Map;

public class CoffeeItem extends MenuItem {

    public CoffeeItem(String name, double hotPrice, double icedRegularPrice, double icedLargePrice) {
        super(name, hotPrice, icedRegularPrice, icedLargePrice);
    }

    @Override
    public String getCategory() {
        return "Coffee";
    }

    public Map<String, Double> getIngredients() {
        // If ingredients were loaded/edited from persistence, prefer those values.
        if (!ingredients.isEmpty()) {
            return super.getIngredients();
        }
        Map<String, Double> ingredients = new HashMap<>();

        ingredients.put("Coffee Beans", 18.0);
        ingredients.put("Cup", 1.0);
        ingredients.put("Lid", 1.0);
        ingredients.put("Straw", 1.0);
        ingredients.put("Cup Holder", 1.0);

        if (name.toLowerCase().contains("latte") || name.toLowerCase().contains("cappuccino")) {
            ingredients.put("Milk", 120.0);
        }

        if (name.toLowerCase().contains("dark mocha")) {
            ingredients.put("Milk", 120.0);
            ingredients.put("Dark Chocolate", 50.0);
        }

        if (name.toLowerCase().contains("white mocha")) {
            ingredients.put("Milk", 120.0);
            ingredients.put("White Chocolate", 50.0);
        }

        if (name.toLowerCase().contains("caramel")) {
            ingredients.put("Caramel Syrup", 30.0);
        }

        if (name.toLowerCase().contains("spanish") || name.toLowerCase().contains("vietnamese")) {
            ingredients.put("Condensed Milk", 30.0);
        }

        if (name.toLowerCase().contains("ube")) {
            ingredients.put("Ube Flavoring", 20.0);
        }

        if (name.toLowerCase().contains("matcha")) {
            ingredients.put("Matcha Powder", 20.0);
        }

        return ingredients;
    }
}
