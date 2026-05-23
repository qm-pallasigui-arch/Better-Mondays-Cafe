package inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import persistence.AppDatabase;
import persistence.sqlite.SQLiteInventoryRepository;
import util.StringUtil;

class InventoryDeductionEdgeCasesTest {

    @Test
    void deductZeroOrNegativeIsNoOp() throws Exception {
        SQLiteInventoryRepository repo = new SQLiteInventoryRepository();
        String itemName = "EdgeZero-" + System.nanoTime();
        cleanup(itemName);

        // add a batch
        InventoryBatch b = new InventoryBatch("S1", 5.0, LocalDate.now().plusDays(5).toString());
        repo.addBatch(itemName, b);
        double before = repo.findByName(itemName).get().getQuantity();

        repo.deductFEFO(itemName, 0.0);
        repo.deductFEFO(itemName, -3.0);

        double after = repo.findByName(itemName).get().getQuantity();
        assertEquals(before, after, 1e-9, "No-op for zero/negative deduction");
        cleanup(itemName);
    }

    @Test
    void deductOnMissingItemThrows() {
        SQLiteInventoryRepository repo = new SQLiteInventoryRepository();
        String itemName = "NonExistent-" + System.nanoTime();
        assertThrows(IllegalStateException.class, () -> repo.deductFEFO(itemName, 1.0));
    }

    @Test
    void exactBatchBoundaryConsumesOnlyFirstBatch() throws Exception {
        SQLiteInventoryRepository repo = new SQLiteInventoryRepository();
        String itemName = "Boundary-" + System.nanoTime();
        cleanup(itemName);

        InventoryBatch b1 = new InventoryBatch("A", 5.0, LocalDate.now().plusDays(1).toString());
        InventoryBatch b2 = new InventoryBatch("B", 5.0, LocalDate.now().plusDays(10).toString());
        repo.addBatch(itemName, b1);
        repo.addBatch(itemName, b2);

        repo.deductFEFO(itemName, 5.0); // should zero out first batch
        List<InventoryBatch> after = repo.findBatchesForItem(itemName);
        double sum = after.stream().mapToDouble(InventoryBatch::getQuantity).sum();
        assertEquals(5.0, sum, 1e-6);
        assertTrue(after.stream().anyMatch(b -> b.getSku().equals("A") && b.getQuantity() == 0.0));
        cleanup(itemName);
    }

    @Test
    void overDeductionConsumesAllAndLeavesZero() throws Exception {
        SQLiteInventoryRepository repo = new SQLiteInventoryRepository();
        String itemName = "OverDeduct-" + System.nanoTime();
        cleanup(itemName);

        InventoryBatch b1 = new InventoryBatch("X", 2.0, LocalDate.now().plusDays(1).toString());
        InventoryBatch b2 = new InventoryBatch("Y", 3.0, LocalDate.now().plusDays(2).toString());
        repo.addBatch(itemName, b1);
        repo.addBatch(itemName, b2);

        repo.deductFEFO(itemName, 10.0); // greater than total(5.0)
        List<InventoryBatch> after = repo.findBatchesForItem(itemName);
        double sum = after.stream().mapToDouble(InventoryBatch::getQuantity).sum();
        assertEquals(0.0, sum, 1e-6);

        assertEquals(0.0, repo.findByName(itemName).get().getQuantity(), 1e-6);
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
