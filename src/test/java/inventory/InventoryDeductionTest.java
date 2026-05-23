package inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import persistence.AppDatabase;
import persistence.sqlite.SQLiteInventoryRepository;
import util.StringUtil;

class InventoryDeductionTest {

    @Test
    void fefoDeductionConsumesEarliestExpiryFirstAndUpdatesAggregate() throws Exception {
        SQLiteInventoryRepository repo = new SQLiteInventoryRepository();
        String itemName = "TestIngredient-" + System.nanoTime();
        cleanup(itemName);

        String expSoon = LocalDate.now().plusDays(1).toString();
        String expLater = LocalDate.now().plusDays(10).toString();

        InventoryBatch b1 = new InventoryBatch("SKU1", 5.0, expSoon);
        InventoryBatch b2 = new InventoryBatch("SKU2", 10.0, expLater);

        repo.addBatch(itemName, b1);
        repo.addBatch(itemName, b2);

        List<InventoryBatch> before = repo.findBatchesForItem(itemName);
        assertEquals(2, before.size(), "Two batches should exist after addBatch");

        // Deduct an amount that partially consumes the first (earliest expiry) batch
        repo.deductFEFO(itemName, 3.0);

        List<InventoryBatch> after = repo.findBatchesForItem(itemName);
        assertEquals(2, after.size(), "Still two batches after partial deduction");

        // Find batch by sku
        Optional<InventoryBatch> nb1 = after.stream().filter(b -> "SKU1".equals(b.getSku())).findFirst();
        Optional<InventoryBatch> nb2 = after.stream().filter(b -> "SKU2".equals(b.getSku())).findFirst();
        assertTrue(nb1.isPresent());
        assertTrue(nb2.isPresent());

        // b1 should have 2.0 remaining, b2 unchanged
        assertEquals(2.0, nb1.get().getQuantity(), 1e-6);
        assertEquals(10.0, nb2.get().getQuantity(), 1e-6);

        // Aggregate quantity in inventory_items should be sum(2.0 + 10.0) = 12.0
        Optional<inventory.InventoryItem> agg = repo.findByName(itemName);
        assertTrue(agg.isPresent(), "Inventory item row should exist");
        assertEquals(12.0, agg.get().getQuantity(), 1e-6);

        // Now deduct more than remaining in first batch to force consuming it and reducing second
        repo.deductFEFO(itemName, 4.0); // consumes 2 from b1 and 2 from b2
        List<InventoryBatch> after2 = repo.findBatchesForItem(itemName);
        Optional<InventoryBatch> nb1b = after2.stream().filter(b -> "SKU1".equals(b.getSku())).findFirst();
        Optional<InventoryBatch> nb2b = after2.stream().filter(b -> "SKU2".equals(b.getSku())).findFirst();
        assertTrue(nb1b.isPresent());
        assertTrue(nb2b.isPresent());
        assertEquals(0.0, nb1b.get().getQuantity(), 1e-6);
        assertEquals(8.0, nb2b.get().getQuantity(), 1e-6);

        Optional<inventory.InventoryItem> agg2 = repo.findByName(itemName);
        assertTrue(agg2.isPresent());
        assertEquals(8.0, agg2.get().getQuantity(), 1e-6);

        cleanup(itemName);
    }

    private void cleanup(String itemName) throws Exception {
        String normalizedName = StringUtil.normalizeName(itemName);
        try (Connection connection = AppDatabase.openConnection()) {
            try (PreparedStatement deleteBatches = connection.prepareStatement(
                    "DELETE FROM inventory_batches WHERE inventory_item_id IN (SELECT id FROM inventory_items WHERE name = ?)")) {
                deleteBatches.setString(1, normalizedName);
                deleteBatches.executeUpdate();
            }
            try (PreparedStatement deleteItem = connection.prepareStatement(
                    "DELETE FROM inventory_items WHERE name = ?")) {
                deleteItem.setString(1, normalizedName);
                deleteItem.executeUpdate();
            }
        }
    }
}
