package persistence;

import java.util.List;
import java.util.Optional;
import inventory.InventoryItem;

public interface InventoryRepository {

    List<InventoryItem> findAll() throws Exception;

    Optional<InventoryItem> findByName(String name) throws Exception;

    void save(InventoryItem item) throws Exception;

    void delete(String name) throws Exception;
}
