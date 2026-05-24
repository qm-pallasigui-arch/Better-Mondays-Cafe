package pos;

import javax.swing.ImageIcon;
import java.util.HashMap;
import java.util.Map;

public abstract class MenuItem {
    protected String name;
    protected double hotPrice;          
    protected double icedRegularPrice; 
    protected double icedLargePrice;   
    protected Map<String, Double> ingredients;
    private String imagePath;

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public ImageIcon loadImage(int width, int height) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        try {
            java.net.URL url = getClass().getResource("/" + imagePath);
            if (url != null) {
                ImageIcon raw = new ImageIcon(url);
                java.awt.Image scaled = raw.getImage()
                    .getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
            java.io.File file = new java.io.File(imagePath);
            if (file.exists()) {
                ImageIcon raw = new ImageIcon(file.getAbsolutePath());
                java.awt.Image scaled = raw.getImage()
                    .getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Could not load image: " + imagePath + " - " + e.getMessage());
        }
        return null;
    }

    public String getName()               { return name; }
    public double getHotPrice()           { return hotPrice; }
    public double getIcedRegularPrice()   { return icedRegularPrice; }
    public double getIcedLargePrice()     { return icedLargePrice; }

    public abstract String getCategory();
}