package pos;

import java.util.HashMap;
import java.util.Map;

public abstract class MenuItem {
    protected String name;
    protected double hotPrice;          
    protected double icedRegularPrice; 
    protected double icedLargePrice;   
    protected Map<String, Double> ingredients;

    public MenuItem(String name,
                    double hotPrice,
                    double icedRegularPrice,
                    double icedLargePrice) {
        this.name = name;
        this.hotPrice = hotPrice;
        this.icedRegularPrice = icedRegularPrice;
        this.icedLargePrice = icedLargePrice;
        this.ingredients = new HashMap<>();
    }

    public Map<String, Double> getIngredients() {
        return new HashMap<>(ingredients);   
    }

    public void replaceIngredients(Map<String, Double> newIngredients) {
        ingredients.clear();
        if (newIngredients != null) {
            ingredients.putAll(newIngredients);
        }
    }

    public void addIngredient(String ingredient, double quantity) {
        ingredients.put(ingredient, quantity);
    }

    public String getName()               { return name; }
    public double getHotPrice()           { return hotPrice; }
    public double getIcedRegularPrice()   { return icedRegularPrice; }
    public double getIcedLargePrice()     { return icedLargePrice; }

    public abstract String getCategory();
}