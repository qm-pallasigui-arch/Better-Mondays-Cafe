package persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the initial schema used by the local SQLite implementation.
 * The schema is intentionally relational so future Firebase adapters can map the same entities.
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
                    + "iced_large_price REAL NOT NULL"
                    + ")");
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
                    + "alert_level REAL NOT NULL"
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
            statement.execute("CREATE TABLE IF NOT EXISTS sales_transaction_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "transaction_id INTEGER NOT NULL, "
                    + "product_name TEXT NOT NULL, "
                    + "quantity INTEGER NOT NULL, "
                    + "price REAL NOT NULL, "
                    + "total REAL NOT NULL, "
                    + "FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id) ON DELETE CASCADE"
                    + ")");
        }
    }
}
