import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Paths;
import java.util.*;

public class DebugUbeLatte {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:" + Paths.get("data", "coffee-cafe.db").toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            // 1. Check Ube Latte in menu_items
            System.out.println("=== 1. Ube Latte in menu_items ===");
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM menu_items WHERE name = ?")) {
                ps.setString(1, "Ube Latte");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("  Found: id=" + rs.getInt("id") + " category=" + rs.getString("category"));
                    } else {
                        System.out.println("  NOT FOUND!");
                    }
                }
            }

            // 2. Check ingredients
            System.out.println("\n=== 2. Ube Latte ingredients ===");
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ingredient_name, quantity FROM menu_item_ingredients WHERE menu_item_id = (SELECT id FROM menu_items WHERE name = 'Ube Latte')")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ing = rs.getString("ingredient_name");
                        double qty = rs.getDouble("quantity");
                        System.out.println("  " + ing + " = " + qty);
                    }
                }
            }

            // 3. Check inventory for all required ingredients
            System.out.println("\n=== 3. Inventory check ===");
            String[] ings = {"Cup", "Lid", "Straw", "Cup Holder", "Milk", "Ube Flavoring"};
            for (String ing : ings) {
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT quantity FROM inventory_items WHERE REPLACE(LOWER(TRIM(name)), '  ', ' ') = REPLACE(LOWER(TRIM(?)), '  ', ' ')")) {
                    ps.setString(1, ing);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("  " + ing + " = " + rs.getDouble("quantity") + " ✓");
                        } else {
                            System.out.println("  " + ing + " = MISSING ✗");
                        }
                    }
                }
            }

            // 4. Check the exact SQL that would be used by the app to find Ube Latte in categories
            System.out.println("\n=== 4. Category items containing 'Ube Latte' ===");
            // Simulate Normalize: check all category items
            try (PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT category FROM menu_items ORDER BY category")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) System.out.println("  DB category: " + rs.getString("category"));
                }
            }
        }
    }
}
