package ui;

import java.util.HashMap;
import java.util.Map;

public class ImageRegistry {

    private static final Map<String, String> NAME_TO_PATH = new HashMap<>();

    static {
        NAME_TO_PATH.put("Americano", "images/menu/americano.png");
        NAME_TO_PATH.put("Brewed Coffee", "images/menu/brewed_coffee.png");
        NAME_TO_PATH.put("Cappuccino", "images/menu/cappuccino.png");
        NAME_TO_PATH.put("Caramel Macchiato", "images/menu/caramel_macchiato.png");
        NAME_TO_PATH.put("Dark Mocha", "images/menu/dark_mocha.png");
        NAME_TO_PATH.put("Latte", "images/menu/latte.png");
        NAME_TO_PATH.put("White Mocha", "images/menu/white_mocha.png");

        NAME_TO_PATH.put("Vietnamese Coffee", "images/menu/vietnamese_coffee.png");
        NAME_TO_PATH.put("Ube Espresso", "images/menu/ube_espresso.png");
        NAME_TO_PATH.put("Manila Latte", "images/menu/manila_latte.png");
        NAME_TO_PATH.put("Pumpkin Spice Latte", "images/menu/pumpkin_spice_latte.png");
        NAME_TO_PATH.put("Spiced Cookie Latte", "images/menu/spiced_cookie_latte.png");

        NAME_TO_PATH.put("Matcha Latte", "images/menu/matcha_latte.png");
        NAME_TO_PATH.put("Chocolate Matcha", "images/menu/chocolate_matcha.png");
        NAME_TO_PATH.put("Matcha Espresso", "images/menu/matcha_espresso.png");
        NAME_TO_PATH.put("Hojicha Latte", "images/menu/hojicha_latte.png");
        NAME_TO_PATH.put("Chai Latte", "images/menu/chai_latte.png");

        NAME_TO_PATH.put("Chocolate Latte", "images/menu/chocolate_latte.png");
        NAME_TO_PATH.put("Strawberry Latte", "images/menu/strawberry_latte.png");
        NAME_TO_PATH.put("Mango Latte", "images/menu/mango_latte.png");
        NAME_TO_PATH.put("Dragon Fruit Coconut Latte", "images/menu/dragon_fruit_coconut_latte.png");
        NAME_TO_PATH.put("Ube Latte", "images/menu/ube_latte.png");

        NAME_TO_PATH.put("Salted Cream Latte", "images/menu/salted_cream_latte.png");
        NAME_TO_PATH.put("Spanish Latte", "images/menu/spanish_latte.png");

        NAME_TO_PATH.put("Strawberry Green Tea", "images/menu/strawberry_green_tea.png");
        NAME_TO_PATH.put("Mango Green Tea", "images/menu/mango_green_tea.png");
        NAME_TO_PATH.put("Peach Green Tea", "images/menu/peach_green_tea.png");
        NAME_TO_PATH.put("Passion Fruit Green Tea", "images/menu/passion_fruit_green_tea.png");

        NAME_TO_PATH.put("Peppermint", "images/menu/peppermint.png");
        NAME_TO_PATH.put("Chamomile", "images/menu/chamomile.png");
        NAME_TO_PATH.put("Earl Grey", "images/menu/earl_grey.png");
        NAME_TO_PATH.put("Cinnamon", "images/menu/cinnamon.png");

        NAME_TO_PATH.put("Signature Ham & Cheese", "images/menu/ham_cheese_sandwich.png");
        NAME_TO_PATH.put("Classic Grilled Cheese", "images/menu/grilled_cheese.png");
        NAME_TO_PATH.put("Homestyle Pesto & Cheese", "images/menu/pesto_cheese.png");

        NAME_TO_PATH.put("Ham & Cheese", "images/menu/ham_cheese_pandesal.png");
        NAME_TO_PATH.put("Cheesy Pesto", "images/menu/cheesy_pesto_pandesal.png");
        NAME_TO_PATH.put("Spam & Cheese", "images/menu/spam_cheese_pandesal.png");

        NAME_TO_PATH.put("Chocolate Crinkles", "images/menu/chocolate_crinkles.png");
        NAME_TO_PATH.put("Chocolate Cookies", "images/menu/chocolate_cookies.png");
        NAME_TO_PATH.put("Brownies", "images/menu/brownies.png");
        NAME_TO_PATH.put("Banana Bread", "images/menu/banana_bread.png");
        NAME_TO_PATH.put("Chocolate Tiramisu", "images/menu/chocolate_tiramisu.png");
        NAME_TO_PATH.put("Matcha Tiramisu", "images/menu/matcha_tiramisu.png");
        NAME_TO_PATH.put("Creamy Spinach", "images/menu/creamy_spinach.png");
        NAME_TO_PATH.put("Blueberry Cheesecake", "images/menu/blueberry_cheesecake.png");
    }

    public static String getPath(String itemName) {
        return NAME_TO_PATH.getOrDefault(itemName, "images/menu/placeholder.png");
    }
}