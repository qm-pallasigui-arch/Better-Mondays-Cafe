package pos;

import inventory.Inventory;
import inventory.InventoryItem;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.LineBorder;
import monitoring.Monitoring;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import monitoring.SalesRecord;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import loginregister.UserDataManager;
import persistence.Phase2Bootstrap;
import ui.MenuMaintenancePanel;
import ui.SearchModule;
import ui.AboutModule;
import ui.HelpModule;
import ui.InventoryRegistrationPanel;
import ui.StaffPanel;
import ui.InventoryGuidePanel;
import ui.AppTheme;
import persistence.sqlite.SQLiteInventoryRepository;
import persistence.sqlite.SQLiteStaffShiftRepository;
import persistence.sqlite.SQLiteSalesRepository;
import persistence.sqlite.SQLiteUserRepository;
import controller.InventoryController;
import controller.InventoryRowView;
import controller.OrderController;

public class POSSystem extends javax.swing.JFrame {

    private Monitoring monitoring;
    private UserDataManager.Role currentUserRole;
    private String currentUsername;
    private InventoryController inventoryController;
    private OrderController orderController;

    private static final Map<String, List<String>> CATEGORY_ITEMS = new LinkedHashMap<>();
    static {
        CATEGORY_ITEMS.put("Espresso & Coffee", Arrays.asList(
            "Americano", "Latte", "Cappuccino", "Salted Cream Latte", "Spanish Latte",
            "Dark Mocha", "White Mocha", "Caramel Macchiato", "Brewed Coffee"));
        CATEGORY_ITEMS.put("Specialty Drinks", Arrays.asList(
            "Vietnamese Coffee", "Ube Espresso", "Manila Latte",
            "Pumpkin Spice Latte", "Spiced Cookie Latte"));
        CATEGORY_ITEMS.put("Tea Latte", Arrays.asList(
            "Matcha Latte", "Chocolate Matcha", "Matcha Espresso",
            "Hojicha Latte", "Chai Latte"));
        CATEGORY_ITEMS.put("Non-Coffee", Arrays.asList(
            "Chocolate Latte", "Strawberry Latte", "Mango Latte",
            "Dragon Fruit Coconut Latte", "Ube Latte"));
        CATEGORY_ITEMS.put("House Favorites", Arrays.asList(
            "Mango Latte", "Strawberry Latte", "Salted Cream Latte", "Spanish Latte"));
        CATEGORY_ITEMS.put("Fruit Tea", Arrays.asList(
            "Strawberry Green Tea", "Mango Green Tea", "Peach Green Tea",
            "Passion Fruit Green Tea"));
        CATEGORY_ITEMS.put("Herbal Tea", Arrays.asList(
            "Peppermint", "Chamomile", "Earl Grey", "Cinnamon"));
        CATEGORY_ITEMS.put("Sandwiches", Arrays.asList(
            "Signature Ham & Cheese", "Classic Grilled Cheese", "Homestyle Pesto & Cheese"));
        CATEGORY_ITEMS.put("Pandesal Pairs", Arrays.asList(
            "Ham & Cheese", "Cheesy Pesto", "Spam & Cheese"));
        CATEGORY_ITEMS.put("Pastries", Arrays.asList(
            "Chocolate Crinkles", "Chocolate Cookies", "Brownies", "Banana Bread",
            "Chocolate Tiramisu", "Matcha Tiramisu", "Creamy Spinach", "Blueberry Cheesecake"));
    }

    private static final Map<String, Double> HOUSE_FAVORITES_PRICES = Map.of(
        "Mango Latte", 210.0,
        "Strawberry Latte", 200.0,
        "Salted Cream Latte", 190.0,
        "Spanish Latte", 180.0
    );

    private static class OrderEntry {
        String name;
        String variant;
        int quantity;
        double unitPrice;
        OrderEntry(String name, String variant, int quantity, double unitPrice) {
            this.name = name;
            this.variant = variant;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
        String displayName() {
            return variant.isEmpty() ? name : name + " (" + variant + ")";
        }
        double lineTotal() {
            return quantity * unitPrice;
        }
    }

    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private int orderCount = 0;
    private String activeCategory = "Espresso & Coffee";

    // Custom panels for ordering tab
    private JPanel sidebarPanel;
    private JPanel centerPanel;
    private JPanel productGridPanel;
    private JTextField searchField;
    private JPanel orderPanel;
    private JPanel receiptPanel;

    public POSSystem(String username, UserDataManager.Role role) {
        this.currentUserRole = role;
        this.currentUsername = username;
        initComponents();
        setTitle("Better Mondays Coffeee Cafe Management System - " + username);
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setResizable(true);
        setLocationRelativeTo(null);
        try {
            Phase2Bootstrap.seedCatalogIfEmpty();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database initialization warning: " + e.getMessage(),
                    "Database",
                    JOptionPane.WARNING_MESSAGE);
        }

        loadInventoryTable();

        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        orderController = new OrderController(new SQLiteSalesRepository());
        applyInventoryStatusColorRenderer();

        monitoring = new Monitoring(jTableMonitoring, jTableSales);
        monitoring.loadLowStockIngredients();

        AppTheme.applyToFrame(this);
        styleSearchField();

        applyRoleBasedAccessControl();

        showCategory("Espresso & Coffee");
    }

