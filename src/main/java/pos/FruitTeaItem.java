package pos;

import java.util.HashMap;
import java.util.Map;

public class FruitTeaItem extends MenuItem {

    public FruitTeaItem(String name, double hotPrice, double icedRegularPrice, double icedLargePrice) {
        super(name, hotPrice, icedRegularPrice, icedLargePrice);
    }

    @Override
    public String getCategory() {
        return "Fruit Tea";
    }

    @Override
    public Map<String, Double> getIngredients() {
        Map<String, Double> ingredients = new HashMap<>();

        
        ingredients.put("Green Tea", 120.0);
        ingredients.put("Cup", 1.0);
        ingredients.put("Lid", 1.0);
        ingredients.put("Straw", 1.0);
        ingredients.put("Cup Holder", 1.0);

        
        if (name.toLowerCase().contains("strawberry")) {
            ingredients.put("Strawberry Syrup", 30.0);  
        }

        if (name.toLowerCase().contains("mango")) {
            ingredients.put("Mango Syrup", 30.0);
        }

        if (name.toLowerCase().contains("peach")) {
            ingredients.put("Peach Syrup", 30.0);
        }

        if (name.toLowerCase().contains("passion")) {
            ingredients.put("Passion Fruit Syrup", 30.0);
        }

        return ingredients;
    }
}

