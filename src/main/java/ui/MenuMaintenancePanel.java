package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import persistence.MenuRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.Menu;
import pos.MenuItem;

public class MenuMaintenancePanel extends JPanel {

    private final MenuRepository repo;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> categoryFilter;
    private final JTextField searchField;
    private java.util.List<MenuItem> cachedItems = new ArrayList<>();

    public MenuMaintenancePanel() throws Exception {
        super(new BorderLayout());
        repo = new SQLiteMenuRepository();

        model = new DefaultTableModel(new String[] { "Name", "Category", "Hot", "Iced Regular", "Iced Large" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(700, 300));
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        AppTheme.applyTableDefaults(table);

        FilterRow filterPanel = new FilterRow();
        categoryFilter = new JComboBox<>(new String[]{"All", "Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"});
        searchField = new JTextField(24);
        filterPanel.addLabeled("Category", categoryFilter);
        filterPanel.addLabeled("Search", searchField);

        JPanel buttons = new JPanel();
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");

        add.addActionListener((ActionEvent e) -> onAdd());
        edit.addActionListener((ActionEvent e) -> onEdit());
        delete.addActionListener((ActionEvent e) -> onDelete());
        categoryFilter.addActionListener((ActionEvent e) -> applyFilters());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reload();
        AppTheme.applyToComponent(this);
    }

    private void reload() {
        try {
            cachedItems = repo.findAll();
            applyFilters();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load menu: " + e.getMessage());
        }
    }

    private void applyFilters() {
        model.setRowCount(0);
        String selectedCategory = categoryFilter.getSelectedItem() == null ? "All" : categoryFilter.getSelectedItem().toString();
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        for (MenuItem item : cachedItems) {
            boolean categoryMatch = "All".equalsIgnoreCase(selectedCategory)
                    || normalizeCategory(item.getCategory()).equalsIgnoreCase(normalizeCategory(selectedCategory));
            boolean searchMatch = q.isEmpty()
                    || item.getName().toLowerCase().contains(q)
                    || item.getCategory().toLowerCase().contains(q);

            if (categoryMatch && searchMatch) {
                model.addRow(new Object[]{
                        item.getName(),
                        item.getCategory(),
                        item.getHotPrice(),
                        item.getIcedRegularPrice(),
                        item.getIcedLargePrice()
                });
            }
        }
    }

    private void onAdd() {
        try {
            String name = JOptionPane.showInputDialog(this, "Name:");
            if (name == null || name.trim().isEmpty())
                return;
            String[] categories = {"Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"};
            String category = (String) JOptionPane.showInputDialog(
                    this,
                    "Category:",
                    "Category",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    categories,
                    categories[0]);
            if (category == null)
                return;
            double hot = Double.parseDouble(JOptionPane.showInputDialog(this, "Hot price:"));
            double reg = Double.parseDouble(JOptionPane.showInputDialog(this, "Iced regular price:"));
            double large = Double.parseDouble(JOptionPane.showInputDialog(this, "Iced large price:"));
            MenuItem item = createMenuItem(normalizeCategory(category.trim()), name.trim(), hot, reg, large);
            String ing = JOptionPane.showInputDialog(this, "Ingredients (name:qty,name:qty):");
            parseAndSetIngredients(item, ing);
            Menu.getInstance().saveItem(item);
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding item: " + ex.getMessage());
        }
    }

    private void onEdit() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to edit");
            return;
        }
        String name = model.getValueAt(r, 0).toString();
        try {
            Optional<MenuItem> opt = repo.findByName(name);
            if (opt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item not found in repository");
                return;
            }
            MenuItem item = opt.get();
            String newName = JOptionPane.showInputDialog(this, "Name:", item.getName());
            if (newName == null)
                return;
                String[] categories = {"Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"};
                String newCat = (String) JOptionPane.showInputDialog(
                    this,
                    "Category:",
                    "Category",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    categories,
                    normalizeCategory(item.getCategory()));
            if (newCat == null)
                return;
            double hot = Double.parseDouble(JOptionPane.showInputDialog(this, "Hot price:", item.getHotPrice()));
            double reg = Double
                    .parseDouble(JOptionPane.showInputDialog(this, "Iced regular price:", item.getIcedRegularPrice()));
            double large = Double
                    .parseDouble(JOptionPane.showInputDialog(this, "Iced large price:", item.getIcedLargePrice()));
                MenuItem edited = createMenuItem(normalizeCategory(newCat.trim()), newName.trim(), hot, reg, large);
            String ing = JOptionPane.showInputDialog(this, "Ingredients (name:qty,name:qty):",
                    ingredientsToCsv(item.getIngredients()));
            parseAndSetIngredients(edited, ing);
                // Keep in-memory ordering data and DB in sync
                if (!name.equals(newName.trim())) {
                Menu.getInstance().removeItem(name);
                }
                Menu.getInstance().saveItem(edited);
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error editing item: " + ex.getMessage());
        }
    }

    private void onDelete() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete");
            return;
        }
        String name = model.getValueAt(r, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try {
            Menu.getInstance().removeItem(name);
            reload();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }

    private MenuItem createMenuItem(String category, String name, double hot, double reg, double large) {
        return switch (category) {
            case "Coffee" -> new pos.CoffeeItem(name, hot, reg, large);
            case "Non-Coffee" -> new pos.NonCoffeeItem(name, hot, reg, large);
            case "Fruit Tea" -> new pos.FruitTeaItem(name, hot, reg, large);
            case "Herbal Tea" -> new pos.HerbalTeaItem(name, hot, reg, large);
            case "Food" -> new pos.FoodItem(name, "Food", hot > 0 ? hot : (reg > 0 ? reg : large));
            default -> new pos.CoffeeItem(name, hot, reg, large);
        };
    }

    private String normalizeCategory(String category) {
        if (category == null) return "Coffee";
        String c = category.trim();
        return switch (c) {
            case "NonCoffee" -> "Non-Coffee";
            case "FruitTea" -> "Fruit Tea";
            case "HerbalTea" -> "Herbal Tea";
            default -> c;
        };
    }

    private void parseAndSetIngredients(MenuItem item, String csv) {
        if (csv == null || csv.trim().isEmpty())
            return;
        String[] parts = csv.split(Pattern.quote(","));
        for (String p : parts) {
            String[] kv = p.split(":", 2);
            if (kv.length == 2) {
                try {
                    item.addIngredient(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String ingredientsToCsv(Map<String, Double> map) {
        if (map == null || map.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(k).append(":").append(v);
        });
        return sb.toString();
    }
}
