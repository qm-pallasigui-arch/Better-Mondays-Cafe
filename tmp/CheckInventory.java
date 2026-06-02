import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.nio.file.Paths;

public class CheckInventory {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlite:" + Paths.get("data", "coffee-cafe.db").toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name, quantity, unit FROM inventory_items WHERE LOWER(name) LIKE '%ube%'")) {
            System.out.println("=== Ube-related inventory items ===");
            boolean found = false;
            while (rs.next()) { found = true;
                System.out.println("  " + rs.getString("name") + " | qty=" + rs.getDouble("quantity") + " " + rs.getString("unit")); }
            if (!found) System.out.println("  (none found)");
        }
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM inventory_items ORDER BY name")) {
            System.out.println("\n=== ALL inventory items ===");
            while (rs.next()) System.out.println("  " + rs.getString("name"));
        }
    }
}
