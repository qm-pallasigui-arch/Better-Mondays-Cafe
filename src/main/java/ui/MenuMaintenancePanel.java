package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import persistence.MenuRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.MenuItem;

public class MenuMaintenancePanel extends JPanel {

    private final MenuRepository repo;
    private final DefaultTableModel model;
    private final JTable table;

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

        JPanel buttons = new JPanel();
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");

        add.addActionListener((ActionEvent e) -> onAdd());
        edit.addActionListener((ActionEvent e) -> onEdit());
        delete.addActionListener((ActionEvent e) -> onDelete());

        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reload();
    }

    private void reload() {
        try {
            model.setRowCount(0);
            for (MenuItem item : repo.findAll()) {
                model.addRow(new Object[] { item.getName(), item.getCategory(), item.getHotPrice(),
                        item.getIcedRegularPrice(), item.getIcedLargePrice() });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load menu: " + e.getMessage());
        }
    }

    private void onAdd() {
        try {
            String name = JOptionPane.showInputDialog(this, "Name:");
            if (name == null || name.trim().isEmpty())
                return;
            String category = JOptionPane.showInputDialog(this, "Category (Coffee/NonCoffee/FruitTea/HerbalTea):");
            if (category == null)
                return;
            double hot = Double.parseDouble(JOptionPane.showInputDialog(this, "Hot price:"));
            double reg = Double.parseDouble(JOptionPane.showInputDialog(this, "Iced regular price:"));
            double large = Double.parseDouble(JOptionPane.showInputDialog(this, "Iced large price:"));
            MenuItem item = createMenuItem(category.trim(), name.trim(), hot, reg, large);
            String ing = JOptionPane.showInputDialog(this, "Ingredients (name:qty,name:qty):");
            parseAndSetIngredients(item, ing);
            repo.save(item);
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
            String newCat = JOptionPane.showInputDialog(this, "Category:", item.getCategory());
            if (newCat == null)
                return;
            double hot = Double.parseDouble(JOptionPane.showInputDialog(this, "Hot price:", item.getHotPrice()));
            double reg = Double
                    .parseDouble(JOptionPane.showInputDialog(this, "Iced regular price:", item.getIcedRegularPrice()));
            double large = Double
                    .parseDouble(JOptionPane.showInputDialog(this, "Iced large price:", item.getIcedLargePrice()));
            MenuItem edited = createMenuItem(newCat.trim(), newName.trim(), hot, reg, large);
            String ing = JOptionPane.showInputDialog(this, "Ingredients (name:qty,name:qty):",
                    ingredientsToCsv(item.getIngredients()));
            parseAndSetIngredients(edited, ing);
            repo.save(edited);
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
            repo.delete(name);
            reload();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }

    private MenuItem createMenuItem(String category, String name, double hot, double reg, double large) {
        return switch (category) {
            case "Coffee" -> new pos.CoffeeItem(name, hot, reg, large);
            case "NonCoffee", "Non-Coffee" -> new pos.NonCoffeeItem(name, hot, reg, large);
            case "FruitTea" -> new pos.FruitTeaItem(name, hot, reg, large);
            case "HerbalTea" -> new pos.HerbalTeaItem(name, hot, reg, large);
            default -> new pos.CoffeeItem(name, hot, reg, large);
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
