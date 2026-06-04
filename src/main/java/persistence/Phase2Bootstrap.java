package persistence;

import inventory.Inventory;
import inventory.InventoryItem;
import java.util.Map;
import loginregister.UserDataManager.Role;
import persistence.sqlite.SQLiteInventoryRepository;
import persistence.sqlite.SQLiteMenuRepository;
import persistence.sqlite.SQLiteUserRepository;
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

        seedDefaultAccounts();

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

    private static void seedDefaultAccounts() throws Exception {
        SQLiteUserRepository userRepository = new SQLiteUserRepository();
        if (userRepository.listUsers().isEmpty()) {
            userRepository.createUser("admin", "Admin@123", Role.ADMIN,
                    "Administrator", 0, "", "", "", "");
            userRepository.createUser("staff", "Staff@123", Role.STAFF,
                    "Staff User", 0, "", "", "", "");
        } else {
            // Ensure the default admin password is always correct regardless of
            // which bootstrap method previously created it.
            ensureDefaultPassword(userRepository, "admin", "Admin@123");
            ensureDefaultPassword(userRepository, "staff", "Staff@123");
        }
    }

    private static void ensureDefaultPassword(SQLiteUserRepository repo,
            String username, String expected) throws Exception {
        boolean exists = repo.listUsers().stream()
                .anyMatch(u -> u.getUsername().equals(username));
        if (exists && !repo.verifyCredentials(username, expected)) {
            repo.resetPassword(username, expected);
        }
    }
}
