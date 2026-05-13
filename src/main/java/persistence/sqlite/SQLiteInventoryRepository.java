package persistence.sqlite;

import inventory.InventoryItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import persistence.AppDatabase;
import persistence.InventoryRepository;

public class SQLiteInventoryRepository implements InventoryRepository {

    @Override
    public List<InventoryItem> findAll() throws Exception {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, quantity, unit, alert_level FROM inventory_items ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                items.add(new InventoryItem(
                        resultSet.getString("name"),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level")));
            }
        }
        return items;
    }

    @Override
    public Optional<InventoryItem> findByName(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, quantity, unit, alert_level FROM inventory_items WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new InventoryItem(
                        resultSet.getString("name"),
                        resultSet.getDouble("quantity"),
                        resultSet.getString("unit"),
                        resultSet.getDouble("alert_level")));
            }
        }
    }

    @Override
    public void save(InventoryItem item) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO inventory_items(name, quantity, unit, alert_level) VALUES (?, ?, ?, ?) "
                     + "ON CONFLICT(name) DO UPDATE SET quantity = excluded.quantity, unit = excluded.unit, alert_level = excluded.alert_level")) {
            statement.setString(1, item.getName());
            statement.setDouble(2, item.getQuantity());
            statement.setString(3, item.getUnit());
            statement.setDouble(4, item.getAlertLevel());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM inventory_items WHERE name = ?")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }
}