    private void applyInventoryStatusColorRenderer() {
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    return c;
                }
                String text = value == null ? "" : value.toString();
                if (text.contains("EXPIRED")) {
                    c.setForeground(new Color(180, 0, 0));
                } else if (text.contains("LOW STOCK") || text.contains("EXPIRING")) {
                    c.setForeground(new Color(200, 120, 0));
                } else {
                    c.setForeground(new Color(0, 128, 0));
                }
                return c;
            }
        });
    }

    @Deprecated
    public POSSystem() {
        this("unknown", UserDataManager.Role.STAFF);
    }

    private void applyRoleBasedAccessControl() {
        if (currentUserRole == UserDataManager.Role.STAFF) {
            int inventoryTabIndex = -1;
            int monitoringTabIndex = -1;

            for (int i = 0; i < jTabbedPaneI.getTabCount(); i++) {
                String tabTitle = jTabbedPaneI.getTitleAt(i);
                if ("Inventory".equals(tabTitle)) {
                    inventoryTabIndex = i;
                } else if ("Monitoring".equals(tabTitle)) {
                    monitoringTabIndex = i;
                }
            }

            if (monitoringTabIndex > -1) {
                jTabbedPaneI.removeTabAt(monitoringTabIndex);
            }
            if (inventoryTabIndex > -1) {
                jTabbedPaneI.removeTabAt(inventoryTabIndex);
            }
            for (int i = 0; i < jTabbedPaneI.getTabCount(); i++) {
                if ("Menu Maintenance".equals(jTabbedPaneI.getTitleAt(i))) {
                    jTabbedPaneI.removeTabAt(i);
                    break;
                }
            }
            for (int i = 0; i < jTabbedPaneI.getTabCount(); i++) {
                if ("Register Product".equals(jTabbedPaneI.getTitleAt(i))) {
                    jTabbedPaneI.removeTabAt(i);
                    break;
                }
            }
        }
    }

    // ─── Category display ───────────────────────────────────────
    private void showCategory(String category) {
        activeCategory = category;
        for (java.awt.Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton b) {
                boolean active = category.equals(b.getText());
                b.setBackground(active ? new Color(50, 157, 111) : new Color(36, 55, 83));
                b.setForeground(active ? Color.WHITE : new Color(245, 248, 252));
            }
        }
        rebuildProductGrid(category, searchField.getText());
    }

    private void rebuildProductGrid(String category, String search) {
        productGridPanel.removeAll();
        List<String> itemNames = CATEGORY_ITEMS.getOrDefault(category, List.of());

        List<MenuItem> items = itemNames.stream()
            .map(name -> Menu.getInstance().getMenuItem(name))
            .filter(item -> item != null)
            .collect(Collectors.toList());

        String searchTerm = search.trim();
        if ("Search a product".equals(searchTerm)) {
            searchTerm = "";
        }
        if (!searchTerm.isEmpty()) {
            String finalSearch = searchTerm.toLowerCase();
            items = items.stream()
                .filter(item -> item.getName().toLowerCase().contains(finalSearch))
                .collect(Collectors.toList());
        }

        if (items.isEmpty()) {
            JLabel emptyLabel = new JLabel("No items found", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(197, 209, 224));
            productGridPanel.add(emptyLabel);
        } else {
            for (MenuItem item : items) {
                productGridPanel.add(createProductCard(item, category));
            }
        }

        productGridPanel.revalidate();
        productGridPanel.repaint();
    }

    private JPanel createProductCard(MenuItem item, String category) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(new Color(36, 55, 83));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 85, 120), 1),
            BorderFactory.createEmptyBorder(6, 5, 5, 5)));

        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(56, 44));
        imgLabel.setOpaque(true);
        imgLabel.setBackground(new Color(49, 73, 105));
        card.add(imgLabel, BorderLayout.NORTH);

        double displayPrice = pickDisplayPrice(item, category);
        JLabel nameLabel = new JLabel(
            "<html><div style='text-align:center;'><b style='color:#F5F8FC;font-size:12px;'>" + item.getName()
            + "</b><br><span style='color:#32C075;font-size:11px;'>\u20B1" + String.format("%.2f", displayPrice) + "</span></div></html>",
            SwingConstants.CENTER);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(nameLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        btnPanel.setOpaque(false);
        addButtonsForCategory(btnPanel, item, category);
        card.add(btnPanel, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(150, 134));
        return card;
    }

    private double pickDisplayPrice(MenuItem item, String category) {
        switch (category) {
            case "Specialty Drinks":
                return item.getIcedRegularPrice();
            case "House Favorites":
                return HOUSE_FAVORITES_PRICES.getOrDefault(item.getName(), 0.0);
            default:
                return item.getIcedRegularPrice() > 0 ? item.getIcedRegularPrice()
                     : item.getHotPrice() > 0 ? item.getHotPrice()
                     : item.getIcedLargePrice();
        }
    }

    private void addButtonsForCategory(JPanel panel, MenuItem item, String category) {
        switch (category) {
            case "Espresso & Coffee":
                addHotBtn(panel, item);
                addIcedRegularBtn(panel, item);
                addIcedLargeBtn(panel, item);
                break;
            case "Specialty Drinks":
                addItemBtn(panel, "Regular Iced", item.getIcedRegularPrice(), "Regular Iced", item.getName());
                break;
            case "Tea Latte":
                addHotBtn(panel, item);
                addIcedRegularBtn(panel, item, "Iced Regular");
                break;
            case "Non-Coffee":
                if ("Chocolate Latte".equals(item.getName())) {
                    addHotBtn(panel, item);
                }
                addIcedRegularBtn(panel, item);
                addIcedLargeBtn(panel, item);
                break;
            case "House Favorites":
                double hfPrice = HOUSE_FAVORITES_PRICES.getOrDefault(item.getName(), 0.0);
                addSingleBtn(panel, "Add to cart", hfPrice, item.getName());
                break;
            case "Fruit Tea":
                addIcedRegularBtn(panel, item);
                addIcedLargeBtn(panel, item);
                break;
            case "Herbal Tea":
                addHotBtn(panel, item);
                break;
            default:
                addSingleBtn(panel, "Add to cart", item.getHotPrice(), item.getName());
                break;
        }
    }

    private void addHotBtn(JPanel panel, MenuItem item) {
        if (item.getHotPrice() > 0) {
            JButton btn = new JButton("Hot");
            styleProdBtn(btn);
            btn.addActionListener(e -> addOrderItem(item.getName(), "Hot", item.getHotPrice()));
            panel.add(btn);
        }
    }

    private void addIcedRegularBtn(JPanel panel, MenuItem item) {
        addIcedRegularBtn(panel, item, "Regular");
    }

    private void addIcedRegularBtn(JPanel panel, MenuItem item, String label) {
        if (item.getIcedRegularPrice() > 0) {
            JButton btn = new JButton(label);
            styleProdBtn(btn);
            btn.addActionListener(e -> addOrderItem(item.getName(), "Regular Iced", item.getIcedRegularPrice()));
            panel.add(btn);
        }
    }

    private void addIcedLargeBtn(JPanel panel, MenuItem item) {
        if (item.getIcedLargePrice() > 0) {
            JButton btn = new JButton("Large");
            styleProdBtn(btn);
            btn.addActionListener(e -> addOrderItem(item.getName(), "Large Iced", item.getIcedLargePrice()));
            panel.add(btn);
        }
    }

    private void addSingleBtn(JPanel panel, String text, double price, String itemName) {
        if (price <= 0) return;
        JButton btn = new JButton(text);
        styleProdBtn(btn);
        btn.addActionListener(e -> addOrderItem(itemName, "", price));
        panel.add(btn);
    }

    private void addItemBtn(JPanel panel, String text, double price, String variant, String itemName) {
        if (price <= 0) return;
        JButton btn = new JButton(text);
        styleProdBtn(btn);
        btn.addActionListener(e -> addOrderItem(itemName, variant, price));
        panel.add(btn);
    }

    private void styleProdBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setBackground(new Color(50, 157, 111));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void styleSearchField() {
        searchField.setBackground(new Color(36, 55, 83));
        searchField.setForeground(new Color(197, 209, 224));
        searchField.setCaretColor(new Color(197, 209, 224));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(60, 85, 120), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void addOrderItem(String name, String variant, double price) {
        if (name == null || name.isEmpty()) return;
        for (OrderEntry entry : orderEntries) {
            if (entry.name.equals(name) && entry.variant.equals(variant)) {
                entry.quantity++;
                refreshOrderDisplay();
                return;
            }
        }
        orderEntries.add(new OrderEntry(name, variant, 1, price));
        refreshOrderDisplay();
    }

    private void refreshOrderDisplay() {
        orderPanel.removeAll();
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(new Color(28, 43, 63));
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        for (OrderEntry entry : orderEntries) {
            itemsPanel.add(createOrderRow(entry));
            itemsPanel.add(Box.createVerticalStrut(4));
        }

        orderPanel.add(new JScrollPane(itemsPanel), BorderLayout.CENTER);
        orderPanel.revalidate();
        orderPanel.repaint();
        updateReceipt();
    }

    private JPanel createOrderRow(OrderEntry entry) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(new Color(36, 55, 83));
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel nameLabel = new JLabel(entry.displayName());
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        nameLabel.setForeground(new Color(245, 248, 252));
        row.add(nameLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        rightPanel.setOpaque(false);

        JButton minusBtn = new JButton("-");
        minusBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        minusBtn.setBackground(new Color(60, 85, 120));
        minusBtn.setForeground(Color.WHITE);
        minusBtn.setFocusPainted(false);
        minusBtn.setMargin(new Insets(0, 4, 0, 4));
        minusBtn.addActionListener(e -> {
            if (entry.quantity > 1) {
                entry.quantity--;
            } else {
                orderEntries.remove(entry);
            }
            refreshOrderDisplay();
        });
        rightPanel.add(minusBtn);

        JLabel qtyLabel = new JLabel(String.valueOf(entry.quantity));
        qtyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        qtyLabel.setForeground(new Color(245, 248, 252));
        rightPanel.add(qtyLabel);

        JButton plusBtn = new JButton("+");
        plusBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        plusBtn.setBackground(new Color(60, 85, 120));
        plusBtn.setForeground(Color.WHITE);
        plusBtn.setFocusPainted(false);
        plusBtn.setMargin(new Insets(0, 4, 0, 4));
        plusBtn.addActionListener(e -> {
            entry.quantity++;
            refreshOrderDisplay();
        });
        rightPanel.add(plusBtn);

        row.add(rightPanel, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(9999, 32));
        return row;
    }

    private void updateReceipt() {
        receiptPanel.removeAll();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(new Color(28, 43, 63));
        receiptPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        double totalInclusive = 0;
        for (OrderEntry entry : orderEntries) {
            totalInclusive += entry.lineTotal();
        }
        double subTotalExVat = totalInclusive / 1.12;
        double vat = totalInclusive - subTotalExVat;

        JLabel orderNumLabel = new JLabel("Order #" + (orderCount + 1));
        orderNumLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        orderNumLabel.setForeground(new Color(245, 248, 252));
        orderNumLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(orderNumLabel);

        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        clearBtn.setBackground(new Color(180, 60, 60));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            orderEntries.clear();
            refreshOrderDisplay();
        });
        receiptPanel.add(clearBtn);
        receiptPanel.add(Box.createVerticalStrut(10));

        receiptPanel.add(new JScrollPane(orderPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        receiptPanel.add(Box.createVerticalStrut(8));

        JLabel line = new JLabel("─────────────────────");
        line.setForeground(new Color(100, 130, 160));
        line.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(line);

        receiptPanel.add(makeReceiptRow("Subtotal:", String.format("\u20B1%.2f", subTotalExVat)));
        receiptPanel.add(makeReceiptRow("Tax (12%):", String.format("\u20B1%.2f", vat)));
        receiptPanel.add(makeReceiptRow("Total:", String.format("\u20B1%.2f", totalInclusive)));

        receiptPanel.add(Box.createVerticalStrut(6));
        receiptPanel.add(makeReceiptRow("Cash:", ""));

        JTextField cashField = new JTextField(10);
        cashField.setHorizontalAlignment(JTextField.RIGHT);
        cashField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cashField.setBackground(new Color(36, 55, 83));
        cashField.setForeground(new Color(197, 209, 224));
        cashField.setCaretColor(new Color(197, 209, 224));
        cashField.setBorder(new LineBorder(new Color(60, 85, 120), 1, true));
        cashField.setMaximumSize(new Dimension(120, 26));
        cashField.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(cashField);

        receiptPanel.add(Box.createVerticalStrut(2));
        receiptPanel.add(makeReceiptRow("Change:", "\u20B10.00"));

        receiptPanel.add(Box.createVerticalStrut(10));

        final double fSubTotalExVat = subTotalExVat;
        final double fVat = vat;
        final double fTotal = totalInclusive;

        JButton printBtn = new JButton("Print Bills");
        printBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        printBtn.setBackground(new Color(50, 157, 111));
        printBtn.setForeground(Color.WHITE);
        printBtn.setFocusPainted(false);
        printBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        printBtn.setMaximumSize(new Dimension(200, 35));
        printBtn.addActionListener(e -> {
            String cashStr = cashField.getText().trim();
            if (cashStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter cash amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double cash = Double.parseDouble(cashStr);
                if (cash < fTotal) {
                    JOptionPane.showMessageDialog(this, "Insufficient cash.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                double change = cash - fTotal;
                showReceipt(fSubTotalExVat, fVat, fTotal, cash, change);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid cash amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        receiptPanel.add(printBtn);

        receiptPanel.revalidate();
        receiptPanel.repaint();
    }

    private JPanel makeReceiptRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(197, 209, 224));
        row.add(lbl, BorderLayout.WEST);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(new Color(245, 248, 252));
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private void showReceipt(double subTotal, double vat, double total, double cash, double change) {
        String lineSep = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        String receipt = " ☕ Better Mondays Cafe ☕\n"
               + " 123 Main St., Manila\n"
               + " VAT REG TIN: 123-456-789\n"
               + lineSep
               + "Date: " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date()) + "\n"
               + "Txn #: TXN" + String.format("%06d", ++transactionCounter) + "\n"
               + lineSep
               + String.format("%-15s %3s %8s\n", "ITEM", "QTY", "AMOUNT")
               + "───────────────────────────────\n";

        for (OrderEntry entry : orderEntries) {
            receipt += String.format("%-15s %3d %8.2f\n",
                    truncate(entry.displayName(), 15), entry.quantity, entry.lineTotal());
        }

        receipt += "───────────────────────────────\n"
                 + String.format("%-23s \u20B1%7.2f\n", "Subtotal (excl VAT):", subTotal)
                 + String.format("%-23s \u20B1%7.2f\n", "VAT (12%):", vat)
                 + String.format("%-23s \u20B1%7.2f\n", "TOTAL (incl VAT):", total)
                 + String.format("%-23s \u20B1%7.2f\n", "Cash:", cash)
                 + String.format("%-23s \u20B1%7.2f\n", "Change:", change)
                 + lineSep
                 + " Thank you! Come again!\n"
                 + " *** Have a nice day ***\n";

        JOptionPane.showMessageDialog(this, receipt,
                "✅ RECEIPT - TXN" + String.format("%06d", transactionCounter),
                JOptionPane.INFORMATION_MESSAGE);

        // Deduct ingredients
        Inventory inv = Inventory.getInstance();
        Menu menu = Menu.getInstance();
        for (OrderEntry entry : orderEntries) {
            MenuItem menuItem = menu.getMenuItem(entry.name);
            if (menuItem != null) {
                for (Map.Entry<String, Double> e : menuItem.getIngredients().entrySet()) {
                    inv.deductIngredient(e.getKey(), e.getValue());
                }
            }
        }

        List<SalesRecord> salesList = new ArrayList<>();
        for (OrderEntry entry : orderEntries) {
            salesList.add(new SalesRecord(entry.displayName(), entry.quantity, entry.unitPrice, entry.lineTotal()));
        }

        if (orderController == null) {
            orderController = new OrderController(new SQLiteSalesRepository());
        }

        String transactionRef = "TXN" + String.format("%06d", transactionCounter);
        try {
            orderController.persistCompletedTransaction(transactionRef, salesList, subTotal, cash, change);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save sales to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        monitoring.addMultipleSales(salesList);
        orderCount++;
        orderEntries.clear();
        refreshOrderDisplay();
        loadInventoryTable();
        monitoring.loadLowStockIngredients();
    }

    // ─── initComponents (replaces GUI builder code) ─────────────
    private void initComponents() {
        jTabbedPaneI = new javax.swing.JTabbedPane();
        jPanelPOS = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        // ═══════════════════════════════════════════════════════════
        //  ORDERING TAB
        // ═══════════════════════════════════════════════════════════
        jPanelPOS.setBackground(new Color(28, 43, 63));
        jPanelPOS.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Ordering");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        titleLabel.setForeground(new Color(255, 255, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 0));

        // ─── Category pills (horizontal nav) ───────────────────
        sidebarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        sidebarPanel.setBackground(new Color(23, 36, 54));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        String[] categories = {
            "Espresso & Coffee", "Specialty Drinks", "Tea Latte", "Non-Coffee",
            "House Favorites", "Fruit Tea", "Herbal Tea",
            "Sandwiches", "Pandesal Pairs", "Pastries"
        };

        for (String cat : categories) {
            JButton btn = new JButton(cat);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btn.setBackground(new Color(36, 55, 83));
            btn.setForeground(new Color(245, 248, 252));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 85, 120), 1, true),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
            btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            btn.addActionListener(e -> showCategory(btn.getText()));
            sidebarPanel.add(btn);
        }

        // ─── Center panel (pills + search + product grid) ──────
        centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setBackground(new Color(28, 43, 63));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));

        JPanel centerTopPanel = new JPanel(new BorderLayout(4, 0));
        centerTopPanel.setOpaque(false);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchIcon.setForeground(new Color(100, 130, 160));
        searchPanel.add(searchIcon);

        searchField = new JTextField("Search a product", 20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBackground(new Color(36, 55, 83));
        searchField.setForeground(new Color(100, 130, 160));
        searchField.setCaretColor(new Color(197, 209, 224));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(60, 85, 120), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchField.setPreferredSize(new Dimension(250, 32));
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Search a product".equals(searchField.getText())) {
                    searchField.setText("");
                    searchField.setForeground(new Color(197, 209, 224));
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search a product");
                    searchField.setForeground(new Color(100, 130, 160));
                }
            }
        });
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                rebuildProductGrid(activeCategory, searchField.getText());
            }
        });
        searchPanel.add(searchField);
        centerTopPanel.add(sidebarPanel, BorderLayout.NORTH);
        centerTopPanel.add(searchPanel, BorderLayout.SOUTH);
        centerPanel.add(centerTopPanel, BorderLayout.NORTH);

        productGridPanel = new JPanel(new GridLayout(0, 4, 8, 8));
        productGridPanel.setBackground(new Color(28, 43, 63));
        centerPanel.add(new JScrollPane(productGridPanel), BorderLayout.CENTER);

        // ─── Right sidebar (order summary) ─────────────────────
        orderPanel = new JPanel(new BorderLayout());
        orderPanel.setBackground(new Color(28, 43, 63));

        receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(new Color(28, 43, 63));
        receiptPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane receiptScroll = new JScrollPane(receiptPanel);
        receiptScroll.setBorder(null);
        receiptScroll.setPreferredSize(new Dimension(320, 0));

        // Assemble ordering panel
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(centerPanel, BorderLayout.CENTER);
        body.add(receiptScroll, BorderLayout.EAST);

        JPanel orderingPanel = new JPanel(new BorderLayout());
        orderingPanel.setBackground(new Color(28, 43, 63));
        orderingPanel.add(topBar, BorderLayout.NORTH);
        orderingPanel.add(body, BorderLayout.CENTER);

        jTabbedPaneI.addTab("Ordering", orderingPanel);
        jTabbedPaneI.setBackgroundAt(0, new Color(28, 43, 63));

        // Add other tabs
        jTabbedPaneI.addTab("Search", new SearchModule());
        jTabbedPaneI.setBackground(new Color(28, 43, 63));
        jTabbedPaneI.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 51)));
        jTabbedPaneI.setForeground(new java.awt.Color(255, 255, 255));
        jTabbedPaneI.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPaneI.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jTabbedPaneI.setFocusable(false);

        // ═══════════════════════════════════════════════════════════
        //  INVENTORY TAB
        // ═══════════════════════════════════════════════════════════
        jPanelInventory = new javax.swing.JPanel();
        jPanelInventory.setBackground(new java.awt.Color(28, 43, 63));

        jLabel2 = new javax.swing.JLabel();
        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 36));
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Inventory");

        jScrollPane3 = new javax.swing.JScrollPane();
        inventoryTable = new javax.swing.JTable();
        inventoryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Item Name", "Quantity", "Unit", "Low Stock Alert", "Status"
            }
        ) {
            boolean[] canEdit = new boolean[] { false, false, false, false, false };
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane3.setViewportView(inventoryTable);

        InventoryEdit = new javax.swing.JButton();
        InventoryEdit.setText("Edit");
        InventoryEdit.setFocusable(false);
        InventoryEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InventoryEditActionPerformed(evt);
            }
        });

        InventoryRemove = new javax.swing.JButton();
        InventoryRemove.setText("Remove");
        InventoryRemove.setFocusable(false);
        InventoryRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InventoryRemoveActionPerformed(evt);
            }
        });

        InventoryAdd = new javax.swing.JButton();
        InventoryAdd.setText("Add");
        InventoryAdd.setFocusable(false);
        InventoryAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InventoryAddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelInventoryLayout = new javax.swing.GroupLayout(jPanelInventory);
        jPanelInventory.setLayout(jPanelInventoryLayout);
        jPanelInventoryLayout.setHorizontalGroup(
            jPanelInventoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInventoryLayout.createSequentialGroup()
                .addGroup(jPanelInventoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelInventoryLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(InventoryAdd)
                        .addGap(18, 18, 18)
                        .addComponent(InventoryEdit)
                        .addGap(18, 18, 18)
                        .addComponent(InventoryRemove))
                    .addGroup(jPanelInventoryLayout.createSequentialGroup()
                        .addContainerGap(106, Short.MAX_VALUE)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 1178, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(107, 107, 107))
        );
        jPanelInventoryLayout.setVerticalGroup(
            jPanelInventoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInventoryLayout.createSequentialGroup()
                .addGroup(jPanelInventoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInventoryLayout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel2))
                    .addGroup(jPanelInventoryLayout.createSequentialGroup()
                        .addContainerGap(61, Short.MAX_VALUE)
                        .addGroup(jPanelInventoryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(InventoryRemove)
                            .addComponent(InventoryEdit)
                            .addComponent(InventoryAdd))))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 655, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(50, Short.MAX_VALUE))
        );
        jTabbedPaneI.addTab("Inventory", jPanelInventory);

        // ═══════════════════════════════════════════════════════════
        //  MONITORING TAB
        // ═══════════════════════════════════════════════════════════
        jPanelMonitoring = new javax.swing.JPanel();
        jPanelMonitoring.setBackground(new java.awt.Color(28, 43, 63));
        jPanelMonitoring.setForeground(new java.awt.Color(255, 255, 255));

        jLabel3 = new javax.swing.JLabel();
        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 36));
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Monitoring");

        jTableSales = new javax.swing.JTable();
        jTableSales.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] { {null, null, null, null} },
            new String[] { "Item", "Variant", "Quantity", "Total Price" }
        ));
        jScrollPane4 = new javax.swing.JScrollPane(jTableSales);

        jTableMonitoring = new javax.swing.JTable();
        jTableMonitoring.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] { {null, null, null, null, null} },
            new String[] { "Ingredient", "Quantity", "Unit", "Alert Level", "Status" }
        ));
        jScrollPane5 = new javax.swing.JScrollPane(jTableMonitoring);

        jLabel4 = new javax.swing.JLabel();
        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18));
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Inventory Low Stock Alert");

        jLabel5 = new javax.swing.JLabel();
        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 18));
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Sales");

        javax.swing.GroupLayout jPanelMonitoringLayout = new javax.swing.GroupLayout(jPanelMonitoring);
        jPanelMonitoring.setLayout(jPanelMonitoringLayout);
        jPanelMonitoringLayout.setHorizontalGroup(
            jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMonitoringLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelMonitoringLayout.createSequentialGroup()
                        .addGroup(jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 725, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 62, Short.MAX_VALUE)
                        .addGroup(jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 532, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(48, 48, 48))
                    .addGroup(jPanelMonitoringLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))));
        jPanelMonitoringLayout.setVerticalGroup(
            jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelMonitoringLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addGroup(jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelMonitoringLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
                    .addComponent(jScrollPane5))
                .addContainerGap(36, Short.MAX_VALUE)));
        jTabbedPaneI.addTab("Monitoring", jPanelMonitoring);

        // Other tabs
        try {
            jTabbedPaneI.addTab("Menu Maintenance", new MenuMaintenancePanel());
        } catch (Exception e) { System.err.println("MenuMaintenancePanel init failed: " + e.getMessage()); }
        try {
            jTabbedPaneI.addTab("Register Product", new InventoryRegistrationPanel(new SQLiteInventoryRepository()));
        } catch (Exception e) { System.err.println("InventoryRegistrationPanel init failed: " + e.getMessage()); }
        try {
            jTabbedPaneI.addTab("Staff", new StaffPanel(new SQLiteStaffShiftRepository(), new SQLiteUserRepository(), currentUsername, currentUserRole));
        } catch (Exception e) { System.err.println("StaffPanel init failed: " + e.getMessage()); }
        jTabbedPaneI.addTab("Inventory Guide", new InventoryGuidePanel());
        jTabbedPaneI.addTab("About", new AboutModule());
        jTabbedPaneI.addTab("Help", new HelpModule());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPaneI, javax.swing.GroupLayout.PREFERRED_SIZE, 1482, javax.swing.GroupLayout.PREFERRED_SIZE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPaneI, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));

        pack();
        setLocationRelativeTo(null);
    }

    // ─── Old handler stubs for compatibility ────────────────────
    public void ItemCost() {
        double preTaxTotal = 0.0;
        for (int i = 0; i < jTable1.getRowCount(); i++) {
            double lineTotal = Double.parseDouble(jTable1.getValueAt(i, 2).toString());
            preTaxTotal += lineTotal / 1.12;
        }
        double vat = preTaxTotal * 0.12;
        double inclusiveTotal = preTaxTotal + vat;
        jTextFieldSubTotal.setText(String.format("\u20B1%.2f", preTaxTotal));
        jTextFieldTax.setText(String.format("\u20B1%.2f", vat));
        jTextFieldTotal.setText(String.format("\u20B1%.2f", inclusiveTotal));
    }

    public boolean Change() {
        try {
            double cash = Double.parseDouble(cashpayment.getText().replace("\u20B1", "").replace("P", "").trim());
            double total = Double.parseDouble(jTextFieldTotal.getText().replace("\u20B1", "").replace("P", "").trim());
            if (cash < total) {
                JOptionPane.showMessageDialog(this, "Insufficient cash.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            double change = cash - total;
            jTextFieldChange.setText(String.format("\u20B1%.2f", change));
            JOptionPane.showMessageDialog(this,
                "Payment Successful!\nSubtotal: " + jTextFieldSubTotal.getText() + "\nVAT (12%): " + jTextFieldTax.getText()
                + "\nTotal: " + jTextFieldTotal.getText() + "\nCash: \u20B1" + String.format("%.2f", cash)
                + "\nChange: \u20B1" + String.format("%.2f", change), "Success", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please Enter Cash Amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void addItem(String productName, double price) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        boolean found = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).toString().equals(productName)) {
                int qty = Integer.parseInt(model.getValueAt(i, 1).toString());
                qty++;
                model.setValueAt(qty, i, 1);
                model.setValueAt(price * qty, i, 2);
                found = true;
                break;
            }
        }
        if (!found) {
            model.addRow(new Object[]{productName, 1, price});
        }
        ItemCost();
    }

    private void loadInventoryTable() {
        DefaultTableModel model = (DefaultTableModel) inventoryTable.getModel();
        model.setRowCount(0);
        if (inventoryController == null) {
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        }
        for (InventoryRowView row : inventoryController.buildInventoryRows()) {
            model.addRow(new Object[]{row.getName(), row.getQuantity(), row.getUnit(), row.getAlertLevel(), row.getStatus()});
        }
    }

    private void chooseHotOrIced(String rawProductName) {
        String productName = rawProductName;
        Object[] options = {"Hot", "Regular Iced", "Large Iced"};
        int choice = JOptionPane.showOptionDialog(this, "Choose variant for " + productName + ":", "Choose Variant",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == JOptionPane.CLOSED_OPTION || choice < 0) return;
        String variant = options[choice].toString();
        String displayName = productName + " (" + variant + ")";
        double price = Menu.getInstance().getPrice(productName, variant);
        if (price > 0) addItem(displayName, price);
    }

    // ─── Event handlers ─────────────────────────────────────────
    private void removeitemActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow >= 0) {
            int qty = Integer.parseInt(model.getValueAt(selectedRow, 1).toString());
            if (qty > 1) {
                qty--;
                model.setValueAt(qty, selectedRow, 1);
                double pricePerItem = Double.parseDouble(model.getValueAt(selectedRow, 2).toString()) / (qty + 1);
                model.setValueAt(pricePerItem * qty, selectedRow, 2);
            } else {
                model.removeRow(selectedRow);
            }
            ItemCost();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to remove.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cashpaymentActionPerformed(java.awt.event.ActionEvent evt) {
        String EnterNumber = cashpayment.getText();
        if (EnterNumber == "") {
            cashpayment.setText(cashpayment.getText());
        } else {
            EnterNumber = cashpayment.getText() + cashpayment.getText();
            cashpayment.setText(EnterNumber);
        }
    }

    private void resetorderActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        jTextFieldChange.setText("");
        jTextFieldTax.setText("");
        jTextFieldTotal.setText("");
        jTextFieldSubTotal.setText("");
        cashpayment.setText("");
    }

    private void exitActionPerformed(java.awt.event.ActionEvent evt) {
        int option = JOptionPane.showConfirmDialog(this, "Do you want to exit?",
            "Better Mondays Coffeee Cafe Management System", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option == JOptionPane.YES_OPTION) System.exit(0);
    }

    private static int transactionCounter = 1000;

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }

    private void payActionPerformed(java.awt.event.ActionEvent evt) {
        boolean paymentSuccess = Change();
        if (!paymentSuccess) return;

        DefaultTableModel orderModel = (DefaultTableModel) jTable1.getModel();
        Inventory inv = Inventory.getInstance();
        Menu menu = Menu.getInstance();

        for (int i = 0; i < orderModel.getRowCount(); i++) {
            String itemName = orderModel.getValueAt(i, 0).toString();
            String baseName = itemName.split(" \\(")[0].trim();
            MenuItem menuItem = menu.getMenuItem(baseName);
            if (menuItem != null) {
                for (Map.Entry<String, Double> e : menuItem.getIngredients().entrySet()) {
                    inv.deductIngredient(e.getKey(), e.getValue());
                }
            }
        }

        List<SalesRecord> salesList = new ArrayList<>();
        double subTotal = 0.0;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            String name = orderModel.getValueAt(i, 0).toString();
            int qty = Integer.parseInt(orderModel.getValueAt(i, 1).toString());
            double lineTotal = Double.parseDouble(orderModel.getValueAt(i, 2).toString());
            subTotal += lineTotal;
            salesList.add(new SalesRecord(name, qty, lineTotal / qty, lineTotal));
        }
        monitoring.addMultipleSales(salesList);

        double cash = Double.parseDouble(cashpayment.getText().replace("\u20B1", "").replace("P", "").trim());
        double change = Double.parseDouble(jTextFieldChange.getText().replace("\u20B1", "").replace("P", "").trim());

        String transactionRef = "TXN" + String.format("%06d", transactionCounter);
        try {
            if (orderController == null) orderController = new OrderController(new SQLiteSalesRepository());
            orderController.persistCompletedTransaction(transactionRef, salesList, subTotal, cash, change);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to save sales to database: " + e.getMessage(), "Database", JOptionPane.WARNING_MESSAGE);
        }

        String lineSep = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        String receiptStr = " ☕ Better Mondays Cafe ☕\n 123 Main St., Manila\n VAT REG TIN: 123-456-789\n" + lineSep
            + "Date: " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date()) + "\n"
            + "Txn #: TXN" + String.format("%06d", ++transactionCounter) + "\n" + lineSep
            + String.format("%-15s %3s %8s\n", "ITEM", "QTY", "AMOUNT") + "───────────────────────────────\n";
        for (SalesRecord s : salesList) {
            receiptStr += String.format("%-15s %3d %8.2f\n", truncate(s.getProductName(), 15), s.getQuantity(), s.getTotal());
        }
        receiptStr += "───────────────────────────────\n"
            + String.format("%-23s %8.2f\n", "Subtotal (excl VAT):", subTotal / 1.12)
            + String.format("%-23s %8.2f\n", "VAT (12%):", subTotal * 0.12 / 1.12)
            + String.format("%-23s %8.2f\n", "TOTAL (incl VAT):", subTotal)
            + String.format("%-23s %8.2f\n", "Cash:", cash)
            + String.format("%-23s %8.2f\n", "Change:", change) + lineSep
            + " Thank you! Come again!\n *** Have a nice day ***\n";

        JOptionPane.showMessageDialog(this, receiptStr, "✅ RECEIPT - TXN" + String.format("%06d", transactionCounter), JOptionPane.INFORMATION_MESSAGE);

        orderModel.setRowCount(0);
        jTextFieldChange.setText("");
        jTextFieldTax.setText("");
        jTextFieldTotal.setText("");
        jTextFieldSubTotal.setText("");
        cashpayment.setText("");

        loadInventoryTable();
        monitoring.loadLowStockIngredients();
    }

    private void InventoryEditActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit.");
            return;
        }
        Inventory inventory = Inventory.getInstance();
        String currentName = inventoryTable.getValueAt(selectedRow, 0).toString();
        InventoryItem item = inventory.getItem(currentName);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Error: Item not found in inventory.");
            return;
        }
        try {
            String newName = JOptionPane.showInputDialog(this, "Enter new name:", item.getName());
            if (newName == null || newName.trim().isEmpty()) return;
            newName = newName.trim();
            String quantityStr = JOptionPane.showInputDialog(this, "Enter new quantity:", item.getQuantity());
            String unit = JOptionPane.showInputDialog(this, "Enter new unit:", item.getUnit());
            String alertStr = JOptionPane.showInputDialog(this, "Enter new alert level:", item.getAlertLevel());
            if (inventoryController == null) inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.updateItem(currentName.trim(), newName, quantityStr, unit, alertStr);
            loadInventoryTable();
            monitoring.loadLowStockIngredients();
            JOptionPane.showMessageDialog(this, "Item updated successfully!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number entered.");
        }
    }

    private void InventoryRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to remove.");
            return;
        }
        String name = inventoryTable.getValueAt(selectedRow, 0).toString().trim();
        int confirm = JOptionPane.showConfirmDialog(this, "Remove " + name + " from inventory?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (inventoryController == null) inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.removeItem(name);
            loadInventoryTable();
        }
    }

    private void InventoryAddActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String name = JOptionPane.showInputDialog(this, "Enter item name:");
            if (name == null || name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item name cannot be empty.");
                return;
            }
            String quantityStr = JOptionPane.showInputDialog(this, "Enter quantity:");
            if (quantityStr == null || quantityStr.trim().isEmpty()) return;
            String unit = JOptionPane.showInputDialog(this, "Enter unit (e.g., kg, pcs):");
            if (unit == null || unit.trim().isEmpty()) return;
            String alertStr = JOptionPane.showInputDialog(this, "Enter alert level:");
            if (alertStr == null || alertStr.trim().isEmpty()) return;
            if (inventoryController == null) inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.addItem(name, quantityStr, unit, alertStr);
            loadInventoryTable();
            monitoring.loadLowStockIngredients();
            JOptionPane.showMessageDialog(this, "Item added successfully!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number entered. Please enter valid numeric values for quantity and alert level.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Variables declaration
    // ═════════════════════════════════════════════════════════════
    private javax.swing.JButton InventoryAdd;
    private javax.swing.JButton InventoryEdit;
    private javax.swing.JButton InventoryRemove;
    private javax.swing.JLabel JLabelTax;
    private javax.swing.JButton backbtn;
    private javax.swing.JTextField cashpayment;
    private javax.swing.JButton coffeemenu;
    private javax.swing.JButton exit;
    private javax.swing.JButton fruitteamenu;
    private javax.swing.JButton herbalteamenu;
    public javax.swing.JTable inventoryTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelSubTotal;
    private javax.swing.JLabel jLabelTotal;
    private javax.swing.JLabel jLabelTotal1;
    private javax.swing.JLabel jLabelTotal2;
    private javax.swing.JPanel jPanel4;
    public javax.swing.JPanel jPanelInventory;
    private javax.swing.JPanel jPanelMonitoring;
    private javax.swing.JPanel jPanelPOS;
    private javax.swing.JPanel jPanelSummary;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPaneI;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTableMonitoring;
    private javax.swing.JTable jTableSales;
    private javax.swing.JTextField jTextFieldChange;
    private javax.swing.JTextField jTextFieldSubTotal;
    private javax.swing.JTextField jTextFieldTax;
    private javax.swing.JTextField jTextFieldTotal;
    private javax.swing.JButton menu1;
    private javax.swing.JButton menu10;
    private javax.swing.JButton menu11;
    private javax.swing.JButton menu12;
    private javax.swing.JButton menu2;
    private javax.swing.JButton menu3;
    private javax.swing.JButton menu4;
    private javax.swing.JButton menu5;
    private javax.swing.JButton menu6;
    private javax.swing.JButton menu7;
    private javax.swing.JButton menu8;
    private javax.swing.JButton menu9;
    private javax.swing.JButton noncoffeemenu;
    private javax.swing.JButton pay;
    private javax.swing.JButton removeitem;
    private javax.swing.JButton resetorder;
}
