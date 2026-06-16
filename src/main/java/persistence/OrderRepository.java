package persistence;

import ui.OrderQueuePanel;
import java.util.List;

/**
 * Repository interface for kitchen order persistence.
 * Follows the same pattern as SalesRepository, MenuRepository, etc.
 */
public interface OrderRepository {

    /**
     * Persist a new order when the POS confirms a transaction.
     * Called once per order — status starts as "PENDING".
     */
    void saveOrder(OrderQueuePanel.Receipt receipt);

    /**
     * Update the kitchen status of an existing order.
     *
     * @param orderId    the POS order ID
     * @param statusCode one of OrderQueuePanel.STATUS_* constants
     *                   (1=PREPARING, 2=READY, 3=COMPLETED, 4=CANCELLED)
     */
    void updateOrderStatus(int orderId, int statusCode);

    /**
     * Load all orders that are not yet completed or cancelled.
     * Used on startup / reconnect so every screen sees the live queue.
     */
    List<OrderQueuePanel.Receipt> loadActiveOrders();

    /**
     * Load every order regardless of status (for history / completed dialog).
     */
    List<OrderQueuePanel.Receipt> loadAllOrders();

    /**
     * Return the integer status code currently stored for this order,
     * or -1 if not found.
     */
    int getOrderStatus(int orderId);
}
