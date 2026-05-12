package pos;

import java.util.HashMap;
import java.util.Map;

public class HerbalTeaItem extends MenuItem {

    public HerbalTeaItem(String name, double hotPrice, double icedRegularPrice, double icedLargePrice) {
        super(name, hotPrice, icedRegularPrice, icedLargePrice);
    }

    @Override
    public String getCategory() {
        return "Herbal Tea";
    }

    public Map<String, Double> getIngredients() {
        Map<String, Double> ingredients = new HashMap<>();

        ingredients.put("Cup", 1.0);
        ingredients.put("Lid", 1.0);
        ingredients.put("Cup Holder", 1.0);

        if (name.toLowerCase().contains("peppermint")) {
            ingredients.put("Peppermint Tea Bag", 1.0);
        }

        if (name.toLowerCase().contains("chamomile")) {
            ingredients.put("Chamomile Tea Bag", 1.0);
        }

        if (name.toLowerCase().contains("early")) {
            ingredients.put("Earl Grey Tea Bag", 1.0);
        }

        if (name.toLowerCase().contains("cinnamon")) {
            ingredients.put("Cinnamon Tea Bag", 1.0);
        }

        return ingredients;
    }
}
