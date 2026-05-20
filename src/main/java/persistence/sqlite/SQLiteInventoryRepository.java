package persistence.sqlite;

import inventory.InventoryItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import inventory.InventoryBatch;
import persistence.AppDatabase;
import persistence.InventoryRepository;

public class SQLiteInventoryRepository implements InventoryRepository {

    @Override
    public List<InventoryItem> findAll() throws Exception {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                      "SELECT name, quantity, unit, alert_level, storage_location, last_updated FROM inventory_items ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(new InventoryItem(
                        resultSet.getString("name"),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level"),
                        resultSet.getString("storage_location"),
                        resultSet.getString("last_updated")));
            }
        }
        return items;
    }

    @Override
    public Optional<InventoryItem> findByName(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, quantity, unit, alert_level, storage_location, last_updated FROM inventory_items WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InventoryItem(
                        resultSet.getString("name"),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level"),
                        resultSet.getString("storage_location"),
                        resultSet.getString("last_updated")));
            }
        }
    }

    @Override
    public void save(InventoryItem item) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO inventory_items(name, quantity, unit, alert_level, storage_location, last_updated) VALUES (?, ?, ?, ?, ?, ?) "
                     + "ON CONFLICT(name) DO UPDATE SET quantity = excluded.quantity, unit = excluded.unit, alert_level = excluded.alert_level, storage_location = excluded.storage_location, last_updated = excluded.last_updated")) {
            statement.setString(1, item.getName());
            statement.setDouble(2, item.getQuantity());
            statement.setString(3, item.getUnit());
            statement.setDouble(4, item.getAlertLevel());
            statement.setString(5, item.getStorageLocation());
            statement.setString(6, item.getLastUpdated());
            statement.executeUpdate();
        }
    }

    @Override
    public List<InventoryBatch> findBatchesForItem(String itemName) throws Exception {
        List<InventoryBatch> batches = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection()) {
            // find item id
            try (PreparedStatement findItem = connection.prepareStatement("SELECT id FROM inventory_items WHERE name = ?")) {
                findItem.setString(1, itemName);
                try (ResultSet rs = findItem.executeQuery()) {
                    if (!rs.next()) return batches;
                    long itemId = rs.getLong("id");
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "SELECT id, sku, quantity, expiry_date FROM inventory_batches WHERE inventory_item_id = ? ORDER BY expiry_date ASC NULLS LAST, id")) {
                        stmt.setLong(1, itemId);
                        try (ResultSet r2 = stmt.executeQuery()) {
                            while (r2.next()) {
                                batches.add(new InventoryBatch(
                                        r2.getLong("id"),
                                        r2.getString("sku"),
                                        r2.getDouble("quantity"),
                                        r2.getString("expiry_date")
                                ));
                            }
                        }
                    }
                }
            }
        }
        return batches;
    }

    @Override
    public void addBatch(String itemName, InventoryBatch batch) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long itemId;
                try (PreparedStatement findItem = connection.prepareStatement("SELECT id, quantity FROM inventory_items WHERE name = ?")) {
                    findItem.setString(1, itemName);
                    try (ResultSet rs = findItem.executeQuery()) {
                        if (!rs.next()) {
                            // create base inventory item with quantity 0
                            try (PreparedStatement create = connection.prepareStatement(
                                    "INSERT INTO inventory_items(name, quantity, unit, alert_level, storage_location, last_updated) VALUES (?, 0, '', 0, '', '')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                                create.setString(1, itemName);
                                create.executeUpdate();
                                try (ResultSet keys = create.getGeneratedKeys()) {
                                    if (!keys.next()) throw new IllegalStateException("Unable to create inventory item");
                                    itemId = keys.getLong(1);
                                }
                            }
                        } else {
                            itemId = rs.getLong("id");
                        }
                    }
                }

                try (PreparedStatement insertBatch = connection.prepareStatement(
                        "INSERT INTO inventory_batches(inventory_item_id, sku, quantity, expiry_date) VALUES (?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertBatch.setLong(1, itemId);
                    insertBatch.setString(2, batch.getSku());
                    insertBatch.setDouble(3, batch.getQuantity());
                    insertBatch.setString(4, batch.getExpiryDate());
                    insertBatch.executeUpdate();
                }

                // update aggregated quantity on inventory_items
                try (PreparedStatement sumStmt = connection.prepareStatement(
                        "SELECT COALESCE(SUM(quantity),0) AS sumq FROM inventory_batches WHERE inventory_item_id = ?")) {
                    sumStmt.setLong(1, itemId);
                    try (ResultSet r = sumStmt.executeQuery()) {
                        double total = r.next() ? r.getDouble("sumq") : 0.0;
                        try (PreparedStatement upd = connection.prepareStatement(
                                "UPDATE inventory_items SET quantity = ? WHERE id = ?")) {
                            upd.setDouble(1, total);
                            upd.setLong(2, itemId);
                            upd.executeUpdate();
                        }
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void deductFEFO(String itemName, double amount) throws Exception {
        if (amount <= 0) return;
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long itemId;
                try (PreparedStatement findItem = connection.prepareStatement("SELECT id FROM inventory_items WHERE name = ?")) {
                    findItem.setString(1, itemName);
                    try (ResultSet rs = findItem.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Inventory item not found: " + itemName);
                        itemId = rs.getLong("id");
                    }
                }

                try (PreparedStatement selectBatches = connection.prepareStatement(
                        "SELECT id, quantity FROM inventory_batches WHERE inventory_item_id = ? ORDER BY expiry_date ASC NULLS LAST, id")) {
                    selectBatches.setLong(1, itemId);
                    try (ResultSet rs = selectBatches.executeQuery()) {
                        while (rs.next() && amount > 0) {
                            long batchId = rs.getLong("id");
                            double q = rs.getDouble("quantity");
                            double deduct = Math.min(q, amount);
                            double remaining = q - deduct;
                            try (PreparedStatement upd = connection.prepareStatement(
                                    "UPDATE inventory_batches SET quantity = ? WHERE id = ?")) {
                                upd.setDouble(1, remaining);
                                upd.setLong(2, batchId);
                                upd.executeUpdate();
                            }
                            amount -= deduct;
                        }
                    }
                }

                // recompute total
                try (PreparedStatement sumStmt = connection.prepareStatement(
                        "SELECT COALESCE(SUM(quantity),0) AS sumq FROM inventory_batches WHERE inventory_item_id = ?")) {
                    sumStmt.setLong(1, itemId);
                    try (ResultSet r = sumStmt.executeQuery()) {
                        double total = r.next() ? r.getDouble("sumq") : 0.0;
                        try (PreparedStatement upd = connection.prepareStatement(
                                "UPDATE inventory_items SET quantity = ? WHERE id = ?")) {
                            upd.setDouble(1, total);
                            upd.setLong(2, itemId);
                            upd.executeUpdate();
                        }
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public void delete(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM inventory_items WHERE name = ?")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }
}
