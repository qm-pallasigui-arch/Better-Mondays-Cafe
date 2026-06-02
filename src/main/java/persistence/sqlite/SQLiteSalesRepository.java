package persistence.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import monitoring.SalesRecord;
import persistence.AppDatabase;
import persistence.SalesRepository;

public class SQLiteSalesRepository implements SalesRepository {

    @Override
    public List<SalesRecord> findAll() throws Exception {
        List<SalesRecord> records = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT product_name, quantity, price, total FROM sales_records ORDER BY id DESC");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                records.add(new SalesRecord(
                        resultSet.getString("product_name"),
                        resultSet.getInt("quantity"),
                        resultSet.getDouble("price"),
                        resultSet.getDouble("total")));
            }
        }
        return records;
    }

    @Override
    public void saveAll(String transactionRef, List<SalesRecord> records, double subtotal, double tax, double total,
            double cash, double changeAmount, String customerName) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long transactionId;

                // ✅ FIXED: ON CONFLICT DO UPDATE prevents the SQLITE_CONSTRAINT_UNIQUE crash
                // when saveAll() is called more than once with the same transaction_ref.
                try (PreparedStatement transaction = connection.prepareStatement(
                        "INSERT INTO sales_transactions(transaction_ref, customer_name, subtotal, tax, total, cash, change_amount) VALUES (?, ?, ?, ?, ?, ?, ?) "
                                + "ON CONFLICT(transaction_ref) DO UPDATE SET "
                                + "  customer_name = excluded.customer_name, "
                                + "  subtotal      = excluded.subtotal, "
                                + "  tax           = excluded.tax, "
                                + "  total         = excluded.total, "
                                + "  cash          = excluded.cash, "
                                + "  change_amount = excluded.change_amount",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    transaction.setString(1, transactionRef);
                    transaction.setString(2, customerName);
                    transaction.setDouble(3, subtotal);
                    transaction.setDouble(4, tax);
                    transaction.setDouble(5, total);
                    transaction.setDouble(6, cash);
                    transaction.setDouble(7, changeAmount);
                    transaction.executeUpdate();
                    try (ResultSet keys = transaction.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new IllegalStateException("Unable to create transaction row");
                        }
                        transactionId = keys.getLong(1);
                    }
                }

                try (PreparedStatement itemStatement = connection.prepareStatement(
                        "INSERT INTO sales_transaction_items(transaction_id, product_name, quantity, price, total) VALUES (?, ?, ?, ?, ?)");
                        PreparedStatement recordStatement = connection.prepareStatement(
                                "INSERT INTO sales_records(product_name, quantity, price, total) VALUES (?, ?, ?, ?)")) {
                    for (SalesRecord record : records) {
                        itemStatement.setLong(1, transactionId);
                        itemStatement.setString(2, record.getProductName());
                        itemStatement.setInt(3, record.getQuantity());
                        itemStatement.setDouble(4, record.getPrice());
                        itemStatement.setDouble(5, record.getTotal());
                        itemStatement.addBatch();

                        recordStatement.setString(1, record.getProductName());
                        recordStatement.setInt(2, record.getQuantity());
                        recordStatement.setDouble(3, record.getPrice());
                        recordStatement.setDouble(4, record.getTotal());
                        recordStatement.addBatch();
                    }
                    itemStatement.executeBatch();
                    recordStatement.executeBatch();
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
    public void updateOrderStatus(String transactionRef, String status) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long transactionId;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT id FROM sales_transactions WHERE transaction_ref = ?")) {
                    find.setString(1, transactionRef);
                    try (ResultSet rs = find.executeQuery()) {
                        if (!rs.next())
                            throw new IllegalStateException("Transaction not found: " + transactionRef);
                        transactionId = rs.getLong("id");
                    }
                }

                try (PreparedStatement upsert = connection.prepareStatement(
                        "INSERT INTO sales_order_status(transaction_id, status) VALUES (?, ?) "
                                + "ON CONFLICT(transaction_id) DO UPDATE SET status = excluded.status, updated_at = CURRENT_TIMESTAMP")) {
                    upsert.setLong(1, transactionId);
                    upsert.setString(2, status);
                    upsert.executeUpdate();
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
}