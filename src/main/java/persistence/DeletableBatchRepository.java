package persistence;

/**
 * Optional capability interface for repositories that support batch deletion.
 *
 * Implement this alongside {@link InventoryRepository} in your SQLite
 * repository if you want the delete button in {@code InventoryRegistrationPanel}
 * to also remove the record from the database.
 *
 * Example:
 * <pre>
 *   public class SQLiteInventoryRepository
 *           implements InventoryRepository, DeletableBatchRepository {
 *
 *       {@literal @}Override
 *       public void deleteBatch(String batchId) {
 *           String sql = "DELETE FROM inventory_batches WHERE id = ?";
 *           try (Connection conn = AppDatabase.connect();
 *                PreparedStatement ps = conn.prepareStatement(sql)) {
 *               ps.setString(1, batchId);
 *               ps.executeUpdate();
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Failed to delete batch: " + batchId, e);
 *           }
 *       }
 *   }
 * </pre>
 *
 * If your repository does not implement this interface the delete button still
 * works — it removes the entry from the in-memory cache and the JSON fallback
 * file; only the SQLite row is left untouched.
 */
public interface DeletableBatchRepository {

    /**
     * Permanently removes the inventory batch with the given ID.
     *
     * @param batchId the string representation of the batch primary key
     *                (as stored in the "Batch ID" column of the UI table)
     * @throws RuntimeException (or a subclass) if the deletion fails
     */
    void deleteBatch(String batchId);
}
