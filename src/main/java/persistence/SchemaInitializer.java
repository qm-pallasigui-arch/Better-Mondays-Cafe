package persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the initial schema used by the local SQLite implementation.
 * The schema is intentionally relational so future Firebase adapters can map
 * the same entities.
 */
public final class SchemaInitializer {

        private SchemaInitializer() {
        }

        public static void initialize(Connection connection) throws SQLException {
                try (Statement statement = connection.createStatement()) {
                        statement.execute("PRAGMA foreign_keys = ON");
                        statement.execute("CREATE TABLE IF NOT EXISTS users ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "username TEXT NOT NULL UNIQUE, "
                                        + "password_hash TEXT NOT NULL, "
                                        + "role TEXT NOT NULL, "
                                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                        + ")");
                        statement.execute("CREATE TABLE IF NOT EXISTS menu_items ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "name TEXT NOT NULL UNIQUE, "
                                        + "category TEXT NOT NULL, "
                                        + "hot_price REAL NOT NULL, "
                                        + "iced_regular_price REAL NOT NULL, "
                                        + "iced_large_price REAL NOT NULL, "
                                        + "is_available INTEGER NOT NULL DEFAULT 1"
                                        + ")");
                        // Migration: add is_available to existing databases that predate this column
                        try {
                                statement.execute(
                                                "ALTER TABLE menu_items ADD COLUMN is_available INTEGER NOT NULL DEFAULT 1");
                        } catch (SQLException ignored) {
                        }
                        statement.execute("CREATE TABLE IF NOT EXISTS menu_item_ingredients ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "menu_item_id INTEGER NOT NULL, "
                                        + "ingredient_name TEXT NOT NULL, "
                                        + "quantity REAL NOT NULL, "
                                        + "FOREIGN KEY(menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE"
                                        + ")");
                        statement.execute("CREATE TABLE IF NOT EXISTS inventory_items ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "name TEXT NOT NULL UNIQUE, "
                                        + "quantity REAL NOT NULL, "
                                        + "unit TEXT NOT NULL, "
                                        + "alert_level REAL NOT NULL, "
                                        + "storage_location TEXT NOT NULL DEFAULT '', "
                                        + "last_updated TEXT NOT NULL DEFAULT ''"
                                        + ")");
                        // Migration: add columns to existing databases that predate them
                        for (String col : new String[]{"storage_location", "last_updated"}) {
                            try {
                                statement.execute("ALTER TABLE inventory_items ADD COLUMN " + col
                                        + " TEXT NOT NULL DEFAULT ''");
                            } catch (java.sql.SQLException ignored) {}
                        }
                        // Batches allow tracking SKU, expiry dates and per-batch quantities for FEFO
                        // logic
                        statement.execute("CREATE TABLE IF NOT EXISTS inventory_batches ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "inventory_item_id INTEGER NOT NULL, "
                                        + "sku TEXT, "
                                        + "quantity REAL NOT NULL, "
                                        + "expiry_date TEXT, "
                                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                                        + "FOREIGN KEY(inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE"
                                        + ")");
                        statement.execute("CREATE TABLE IF NOT EXISTS sales_records ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "product_name TEXT NOT NULL, "
                                        + "quantity INTEGER NOT NULL, "
                                        + "price REAL NOT NULL, "
                                        + "total REAL NOT NULL, "
                                        + "sold_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                        + ")");
                        statement.execute("CREATE TABLE IF NOT EXISTS sales_transactions ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "transaction_ref TEXT NOT NULL UNIQUE, "
                                        + "subtotal REAL NOT NULL, "
                                        + "tax REAL NOT NULL, "
                                        + "total REAL NOT NULL, "
                                        + "cash REAL NOT NULL, "
                                        + "change_amount REAL NOT NULL, "
                                        + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                        + ")");
                        // Order status table stores status for transactions (PENDING, COMPLETED,
                        // CANCELLED, etc.)
                        statement.execute("CREATE TABLE IF NOT EXISTS sales_order_status ("
                                        + "transaction_id INTEGER NOT NULL UNIQUE, "
                                        + "status TEXT NOT NULL, "
                                        + "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                                        + "FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id) ON DELETE CASCADE"
                                        + ")");
                        statement.execute("CREATE TABLE IF NOT EXISTS sales_transaction_items ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "transaction_id INTEGER NOT NULL, "
                                        + "product_name TEXT NOT NULL, "
                                        + "quantity INTEGER NOT NULL, "
                                        + "price REAL NOT NULL, "
                                        + "total REAL NOT NULL, "
                                        + "FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id) ON DELETE CASCADE"
                                        + ")");
                        // Staff shift tracking
                        statement.execute("CREATE TABLE IF NOT EXISTS staff_shifts ("
                                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                        + "username TEXT NOT NULL, "
                                        + "started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                                        + "ended_at TEXT, "
                                        + "notes TEXT"
                                        + ")");
                }
        }
}
