package pos;

import inventory.Inventory;
import inventory.InventoryItem;
import pos.Menu;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.LineBorder;
import ui.MonitoringPanel;
import java.util.List;
import java.util.ArrayList;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import persistence.sqlite.SQLiteProfilePictureRepository;
import controller.InventoryController;
import controller.InventoryRowView;
import controller.OrderController;
import inventory.InventoryItem;
import inventory.analytics.InventoryPolicyService;

public class POSSystem extends javax.swing.JFrame {

    // Zebra-striping colors — kept consistent with the Register Product batch table
    private static final Color INVENTORY_ROW_ALT = new Color(0xF3F4F6);
    private static final Color INVENTORY_SELECTION_BG = new Color(0x1A3A5C);

    private MonitoringPanel monitoringPanel;
    private InventoryRegistrationPanel inventoryRegistrationPanel;
    private UserDataManager.Role currentUserRole;
    private String currentUsername;
    private InventoryController inventoryController;
    private OrderController orderController;

    private static final List<String> ORDERING_CATEGORIES = List.of(
            "Espresso & Coffee",
            "Specialty Drinks",
            "Tea Latte",
            "Non-Coffee",
            "House Favorites",
            "Fruit Tea",
            "Herbal Tea",
            "Sandwiches",
            "Pandesal Pairs",
            "Pastries");

    private final Map<String, List<String>> categoryItems;

    private static final Map<String, Double> HOUSE_FAVORITES_PRICES = Map.of(
            "Mango Latte", 210.0,
            "Strawberry Latte", 200.0,
            "Salted Cream Latte", 190.0,
            "Spanish Latte", 180.0);

    private static final List<String> SPECIALTY_DRINKS = List.of(
            "Vietnamese Coffee",
            "Ube Espresso",
            "Manila Latte",
            "Pumpkin Spice Latte",
            "Spiced Cookie Latte");

    private static final List<String> TEA_LATTE_ITEMS = List.of(
            "Matcha Latte",
            "Chocolate Matcha",
            "Matcha Espresso",
            "Hojicha Latte",
            "Chai Latte");

    private static final List<String> NON_COFFEE_ITEMS = List.of(
            "Chocolate Latte",
            "Strawberry Latte",
            "Mango Latte",
            "Dragon Fruit Coconut Latte",
            "Ube Latte");

    private static final List<String> FRUIT_TEA_ITEMS = List.of(
            "Strawberry Green Tea",
            "Mango Green Tea",
            "Peach Green Tea",
            "Passion Fruit Green Tea");

    private static final List<String> HERBAL_TEA_ITEMS = List.of(
            "Peppermint",
            "Chamomile",
            "Earl Grey",
            "Cinnamon");

    private static final List<String> SANDWICH_ITEMS = List.of(
            "Signature Ham & Cheese",
            "Classic Grilled Cheese",
            "Homestyle Pesto & Cheese");

    private static final List<String> PANDESAL_PAIR_ITEMS = List.of(
            "Ham & Cheese",
            "Cheesy Pesto",
            "Spam & Cheese");

