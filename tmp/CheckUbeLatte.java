import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.nio.file.Paths;

public class CheckUbeLatte {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:" + Paths.get("data", "coffee-cafe.db").toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            // Check Ube Latte in menu_items
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, category, hot_price, iced_regular_price, iced_large_price FROM menu_items WHERE name = ?")) {
                ps.setString(1, "Ube Latte");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("=== Ube Latte in menu_items ===");
                        System.out.println("  id=" + rs.getInt("id") + " name=" + rs.getString("name") + " category=" + rs.getString("category"));
                        System.out.println("  hot=" + rs.getDouble("hot_price") + " iced_reg=" + rs.getDouble("iced_regular_price") + " iced_lg=" + rs.getDouble("iced_large_price"));
                    } else {
                        System.out.println("ERROR: Ube Latte NOT FOUND in menu_items!");
                    }
                }
            }
            // Check Ube Latte ingredients
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT mi.id, mi.name, mii.ingredient_name, mii.quantity FROM menu_items mi LEFT JOIN menu_item_ingredients mii ON mii.menu_item_id = mi.id WHERE mi.name = ? ORDER BY mii.id")) {
                ps.setString(1, "Ube Latte");
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("\n=== Ube Latte ingredients ===");
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        String ing = rs.getString("ingredient_name");
                        if (ing != null)
                            System.out.println("  " + ing + " = " + rs.getDouble("quantity"));
                        else
                            System.out.println("  (no ingredients)");
                    }
                    if (!found) System.out.println("  (not found)");
                }
            }
            // Also check the normalized name
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM menu_items WHERE LOWER(REPLACE(name, ' ', '')) = LOWER(REPLACE(?, ' ', ''))")) {
                ps.setString(1, "Ube Latte");
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println("\n=== Normalized lookup ===");
                    while (rs.next()) System.out.println("  id=" + rs.getInt("id") + " name='" + rs.getString("name") + "'");
                }
            }
        }
    }
}
