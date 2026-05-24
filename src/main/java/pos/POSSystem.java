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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import loginregister.Login;
import javax.swing.table.TableCellEditor;
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
import ui.CardPanel;
import ui.SidebarPanel;
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
    private JTextArea inventoryDetailArea;
    private JTextField inventorySearchField;
    private JComboBox<String> inventoryCategoryFilter;
    private final List<InventoryRowView> inventoryRowsCache = new ArrayList<>();
    private static final String INVENTORY_SEARCH_PLACEHOLDER = "Search ingredients";

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

        monitoring = new Monitoring(jTableMonitoring, jTableSales);
        monitoring.loadLowStockIngredients();

        AppTheme.applyToFrame(this);

        // OrderingPanel handles its own initial category state
    }

    @Deprecated
    public POSSystem() {
        this("unknown", UserDataManager.Role.STAFF);
    }

    private void logoutAndReturnToLogin() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out of the current session?",
                "Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
        dispose();
    }


    // ─── Category display (legacy, kept for compatibility) ──────
    private void showCategory(String category) {
        activeCategory = category;
        if (sidebarPanel != null) {
            for (java.awt.Component c : sidebarPanel.getComponents()) {
                if (c instanceof JButton b) {
                    boolean active = category.equals(b.getText());
                    b.setBackground(active ? new Color(50, 157, 111) : new Color(36, 55, 83));
                    b.setForeground(active ? Color.WHITE : new Color(245, 248, 252));
                }
            }
        }
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
        card.setBorder(ui.AppTheme.inputBorderRegular());

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
        AppTheme.styleSearchField(searchField);
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
        AppTheme.styleSearchField(cashField);
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
                    inv.deductIngredient(e.getKey(), e.getValue() * entry.quantity);
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
        jPanelPOS = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        sidebar = new SidebarPanel(currentUsername, currentUserRole, page -> {
            cardLayout.show(contentPanel, page);
        }, this::logoutAndReturnToLogin);
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(28, 43, 63));

        // ═══════════════════════════════════════════════════════════
        //  ORDERING TAB  (redesigned OrderingPanel)
        // ═══════════════════════════════════════════════════════════
        orderingPanel = new OrderingPanel(() -> {
            loadInventoryTable();
            if (monitoring != null) monitoring.loadLowStockIngredients();
        });
        orderingPanel.setBackground(new Color(28, 43, 63));
        contentPanel.add(orderingPanel, "Ordering");
        contentPanel.add(new SearchModule(), "Search");

        // ═══════════════════════════════════════════════════════════
        //  INVENTORY TAB - Card-on-Canvas Design
        // ═══════════════════════════════════════════════════════════
        Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
        Font SUB_FONT = new Font("Segoe UI", Font.PLAIN, 13);
        Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
        Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);
        Font CARD_NUM_FONT = new Font("Segoe UI", Font.BOLD, 24);
        Font CARD_LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

        jPanelInventory = new JPanel();
        jPanelInventory.setBackground(AppTheme.BG_PRIMARY);
        jPanelInventory.setLayout(new BorderLayout(0, 16));
        jPanelInventory.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        // ─── Top Header ─────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel pageTitle = new JLabel("Inventory Management");
        pageTitle.setFont(TITLE_FONT);
        pageTitle.setForeground(AppTheme.FG_PRIMARY);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MM/dd/yyyy");
        JLabel dateSubtitle = new JLabel("As of " + sdf.format(new Date()));
        dateSubtitle.setFont(SUB_FONT);
        dateSubtitle.setForeground(AppTheme.FG_MUTED);

        JPanel titleStack = new JPanel(new BorderLayout(0, 2));
        titleStack.setOpaque(false);
        titleStack.add(pageTitle, BorderLayout.NORTH);
        titleStack.add(dateSubtitle, BorderLayout.SOUTH);
        headerPanel.add(titleStack, BorderLayout.WEST);

        jPanelInventory.add(headerPanel, BorderLayout.NORTH);

        // ─── Summary Metric Cards ───────────────────────────
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        summaryPanel.setOpaque(false);

        Color[] cardTints = {AppTheme.ACCENT, new Color(230, 150, 40), new Color(200, 60, 60), new Color(80, 80, 80)};
        String[] cardLabels = {"Total Items", "Low Stock", "Expired", "Out of Stock"};
        String[] cardIcons = {"\u25A0", "\u23F0", "\u26A0", "\u25A1"};

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            CardPanel card = new CardPanel(16, AppTheme.BG_SURFACE);
            card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            card.setLayout(new BorderLayout(12, 0));

            JPanel iconBox = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(cardTints[idx]);
                    g2.fillRoundRect(0, 0, 44, 44, 10, 10);
                    g2.dispose();
                }
            };
            iconBox.setPreferredSize(new Dimension(44, 44));
            iconBox.setOpaque(false);

            JLabel iconLabel = new JLabel(cardIcons[idx], SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            iconLabel.setForeground(Color.WHITE);
            iconBox.setLayout(new BorderLayout());
            iconBox.add(iconLabel, BorderLayout.CENTER);

            JPanel textStack = new JPanel(new BorderLayout(0, 2));
            textStack.setOpaque(false);
            JLabel countLabel = new JLabel("0");
            countLabel.setFont(CARD_NUM_FONT);
            countLabel.setForeground(AppTheme.FG_PRIMARY);
            countLabel.setName("summaryCount_" + i);

            JLabel descLabel = new JLabel(cardLabels[i]);
            descLabel.setFont(CARD_LABEL_FONT);
            descLabel.setForeground(AppTheme.FG_MUTED);

            textStack.add(countLabel, BorderLayout.NORTH);
            textStack.add(descLabel, BorderLayout.SOUTH);

            card.add(iconBox, BorderLayout.WEST);
            card.add(textStack, BorderLayout.CENTER);
            summaryPanel.add(card);
        }

        jPanelInventory.add(summaryPanel, BorderLayout.NORTH);

        // ─── Main Table Card ────────────────────────────────
        CardPanel mainCard = new CardPanel(16, AppTheme.BG_SURFACE);
        mainCard.setLayout(new BorderLayout(0, 12));
        mainCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        // Controls row
        JPanel controlsPanel = new JPanel(new BorderLayout(12, 0));
        controlsPanel.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftControls.setOpaque(false);

        inventorySearchField = new JTextField(18);
        AppTheme.styleSearchField(inventorySearchField);
        // UX: placeholder and tooltip so users know the field is interactive
        inventorySearchField.setText(INVENTORY_SEARCH_PLACEHOLDER);
        inventorySearchField.setToolTipText("Type to filter ingredients (press Esc to clear)");
        inventorySearchField.putClientProperty("JTextField.roundPlaceholder", true);
        inventorySearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (isInventorySearchPlaceholderVisible()) {
                    inventorySearchField.setText("");
                }
                inventorySearchField.setBorder(ui.AppTheme.focusInputBorder(new Color(90, 140, 190)));
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (inventorySearchField.getText().isBlank()) {
                    inventorySearchField.setText(INVENTORY_SEARCH_PLACEHOLDER);
                }
                inventorySearchField.setBorder(ui.AppTheme.inputBorderRegular());
            }
        });
        inventorySearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilters(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilters(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyInventoryFilters(); }
        });

        JLabel searchIconLabel = new JLabel("\uD83D\uDD0D");
        searchIconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIconLabel.setForeground(AppTheme.FG_MUTED);

        JPanel searchPill = new JPanel(new BorderLayout(6, 0));
        searchPill.setOpaque(false);
        searchPill.setBorder(ui.AppTheme.inputBorderPill());
        searchPill.add(searchIconLabel, BorderLayout.WEST);
        searchPill.add(inventorySearchField, BorderLayout.CENTER);
        leftControls.add(searchPill);

        inventoryCategoryFilter = new JComboBox<>(new String[] {
            "All",
            "Coffee",
            "Non-Coffee",
            "Fruit Tea",
            "Herbal Tea",
            "Food"
        });
        inventoryCategoryFilter.setFont(BODY_FONT);
        inventoryCategoryFilter.addActionListener(e -> applyInventoryFilters());
        leftControls.add(inventoryCategoryFilter);

        JButton filterBtn = new JButton("\u2699 Filter");
        filterBtn.setFont(BODY_FONT);
        filterBtn.setForeground(AppTheme.FG_PRIMARY);
        filterBtn.setBackground(AppTheme.BG_SURFACE);
        filterBtn.setBorder(ui.AppTheme.inputBorderRegular());
        filterBtn.setFocusPainted(false);
        filterBtn.addActionListener(e -> applyInventoryFilters());
        leftControls.add(filterBtn);

        controlsPanel.add(leftControls, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightControls.setOpaque(false);

        JButton addBtn = new JButton("+ Add Item");
        addBtn.setFont(BOLD_FONT);
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(AppTheme.ACCENT);
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> InventoryAddActionPerformed(null));
        rightControls.add(addBtn);

        controlsPanel.add(rightControls, BorderLayout.EAST);

        mainCard.add(controlsPanel, BorderLayout.NORTH);

        JPanel tableAndDetail = new JPanel(new BorderLayout(16, 0));
        tableAndDetail.setOpaque(false);

        // Table
        jScrollPane3 = new javax.swing.JScrollPane();
        jScrollPane3.setBorder(null);
        jScrollPane3.getViewport().setBackground(AppTheme.BG_SURFACE);

        inventoryTable = new javax.swing.JTable();
        inventoryTable.setModel(new DefaultTableModel(
            new Object[][]{},
            new String[]{"Item Name", "Quantity", "Last Updated", "Status", "Actions"}
        ) {
            boolean[] canEdit = new boolean[]{false, false, false, false, false};
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        AppTheme.applyTableDefaults(inventoryTable);
        inventoryTable.setShowHorizontalLines(true);
        inventoryTable.setShowVerticalLines(false);
        inventoryTable.setRowMargin(4);
        inventoryTable.getTableHeader().setReorderingAllowed(false);

        // Column widths (storage & used-in removed)
        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < 5; i++) {
            inventoryTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Actions column: three-dot button (now at index 4)
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(new ActionsCellRenderer());
        inventoryTable.getColumnModel().getColumn(4).setCellEditor(new ActionsCellEditor());

        // Status column: colored badge renderer (now at index 3)
        inventoryTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());

        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateInventoryDetailPanel();
            }
        });

        jScrollPane3.setViewportView(inventoryTable);

        CardPanel detailCard = new CardPanel(16, AppTheme.BG_PRIMARY);
        detailCard.setLayout(new BorderLayout(0, 10));
        detailCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        detailCard.setPreferredSize(new Dimension(330, 0));

        JLabel detailTitle = new JLabel("Ingredient Details");
        detailTitle.setFont(BOLD_FONT);
        detailTitle.setForeground(AppTheme.FG_PRIMARY);
        detailCard.add(detailTitle, BorderLayout.NORTH);

        inventoryDetailArea = new JTextArea("Select an ingredient to see its full stock and menu usage.");
        inventoryDetailArea.setEditable(false);
        inventoryDetailArea.setLineWrap(true);
        inventoryDetailArea.setWrapStyleWord(true);
        inventoryDetailArea.setFont(BODY_FONT);
        inventoryDetailArea.setForeground(AppTheme.FG_PRIMARY);
        inventoryDetailArea.setBackground(AppTheme.BG_PRIMARY);
        inventoryDetailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane detailScroll = new JScrollPane(inventoryDetailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 85, 120), 1, true));
        detailScroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        detailCard.add(detailScroll, BorderLayout.CENTER);

        tableAndDetail.add(jScrollPane3, BorderLayout.CENTER);
        tableAndDetail.add(detailCard, BorderLayout.EAST);
        mainCard.add(tableAndDetail, BorderLayout.CENTER);

        jPanelInventory.add(mainCard, BorderLayout.CENTER);
        contentPanel.add(jPanelInventory, "Inventory");

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
        AppTheme.applyTableDefaults(jTableSales);
        jScrollPane4 = new javax.swing.JScrollPane(jTableSales);

        jTableMonitoring = new javax.swing.JTable();
        jTableMonitoring.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] { {null, null, null, null, null} },
            new String[] { "Ingredient", "Quantity", "Unit", "Alert Level", "Status" }
        ));
        AppTheme.applyTableDefaults(jTableMonitoring);
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
        contentPanel.add(jPanelMonitoring, "Monitoring");

        // Other tabs
        try {
            contentPanel.add(new MenuMaintenancePanel(), "Menu Maintenance");
        } catch (Exception e) { System.err.println("MenuMaintenancePanel init failed: " + e.getMessage()); }
        try {
                contentPanel.add(new InventoryRegistrationPanel(
                    new SQLiteInventoryRepository(),
                    this::loadInventoryTable,
                    () -> {
                        if (monitoring != null) {
                            monitoring.loadLowStockIngredients();
                        }
                    }), "Register Product");
        } catch (Exception e) { System.err.println("InventoryRegistrationPanel init failed: " + e.getMessage()); }
        try {
            contentPanel.add(new StaffPanel(new SQLiteStaffShiftRepository(), new SQLiteUserRepository(), currentUsername, currentUserRole), "Staff");
        } catch (Exception e) { System.err.println("StaffPanel init failed: " + e.getMessage()); }
        contentPanel.add(new InventoryGuidePanel(), "Inventory Guide");
        contentPanel.add(new AboutModule(), "About");
        contentPanel.add(new HelpModule(), "Help");

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(contentPanel, BorderLayout.CENTER);

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
        if (inventoryController == null) {
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        }
        inventoryRowsCache.clear();
        inventoryRowsCache.addAll(inventoryController.buildInventoryRows());
        updateInventorySummaryCards();
        applyInventoryFilters();
    }

    private void applyInventoryFilters() {
        if (inventoryTable == null) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) inventoryTable.getModel();
        model.setRowCount(0);

        String query = getInventorySearchQuery();
        String selectedCategory = inventoryCategoryFilter == null || inventoryCategoryFilter.getSelectedItem() == null
                ? "All"
                : inventoryCategoryFilter.getSelectedItem().toString();

        for (InventoryRowView row : inventoryRowsCache) {
            if (!matchesInventoryFilters(row, query, selectedCategory)) {
                continue;
            }
            String qtyDisplay = row.getQuantity() + " " + row.getUnit();
            model.addRow(new Object[]{row.getName(), qtyDisplay, row.getLastUpdated(), row.getStatus(), "..."});
        }

        if (model.getRowCount() > 0) {
            inventoryTable.setRowSelectionInterval(0, 0);
        } else if (inventoryDetailArea != null) {
            inventoryDetailArea.setText("No ingredients match the current search and category filter.");
        }
    }

    private boolean matchesInventoryFilters(InventoryRowView row, String query, String selectedCategory) {
        boolean categoryMatch = "All".equalsIgnoreCase(selectedCategory)
                || containsToken(row.getCategories(), selectedCategory);

        if (!categoryMatch) {
            return false;
        }

        if (query == null || query.isEmpty()) {
            return true;
        }

        String haystack = String.join(" ",
            row.getName(),
            row.getUnit(),
            row.getStatus(),
            safeText(row.getCategories()),
            safeText(row.getLastUpdated())).toLowerCase();
        return haystack.contains(query);
    }

    private String getInventorySearchQuery() {
        if (inventorySearchField == null) {
            return "";
        }
        String query = inventorySearchField.getText() == null ? "" : inventorySearchField.getText().trim();
        if (query.isEmpty() || INVENTORY_SEARCH_PLACEHOLDER.equalsIgnoreCase(query)) {
            return "";
        }
        return query.toLowerCase();
    }

    private boolean isInventorySearchPlaceholderVisible() {
        return inventorySearchField != null
                && INVENTORY_SEARCH_PLACEHOLDER.equalsIgnoreCase(
                        inventorySearchField.getText() == null ? "" : inventorySearchField.getText().trim());
    }

    private boolean containsToken(String csv, String token) {
        if (csv == null || csv.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        for (String part : csv.split(",")) {
            if (part.trim().equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void updateInventorySummaryCards() {
        int totalItems = 0, lowStock = 0, expired = 0, outOfStock = 0;
        for (InventoryRowView row : inventoryRowsCache) {
            totalItems++;
            switch (row.getStatus()) {
                case "Low Stock" -> lowStock++;
                case "Out of Stock" -> outOfStock++;
                case "Expired" -> expired++;
            }
        }
        // Update summary cards
        Component[] summaryParent = jPanelInventory.getComponents();
        for (Component comp : summaryParent) {
            if (comp instanceof JPanel) {
                for (Component card : ((JPanel) comp).getComponents()) {
                    if (card instanceof CardPanel) {
                        for (Component c2 : ((CardPanel) card).getComponents()) {
                            if (c2 instanceof JPanel) {
                                for (Component c3 : ((JPanel) c2).getComponents()) {
                                    if (c3 instanceof JLabel && "summaryCount_0".equals(c3.getName())) {
                                        ((JLabel) c3).setText(String.valueOf(totalItems));
                                    } else if (c3 instanceof JLabel && "summaryCount_1".equals(c3.getName())) {
                                        ((JLabel) c3).setText(String.valueOf(lowStock));
                                    } else if (c3 instanceof JLabel && "summaryCount_2".equals(c3.getName())) {
                                        ((JLabel) c3).setText(String.valueOf(expired));
                                    } else if (c3 instanceof JLabel && "summaryCount_3".equals(c3.getName())) {
                                        ((JLabel) c3).setText(String.valueOf(outOfStock));
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                int qty = Integer.parseInt(orderModel.getValueAt(i, 1).toString());
                for (Map.Entry<String, Double> e : menuItem.getIngredients().entrySet()) {
                    inv.deductIngredient(e.getKey(), e.getValue() * qty);
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

    private void filterInventoryTable(String query) {
        if (inventoryTable == null) return;
        DefaultTableModel model = (DefaultTableModel) inventoryTable.getModel();
        model.setRowCount(0);
        if (inventoryController == null) {
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        }
        List<InventoryRowView> rows = inventoryController.buildInventoryRows();
        for (InventoryRowView row : rows) {
            if (query == null || query.trim().isEmpty()
                    || row.getName().toLowerCase().contains(query.toLowerCase())) {
                String qtyDisplay = row.getQuantity() + " " + row.getUnit();
                model.addRow(new Object[]{row.getName(), qtyDisplay, row.getLastUpdated(), row.getStatus(), "..."});
            }
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel label) {
                String text = value == null ? "" : value.toString();
                label.setText(text);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (isSelected) {
                    return label;
                }
                switch (text) {
                    case "Good" -> label.setForeground(new Color(40, 167, 69));
                    case "Low Stock" -> label.setForeground(new Color(200, 160, 40));
                    case "Out of Stock" -> label.setForeground(new Color(230, 130, 50));
                    case "Expired" -> label.setForeground(new Color(200, 50, 50));
                    default -> label.setForeground(new Color(120, 120, 120));
                }
            }
            return c;
        }
    }

    private static class ActionsCellRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = new JLabel("...", SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 16));
            label.setForeground(new Color(150, 150, 150));
            label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            return label;
        }
    }

    private class ActionsCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new BorderLayout());
        private final JLabel dotsLabel = new JLabel("...", SwingConstants.CENTER);
        private int editingRow;

        ActionsCellEditor() {
            dotsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            dotsLabel.setForeground(new Color(150, 150, 150));
            dotsLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            panel.add(dotsLabel, BorderLayout.CENTER);
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (editingRow < 0) return;
                    String itemName = inventoryTable.getValueAt(editingRow, 0).toString();
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    JMenuItem changeStatus = new JMenuItem("Change Status");
                    deleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    changeStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    deleteItem.addActionListener(ev -> {
                        int confirm = JOptionPane.showConfirmDialog(POSSystem.this,
                                "Remove " + itemName + " from inventory?", "Confirm",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (inventoryController == null)
                                inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
                            inventoryController.removeItem(itemName);
                            loadInventoryTable();
                        }
                    });
                    changeStatus.addActionListener(ev -> {

                if (inventoryTable.getRowCount() > 0) {
                    inventoryTable.setRowSelectionInterval(0, 0);
                } else {
                    updateInventoryDetailPanel();
                }
                        String newStatus = JOptionPane.showInputDialog(POSSystem.this,
                                "Change status for " + itemName + " (Good, Low Stock, Out of Stock, Expired):");
                        if (newStatus != null && !newStatus.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(POSSystem.this, "Status updated for " + itemName);
                        }
                    });
                    menu.add(deleteItem);
                    menu.add(changeStatus);
                    menu.show(panel, e.getX(), e.getY());
                }
            });
        }

        @Override
        public Object getCellEditorValue() { return "..."; }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            editingRow = row;
            return panel;
        }
    }

    private void updateInventoryDetailPanel() {
        if (inventoryDetailArea == null || inventoryController == null || inventoryTable == null) {
            return;
        }

        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            inventoryDetailArea.setText("Select an ingredient to see its full stock and menu usage.");
            return;
        }

        String itemName = String.valueOf(inventoryTable.getValueAt(selectedRow, 0));
        List<InventoryRowView> rows = inventoryController.buildInventoryRows();
        for (InventoryRowView row : rows) {
            if (!row.getName().equals(itemName)) {
                continue;
            }

            StringBuilder details = new StringBuilder();
            details.append("Name: ").append(row.getName()).append('\n');
            details.append("Quantity: ").append(row.getQuantity()).append(' ').append(row.getUnit()).append('\n');
            details.append("Alert Level: ").append(row.getAlertLevel()).append(' ').append(row.getUnit()).append('\n');
            details.append("Status: ").append(row.getStatus()).append('\n');
            details.append("Updated: ").append(row.getLastUpdated() == null || row.getLastUpdated().isBlank() ? "N/A" : row.getLastUpdated()).append('\n');
                details.append("Categories: ").append(row.getCategories() == null || row.getCategories().isBlank() ? "N/A" : row.getCategories()).append('\n');
            inventoryDetailArea.setText(details.toString());
            inventoryDetailArea.setCaretPosition(0);
            return;
        }

        inventoryDetailArea.setText("No detail record found for the selected ingredient.");
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
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private SidebarPanel sidebar;
    private JPanel orderingPanel;
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