    private static final List<String> PASTRY_ITEMS = List.of(
            "Chocolate Crinkles",
            "Chocolate Cookies",
            "Brownies",
            "Banana Bread",
            "Chocolate Tiramisu",
            "Matcha Tiramisu",
            "Creamy Spinach",
            "Blueberry Cheesecake");

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
        try {
            Phase2Bootstrap.seedCatalogIfEmpty();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database initialization warning: " + e.getMessage(),
                    "Database",
                    JOptionPane.WARNING_MESSAGE);
        }

        categoryItems = buildCategoryItems();
        initComponents();
        setTitle("Better Mondays Coffeee Cafe Management System - " + username);
        setResizable(true);
        setMinimumSize(new java.awt.Dimension(1100, 700));
        setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        loadInventoryTable();

        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        orderController = new OrderController(new SQLiteSalesRepository());

        monitoringPanel = new MonitoringPanel();

        AppTheme.applyToFrame(this);

        // Re-apply ordering category pill styles after the global theme
        // because `AppTheme.applyToFrame` overrides JButton backgrounds.
        if (orderingPanel != null)
            orderingPanel.refreshCategoryPills();

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

        // ── Auto-end shift on logout ───────────────────────────────────────
        try {
            persistence.StaffShiftRepository shiftRepo = new persistence.sqlite.SQLiteStaffShiftRepository();
            shiftRepo.endShift(currentUsername, "Auto-ended on logout");
        } catch (Exception e) {
            // Non-fatal: log but don't block logout
            java.util.logging.Logger.getLogger(POSSystem.class.getName())
                    .log(java.util.logging.Level.WARNING,
                            "Could not auto-end shift for " + currentUsername, e);
            JOptionPane.showMessageDialog(this,
                    "Logged out, but shift could not be ended automatically.\n" + e.getMessage(),
                    "Shift Warning", JOptionPane.WARNING_MESSAGE);
        }
        // ──────────────────────────────────────────────────────────────────

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
        List<String> itemNames = categoryItems.getOrDefault(category, List.of());

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
            JPanel emptyRow = new JPanel(new BorderLayout());
            emptyRow.setOpaque(false);
            emptyRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            JLabel emptyLabel = new JLabel("No items found", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            emptyLabel.setForeground(new Color(197, 209, 224));
            emptyRow.add(emptyLabel, BorderLayout.CENTER);
            productGridPanel.add(emptyRow);
        } else {
            for (int index = 0; index < items.size(); index += 3) {
                JPanel rowPanel = new JPanel(new GridLayout(1, 3, 14, 0));
                rowPanel.setOpaque(false);
                rowPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

                int endIndex = Math.min(index + 3, items.size());
                for (int itemIndex = index; itemIndex < endIndex; itemIndex++) {
                    rowPanel.add(createProductCard(items.get(itemIndex), category));
                }

                for (int filler = endIndex - index; filler < 3; filler++) {
                    JPanel emptyCell = new JPanel();
                    emptyCell.setOpaque(false);
                    rowPanel.add(emptyCell);
                }

                productGridPanel.add(rowPanel);
            }
        }

        productGridPanel.revalidate();
        productGridPanel.repaint();
    }

    private JPanel createProductCard(MenuItem item, String category) {
        CardPanel card = new CardPanel(12, ui.AppTheme.BG_SURFACE);
        card.setLayout(new BorderLayout(0, 6));
        card.setFillColor(ui.AppTheme.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        card.setBorderColor(AppTheme.BORDER);
        card.setPreferredSize(new Dimension(188, 184));
        card.setMinimumSize(new Dimension(188, 184));
        card.setMaximumSize(new Dimension(188, 184));

        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(56, 46));
        imgLabel.setOpaque(true);
        imgLabel.setBackground(AppTheme.BG_SURFACE);
        card.add(imgLabel, BorderLayout.NORTH);

        double displayPrice = pickDisplayPrice(item, category);
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.setPreferredSize(new Dimension(160, 66));
        textPanel.setMinimumSize(new Dimension(160, 66));
        textPanel.setMaximumSize(new Dimension(160, 66));

        JLabel nameLabel = new JLabel(
                "<html><div style='width:164px;text-align:center;line-height:1.15;'><b style='color:#F5F8FC;font-size:11px;'>"
                        + item.getName()
                        + "</b><br><span style='color:#32C075;font-size:10px;'>\u20B1"
                        + String.format("%.2f", displayPrice) + "</span></div></html>",
                SwingConstants.CENTER);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nameLabel.setVerticalAlignment(SwingConstants.CENTER);
        textPanel.add(nameLabel, BorderLayout.CENTER);
        card.add(textPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 0, 4));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        btnPanel.setPreferredSize(new Dimension(160, 74));
        btnPanel.setMinimumSize(new Dimension(160, 74));
        btnPanel.setMaximumSize(new Dimension(160, 74));
        addButtonsForCategory(btnPanel, item, category);
        while (btnPanel.getComponentCount() < 3) {
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            filler.setPreferredSize(new Dimension(1, 24));
            btnPanel.add(filler);
        }
        card.add(btnPanel, BorderLayout.SOUTH);
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

    private Map<String, List<String>> buildCategoryItems() {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        for (String category : ORDERING_CATEGORIES) {
            categories.put(category, new ArrayList<>());
        }

        for (MenuItem item : Menu.getInstance().getAllItems().values()) {
            String category = resolveOrderingCategory(item);
            if (category == null) {
                continue;
            }
            categories.computeIfAbsent(category, ignored -> new ArrayList<>()).add(item.getName());
        }

        return categories;
    }

    private String resolveOrderingCategory(MenuItem item) {
        String itemName = item.getName();
        if (itemName == null) {
            return null;
        }

        if (SPECIALTY_DRINKS.contains(itemName)) {
            return "Specialty Drinks";
        }
        if (TEA_LATTE_ITEMS.contains(itemName)) {
            return "Tea Latte";
        }
        if (NON_COFFEE_ITEMS.contains(itemName)) {
            return "Non-Coffee";
        }
        if (FRUIT_TEA_ITEMS.contains(itemName)) {
            return "Fruit Tea";
        }
        if (HERBAL_TEA_ITEMS.contains(itemName)) {
            return "Herbal Tea";
        }
        if (SANDWICH_ITEMS.contains(itemName)) {
            return "Sandwiches";
        }
        if (PANDESAL_PAIR_ITEMS.contains(itemName)) {
            return "Pandesal Pairs";
        }
        if (PASTRY_ITEMS.contains(itemName)) {
            return "Pastries";
        }
        if (HOUSE_FAVORITES_PRICES.containsKey(itemName)) {
            return "House Favorites";
        }

        if ("Coffee".equals(item.getCategory())) {
            return "Espresso & Coffee";
        }

        return null;
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
        if (price <= 0)
            return;
        JButton btn = new JButton(text);
        styleProdBtn(btn);
        btn.addActionListener(e -> addOrderItem(itemName, "", price));
        panel.add(btn);
    }

    private void addItemBtn(JPanel panel, String text, double price, String variant, String itemName) {
        if (price <= 0)
            return;
        JButton btn = new JButton(text);
        styleProdBtn(btn);
        btn.addActionListener(e -> addOrderItem(itemName, variant, price));
        panel.add(btn);
    }

    private void styleProdBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        btn.setBackground(new Color(50, 157, 111));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(144, 20));
        btn.setMaximumSize(new Dimension(144, 20));
        btn.setMinimumSize(new Dimension(144, 20));
        btn.setMargin(new Insets(0, 6, 0, 6));
        btn.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void styleSearchField() {
        AppTheme.styleSearchField(searchField);
    }

    private void addOrderItem(String name, String variant, double price) {
        if (name == null || name.isEmpty())
            return;
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
        itemsPanel.setBackground(AppTheme.BG_PRIMARY);
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
        row.setBackground(AppTheme.BG_SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel nameLabel = new JLabel(entry.displayName());
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        nameLabel.setForeground(new Color(245, 248, 252));
        row.add(nameLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        rightPanel.setOpaque(false);

        JButton minusBtn = new JButton("-");
        minusBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        minusBtn.setBackground(AppTheme.BG_BADGE_BLUE);
        minusBtn.setForeground(AppTheme.FG_PRIMARY);
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
        plusBtn.setBackground(AppTheme.ACCENT);
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
        receiptPanel.setBackground(AppTheme.BG_SURFACE);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.BORDER, ui.AppTheme.BORDER_THICKNESS, ui.AppTheme.BORDER_RADIUS),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        double totalInclusive = 0;
        for (OrderEntry entry : orderEntries) {
            totalInclusive += entry.lineTotal();
        }
        double subTotalExVat = totalInclusive / 1.12;
        double vat = totalInclusive - subTotalExVat;

        JLabel orderNumLabel = new JLabel("Order #" + (orderCount + 1));
        orderNumLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        orderNumLabel.setForeground(new Color(245, 248, 252));
        orderNumLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(orderNumLabel);

        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        clearBtn.setBackground(new Color(180, 60, 60));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        clearBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            orderEntries.clear();
            refreshOrderDisplay();
        });
        receiptPanel.add(clearBtn);
        receiptPanel.add(Box.createVerticalStrut(4));

        receiptPanel.add(new JScrollPane(orderPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        receiptPanel.add(Box.createVerticalStrut(4));

        JLabel line = new JLabel("─────────────────────");
        line.setForeground(new Color(100, 130, 160));
        line.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(line);

        receiptPanel.add(makeReceiptRow("Subtotal:", String.format("\u20B1%.2f", subTotalExVat)));
        receiptPanel.add(makeReceiptRow("Tax (12%):", String.format("\u20B1%.2f", vat)));
        receiptPanel.add(makeReceiptRow("Total:", String.format("\u20B1%.2f", totalInclusive)));

        receiptPanel.add(Box.createVerticalStrut(3));
        receiptPanel.add(makeReceiptRow("Cash:", ""));

        JTextField cashField = new JTextField(10);
        cashField.setHorizontalAlignment(JTextField.RIGHT);
        AppTheme.styleSearchField(cashField);
        cashField.setMaximumSize(new Dimension(110, 24));
        cashField.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        receiptPanel.add(cashField);

        receiptPanel.add(Box.createVerticalStrut(1));
        receiptPanel.add(makeReceiptRow("Change:", "\u20B10.00"));

        receiptPanel.add(Box.createVerticalStrut(4));

        final double fSubTotalExVat = subTotalExVat;
        final double fVat = vat;
        final double fTotal = totalInclusive;

        JButton printBtn = new JButton("Print Bills");
        printBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        printBtn.setBackground(new Color(50, 157, 111));
        printBtn.setForeground(Color.WHITE);
        printBtn.setFocusPainted(false);
        printBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        printBtn.setMaximumSize(new Dimension(180, 32));
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
        row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(197, 209, 224));
        row.add(lbl, BorderLayout.WEST);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 11));
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

        monitoringPanel.refreshData();
        orderCount++;
        orderEntries.clear();
        refreshOrderDisplay();
        loadInventoryTable();
        monitoringPanel.refreshData();
    }

    // ─── initComponents (replaces GUI builder code) ─────────────
    private void initComponents() {
        jPanelPOS = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        sidebar = new SidebarPanel(currentUsername, currentUserRole, page -> {
            cardLayout.show(contentPanel, page);
        }, this::logoutAndReturnToLogin, newUsername -> {
            currentUsername = newUsername;
            setTitle("Better Mondays Coffeee Cafe Management System - " + newUsername);
            if (sidebar != null) {
                sidebar.setUsername(newUsername);
            }
        });
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_PRIMARY);

        // ═══════════════════════════════════════════════════════════
        // ORDERING TAB (redesigned OrderingPanel)
        // ═══════════════════════════════════════════════════════════
        orderingPanel = new OrderingPanel(() -> {
            loadInventoryTable();
            if (monitoringPanel != null)
                monitoringPanel.refreshData();
        });
        orderingPanel.setBackground(AppTheme.BG_PRIMARY);
        contentPanel.add(orderingPanel, "Ordering");
        contentPanel.add(new SearchModule(), "Search");

        Menu.getInstance().addChangeListener(() -> javax.swing.SwingUtilities.invokeLater(() -> {
            if (orderingPanel != null) {
                orderingPanel.refreshCategoryPills();
                orderingPanel.rebuildProducts();
            }
        }));

        // ═══════════════════════════════════════════════════════════
        // INVENTORY TAB - Card-on-Canvas Design
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

        JLabel dateSubtitle = new JLabel();
        dateSubtitle.setFont(SUB_FONT);
        dateSubtitle.setForeground(AppTheme.FG_MUTED);

        new javax.swing.Timer(1000, e -> {
            dateSubtitle.setText("As of " + new SimpleDateFormat("EEEE, MM/dd/yyyy hh:mm:ss a").format(new Date()));
        }).start();

        JPanel titleStack = new JPanel(new BorderLayout(0, 2));
        titleStack.setOpaque(false);
        titleStack.add(pageTitle, BorderLayout.NORTH);
        titleStack.add(dateSubtitle, BorderLayout.SOUTH);
        headerPanel.add(titleStack, BorderLayout.WEST);

        jPanelInventory.add(headerPanel, BorderLayout.NORTH);

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
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyInventoryFilters();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyInventoryFilters();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyInventoryFilters();
            }
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

        inventoryTable = new javax.swing.JTable() {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? AppTheme.BG_SURFACE : INVENTORY_ROW_ALT);
                return c;
            }
        };
        inventoryTable.setModel(new DefaultTableModel(
                new Object[][] {},
                new String[] { "Item Name", "Quantity", "Alert Level", "ABC", "Reorder Guide", "Batches", "Status", "Actions" }) {
            boolean[] canEdit = new boolean[] { false, false, false, false, false, false, false, true };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        AppTheme.applyTableDefaults(inventoryTable);
        inventoryTable.setShowGrid(false);
        inventoryTable.setIntercellSpacing(new Dimension(0, 0));
        inventoryTable.setSelectionBackground(INVENTORY_SELECTION_BG);
        inventoryTable.setSelectionForeground(AppTheme.FG_PRIMARY);
        inventoryTable.getTableHeader().setReorderingAllowed(false);

        // Column widths: Name | Quantity | Alert Level | ABC | Reorder Guide |
        // Batches | Status | Actions
        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(170);
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(130);
        inventoryTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < 8; i++) {
            inventoryTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // ABC column at index 3 — colored badge by tier
        inventoryTable.getColumnModel().getColumn(3).setCellRenderer(new AbcTierRenderer());

        // Reorder Guide column at index 4 — EOQ/ROP at a glance, ROP highlighted
        // in red once stock has reached or fallen below it (time to reorder).
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(new ReorderGuideRenderer());

        // Batches column at index 5
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new BatchSummaryRenderer());

        // Actions column at index 7
        inventoryTable.getColumnModel().getColumn(7).setCellRenderer(new ActionsCellRenderer());
        inventoryTable.getColumnModel().getColumn(7).setCellEditor(new ActionsCellEditor());

        // Single-click: col 5 opens batch modal, col 7 opens actions menu
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int col = inventoryTable.columnAtPoint(e.getPoint());
                int row = inventoryTable.rowAtPoint(e.getPoint());
                if (row < 0)
                    return;
                if (col == 5) {
                    Object cellVal = inventoryTable.getModel().getValueAt(row, 5);
                    if (!(cellVal instanceof controller.InventoryRowView rv))
                        return;
                    if (inventoryController == null)
                        return;
                    new ui.InventoryBatchModal(POSSystem.this, rv.getName(),
                            inventoryController, () -> {
                                loadInventoryTable();
                                if (inventoryRegistrationPanel != null) {
                                    inventoryRegistrationPanel.refreshFromRepository();
                                }
                            }).setVisible(true);
                } else if (col == 7) {
                    inventoryTable.editCellAt(row, col);
                }
            }
        });

        // Status column: colored badge renderer at index 6
        inventoryTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

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
        detailScroll.setBorder(
                new ui.RoundedLineBorder(AppTheme.BORDER, ui.AppTheme.BORDER_THICKNESS, ui.AppTheme.BORDER_RADIUS));
        detailScroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        detailCard.add(detailScroll, BorderLayout.CENTER);

        tableAndDetail.add(jScrollPane3, BorderLayout.CENTER);
        tableAndDetail.add(detailCard, BorderLayout.EAST);
        mainCard.add(tableAndDetail, BorderLayout.CENTER);

        jPanelInventory.add(mainCard, BorderLayout.CENTER);
        contentPanel.add(jPanelInventory, "Inventory");

        // ═══════════════════════════════════════════════════════════
        // MONITORING TAB (redesigned MonitoringPanel)
        // ═══════════════════════════════════════════════════════════
        monitoringPanel = new MonitoringPanel();
        monitoringPanel.setBackground(AppTheme.BG_PRIMARY);
        contentPanel.add(monitoringPanel, "Monitoring");

        // Other tabs
        try {
            contentPanel.add(new MenuMaintenancePanel(), "Menu Maintenance");
        } catch (Exception e) {
            System.err.println("MenuMaintenancePanel init failed: " + e.getMessage());
        }
        try {
            inventoryRegistrationPanel = new InventoryRegistrationPanel(
                    new SQLiteInventoryRepository(),
                    this::loadInventoryTable,
                    () -> {
                        if (monitoringPanel != null) {
                            monitoringPanel.refreshData();
                        }
                    });
            contentPanel.add(inventoryRegistrationPanel, "Register Product");
        } catch (Exception e) {
            System.err.println("InventoryRegistrationPanel init failed: " + e.getMessage());
        }
        try {
            contentPanel.add(new StaffPanel(new SQLiteStaffShiftRepository(), new SQLiteUserRepository(),
                    new SQLiteProfilePictureRepository(), currentUsername, currentUserRole), "Staff");
        } catch (Exception e) {
            System.err.println("StaffPanel init failed: " + e.getMessage());
        }
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
                    "Payment Successful!\nSubtotal: " + jTextFieldSubTotal.getText() + "\nVAT (12%): "
                            + jTextFieldTax.getText()
                            + "\nTotal: " + jTextFieldTotal.getText() + "\nCash: \u20B1" + String.format("%.2f", cash)
                            + "\nChange: \u20B1" + String.format("%.2f", change),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
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
            model.addRow(new Object[] { productName, 1, price });
        }
        ItemCost();
    }

    private void loadInventoryTable() {
        if (inventoryController == null) {
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        }
        inventoryRowsCache.clear();
        inventoryRowsCache.addAll(inventoryController.buildInventoryRows());
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

        // ABC tiers rank items by usage value relative to the whole inventory,
        // so they're computed once across all rows up front, not per-row.
        InventoryPolicyService policyService = new InventoryPolicyService();
        Map<String, InventoryItem> itemsByName = new java.util.LinkedHashMap<>();
        for (InventoryRowView r : inventoryRowsCache) {
            itemsByName.put(r.getName(), new InventoryItem(r.getName(), r.getQuantity(), r.getUnit(), r.getAlertLevel()));
        }
        Map<String, String> abcTiers = policyService.classifyAbc(itemsByName);

        for (InventoryRowView row : inventoryRowsCache) {
            if (!matchesInventoryFilters(row, query, selectedCategory)) {
                continue;
            }
            String qtyDisplay = row.getQuantity() + " " + row.getUnit();
            String alertDisplay = row.getAlertLevel() + " " + row.getUnit();
            InventoryItem item = itemsByName.get(row.getName());
            String tier = abcTiers.getOrDefault(row.getName(), "C");
            ReorderGuideInfo reorderGuide = new ReorderGuideInfo(
                    policyService.computeRecommendedEoq(item),
                    policyService.computeReorderPoint(item),
                    row.getQuantity(), row.getUnit());
            model.addRow(new Object[] { row.getName(), qtyDisplay, alertDisplay, tier, reorderGuide, row, row.getStatus(), "..." });
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

    private void chooseHotOrIced(String rawProductName) {
        String productName = rawProductName;
        Object[] options = { "Hot", "Regular Iced", "Large Iced" };
        int choice = JOptionPane.showOptionDialog(this, "Choose variant for " + productName + ":", "Choose Variant",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == JOptionPane.CLOSED_OPTION || choice < 0)
            return;
        String variant = options[choice].toString();
        String displayName = productName + " (" + variant + ")";
        double price = Menu.getInstance().getPrice(productName, variant);
        if (price > 0)
            addItem(displayName, price);
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
                "Better Mondays Coffeee Cafe Management System", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (option == JOptionPane.YES_OPTION)
            System.exit(0);
    }

    private static int transactionCounter = loadTransactionCounter();

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }

    private void payActionPerformed(java.awt.event.ActionEvent evt) {
        boolean paymentSuccess = Change();
        if (!paymentSuccess)
            return;

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
        double cash = Double.parseDouble(cashpayment.getText().replace("\u20B1", "").replace("P", "").trim());
        double change = Double.parseDouble(jTextFieldChange.getText().replace("\u20B1", "").replace("P", "").trim());

        String transactionRef = nextTransactionRef();
        try {
            if (orderController == null)
                orderController = new OrderController(new SQLiteSalesRepository());
            orderController.persistCompletedTransaction(transactionRef, salesList, subTotal, cash, change);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to save sales to database: " + e.getMessage(), "Database",
                    JOptionPane.WARNING_MESSAGE);
        }

        String lineSep = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        String receiptStr = " ☕ Better Mondays Cafe ☕\n 123 Main St., Manila\n VAT REG TIN: 123-456-789\n" + lineSep
                + "Date: " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date()) + "\n"
                + "Txn #: " + transactionRef + "\n" + lineSep
                + String.format("%-15s %3s %8s\n", "ITEM", "QTY", "AMOUNT") + "───────────────────────────────\n";
        for (SalesRecord s : salesList) {
            receiptStr += String.format("%-15s %3d %8.2f\n", truncate(s.getProductName(), 15), s.getQuantity(),
                    s.getTotal());
        }
        receiptStr += "───────────────────────────────\n"
                + String.format("%-23s %8.2f\n", "Subtotal (excl VAT):", subTotal / 1.12)
                + String.format("%-23s %8.2f\n", "VAT (12%):", subTotal * 0.12 / 1.12)
                + String.format("%-23s %8.2f\n", "TOTAL (incl VAT):", subTotal)
                + String.format("%-23s %8.2f\n", "Cash:", cash)
                + String.format("%-23s %8.2f\n", "Change:", change) + lineSep
                + " Thank you! Come again!\n *** Have a nice day ***\n";

        JOptionPane.showMessageDialog(this, receiptStr, "✅ RECEIPT - " + transactionRef,
                JOptionPane.INFORMATION_MESSAGE);

        orderModel.setRowCount(0);
        jTextFieldChange.setText("");
        jTextFieldTax.setText("");
        jTextFieldTotal.setText("");
        jTextFieldSubTotal.setText("");
        cashpayment.setText("");

        loadInventoryTable();
        monitoringPanel.refreshData();
    }

    private static int loadTransactionCounter() {
        try (Connection conn = persistence.AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT transaction_ref FROM sales_transactions ORDER BY id DESC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String ref = rs.getString(1);
                return parseTransactionNumber(ref);
            }
        } catch (Exception ignored) {
        }
        return 1000;
    }

    private static int parseTransactionNumber(String ref) {
        if (ref == null)
            return 1000;
        String digits = ref.replaceAll("\\D", "");
        if (digits.isEmpty())
            return 1000;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 1000;
        }
    }

    private static String nextTransactionRef() {
        transactionCounter = Math.max(transactionCounter, loadTransactionCounter()) + 1;
        return "TXN" + String.format("%06d", transactionCounter);
    }

    /** Carries the EOQ/ROP figures for a row so ReorderGuideRenderer can color ROP once stock reaches it. */
    private static final class ReorderGuideInfo {
        final double eoq;
        final double rop;
        final double quantity;
        final String unit;

        ReorderGuideInfo(double eoq, double rop, double quantity, String unit) {
            this.eoq = eoq;
            this.rop = rop;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    private static class AbcTierRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel label) {
                String tier = value == null ? "C" : value.toString();
                label.setText(tier);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!isSelected) {
                    switch (tier) {
                        case "A" -> label.setForeground(new Color(0x16A34A));
                        case "B" -> label.setForeground(new Color(0xD97706));
                        default -> label.setForeground(new Color(0x6B7280));
                    }
                }
            }
            return c;
        }
    }

    private static class ReorderGuideRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (value instanceof ReorderGuideInfo info) {
                    long eoqRounded = Math.round(info.eoq);
                    long ropRounded = Math.round(info.rop);
                    label.setText("EOQ~" + eoqRounded + "  ·  ROP~" + ropRounded);
                    if (!isSelected) {
                        // Red flags the actionable case: stock has reached its
                        // reorder point and a new order should be placed now.
                        label.setForeground(info.quantity <= info.rop
                                ? new Color(0xDC2626)
                                : AppTheme.FG_MUTED);
                    }
                } else {
                    label.setText("—");
                    if (!isSelected) {
                        label.setForeground(AppTheme.FG_MUTED);
                    }
                }
            }
            return c;
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

    private static class BatchSummaryRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!(c instanceof JLabel label))
                return c;
            label.setHorizontalAlignment(SwingConstants.CENTER);

            String text = "—";
            Color fg = AppTheme.FG_MUTED;

            if (value instanceof controller.InventoryRowView rv) {
                java.util.List<inventory.InventoryBatch> batches = rv.getBatches();
                if (batches != null && !batches.isEmpty()) {
                    int expired = 0, expiring = 0, active = 0;
                    java.time.LocalDate today = java.time.LocalDate.now();
                    for (inventory.InventoryBatch b : batches) {
                        if (b.isArchived())
                            continue;
                        active++;
                        String exp = b.getExpiryDate();
                        if (exp == null || exp.isBlank())
                            continue;
                        try {
                            java.time.LocalDate d = java.time.LocalDate.parse(exp);
                            if (!d.isAfter(today))
                                expired++;
                            else if (!d.isAfter(today.plusDays(7)))
                                expiring++;
                        } catch (Exception ignored) {
                        }
                    }
                    if (expired > 0) {
                        text = "⚠ " + expired + " expired";
                        fg = new Color(200, 50, 50);
                    } else if (expiring > 0) {
                        text = "⚠ " + expiring + " expiring";
                        fg = new Color(200, 160, 40);
                    } else if (active > 0) {
                        text = active + " batch" + (active == 1 ? "" : "es");
                    }
                }
            }

            label.setText(text);
            if (!isSelected)
                label.setForeground(fg);
            return label;
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
                    if (editingRow < 0)
                        return;
                    String itemName = inventoryTable.getValueAt(editingRow, 0).toString();
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem editItem = new JMenuItem("Edit");
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    editItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    deleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    final int rowToEdit = editingRow;
                    editItem.addActionListener(ev -> {
                        fireEditingStopped();
                        openInventoryEditDialog(rowToEdit);
                    });
                    deleteItem.addActionListener(ev -> {
                        int confirm = JOptionPane.showConfirmDialog(POSSystem.this,
                                "Remove " + itemName + " from inventory?", "Confirm",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (inventoryController == null)
                                inventoryController = new InventoryController(Inventory.getInstance(),
                                        new SQLiteInventoryRepository());
                            inventoryController.removeItem(itemName);
                            loadInventoryTable();
                        }
                    });
                    menu.add(editItem);
                    menu.add(deleteItem);
                    menu.show(panel, e.getX(), e.getY());
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "...";
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            editingRow = row;
            return panel;
        }
    }

    private void openInventoryEditDialog(int tableRow) {
        String itemName = inventoryTable.getValueAt(tableRow, 0).toString();
        InventoryItem item = Inventory.getInstance().getItem(itemName);
        if (item == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Item not found: " + itemName);
            return;
        }

        javax.swing.JDialog dialog = new javax.swing.JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Inventory Item", true);
        dialog.setLayout(new java.awt.BorderLayout(0, 0));
        dialog.setResizable(false);

        JPanel content = new JPanel(new java.awt.GridBagLayout());
        content.setBackground(AppTheme.BG_SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.insets = new java.awt.Insets(6, 0, 6, 12);
        gc.anchor = java.awt.GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Item name (read-only)
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        JLabel nameLabel = new JLabel("Item:");
        nameLabel.setFont(labelFont);
        nameLabel.setForeground(AppTheme.FG_MUTED);
        content.add(nameLabel, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        JLabel nameValue = new JLabel(item.getName());
        nameValue.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameValue.setForeground(AppTheme.FG_PRIMARY);
        content.add(nameValue, gc);

        // Quantity field
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0;
        gc.fill = java.awt.GridBagConstraints.NONE;
        JLabel qtyLabel = new JLabel("Quantity:");
        qtyLabel.setFont(labelFont);
        qtyLabel.setForeground(AppTheme.FG_MUTED);
        content.add(qtyLabel, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        JTextField qtyField = new JTextField(String.valueOf(item.getQuantity()), 12);
        qtyField.setFont(fieldFont);
        qtyField.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.BORDER, 1, 6),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        content.add(qtyField, gc);

        // Alert level field
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0;
        gc.fill = java.awt.GridBagConstraints.NONE;
        JLabel alertLabel = new JLabel("Alert Level:");
        alertLabel.setFont(labelFont);
        alertLabel.setForeground(AppTheme.FG_MUTED);
        content.add(alertLabel, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        JTextField alertField = new JTextField(String.valueOf(item.getAlertLevel()), 12);
        alertField.setFont(fieldFont);
        alertField.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.BORDER, 1, 6),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        content.add(alertField, gc);

        dialog.add(content, java.awt.BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 12));
        btnRow.setBackground(AppTheme.BG_SURFACE);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setBackground(AppTheme.BG_PRIMARY);
        cancelBtn.setForeground(AppTheme.FG_PRIMARY);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.BORDER, 1, 6),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        cancelBtn.addActionListener(ev -> dialog.dispose());

        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(AppTheme.ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.ACCENT, 1, 6),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        saveBtn.addActionListener(ev -> {
            String qtyText = qtyField.getText().trim();
            String alertText = alertField.getText().trim();
            if (qtyText.isEmpty() || alertText.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Quantity and Alert Level cannot be empty.");
                return;
            }
            try {
                Double.parseDouble(qtyText);
                Double.parseDouble(alertText);
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Please enter valid numbers for Quantity and Alert Level.");
                return;
            }
            if (inventoryController == null)
                inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.updateItem(itemName, itemName, qtyText, item.getUnit(), alertText);
            loadInventoryTable();
            if (monitoringPanel != null)
                monitoringPanel.refreshData();
            dialog.dispose();
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        dialog.add(btnRow, java.awt.BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(340, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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

        // ABC tiers rank items by usage value relative to the whole inventory,
        // so they must be computed across all rows, not just the selected one.
        InventoryPolicyService policyService = new InventoryPolicyService();
        Map<String, InventoryItem> itemsByName = new java.util.LinkedHashMap<>();
        for (InventoryRowView r : rows) {
            itemsByName.put(r.getName(), new InventoryItem(r.getName(), r.getQuantity(), r.getUnit(), r.getAlertLevel()));
        }
        Map<String, String> abcTiers = policyService.classifyAbc(itemsByName);

        for (InventoryRowView row : rows) {
            if (!row.getName().equals(itemName)) {
                continue;
            }

            InventoryItem item = itemsByName.get(row.getName());
            double eoq = policyService.computeRecommendedEoq(item);
            double rop = policyService.computeReorderPoint(item);
            String tier = abcTiers.getOrDefault(row.getName(), "C");

            StringBuilder details = new StringBuilder();
            details.append("Name: ").append(row.getName()).append('\n');
            details.append("Quantity: ").append(row.getQuantity()).append(' ').append(row.getUnit()).append('\n');
            details.append("Alert Level: ").append(row.getAlertLevel()).append(' ').append(row.getUnit()).append('\n');
            details.append("Status: ").append(row.getStatus()).append('\n');
            details.append("Updated: ").append(
                    row.getLastUpdated() == null || row.getLastUpdated().isBlank() ? "N/A" : row.getLastUpdated())
                    .append('\n');
            details.append("Categories: ")
                    .append(row.getCategories() == null || row.getCategories().isBlank() ? "N/A" : row.getCategories())
                    .append('\n');
            details.append('\n');
            details.append("--- Replenishment Insights ---\n");
            details.append("ABC Tier: ").append(tier)
                    .append(" (").append(tier.equals("A") ? "high priority — drives ~80% of usage"
                            : tier.equals("B") ? "medium priority — next ~15%"
                            : "low priority — remaining ~5%")
                    .append(")\n");
            details.append("Recommended Order Qty (EOQ): ~").append(Math.round(eoq)).append(' ').append(row.getUnit()).append('\n');
            details.append("Reorder Point (ROP): ~").append(Math.round(rop)).append(' ').append(row.getUnit());
            if (row.getQuantity() <= rop) {
                details.append("  ← reorder now, stock has reached its reorder point");
            }
            details.append('\n');
            details.append("Deduction order: FEFO (earliest-expiring batches used first)\n");
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
            if (newName == null || newName.trim().isEmpty())
                return;
            newName = newName.trim();
            String quantityStr = JOptionPane.showInputDialog(this, "Enter new quantity:", item.getQuantity());
            String unit = JOptionPane.showInputDialog(this, "Enter new unit:", item.getUnit());
            String alertStr = JOptionPane.showInputDialog(this, "Enter new alert level:", item.getAlertLevel());
            if (inventoryController == null)
                inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.updateItem(currentName.trim(), newName, quantityStr, unit, alertStr);
            loadInventoryTable();
            monitoringPanel.refreshData();
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
        int confirm = JOptionPane.showConfirmDialog(this, "Remove " + name + " from inventory?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (inventoryController == null)
                inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
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
            if (quantityStr == null || quantityStr.trim().isEmpty())
                return;
            String unit = JOptionPane.showInputDialog(this, "Enter unit (e.g., kg, pcs):");
            if (unit == null || unit.trim().isEmpty())
                return;
            String alertStr = JOptionPane.showInputDialog(this, "Enter alert level:");
            if (alertStr == null || alertStr.trim().isEmpty())
                return;
            if (inventoryController == null)
                inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
            inventoryController.addItem(name, quantityStr, unit, alertStr);
            loadInventoryTable();
            monitoringPanel.refreshData();
            JOptionPane.showMessageDialog(this, "Item added successfully!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid number entered. Please enter valid numeric values for quantity and alert level.");
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
    private OrderingPanel orderingPanel;
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