package persistence;

import java.util.List;
import java.util.Optional;
import inventory.InventoryItem;
import inventory.InventoryBatch;

public interface InventoryRepository {

    List<InventoryItem> findAll() throws Exception;

    Optional<InventoryItem> findByName(String name) throws Exception;

    void save(InventoryItem item) throws Exception;

    void delete(String name) throws Exception;

    // Batch-level operations for product registration and FEFO handling
    List<InventoryBatch> findBatchesForItem(String itemName) throws Exception;

    void addBatch(String itemName, InventoryBatch batch) throws Exception;

    // Deduct inventory using FEFO (first-expire-first-out) behavior
    void deductFEFO(String itemName, double amount) throws Exception;

    void deleteBatch(long batchId) throws Exception;

    void archiveBatch(long batchId) throws Exception;

    void deductFromBatch(long batchId, double amount) throws Exception;
}
