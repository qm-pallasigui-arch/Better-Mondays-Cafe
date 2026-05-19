package persistence.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import persistence.AppDatabase;
import persistence.MenuRepository;
import pos.CoffeeItem;
import pos.FoodItem;
import pos.FruitTeaItem;
import pos.HerbalTeaItem;
import pos.MenuItem;
import pos.NonCoffeeItem;

public class SQLiteMenuRepository implements MenuRepository {

    @Override
    public List<MenuItem> findAll() throws Exception {
        List<MenuItem> items = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, category, hot_price, iced_regular_price, iced_large_price FROM menu_items ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                MenuItem item = mapRow(resultSet.getString("category"),
                        resultSet.getString("name"),
                        resultSet.getDouble("hot_price"),
                        resultSet.getDouble("iced_regular_price"),
                        resultSet.getDouble("iced_large_price"));
                item.replaceIngredients(loadIngredients(connection, resultSet.getLong("id")));
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Optional<MenuItem> findByName(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, category, hot_price, iced_regular_price, iced_large_price FROM menu_items WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                MenuItem item = mapRow(resultSet.getString("category"),
                        resultSet.getString("name"),
                        resultSet.getDouble("hot_price"),
                        resultSet.getDouble("iced_regular_price"),
                        resultSet.getDouble("iced_large_price"));
                item.replaceIngredients(loadIngredients(connection, resultSet.getLong("id")));
                return Optional.of(item);
            }
        }
    }

    @Override
    public void save(MenuItem item) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO menu_items(name, category, hot_price, iced_regular_price, iced_large_price) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT(name) DO UPDATE SET category = excluded.category, hot_price = excluded.hot_price, iced_regular_price = excluded.iced_regular_price, iced_large_price = excluded.iced_large_price")) {
                    statement.setString(1, item.getName());
                    statement.setString(2, item.getCategory());
                    statement.setDouble(3, item.getHotPrice());
                    statement.setDouble(4, item.getIcedRegularPrice());
                    statement.setDouble(5, item.getIcedLargePrice());
                    statement.executeUpdate();
                }

                long menuItemId = findMenuItemId(connection, item.getName());
                try (PreparedStatement deleteIngredients = connection.prepareStatement(
                        "DELETE FROM menu_item_ingredients WHERE menu_item_id = ?")) {
                    deleteIngredients.setLong(1, menuItemId);
                    deleteIngredients.executeUpdate();
                }

                try (PreparedStatement insertIngredient = connection.prepareStatement(
                        "INSERT INTO menu_item_ingredients(menu_item_id, ingredient_name, quantity) VALUES (?, ?, ?)")) {
                    for (Map.Entry<String, Double> entry : item.getIngredients().entrySet()) {
                        insertIngredient.setLong(1, menuItemId);
                        insertIngredient.setString(2, entry.getKey());
                        insertIngredient.setDouble(3, entry.getValue());
                        insertIngredient.addBatch();
                    }
                    insertIngredient.executeBatch();
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
    public void delete(String name) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM menu_items WHERE name = ?")) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private Map<String, Double> loadIngredients(Connection connection, long menuItemId) throws Exception {
        Map<String, Double> ingredients = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ingredient_name, quantity FROM menu_item_ingredients WHERE menu_item_id = ? ORDER BY id")) {
            statement.setLong(1, menuItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ingredients.put(resultSet.getString("ingredient_name"), resultSet.getDouble("quantity"));
                }
            }
        }
        return ingredients;
    }

    private long findMenuItemId(Connection connection, String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM menu_items WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Menu item not found: " + name);
                }
                return resultSet.getLong("id");
            }
        }
    }

    private MenuItem mapRow(String category, String name, double hotPrice, double regularPrice, double largePrice) {
        return switch (category) {
            case "Coffee" -> new CoffeeItem(name, hotPrice, regularPrice, largePrice);
            case "Non-Coffee" -> new NonCoffeeItem(name, hotPrice, regularPrice, largePrice);
            case "Fruit Tea" -> new FruitTeaItem(name, hotPrice, regularPrice, largePrice);
            case "Herbal Tea" -> new HerbalTeaItem(name, hotPrice, regularPrice, largePrice);
            default -> new FoodItem(name, "Food", hotPrice > 0 ? hotPrice : (regularPrice > 0 ? regularPrice : largePrice));
        };
    }
}
