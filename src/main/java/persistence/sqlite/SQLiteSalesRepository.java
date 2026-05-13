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
    public void saveAll(String transactionRef, List<SalesRecord> records, double subtotal, double tax, double total, double cash, double changeAmount) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long transactionId;
                try (PreparedStatement transaction = connection.prepareStatement(
                        "INSERT INTO sales_transactions(transaction_ref, subtotal, tax, total, cash, change_amount) VALUES (?, ?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    transaction.setString(1, transactionRef);
                    transaction.setDouble(2, subtotal);
                    transaction.setDouble(3, tax);
                    transaction.setDouble(4, total);
                    transaction.setDouble(5, cash);
                    transaction.setDouble(6, changeAmount);
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
}
