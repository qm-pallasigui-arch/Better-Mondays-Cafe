package persistence.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.Test;
import persistence.AppDatabase;
import pos.CoffeeItem;
import pos.MenuItem;
import util.StringUtil;

class SQLiteMenuRepositoryTest {

    @Test
    void savesAndLoadsMenuItemImagePath() throws Exception {
        SQLiteMenuRepository repo = new SQLiteMenuRepository();
        String itemName = "Image Test Latte " + System.nanoTime();
        String normalizedName = StringUtil.normalizeName(itemName);
        String imagePath = "C:\\temp\\image-test-latte.png";
        cleanup(normalizedName);

        CoffeeItem item = new CoffeeItem(itemName, 100, 120, 140);
        item.setImagePath(imagePath);

        repo.save(item);

        MenuItem found = repo.findByName(normalizedName).orElseThrow();
        assertEquals(imagePath, found.getImagePath());
        assertTrue(repo.findAll().stream()
                .anyMatch(menuItem -> normalizedName.equals(menuItem.getName())
                        && imagePath.equals(menuItem.getImagePath())));

        cleanup(normalizedName);
    }

    private void cleanup(String itemName) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM menu_items WHERE name = ?")) {
            statement.setString(1, itemName);
            statement.executeUpdate();
        }
    }
}
