package pos;

public class FoodItem extends MenuItem {

    private final String category;

    public FoodItem(String name, String category, double price) {
        super(name, price, 0, 0);
        this.category = category;
    }

    @Override
    public String getCategory() {
        return category;
    }
}
