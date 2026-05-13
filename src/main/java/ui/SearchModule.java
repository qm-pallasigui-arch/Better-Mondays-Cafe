package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import persistence.AppDatabase;

public class SearchModule extends JPanel {

    private final JTextField queryField = new JTextField(24);
    private final JComboBox<String> targetBox = new JComboBox<>(new String[]{"Menu", "Inventory", "Sales"});
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Type", "Name", "Details", "Date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public SearchModule() {
        super(new BorderLayout(8, 8));
        JTable table = new JTable(model);

        JPanel top = new JPanel(new GridLayout(2, 5, 6, 6));
        top.add(new JLabel("Search")); top.add(queryField); top.add(new JLabel("Target")); top.add(targetBox); top.add(new JLabel(""));
        top.add(new JLabel("From (yyyy-MM-dd)")); top.add(fromDateField); top.add(new JLabel("To (yyyy-MM-dd)")); top.add(toDateField);
        JButton btn = new JButton("Run Search");
        btn.addActionListener((ActionEvent e) -> runSearch());

        JPanel wrapTop = new JPanel(new BorderLayout());
        wrapTop.add(top, BorderLayout.CENTER);
        wrapTop.add(btn, BorderLayout.EAST);

        add(wrapTop, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void runSearch() {
        model.setRowCount(0);
        String q = queryField.getText().trim().toLowerCase();
        String target = targetBox.getSelectedItem().toString();
        try {
            switch (target) {
                case "Menu" -> searchMenu(q);
                case "Inventory" -> searchInventory(q);
                case "Sales" -> searchSales(q);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage());
        }
    }

    private void searchMenu(String q) throws Exception {
        try (Connection c = AppDatabase.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT name, category, hot_price, iced_regular_price, iced_large_price FROM menu_items ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String cat = rs.getString("category");
                if (matches(q, name, cat)) {
                    model.addRow(new Object[]{"Menu", name, cat + " | Hot: " + rs.getDouble("hot_price") + " | Iced: " + rs.getDouble("iced_regular_price") + "/" + rs.getDouble("iced_large_price"), ""});
                }
            }
        }
    }

    private void searchInventory(String q) throws Exception {
        try (Connection c = AppDatabase.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT name, quantity, unit, alert_level FROM inventory_items ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (matches(q, name, rs.getString("unit"))) {
                    model.addRow(new Object[]{"Inventory", name, "Qty: " + rs.getDouble("quantity") + " " + rs.getString("unit") + " | Alert: " + rs.getDouble("alert_level"), ""});
                }
            }
        }
    }

    private void searchSales(String q) throws Exception {
        LocalDate from = parseDate(fromDateField.getText().trim());
        LocalDate to = parseDate(toDateField.getText().trim());
        try (Connection c = AppDatabase.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT product_name, quantity, price, total, sold_at FROM sales_records ORDER BY sold_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("product_name");
                String soldAt = rs.getString("sold_at");
                LocalDate d = parseDateSafe(soldAt);
                if ((from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to)) && matches(q, name, soldAt)) {
                    model.addRow(new Object[]{"Sales", name, "Qty: " + rs.getInt("quantity") + " | Total: " + rs.getDouble("total"), soldAt});
                }
            }
        }
    }

    private boolean matches(String q, String... fields) {
        if (q.isEmpty()) return true;
        for (String f : fields) if (f != null && f.toLowerCase().contains(q)) return true;
        return false;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDate parseDateSafe(String s) {
        try {
            return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
