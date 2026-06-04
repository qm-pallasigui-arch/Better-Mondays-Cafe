package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.StringUtil;

/**
 * Central database entry point for the local SQLite store.
 * This is the storage seam that later can be swapped for Firebase-backed repositories.
 */
public final class AppDatabase {

    private static final Path DATABASE_PATH = Paths.get("data", "coffee-cafe.db");
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static volatile boolean initialized;

    private AppDatabase() {
    }

    public static Connection openConnection() throws SQLException {
        ensureInitialized();
        return DriverManager.getConnection(JDBC_PREFIX + DATABASE_PATH.toAbsolutePath());
    }

    /** Called before overwriting the database file during a restore. */
    public static synchronized void resetForRestore() {
        initialized = false;
    }

    /**
     * Production reset — wipes all operational/test data while keeping
     * reference data (menu, inventory definitions) and resetting users to
     * the seeded defaults only.
     *
     * Tables wiped:
     *   staff_shifts, sales_records, sales_transactions,
     *   sales_transaction_items, sales_order_status,
     *   password_reset_requests, inventory_batches, users
     *
     * Tables kept:
     *   menu_items, menu_item_ingredients,
     *   inventory_items (definitions only, quantities zeroed)
     */
    public static void resetForProduction() throws SQLException {
        try (Connection conn = openConnection();
             java.sql.Statement st = conn.createStatement()) {
            st.execute("DELETE FROM staff_shifts");
            st.execute("DELETE FROM sales_transaction_items");
            st.execute("DELETE FROM sales_order_status");
            st.execute("DELETE FROM sales_transactions");
            st.execute("DELETE FROM sales_records");
            st.execute("DELETE FROM password_reset_requests");
            st.execute("DELETE FROM inventory_batches");
            st.execute("DELETE FROM users");
            // Reset SQLite auto-increment counters
            st.execute("DELETE FROM sqlite_sequence WHERE name IN ("
                    + "'staff_shifts','sales_transaction_items','sales_order_status',"
                    + "'sales_transactions','sales_records','password_reset_requests',"
                    + "'inventory_batches','users')");
        }
        // Re-seed default admin + staff accounts immediately
        try {
            persistence.Phase2Bootstrap.seedCatalogIfEmpty();
        } catch (Exception e) {
            throw new SQLException("Reset succeeded but re-seeding accounts failed: " + e.getMessage(), e);
        }
    }

    public static synchronized void ensureInitialized() throws SQLException {
        if (initialized) {
            return;
        }

        try {
            Path parent = DATABASE_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new SQLException("Unable to create database directory", e);
        }

        try (Connection connection = openRawConnection()) {
            SchemaInitializer.initialize(connection);
            consolidateInventoryItemNames(connection);
        }
        initialized = true;
    }

    private static Connection openRawConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_PREFIX + DATABASE_PATH.toAbsolutePath());
    }

    private static void consolidateInventoryItemNames(Connection connection) throws SQLException {
        class InventoryRow {
            final long id;
            String name;
            double quantity;
            String unit;
            double alertLevel;
            String storageLocation;
            String lastUpdated;

            InventoryRow(long id, String name, double quantity, String unit, double alertLevel, String storageLocation, String lastUpdated) {
                this.id = id;
                this.name = name;
                this.quantity = quantity;
                this.unit = unit;
                this.alertLevel = alertLevel;
                this.storageLocation = storageLocation;
                this.lastUpdated = lastUpdated;
            }
        }

        Map<String, List<InventoryRow>> groups = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, quantity, unit, alert_level, storage_location, last_updated FROM inventory_items ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                InventoryRow row = new InventoryRow(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level"),
                        resultSet.getString("storage_location"),
                        resultSet.getString("last_updated"));
                String key = StringUtil.normalizeName(row.name);
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }

        for (Map.Entry<String, List<InventoryRow>> entry : groups.entrySet()) {
            String normalizedName = entry.getKey();
            List<InventoryRow> rows = entry.getValue();
            if (rows.isEmpty()) {
                continue;
            }

            InventoryRow canonical = rows.get(0);
            double totalQuantity = 0.0;
            String unit = canonical.unit == null ? "" : canonical.unit;
            double alertLevel = canonical.alertLevel;
            String storageLocation = canonical.storageLocation == null ? "" : canonical.storageLocation;
            String lastUpdated = canonical.lastUpdated == null ? "" : canonical.lastUpdated;

            for (InventoryRow row : rows) {
                totalQuantity += row.quantity;
                if ((unit == null || unit.isBlank()) && row.unit != null && !row.unit.isBlank()) {
                    unit = row.unit;
                }
                if ((storageLocation == null || storageLocation.isBlank()) && row.storageLocation != null && !row.storageLocation.isBlank()) {
                    storageLocation = row.storageLocation;
                }
                if ((lastUpdated == null || lastUpdated.isBlank()) && row.lastUpdated != null && !row.lastUpdated.isBlank()) {
                    lastUpdated = row.lastUpdated;
                }
            }

            for (int i = 1; i < rows.size(); i++) {
                InventoryRow duplicate = rows.get(i);
                try (PreparedStatement moveBatches = connection.prepareStatement(
                        "UPDATE inventory_batches SET inventory_item_id = ? WHERE inventory_item_id = ?")) {
                    moveBatches.setLong(1, canonical.id);
                    moveBatches.setLong(2, duplicate.id);
                    moveBatches.executeUpdate();
                }
                try (PreparedStatement deleteDuplicate = connection.prepareStatement(
                        "DELETE FROM inventory_items WHERE id = ?")) {
                    deleteDuplicate.setLong(1, duplicate.id);
                    deleteDuplicate.executeUpdate();
                }
            }

            try (PreparedStatement updateCanonical = connection.prepareStatement(
                    "UPDATE inventory_items SET name = ?, quantity = ?, unit = ?, alert_level = ?, storage_location = ?, last_updated = ? WHERE id = ?")) {
                updateCanonical.setString(1, normalizedName);
                updateCanonical.setDouble(2, totalQuantity);
                updateCanonical.setString(3, unit == null ? "" : unit);
                updateCanonical.setDouble(4, alertLevel);
                updateCanonical.setString(5, storageLocation == null ? "" : storageLocation);
                updateCanonical.setString(6, lastUpdated == null ? "" : lastUpdated);
                updateCanonical.setLong(7, canonical.id);
                updateCanonical.executeUpdate();
            }
        }
    }
}
