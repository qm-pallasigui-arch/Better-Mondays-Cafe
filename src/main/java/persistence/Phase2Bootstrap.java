package persistence;

import inventory.Inventory;
import inventory.InventoryItem;
import java.util.Map;
import persistence.sqlite.SQLiteInventoryRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.Menu;
import pos.MenuItem;

/**
 * Seeds the local SQLite store from the current in-memory catalog definitions.
 * This keeps the Phase 2 database aligned with the existing application state.
 */
public final class Phase2Bootstrap {

    private Phase2Bootstrap() {
    }

    public static void seedCatalogIfEmpty() throws Exception {
        AppDatabase.ensureInitialized();

        SQLiteMenuRepository menuRepository = new SQLiteMenuRepository();
        SQLiteInventoryRepository inventoryRepository = new SQLiteInventoryRepository();

        if (menuRepository.findAll().isEmpty()) {
            for (Map.Entry<String, MenuItem> entry : Menu.getInstance().getAllItems().entrySet()) {
                menuRepository.save(entry.getValue());
            }
        }

        if (inventoryRepository.findAll().isEmpty()) {
            for (Map.Entry<String, InventoryItem> entry : Inventory.getInstance().getAllItems().entrySet()) {
                inventoryRepository.save(entry.getValue());
            }
        }
    }
}
