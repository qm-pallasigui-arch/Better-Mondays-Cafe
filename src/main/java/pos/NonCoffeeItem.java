package pos;

import java.util.HashMap;
import java.util.Map;

public class NonCoffeeItem extends MenuItem {

    public NonCoffeeItem(String name, double hotPrice, double icedRegularPrice, double icedLargePrice) {
        super(name, hotPrice, icedRegularPrice, icedLargePrice);
    }

    @Override
    public String getCategory() {
        return "Non-Coffee";
    }

    @Override
    public Map<String, Double> getIngredients() {
        // If ingredients were loaded/edited from persistence, prefer those values.
        if (!ingredients.isEmpty()) {
            return super.getIngredients();
        }
        Map<String, Double> ingredients = new HashMap<>();

        ingredients.put("Cup", 1.0);
        ingredients.put("Lid", 1.0);
        ingredients.put("Straw", 1.0);
        ingredients.put("Cup Holder", 1.0);

        if (name.toLowerCase().contains("latte")) {
            ingredients.put("Milk", 120.0);
        }

        if (name.toLowerCase().contains("strawberry")) {
            ingredients.put("Strawberry Syrup", 30.0);
        }

        if (name.toLowerCase().contains("mango")) {
            ingredients.put("Mango Syrup", 30.0);
        }

        if (name.toLowerCase().contains("chocolate")) {
            ingredients.put("Milk Chocolate", 30.0);
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
