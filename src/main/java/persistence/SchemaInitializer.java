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
                    + "staff_id INTEGER NOT NULL UNIQUE DEFAULT 0, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, "
                    + "role TEXT NOT NULL, "
                    + "full_name TEXT NOT NULL DEFAULT '', "
                    + "age INTEGER NOT NULL DEFAULT 0, "
                    + "birthdate TEXT NOT NULL DEFAULT '', "
                    + "address TEXT NOT NULL DEFAULT '', "
                    + "mobile TEXT NOT NULL DEFAULT '', "
                    + "gender TEXT NOT NULL DEFAULT '', "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ")");
            // Migrations for existing databases
            for (String colDef : new String[] {
                    "staff_id INTEGER NOT NULL DEFAULT 0",
                    "full_name TEXT NOT NULL DEFAULT ''",
                    "age INTEGER NOT NULL DEFAULT 0",
                    "birthdate TEXT NOT NULL DEFAULT ''",
                    "address TEXT NOT NULL DEFAULT ''",
                    "mobile TEXT NOT NULL DEFAULT ''",
                    "gender TEXT NOT NULL DEFAULT ''" }) {
                try {
                    statement.execute("ALTER TABLE users ADD COLUMN " + colDef);
                } catch (SQLException ignored) {
                }
            }
            // Assign permanent staff_id to any existing users that have the default 0
            try {
                statement.execute(
                        "UPDATE users SET staff_id = id WHERE staff_id = 0 OR staff_id IS NULL");
            } catch (SQLException ignored) {
            }
            statement.execute("CREATE TABLE IF NOT EXISTS menu_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL UNIQUE, "
                    + "category TEXT NOT NULL, "
                    + "hot_price REAL NOT NULL, "
                    + "iced_regular_price REAL NOT NULL, "
                    + "iced_large_price REAL NOT NULL, "
                    + "image_path TEXT, "
                    + "is_available INTEGER NOT NULL DEFAULT 1"
                    + ")");
            // Migrations for existing databases that predate these columns
            try {
                statement.execute(
                        "ALTER TABLE menu_items ADD COLUMN image_path TEXT");
            } catch (SQLException ignored) {
            }
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
            for (String col : new String[] { "storage_location", "last_updated" }) {
                try {
                    statement.execute("ALTER TABLE inventory_items ADD COLUMN " + col
                            + " TEXT NOT NULL DEFAULT ''");
                } catch (java.sql.SQLException ignored) {
                }
            }
            // Batches allow tracking SKU, expiry dates and per-batch quantities for FEFO
            // logic
            statement.execute("CREATE TABLE IF NOT EXISTS inventory_batches ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "inventory_item_id INTEGER NOT NULL, "
                    + "sku TEXT, "
                    + "quantity REAL NOT NULL, "
                    + "expiry_date TEXT, "
                    + "archived INTEGER NOT NULL DEFAULT 0, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY(inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE"
                    + ")");
            // Migration: add archived column to existing databases
            try {
                statement.execute("ALTER TABLE inventory_batches ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
            } catch (java.sql.SQLException ignored) {
            }
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
                    + "customer_name TEXT NOT NULL DEFAULT 'Walk-in', "
                    + "subtotal REAL NOT NULL, "
                    + "tax REAL NOT NULL, "
                    + "total REAL NOT NULL, "
                    + "cash REAL NOT NULL, "
                    + "change_amount REAL NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                    + ")");
            // Migration: add customer_name to existing databases
            try {
                statement.execute(
                        "ALTER TABLE sales_transactions ADD COLUMN customer_name TEXT NOT NULL DEFAULT 'Walk-in'");
            } catch (SQLException ignored) {
            }
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
                    + "started_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), "
                    + "ended_at TEXT, "
                    + "notes TEXT, "
                    + "status TEXT NOT NULL DEFAULT 'active'"
                    + ")");
            try {
                statement.execute("ALTER TABLE staff_shifts ADD COLUMN status TEXT NOT NULL DEFAULT 'active'");
            } catch (java.sql.SQLException ignored) {
            }
            // Weekly staff schedules
            statement.execute("CREATE TABLE IF NOT EXISTS staff_schedules ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "day_of_week INTEGER NOT NULL, "
                    + "shift_type TEXT NOT NULL DEFAULT 'off', "
                    + "UNIQUE(username, day_of_week)"
                    + ")");
            // Password reset requests submitted from the login screen
            statement.execute("CREATE TABLE IF NOT EXISTS password_reset_requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "status TEXT NOT NULL DEFAULT 'PENDING', "
                    + "requested_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), "
                    + "resolved_at TEXT"
                    + ")");
            // Leave requests submitted by Staff
            statement.execute("CREATE TABLE IF NOT EXISTS staff_leave_requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "start_date TEXT NOT NULL, "
                    + "end_date TEXT NOT NULL, "
                    + "reason TEXT, "
                    + "status TEXT NOT NULL DEFAULT 'pending', "
                    + "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), "
                    + "resolved_at TEXT"
                    + ")");
            // Migration: add resolved_at to existing leave request tables
            try {
                statement.execute("ALTER TABLE staff_leave_requests ADD COLUMN resolved_at TEXT");
            } catch (java.sql.SQLException ignored) {
            }
            // Staff reports for Admin notification
            statement.execute("CREATE TABLE IF NOT EXISTS staff_reports ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "type INTEGER NOT NULL, "
                    + "item_name TEXT NOT NULL, "
                    + "value TEXT NOT NULL DEFAULT '', "
                    + "status TEXT NOT NULL DEFAULT 'pending', "
                    + "created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')), "
                    + "acknowledged_at TEXT, "
                    + "acknowledged_by TEXT"
                    + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS profile_pictures ("
                    + "username TEXT PRIMARY KEY, "
                    + "image_data BLOB NOT NULL"
                    + ")");
        }
    }
}
