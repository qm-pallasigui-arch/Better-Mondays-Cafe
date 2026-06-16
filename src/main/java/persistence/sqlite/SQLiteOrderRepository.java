package persistence.sqlite;

import persistence.AppDatabase;
import persistence.OrderRepository;
import ui.OrderQueuePanel;
import ui.OrderQueuePanel.Receipt;
import ui.OrderQueuePanel.ReceiptItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed implementation of OrderRepository.
 *
 * Two tables are created inside the existing AppDatabase file:
 *
 *   kitchen_orders        — one row per order (header)
 *   kitchen_order_items   — one row per line item
 *
 * Status codes stored in `status_code` column:
 *   0 = PENDING   1 = PREPARING   2 = READY
 *   3 = COMPLETED 4 = CANCELLED
 */
public class SQLiteOrderRepository implements OrderRepository {

    // ── status code constants (mirror OrderQueuePanel.STATUS_*) ──────────
    public static final int STATUS_PENDING   = 0;
    public static final int STATUS_PREPARING = 1;   // OrderQueuePanel.STATUS_PREPARING
    public static final int STATUS_READY     = 2;   // OrderQueuePanel.STATUS_READY
    public static final int STATUS_COMPLETED = 3;   // OrderQueuePanel.STATUS_COMPLETED
    public static final int STATUS_CANCELLED = 4;   // OrderQueuePanel.STATUS_CANCELLED

    // ── DDL ──────────────────────────────────────────────────────────────

