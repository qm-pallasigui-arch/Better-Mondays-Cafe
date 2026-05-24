package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import persistence.MenuRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.Menu;
import pos.MenuItem;
import util.StringUtil;

public class MenuMaintenancePanel extends JPanel {

    private final MenuRepository repo;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> categoryFilter;
    private final JTextField searchField;
    private final JTextArea ingredientDetailArea = new JTextArea();
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
        AppTheme.styleSearchField(searchField);
        filterPanel.addLabeled("Category", categoryFilter);
        filterPanel.addLabeled("Search", searchField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
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

        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);
        topBar.add(filterPanel, BorderLayout.CENTER);
        topBar.add(buttons, BorderLayout.EAST);

        JPanel detailPanel = buildIngredientDetailPanel();

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(detailPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateIngredientDetail();
                }
            }
        });
        reload();
        AppTheme.applyToComponent(this);
    }

    private void reload() {
        try {
            cachedItems = repo.findAll();
            applyFilters();
            updateIngredientDetail();
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

        updateIngredientDetail();
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
            Optional<MenuItem> opt = repo.findByName(StringUtil.normalizeName(name));
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
                MenuItem edited = createMenuItem(normalizeCategory(newCat.trim()), StringUtil.normalizeName(newName.trim()), hot, reg, large);
            String ing = JOptionPane.showInputDialog(this, "Ingredients (name:qty,name:qty):",
                    ingredientsToCsv(item.getIngredients()));
            parseAndSetIngredients(edited, ing);
                // Keep in-memory ordering data and DB in sync
                    if (!StringUtil.normalizeName(name).equals(StringUtil.normalizeName(newName.trim()))) {
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
                    item.addIngredient(StringUtil.normalizeName(kv[0].trim()), Double.parseDouble(kv[1].trim()));
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

    private JPanel buildIngredientDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setOpaque(false);

        JLabel title = new JLabel("Ingredients Used");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(AppTheme.FG_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        ingredientDetailArea.setEditable(false);
        ingredientDetailArea.setLineWrap(true);
        ingredientDetailArea.setWrapStyleWord(true);
        ingredientDetailArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ingredientDetailArea.setForeground(AppTheme.FG_PRIMARY);
        ingredientDetailArea.setBackground(AppTheme.BG_SURFACE);
        ingredientDetailArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        ingredientDetailArea.setText("Click an item to see the ingredients it uses.");

        JScrollPane scrollPane = new JScrollPane(ingredientDetailArea);
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        scrollPane.setPreferredSize(new Dimension(260, 220));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void updateIngredientDetail() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= model.getRowCount()) {
            ingredientDetailArea.setText("Click an item to see the ingredients it uses.");
            return;
        }

        String itemName = String.valueOf(model.getValueAt(selectedRow, 0));
        MenuItem item = cachedItems.stream()
                .filter(menuItem -> itemName.equals(menuItem.getName()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            ingredientDetailArea.setText("No ingredient data available for this item.");
            return;
        }

        Map<String, Double> ingredients = item.getIngredients();
        if (ingredients.isEmpty()) {
            ingredientDetailArea.setText(item.getName() + "\n\nNo ingredient data recorded yet.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(item.getName()).append("\n\n");
        text.append("Used ingredients:\n");
        ingredients.forEach((ingredient, qty) -> text.append("• ")
                .append(ingredient)
                .append(" - ")
                .append(qty)
                .append("\n"));
        ingredientDetailArea.setText(text.toString());
        ingredientDetailArea.setCaretPosition(0);
    }
}
