package persistence.sqlite;

import inventory.InventoryItem;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import inventory.InventoryBatch;
import persistence.AppDatabase;
import persistence.InventoryRepository;
import util.StringUtil;

public class SQLiteInventoryRepository implements InventoryRepository {

    private static final Logger LOGGER = Logger.getLogger(SQLiteInventoryRepository.class.getName());

    @Override
    public List<InventoryItem> findAll() throws Exception {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                      "SELECT name, quantity, unit, alert_level, storage_location, last_updated FROM inventory_items ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            Map<String, InventoryItem> deduped = new LinkedHashMap<>();
            while (resultSet.next()) {
                InventoryItem current = new InventoryItem(
                        StringUtil.normalizeName(resultSet.getString("name")),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level"),
                        resultSet.getString("storage_location"),
                        resultSet.getString("last_updated"));
                String key = StringUtil.normalizeName(current.getName());
                InventoryItem existing = deduped.get(key);
                if (existing == null) {
                    deduped.put(key, current);
                } else {
                    existing.setQuantity(existing.getQuantity() + current.getQuantity());
                    if (existing.getUnit() == null || existing.getUnit().isBlank()) existing.setUnit(current.getUnit());
                    if (existing.getStorageLocation() == null || existing.getStorageLocation().isBlank()) existing.setStorageLocation(current.getStorageLocation());
                    if (existing.getLastUpdated() == null || existing.getLastUpdated().isBlank()) existing.setLastUpdated(current.getLastUpdated());
                    existing.setAlertLevel(Math.max(existing.getAlertLevel(), current.getAlertLevel()));
                }
            }
            items.addAll(deduped.values());
        }
        return items;
    }

    @Override
    public Optional<InventoryItem> findByName(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, quantity, unit, alert_level, storage_location, last_updated FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id")) {
            statement.setString(1, StringUtil.normalizeName(name));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                InventoryItem item = new InventoryItem(
                        StringUtil.normalizeName(resultSet.getString("name")),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level"),
                        resultSet.getString("storage_location"),
                        resultSet.getString("last_updated"));
                while (resultSet.next()) {
                    item.setQuantity(item.getQuantity() + resultSet.getDouble("quantity"));
                    if (item.getUnit() == null || item.getUnit().isBlank()) item.setUnit(resultSet.getString("unit"));
                    if (item.getStorageLocation() == null || item.getStorageLocation().isBlank()) item.setStorageLocation(resultSet.getString("storage_location"));
                    if (item.getLastUpdated() == null || item.getLastUpdated().isBlank()) item.setLastUpdated(resultSet.getString("last_updated"));
                    item.setAlertLevel(Math.max(item.getAlertLevel(), resultSet.getDouble("alert_level")));
                }
                return Optional.of(item);
            }
        }
    }

    @Override
    public void save(InventoryItem item) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement find = connection.prepareStatement(
                     "SELECT id FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id")) {
            String normalized = StringUtil.normalizeName(item.getName());
            item.setName(normalized);
            find.setString(1, normalized);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE inventory_items SET name = ?, quantity = ?, unit = ?, alert_level = ?, storage_location = ?, last_updated = ? WHERE id = ?")) {
                        update.setString(1, normalized);
                        update.setDouble(2, item.getQuantity());
                        update.setString(3, item.getUnit());
                        update.setDouble(4, item.getAlertLevel());
                        update.setString(5, item.getStorageLocation());
                        update.setString(6, item.getLastUpdated());
                        update.setLong(7, rs.getLong("id"));
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO inventory_items(name, quantity, unit, alert_level, storage_location, last_updated) VALUES (?, ?, ?, ?, ?, ?)")) {
                        statement.setString(1, normalized);
                        statement.setDouble(2, item.getQuantity());
                        statement.setString(3, item.getUnit());
                        statement.setDouble(4, item.getAlertLevel());
                        statement.setString(5, item.getStorageLocation());
                        statement.setString(6, item.getLastUpdated());
                        statement.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public List<InventoryBatch> findBatchesForItem(String itemName) throws Exception {
        List<InventoryBatch> batches = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection()) {
            // find item id
            try (PreparedStatement findItem = connection.prepareStatement("SELECT id FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id")) {
                findItem.setString(1, StringUtil.normalizeName(itemName));
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
                String normalized = StringUtil.normalizeName(itemName);
                try (PreparedStatement findItem = connection.prepareStatement("SELECT id, name, quantity FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id")) {
                    findItem.setString(1, normalized);
                    try (ResultSet rs = findItem.executeQuery()) {
                        if (!rs.next()) {
                            // create base inventory item with quantity 0
                            try (PreparedStatement create = connection.prepareStatement(
                                    "INSERT INTO inventory_items(name, quantity, unit, alert_level, storage_location, last_updated) VALUES (?, 0, '', 0, '', '')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                                create.setString(1, normalized);
                                create.executeUpdate();
                                try (ResultSet keys = create.getGeneratedKeys()) {
                                    if (!keys.next()) throw new IllegalStateException("Unable to create inventory item");
                                    itemId = keys.getLong(1);
                                }
                            }
                        } else {
                            itemId = rs.getLong("id");
                            try (PreparedStatement rename = connection.prepareStatement("UPDATE inventory_items SET name = ? WHERE id = ?")) {
                                rename.setString(1, normalized);
                                rename.setLong(2, itemId);
                                rename.executeUpdate();
                            }
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
        String normalized = StringUtil.normalizeName(itemName);
        LOGGER.log(Level.INFO, "deductFEFO called for {0} amount={1}", new Object[]{normalized, amount});
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long itemId;
                try (PreparedStatement findItem = connection.prepareStatement("SELECT id FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) ORDER BY id")) {
                    findItem.setString(1, normalized);
                    try (ResultSet rs = findItem.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Inventory item not found: " + normalized);
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
                            LOGGER.log(Level.FINE, "Batch {0} currentQty={1}", new Object[]{batchId, q});
                            double deduct = Math.min(q, amount);
                            double remaining = q - deduct;
                            try (PreparedStatement upd = connection.prepareStatement(
                                    "UPDATE inventory_batches SET quantity = ? WHERE id = ?")) {
                                upd.setDouble(1, remaining);
                                upd.setLong(2, batchId);
                                upd.executeUpdate();
                            }
                            amount -= deduct;
                            LOGGER.log(Level.FINE, "Deducted {0} from batch {1}, remainingToDeduct={2}", new Object[]{deduct, batchId, amount});
                        }
                    }
                }

                // recompute total
                try (PreparedStatement sumStmt = connection.prepareStatement(
                        "SELECT COALESCE(SUM(quantity),0) AS sumq FROM inventory_batches WHERE inventory_item_id = ?")) {
                    sumStmt.setLong(1, itemId);
                    try (ResultSet r = sumStmt.executeQuery()) {
                        double total = r.next() ? r.getDouble("sumq") : 0.0;
                        LOGGER.log(Level.INFO, "After deduction, total for itemId {0} is {1}", new Object[]{itemId, total});
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
             PreparedStatement statement = connection.prepareStatement("DELETE FROM inventory_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))")) {
            statement.setString(1, StringUtil.normalizeName(name));
            statement.executeUpdate();
        }
    }
}