    private static final String CREATE_ORDERS = """
            CREATE TABLE IF NOT EXISTS kitchen_orders (
                order_id        INTEGER PRIMARY KEY,
                customer_name   TEXT    NOT NULL,
                timestamp       TEXT    NOT NULL,
                subtotal        REAL    NOT NULL DEFAULT 0,
                vat             REAL    NOT NULL DEFAULT 0,
                total_inclusive REAL    NOT NULL DEFAULT 0,
                cash            REAL    NOT NULL DEFAULT 0,
                change_amt      REAL    NOT NULL DEFAULT 0,
                discount_type   TEXT    NOT NULL DEFAULT '',
                status_code     INTEGER NOT NULL DEFAULT 0,
                created_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_ITEMS = """
            CREATE TABLE IF NOT EXISTS kitchen_order_items (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id    INTEGER NOT NULL,
                description TEXT    NOT NULL,
                quantity    INTEGER NOT NULL,
                unit_price  REAL    NOT NULL,
                line_total  REAL    NOT NULL,
                FOREIGN KEY (order_id) REFERENCES kitchen_orders(order_id)
            )
            """;

    // ── Constructor — ensures tables exist ───────────────────────────────

    public SQLiteOrderRepository() {
        initSchema();
    }

    private void initSchema() {
        try (Connection conn = AppDatabase.openConnection();
             Statement st = conn.createStatement()) {
            st.execute(CREATE_ORDERS);
            st.execute(CREATE_ITEMS);
        } catch (Exception e) {
            System.err.println("[SQLiteOrderRepository] Schema init failed: " + e.getMessage());
        }
    }

    // ── OrderRepository implementation ───────────────────────────────────

    @Override
    public void saveOrder(Receipt receipt) {
        String insertOrder = """
                INSERT OR IGNORE INTO kitchen_orders
                    (order_id, customer_name, timestamp,
                     subtotal, vat, total_inclusive,
                     cash, change_amt, discount_type, status_code)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """;

        String insertItem = """
                INSERT INTO kitchen_order_items
                    (order_id, description, quantity, unit_price, line_total)
                VALUES (?,?,?,?,?)
                """;

        try (Connection conn = AppDatabase.openConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertOrder)) {
                ps.setInt   (1, receipt.orderId);
                ps.setString(2, receipt.customerName);
                ps.setString(3, receipt.timestamp);
                ps.setDouble(4, receipt.subtotal);
                ps.setDouble(5, receipt.vat);
                ps.setDouble(6, receipt.totalInclusive);
                ps.setDouble(7, receipt.cash);
                ps.setDouble(8, receipt.change);
                ps.setString(9, receipt.discountType == null ? "" : receipt.discountType);
                ps.setInt   (10, STATUS_PENDING);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                for (ReceiptItem item : receipt.items) {
                    ps.setInt   (1, receipt.orderId);
                    ps.setString(2, item.description);
                    ps.setInt   (3, item.quantity);
                    ps.setDouble(4, item.unitPrice);
                    ps.setDouble(5, item.lineTotal);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

        } catch (Exception e) {
            System.err.println("[SQLiteOrderRepository] saveOrder failed: " + e.getMessage());
        }
    }

    @Override
    public void updateOrderStatus(int orderId, int statusCode) {
        String sql = "UPDATE kitchen_orders SET status_code = ? WHERE order_id = ?";
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statusCode);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SQLiteOrderRepository] updateOrderStatus failed: " + e.getMessage());
        }
    }

    @Override
    public List<Receipt> loadActiveOrders() {
        // Active = not COMPLETED (3) and not CANCELLED (4)
        String sql = """
                SELECT order_id FROM kitchen_orders
                WHERE status_code < 3
                ORDER BY order_id ASC
                """;
        return loadByQuery(sql);
    }

    @Override
    public List<Receipt> loadAllOrders() {
        String sql = "SELECT order_id FROM kitchen_orders ORDER BY order_id ASC";
        return loadByQuery(sql);
    }

    @Override
    public int getOrderStatus(int orderId) {
        String sql = "SELECT status_code FROM kitchen_orders WHERE order_id = ?";
        try (Connection conn = AppDatabase.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("status_code");
            }
        } catch (Exception e) {
            System.err.println("[SQLiteOrderRepository] getOrderStatus failed: " + e.getMessage());
        }
        return -1;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Runs a query that returns a list of order_id values, then hydrates
     * each one into a full Receipt (header + items).
     */
    private List<Receipt> loadByQuery(String orderIdQuery) {
        List<Receipt> result = new ArrayList<>();
        try (Connection conn = AppDatabase.openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(orderIdQuery)) {

            while (rs.next()) {
                int id = rs.getInt("order_id");
                Receipt r = loadSingleReceipt(conn, id);
                if (r != null) result.add(r);
            }
        } catch (Exception e) {
            System.err.println("[SQLiteOrderRepository] loadByQuery failed: " + e.getMessage());
        }
        return result;
    }

    private Receipt loadSingleReceipt(Connection conn, int orderId) throws SQLException {
        String hdr = "SELECT * FROM kitchen_orders WHERE order_id = ?";
        String itm = "SELECT * FROM kitchen_order_items WHERE order_id = ? ORDER BY id ASC";

        try (PreparedStatement ps = conn.prepareStatement(hdr)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String customerName   = rs.getString("customer_name");
                String timestamp      = rs.getString("timestamp");
                double subtotal       = rs.getDouble("subtotal");
                double vat            = rs.getDouble("vat");
                double totalInclusive = rs.getDouble("total_inclusive");
                double cash           = rs.getDouble("cash");
                double change         = rs.getDouble("change_amt");
                String discountType   = rs.getString("discount_type");

                List<ReceiptItem> items = new ArrayList<>();
                try (PreparedStatement pi = conn.prepareStatement(itm)) {
                    pi.setInt(1, orderId);
                    try (ResultSet ri = pi.executeQuery()) {
                        while (ri.next()) {
                            items.add(new ReceiptItem(
                                    ri.getString("description"),
                                    ri.getInt   ("quantity"),
                                    ri.getDouble("unit_price"),
                                    ri.getDouble("line_total")));
                        }
                    }
                }

                return new Receipt(orderId, customerName, items,
                        timestamp, subtotal, vat, totalInclusive,
                        cash, change, discountType);
            }
        }
    }
}
