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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
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
import ui.OrderListPanel;
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
        "Pastries"
    );

    private final Map<String, List<String>> categoryItems;

    private static final Map<String, Double> HOUSE_FAVORITES_PRICES = Map.of(
        "Mango Latte", 210.0,
        "Strawberry Latte", 200.0,
        "Salted Cream Latte", 190.0,
        "Spanish Latte", 180.0
    );

    private static final List<String> SPECIALTY_DRINKS = List.of(
        "Vietnamese Coffee",
        "Ube Espresso",
        "Manila Latte",
        "Pumpkin Spice Latte",
        "Spiced Cookie Latte"
    );

    private static final List<String> TEA_LATTE_ITEMS = List.of(
        "Matcha Latte",
        "Chocolate Matcha",
        "Matcha Espresso",
        "Hojicha Latte",
        "Chai Latte"
    );

    private static final List<String> NON_COFFEE_ITEMS = List.of(
        "Chocolate Latte",
        "Strawberry Latte",
        "Mango Latte",
        "Dragon Fruit Coconut Latte",
        "Ube Latte"
    );

    private static final List<String> FRUIT_TEA_ITEMS = List.of(
        "Strawberry Green Tea",
        "Mango Green Tea",
        "Peach Green Tea",
        "Passion Fruit Green Tea"
    );

    private static final List<String> HERBAL_TEA_ITEMS = List.of(
        "Peppermint",
        "Chamomile",
        "Earl Grey",
        "Cinnamon"
    );

    private static final List<String> SANDWICH_ITEMS = List.of(
        "Signature Ham & Cheese",
        "Classic Grilled Cheese",
        "Homestyle Pesto & Cheese"
    );

    private static final List<String> PANDESAL_PAIR_ITEMS = List.of(
        "Ham & Cheese",
        "Cheesy Pesto",
        "Spam & Cheese"
    );

    private static final List<String> PASTRY_ITEMS = List.of(
        "Chocolate Crinkles",
        "Chocolate Cookies",
        "Brownies",
        "Banana Bread",
        "Chocolate Tiramisu",
        "Matcha Tiramisu",
        "Creamy Spinach",
        "Blueberry Cheesecake"
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
    private OrderListPanel orderListPanel;
    private JPanel orderStrip;
    private JLabel totalItemsLabel;
    private JLabel lowStockLabel;
    private JLabel expiredLabel;
    private JLabel outOfStockLabel;
    private JTextArea inventoryDetailArea;
    private JTextField inventorySearchField;
    private JComboBox<String> inventoryCategoryFilter;
    private final List<InventoryRowView> inventoryRowsCache = new ArrayList<>();
    private static final String INVENTORY_SEARCH_PLACEHOLDER = "Search ingredients";
    private JPanel topNavBar;
    private String activeTopNavPage = "Ordering";
    private String receiptCustomerName = "Walk-in";
    private String receiptTableName = "Counter";

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
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setResizable(true);
        setLocationRelativeTo(null);

        loadInventoryTable();

        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        orderController = new OrderController(new SQLiteSalesRepository());

        monitoring = new Monitoring(jTableMonitoring, jTableSales);
        monitoring.loadLowStockIngredients();

        AppTheme.applyToFrame(this);
        refreshTopNavBarStyles();
        styleSearchField();

        showCategory("Espresso & Coffee");
        refreshOrderDisplay();
        SwingUtilities.invokeLater(this::refreshOrderStrip);
    }

    private ImageIcon loadLogoIcon() {
        java.net.URL location = getClass().getResource("/images/logo.png");
        return location != null ? new ImageIcon(location) : null;
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


    // ─── Category display ───────────────────────────────────────
    private void showCategory(String category) {
        activeCategory = category;
        for (java.awt.Component c : sidebarPanel.getComponents()) {
            if (c instanceof JButton b) {
                boolean active = category.equals(b.getText());
                b.setBackground(active ? Color.WHITE : new Color(0xF3F4F6));
                b.setForeground(active ? new Color(0x111827) : new Color(0x9CA3AF));
                b.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
                b.setBorder(active
                    ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                        BorderFactory.createEmptyBorder(5, 13, 5, 13))
                    : BorderFactory.createEmptyBorder(6, 14, 6, 14));
            }
        }
        rebuildProductGrid(category, searchField.getText());
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
            emptyLabel.setForeground(AppTheme.FG_MUTED);
            emptyRow.add(emptyLabel, BorderLayout.CENTER);
            productGridPanel.add(emptyRow);
        } else {
            for (int index = 0; index < items.size(); index += 2) {
                JPanel rowPanel = new JPanel(new GridLayout(1, 2, 14, 0));
                rowPanel.setOpaque(false);
                rowPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

                int endIndex = Math.min(index + 2, items.size());
                for (int itemIndex = index; itemIndex < endIndex; itemIndex++) {
                    rowPanel.add(createProductCard(items.get(itemIndex), category));
                }

                for (int filler = endIndex - index; filler < 2; filler++) {
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
        CardPanel card = new CardPanel(12, Color.WHITE);
        card.setLayout(new BorderLayout(0, 0));
        card.setFillColor(Color.WHITE);
        card.setBorderColor(new Color(0xE5E7EB));
        card.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        card.setPreferredSize(new Dimension(340, 160));
        card.setMinimumSize(new Dimension(300, 160));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel photoLabel = new JLabel();
        photoLabel.setPreferredSize(new Dimension(120, 160));
        photoLabel.setMinimumSize(new Dimension(120, 160));
        photoLabel.setMaximumSize(new Dimension(120, 160));
        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoLabel.setVerticalAlignment(SwingConstants.CENTER);
        photoLabel.setOpaque(true);

        ImageIcon icon = item.loadImage(120, 160);
        if (icon != null) {
            photoLabel.setIcon(icon);
            photoLabel.setBackground(new Color(0xF3F4F6));
        } else {
            photoLabel.setBackground(categoryColor(category));
            String initial = item.getName() == null || item.getName().isBlank()
                ? "?"
                : item.getName().substring(0, 1).toUpperCase();
            photoLabel.setText(initial);
            photoLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
            photoLabel.setForeground(Color.WHITE);
        }
        card.add(photoLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(new Color(0x111827));

        double displayPrice = pickDisplayPrice(item, category);
        JLabel priceLabel = new JLabel(String.format("₱%.2f", displayPrice));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        priceLabel.setForeground(new Color(0x3B82F6));

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.setOpaque(false);
        titleRow.add(nameLabel, BorderLayout.WEST);
        titleRow.add(priceLabel, BorderLayout.EAST);

        JLabel descLabel = new JLabel("<html><div style='width:180px;color:#6B7280;font-size:10px;line-height:1.3;'>"
            + getItemDescription(item.getName(), category)
            + "</div></html>");
        descLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel textPanel = new JPanel(new BorderLayout(0, 3));
        textPanel.setOpaque(false);
        textPanel.add(titleRow, BorderLayout.NORTH);
        textPanel.add(descLabel, BorderLayout.CENTER);
        rightPanel.add(textPanel, BorderLayout.NORTH);

        JPanel steppersPanel = new JPanel();
        steppersPanel.setLayout(new BoxLayout(steppersPanel, BoxLayout.Y_AXIS));
        steppersPanel.setOpaque(false);

        List<String> variants = getVariantsForCategory(item, category);
        for (String variant : variants) {
            steppersPanel.add(buildVariantRow(item, variant, category));
            steppersPanel.add(Box.createVerticalStrut(3));
        }
        rightPanel.add(steppersPanel, BorderLayout.SOUTH);

        card.add(rightPanel, BorderLayout.CENTER);
        return card;
    }

    private List<String> getVariantsForCategory(MenuItem item, String category) {
        List<String> variants = new ArrayList<>();
        switch (category) {
            case "Espresso & Coffee" -> {
                if (item.getHotPrice() > 0) variants.add("Hot");
                if (item.getIcedRegularPrice() > 0) variants.add("Regular");
                if (item.getIcedLargePrice() > 0) variants.add("Large");
            }
            case "Specialty Drinks" -> {
                if (item.getIcedRegularPrice() > 0) variants.add("Regular Iced");
            }
            case "Tea Latte" -> {
                if (item.getHotPrice() > 0) variants.add("Hot");
                if (item.getIcedRegularPrice() > 0) variants.add("Iced Regular");
            }
            case "Non-Coffee" -> {
                if ("Chocolate Latte".equals(item.getName()) && item.getHotPrice() > 0) variants.add("Hot");
                if (item.getIcedRegularPrice() > 0) variants.add("Regular");
                if (item.getIcedLargePrice() > 0) variants.add("Large");
            }
            case "House Favorites" -> variants.add("Add to cart");
            case "Fruit Tea" -> {
                if (item.getIcedRegularPrice() > 0) variants.add("Regular");
                if (item.getIcedLargePrice() > 0) variants.add("Large");
            }
            case "Herbal Tea" -> {
                if (item.getHotPrice() > 0) variants.add("Hot");
            }
            default -> variants.add("Add to cart");
        }
        return variants;
    }

    private JPanel buildVariantRow(MenuItem item, String variantLabel, String category) {
        double price = resolveVariantPrice(item, variantLabel, category);
        String variantKey = variantLabel.equals("Add to cart") ? "" : variantLabel;

        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setPreferredSize(new Dimension(160, 26));

        int currentQty = getOrderQty(item.getName(), variantKey);

        if (currentQty == 0) {
            JButton addBtn = new JButton("+ " + variantLabel);
            addBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            addBtn.setBackground(new Color(0x3B82F6));
            addBtn.setForeground(Color.WHITE);
            addBtn.setFocusPainted(false);
            addBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            addBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            addBtn.addActionListener(e -> {
                addOrderItem(item.getName(), variantKey, price);
                rebuildProductGrid(activeCategory, searchField.getText());
            });
            row.add(addBtn, BorderLayout.CENTER);
        } else {
            JLabel varLabel = new JLabel(variantLabel);
            varLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            varLabel.setForeground(new Color(0x6B7280));

            JButton minusBtn = new JButton("−");
            minusBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            minusBtn.setBackground(new Color(0xF3F4F6));
            minusBtn.setForeground(new Color(0x374151));
            minusBtn.setFocusPainted(false);
            minusBtn.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB), 1));
            minusBtn.setPreferredSize(new Dimension(24, 24));
            minusBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            minusBtn.addActionListener(e -> {
                decrementOrderItem(item.getName(), variantKey);
                rebuildProductGrid(activeCategory, searchField.getText());
            });

            JLabel qtyLabel = new JLabel(String.valueOf(currentQty), SwingConstants.CENTER);
            qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            qtyLabel.setForeground(new Color(0x111827));
            qtyLabel.setPreferredSize(new Dimension(24, 24));

            JButton plusBtn = new JButton("+");
            plusBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            plusBtn.setBackground(new Color(0x3B82F6));
            plusBtn.setForeground(Color.WHITE);
            plusBtn.setFocusPainted(false);
            plusBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            plusBtn.setPreferredSize(new Dimension(24, 24));
            plusBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            plusBtn.addActionListener(e -> {
                addOrderItem(item.getName(), variantKey, price);
                rebuildProductGrid(activeCategory, searchField.getText());
            });

            JPanel stepperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            stepperPanel.setOpaque(false);
            stepperPanel.add(minusBtn);
            stepperPanel.add(qtyLabel);
            stepperPanel.add(plusBtn);

            row.add(varLabel, BorderLayout.WEST);
            row.add(stepperPanel, BorderLayout.EAST);
        }

        return row;
    }

    private double resolveVariantPrice(MenuItem item, String variantLabel, String category) {
        if ("House Favorites".equals(category)) {
            return HOUSE_FAVORITES_PRICES.getOrDefault(item.getName(), 0.0);
        }
        return switch (variantLabel) {
            case "Hot" -> item.getHotPrice();
            case "Regular", "Regular Iced", "Iced Regular" -> item.getIcedRegularPrice();
            case "Large" -> item.getIcedLargePrice();
            default -> item.getHotPrice() > 0 ? item.getHotPrice()
                 : item.getIcedRegularPrice() > 0 ? item.getIcedRegularPrice()
                 : item.getIcedLargePrice();
        };
    }

    private int getOrderQty(String name, String variant) {
        for (OrderEntry entry : orderEntries) {
            if (entry.name.equals(name) && entry.variant.equals(variant)) {
                return entry.quantity;
            }
        }
        return 0;
    }

    private void decrementOrderItem(String name, String variant) {
        for (OrderEntry entry : orderEntries) {
            if (entry.name.equals(name) && entry.variant.equals(variant)) {
                if (entry.quantity > 1) {
                    entry.quantity--;
                } else {
                    orderEntries.remove(entry);
                }
                refreshOrderDisplay();
                return;
            }
        }
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
        btn.setBackground(new Color(0x3B82F6));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(144, 22));
        btn.setMaximumSize(new Dimension(144, 22));
        btn.setMinimumSize(new Dimension(144, 22));
        btn.setMargin(new Insets(0, 6, 0, 6));
        btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
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
        orderPanel.setLayout(new BoxLayout(orderPanel, BoxLayout.Y_AXIS));
        orderPanel.setBackground(Color.WHITE);
        for (OrderEntry entry : orderEntries) {
            orderPanel.add(createOrderRow(entry));
            orderPanel.add(Box.createVerticalStrut(2));
        }

        orderPanel.revalidate();
        orderPanel.repaint();
        updateReceipt();
        SwingUtilities.invokeLater(() -> rebuildProductGrid(activeCategory,
            searchField != null ? searchField.getText() : ""));
    }

    private JPanel createOrderRow(OrderEntry entry) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF3F4F6)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel thumb = new JLabel();
        thumb.setPreferredSize(new Dimension(40, 40));
        thumb.setMinimumSize(new Dimension(40, 40));
        thumb.setMaximumSize(new Dimension(40, 40));
        thumb.setOpaque(true);

        MenuItem menuItem = Menu.getInstance().getMenuItem(entry.name);
        if (menuItem != null) {
            ImageIcon icon = menuItem.loadImage(40, 40);
            if (icon != null) {
                thumb.setIcon(icon);
                thumb.setBackground(new Color(0xF3F4F6));
            } else {
                thumb.setBackground(categoryColorForItem(entry.name));
                thumb.setText(entry.name == null || entry.name.isBlank() ? "?" : entry.name.substring(0, 1).toUpperCase());
                thumb.setFont(new Font("Segoe UI", Font.BOLD, 14));
                thumb.setForeground(Color.WHITE);
                thumb.setHorizontalAlignment(SwingConstants.CENTER);
            }
        } else {
            thumb.setBackground(new Color(0xF3F4F6));
            thumb.setText(entry.name == null || entry.name.isBlank() ? "?" : entry.name.substring(0, 1).toUpperCase());
            thumb.setFont(new Font("Segoe UI", Font.BOLD, 14));
            thumb.setForeground(new Color(0x6B7280));
            thumb.setHorizontalAlignment(SwingConstants.CENTER);
        }
        thumb.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB), 1));
        row.add(thumb, BorderLayout.WEST);

        JLabel nameLabel = new JLabel(entry.displayName());
        nameLabel.setForeground(new Color(0x111827));
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(nameLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);

        JLabel priceLabel = new JLabel(String.format("₱%.2f", entry.lineTotal()));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        priceLabel.setForeground(new Color(0x111827));

        JLabel removeLink = new JLabel("✕");
        removeLink.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        removeLink.setForeground(new Color(0xEF4444));
        removeLink.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        removeLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                orderEntries.remove(entry);
                refreshOrderDisplay();
                rebuildProductGrid(activeCategory, searchField.getText());
            }
        });

        rightPanel.add(priceLabel);
        rightPanel.add(removeLink);

        row.add(rightPanel, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(9999, 48));
        return row;
    }

    private Color categoryColor(String category) {
        return switch (category) {
            case "Espresso & Coffee" -> new Color(0x6F4E37);
            case "Specialty Drinks" -> new Color(0x7B5EA7);
            case "Tea Latte" -> new Color(0x4A7C59);
            case "Non-Coffee" -> new Color(0xC0627A);
            case "House Favorites" -> new Color(0xD4882E);
            case "Fruit Tea" -> new Color(0xE05C2A);
            case "Herbal Tea" -> new Color(0x5B8A5C);
            case "Sandwiches" -> new Color(0xB5702A);
            case "Pandesal Pairs" -> new Color(0xC47E3A);
            case "Pastries" -> new Color(0xA0522D);
            default -> new Color(0x6B7280);
        };
    }

    private String getItemDescription(String itemName, String category) {
        Map<String, String> descriptions = Map.ofEntries(
            Map.entry("Americano", "Bold espresso shots with hot or cold water"),
            Map.entry("Brewed Coffee", "Classic drip brewed to smooth perfection"),
            Map.entry("Cappuccino", "Espresso with steamed milk and thick foam"),
            Map.entry("Caramel Macchiato", "Vanilla, milk, espresso, caramel drizzle"),
            Map.entry("Dark Mocha", "Rich espresso with dark chocolate sauce"),
            Map.entry("Latte", "Smooth espresso with velvety steamed milk"),
            Map.entry("White Mocha", "Espresso with sweet white chocolate sauce"),
            Map.entry("Vietnamese Coffee", "Strong drip coffee with sweet condensed milk"),
            Map.entry("Ube Espresso", "Espresso layered over creamy ube milk"),
            Map.entry("Manila Latte", "Local-inspired espresso with coconut notes"),
            Map.entry("Pumpkin Spice Latte", "Espresso with pumpkin spice and warm milk"),
            Map.entry("Spiced Cookie Latte", "Cookie butter syrup with smooth espresso"),
            Map.entry("Matcha Latte", "Premium stone-ground matcha with milk"),
            Map.entry("Chocolate Matcha", "Rich chocolate meets earthy matcha"),
            Map.entry("Matcha Espresso", "Bold espresso shot over iced matcha"),
            Map.entry("Hojicha Latte", "Roasted green tea with creamy milk"),
            Map.entry("Chai Latte", "Spiced black tea with steamed milk"),
            Map.entry("Chocolate Latte", "Rich chocolate blended with creamy milk"),
            Map.entry("Strawberry Latte", "Fresh strawberry flavor with smooth milk"),
            Map.entry("Mango Latte", "Tropical mango sweetness with milk"),
            Map.entry("Dragon Fruit Coconut Latte", "Exotic dragon fruit with coconut milk"),
            Map.entry("Ube Latte", "Filipino purple yam with creamy milk"),
            Map.entry("Salted Cream Latte", "Sweet and salty cream over espresso"),
            Map.entry("Spanish Latte", "Espresso with sweetened condensed milk"),
            Map.entry("Strawberry Green Tea", "Fresh strawberry blended with green tea"),
            Map.entry("Mango Green Tea", "Tropical mango with light green tea"),
            Map.entry("Peach Green Tea", "Juicy peach flavor with green tea base"),
            Map.entry("Passion Fruit Green Tea", "Tangy passion fruit meets green tea"),
            Map.entry("Peppermint", "Soothing peppermint herbal blend"),
            Map.entry("Chamomile", "Calming chamomile floral herbal tea"),
            Map.entry("Earl Grey", "Classic bergamot-scented black tea"),
            Map.entry("Cinnamon", "Warming cinnamon spice herbal blend"),
            Map.entry("Signature Ham & Cheese", "Ham and melted cheese on toasted bread"),
            Map.entry("Classic Grilled Cheese", "Buttery golden grilled cheese sandwich"),
            Map.entry("Homestyle Pesto & Cheese", "Fresh pesto with melted cheese on bread"),
            Map.entry("Ham & Cheese", "Classic ham and cheese on fresh pandesal"),
            Map.entry("Cheesy Pesto", "Pesto and cheese stuffed pandesal"),
            Map.entry("Spam & Cheese", "Savory spam with melted cheese pandesal"),
            Map.entry("Chocolate Crinkles", "Fudgy chocolate powdered sugar cookies"),
            Map.entry("Chocolate Cookies", "Crispy chocolate chip baked cookies"),
            Map.entry("Brownies", "Dense fudgy chocolate brownies"),
            Map.entry("Banana Bread", "Moist homemade banana bread loaf"),
            Map.entry("Chocolate Tiramisu", "Tiramisu with rich chocolate layers"),
            Map.entry("Matcha Tiramisu", "Classic tiramisu with matcha twist"),
            Map.entry("Creamy Spinach", "Savory creamy spinach pastry"),
            Map.entry("Blueberry Cheesecake", "Rich creamy cheesecake with blueberry topping")
        );
        return descriptions.getOrDefault(itemName, "A signature Better Mondays " + category.toLowerCase() + " item.");
    }

    private Color categoryColorForItem(String itemName) {
        MenuItem item = Menu.getInstance().getMenuItem(itemName);
        String category = item == null ? null : resolveOrderingCategory(item);
        return category != null ? categoryColor(category) : new Color(0x6B7280);
    }

    private void updateReceipt() {
        receiptPanel.removeAll();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(Color.WHITE);
        orderPanel.setBackground(Color.WHITE);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
            new ui.RoundedLineBorder(new Color(0xE5E7EB), ui.AppTheme.BORDER_THICKNESS, ui.AppTheme.BORDER_RADIUS),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel customerHeader = new JLabel("Customer Information");
        customerHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        customerHeader.setForeground(new Color(0x111827));
        customerHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        customerHeader.setHorizontalAlignment(SwingConstants.CENTER);
        receiptPanel.add(customerHeader);
        receiptPanel.add(Box.createVerticalStrut(6));

        receiptCustomerName = "Walk-in";
        receiptTableName = "Counter";

        JTextField customerNameField = new JTextField();
        customerNameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        customerNameField.setForeground(new Color(0x9CA3AF));
        customerNameField.setText("Customer Name");
        customerNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        customerNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE5E7EB), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        customerNameField.setBackground(Color.WHITE);
        customerNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        customerNameField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Customer Name".equals(customerNameField.getText())) {
                    customerNameField.setText("");
                    customerNameField.setForeground(new Color(0x111827));
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (customerNameField.getText().trim().isEmpty()) {
                    customerNameField.setText("Customer Name");
                    customerNameField.setForeground(new Color(0x9CA3AF));
                }
            }
        });
        customerNameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void sync() {
                String text = customerNameField.getText().trim();
                receiptCustomerName = text.isEmpty() || "Customer Name".equals(text) ? "Walk-in" : text;
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                sync();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                sync();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                sync();
            }
        });
        receiptPanel.add(customerNameField);
        receiptPanel.add(Box.createVerticalStrut(6));

        String[] tableOptions = {"Select Table", "Table 1", "Table 2", "Table 3", "Table 4", "Table 5"};
        JComboBox<String> tableSelector = new JComboBox<>(tableOptions);
        tableSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tableSelector.setBackground(Color.WHITE);
        tableSelector.setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB), 1));
        tableSelector.setAlignmentX(Component.CENTER_ALIGNMENT);
        tableSelector.addActionListener(e -> {
            String selection = (String) tableSelector.getSelectedItem();
            receiptTableName = selection == null || "Select Table".equals(selection) ? "Counter" : selection;
        });
        receiptPanel.add(tableSelector);
        receiptPanel.add(Box.createVerticalStrut(12));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE5E7EB));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        receiptPanel.add(sep);
        receiptPanel.add(Box.createVerticalStrut(10));

        JLabel orderDetailsHeader = new JLabel("Order Details");
        orderDetailsHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        orderDetailsHeader.setForeground(new Color(0x111827));
        orderDetailsHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        orderDetailsHeader.setHorizontalAlignment(SwingConstants.CENTER);
        receiptPanel.add(orderDetailsHeader);
        receiptPanel.add(Box.createVerticalStrut(6));

        double totalInclusive = 0;
        for (OrderEntry entry : orderEntries) {
            totalInclusive += entry.lineTotal();
        }
        double subTotalExVat = totalInclusive / 1.12;
        double vat = totalInclusive - subTotalExVat;

        JLabel orderNumLabel = new JLabel("Order #" + (orderCount + 1));
        orderNumLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        orderNumLabel.setForeground(new Color(0x111827));
        orderNumLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        orderNumLabel.setHorizontalAlignment(SwingConstants.CENTER);
        receiptPanel.add(orderNumLabel);

        JButton clearBtn = new JButton("Clear All");
        clearBtn.setBackground(new Color(0xFEE2E2));
        clearBtn.setForeground(new Color(0xEF4444));
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.setFocusPainted(false);
        clearBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        clearBtn.addActionListener(e -> {
            orderEntries.clear();
            refreshOrderDisplay();
        });
        receiptPanel.add(clearBtn);
        receiptPanel.add(Box.createVerticalStrut(4));

        orderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        receiptPanel.add(orderPanel);
        receiptPanel.add(Box.createVerticalStrut(4));

        JLabel line = new JLabel("─────────────────────");
        line.setForeground(new Color(0xE5E7EB));
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
        printBtn.setBackground(new Color(0x3B82F6));
        printBtn.setForeground(Color.WHITE);
        printBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        printBtn.setFocusPainted(false);
        printBtn.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        printBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        printBtn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        printBtn.setText("Process Transaction");
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(0x6B7280));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(lbl, BorderLayout.WEST);
        JLabel val = new JLabel(value);
        val.setForeground(new Color(0x111827));
        val.setFont(new Font("Segoe UI", Font.BOLD, 11));
        val.setHorizontalAlignment(SwingConstants.CENTER);
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

        if (orderListPanel != null) {
            List<String> items = new ArrayList<>();
            for (SalesRecord sale : salesList) {
                items.add(String.format("%dx %s", sale.getQuantity(), sale.getProductName()));
            }
            orderListPanel.addOrder(transactionRef, receiptCustomerName, receiptTableName, total, items);
            refreshOrderStrip();
        }

        monitoring.addMultipleSales(salesList);
        orderCount++;
        orderEntries.clear();
        refreshOrderDisplay();
        loadInventoryTable();
        monitoring.loadLowStockIngredients();
    }

    private JPanel buildOrderStrip() {
        orderStrip = new JPanel();
        orderStrip.setLayout(new BoxLayout(orderStrip, BoxLayout.X_AXIS));
        orderStrip.setBackground(new Color(0xF9FAFB));
        orderStrip.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JScrollPane strip = new JScrollPane(orderStrip,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        strip.setBorder(BorderFactory.createEmptyBorder());
        strip.getViewport().setBackground(new Color(0xF9FAFB));
        strip.setPreferredSize(new Dimension(0, 110));
        strip.setMinimumSize(new Dimension(0, 110));
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(strip, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildOrderMiniCard(OrderListPanel.OrderRecord order) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE5E7EB), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setPreferredSize(new Dimension(180, 90));
        card.setMinimumSize(new Dimension(180, 90));
        card.setMaximumSize(new Dimension(180, 90));
        card.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                activeTopNavPage = "Order List";
                cardLayout.show(contentPanel, "Order List");
                refreshTopNavBarStyles();
            }
        });

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel nameLabel = new JLabel(order.getCustomerName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(new Color(0x111827));
        JLabel numLabel = new JLabel(formatMiniOrderNumber(order.getOrderNumber()));
        numLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        numLabel.setForeground(new Color(0x9CA3AF));
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(numLabel, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        JLabel metaLabel = new JLabel(order.getItems().size() + " items  •  " + order.getTableName());
        metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        metaLabel.setForeground(new Color(0x6B7280));
        card.add(metaLabel, BorderLayout.CENTER);

        JLabel badge = new JLabel(compactStatusLabel(order.getStatus()));
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(compactStatusForeground(order.getStatus()));
        badge.setBackground(compactStatusBackground(order.getStatus()));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        card.add(badge, BorderLayout.SOUTH);

        return card;
    }

    public void refreshOrderStrip() {
        if (orderStrip == null || orderListPanel == null) {
            return;
        }
        orderStrip.removeAll();

        List<OrderListPanel.OrderRecord> recent = orderListPanel.getRecentOrders(10);
        if (recent.isEmpty()) {
            JLabel empty = new JLabel("  No active orders yet");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            empty.setForeground(new Color(0x9CA3AF));
            orderStrip.add(empty);
        } else {
            for (int i = 0; i < recent.size(); i++) {
                if (i > 0) {
                    orderStrip.add(Box.createHorizontalStrut(10));
                }
                orderStrip.add(buildOrderMiniCard(recent.get(i)));
            }
        }

        orderStrip.revalidate();
        orderStrip.repaint();
    }

    private String compactStatusLabel(OrderListPanel.OrderStatus status) {
        return switch (status) {
            case PREPARING -> "Preparing";
            case READY -> "Ready";
            case COMPLETED -> "Complete";
            case CANCELLED -> "Cancelled";
        };
    }

    private Color compactStatusBackground(OrderListPanel.OrderStatus status) {
        return switch (status) {
            case PREPARING -> new Color(0xFEF3C7);
            case READY -> new Color(0xDCFCE7);
            case COMPLETED -> new Color(0xDBEAFE);
            case CANCELLED -> new Color(0xFEE2E2);
        };
    }

    private Color compactStatusForeground(OrderListPanel.OrderStatus status) {
        return switch (status) {
            case PREPARING -> new Color(0xB45309);
            case READY -> new Color(0x15803D);
            case COMPLETED -> new Color(0x1D4ED8);
            case CANCELLED -> new Color(0xDC2626);
        };
    }

    private String formatMiniOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return "#----";
        }
        String digits = orderNumber.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return orderNumber;
        }
        digits = digits.replaceFirst("^0+(?!$)", "");
        return "#" + digits;
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
        contentPanel.setBackground(AppTheme.BG_PRIMARY);

        // ═══════════════════════════════════════════════════════════
        //  ORDERING TAB
        // ═══════════════════════════════════════════════════════════
        jPanelPOS.setBackground(AppTheme.BG_PRIMARY);
        jPanelPOS.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Ordering");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        titleLabel.setForeground(new Color(255, 255, 255));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 0));

        // ─── Category pills (horizontal nav) ───────────────────
        sidebarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        sidebarPanel.setBackground(new Color(0xF3F4F6));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        for (String cat : ORDERING_CATEGORIES) {
            JButton btn = new JButton(cat);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setBackground(new Color(0xF3F4F6));
            btn.setForeground(new Color(0x6B7280));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            btn.addActionListener(e -> showCategory(btn.getText()));
            sidebarPanel.add(btn);
        }

        // ─── Center panel (pills + search + product grid) ──────
        centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(AppTheme.BG_PRIMARY);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 10));

        JPanel centerTopPanel = new JPanel(new BorderLayout(4, 0));
        centerTopPanel.setOpaque(true);
        centerTopPanel.setBackground(new Color(0xF3F4F6));
        centerTopPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchIcon.setForeground(AppTheme.FG_MUTED);
        searchPanel.add(searchIcon);

        searchField = new JTextField("Search a product", 20);
        AppTheme.styleSearchField(searchField);
        searchField.setPreferredSize(new Dimension(250, 32));
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("Search a product".equals(searchField.getText())) {
                    searchField.setText("");
                    searchField.setForeground(AppTheme.FG_PRIMARY);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search a product");
                    searchField.setForeground(AppTheme.FG_SUBTLE);
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
        sidebarPanel.setPreferredSize(new Dimension(0, 56));
        centerTopPanel.add(sidebarPanel, BorderLayout.NORTH);
        centerTopPanel.add(searchPanel, BorderLayout.SOUTH);
        centerPanel.add(centerTopPanel, BorderLayout.NORTH);

        productGridPanel = new JPanel();
        productGridPanel.setLayout(new BoxLayout(productGridPanel, BoxLayout.Y_AXIS));
        productGridPanel.setBackground(AppTheme.BG_PRIMARY);
        productGridPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane productScroll = new JScrollPane(
            productGridPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        productScroll.setBorder(BorderFactory.createCompoundBorder(
            new ui.RoundedLineBorder(AppTheme.BORDER, ui.AppTheme.BORDER_THICKNESS, ui.AppTheme.BORDER_RADIUS),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        productScroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        centerPanel.add(productScroll, BorderLayout.CENTER);

        // ─── Right sidebar (order summary) ─────────────────────
        orderPanel = new JPanel(new BorderLayout());
        orderPanel.setBackground(Color.WHITE);
        orderPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(Color.WHITE);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE5E7EB), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Assemble ordering panel
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel northSection = new JPanel(new BorderLayout(0, 8));
        northSection.setOpaque(false);
        northSection.add(topBar, BorderLayout.NORTH);
        northSection.add(buildOrderStrip(), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        body.add(centerPanel, BorderLayout.CENTER);

        JScrollPane sidebarScroll = new JScrollPane(receiptPanel);
        sidebarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setPreferredSize(new Dimension(320, 0));
        sidebarScroll.setMinimumSize(new Dimension(320, 0));
        sidebarScroll.setMaximumSize(new Dimension(320, Integer.MAX_VALUE));
        sidebarScroll.getViewport().setBackground(Color.WHITE);
        sidebarScroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xE5E7EB)));
        body.add(sidebarScroll, BorderLayout.EAST);

        orderingPanel = new JPanel(new BorderLayout());
        orderingPanel.setBackground(AppTheme.BG_PRIMARY);
        orderingPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        orderingPanel.add(northSection, BorderLayout.NORTH);
        orderingPanel.add(body, BorderLayout.CENTER);

        orderListPanel = new OrderListPanel();
        orderListPanel.setRefreshCallback(this::refreshOrderStrip);
        contentPanel.add(orderingPanel, "Ordering");
        contentPanel.add(new SearchModule(), "Search");
        contentPanel.add(orderListPanel, "Order List");
        SwingUtilities.invokeLater(this::refreshOrderStrip);

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
            if (i == 0) totalItemsLabel = countLabel;
            else if (i == 1) lowStockLabel = countLabel;
            else if (i == 2) expiredLabel = countLabel;
            else if (i == 3) outOfStockLabel = countLabel;

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
        detailScroll.setBorder(new ui.RoundedLineBorder(new Color(60, 85, 120), ui.AppTheme.BORDER_THICKNESS, ui.AppTheme.BORDER_RADIUS));
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
        JPanel topNav = buildTopNavBar();
        getContentPane().add(topNav, BorderLayout.NORTH);
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildTopNavBar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(Color.WHITE);
        nav.setPreferredSize(new Dimension(0, 56));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        leftPanel.setOpaque(false);
        JLabel logoLabel = new JLabel("☕ Better Mondays");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoLabel.setForeground(new Color(0x111827));
        leftPanel.add(logoLabel);
        nav.add(leftPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 12));
        centerPanel.setOpaque(false);
        List<String> pages = new ArrayList<>(List.of(
            "Ordering",
            "Order List",
            "Search",
            "Inventory",
            "Monitoring",
            "Staff",
            "Inventory Guide",
            "About",
            "Help"
        ));
        if (currentUserRole == UserDataManager.Role.ADMIN) {
            pages.add(4, "Menu Maintenance");
            pages.add(5, "Register Product");
        }
        for (String page : pages) {
            JButton link = new JButton(page);
            link.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            link.setForeground(new Color(0x6B7280));
            link.setBackground(Color.WHITE);
            link.setFocusPainted(false);
            link.setBorderPainted(false);
            link.setContentAreaFilled(false);
            link.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            link.addActionListener(e -> {
                activeTopNavPage = page;
                cardLayout.show(contentPanel, page);
                refreshTopNavBarStyles();
            });
            centerPanel.add(link);
        }
        nav.add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        rightPanel.setOpaque(false);
        JLabel userLabel = new JLabel("👤 " + currentUsername);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setForeground(new Color(0x374151));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setForeground(new Color(0xEF4444));
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logoutAndReturnToLogin());
        rightPanel.add(userLabel);
        rightPanel.add(logoutBtn);
        nav.add(rightPanel, BorderLayout.EAST);

        topNavBar = nav;
        return nav;
    }

    private void refreshTopNavBarStyles() {
        if (topNavBar == null) {
            return;
        }
        topNavBar.setBackground(Color.WHITE);
        topNavBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)));
        for (java.awt.Component section : topNavBar.getComponents()) {
            if (!(section instanceof JPanel panel)) {
                continue;
            }
            for (java.awt.Component child : panel.getComponents()) {
                if (child instanceof JLabel label) {
                    String text = label.getText();
                    if (text != null && text.startsWith("👤")) {
                        label.setForeground(new Color(0x374151));
                    } else {
                        label.setForeground(new Color(0x111827));
                    }
                } else if (child instanceof JButton button) {
                    if ("Logout".equals(button.getText())) {
                        button.setForeground(new Color(0xEF4444));
                        button.setBackground(Color.WHITE);
                        button.setBorderPainted(false);
                        button.setContentAreaFilled(false);
                    } else {
                        boolean active = activeTopNavPage != null && activeTopNavPage.equals(button.getText());
                        button.setForeground(active ? new Color(0x3B82F6) : new Color(0x6B7280));
                        button.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
                        button.setBackground(Color.WHITE);
                        button.setBorderPainted(false);
                        button.setContentAreaFilled(false);
                    }
                }
            }
        }
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
        if (totalItemsLabel != null) totalItemsLabel.setText(String.valueOf(totalItems));
        if (lowStockLabel != null) lowStockLabel.setText(String.valueOf(lowStock));
        if (expiredLabel != null) expiredLabel.setText(String.valueOf(expired));
        if (outOfStockLabel != null) outOfStockLabel.setText(String.valueOf(outOfStock));
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
        String entered = cashpayment.getText();
        if (!entered.isEmpty()) {
            cashpayment.setText(entered);
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
    // TODO: legacy — used only in old pay/reset flow, refactor to use orderEntries instead
    private javax.swing.JLabel JLabelTax;
    private javax.swing.JTextField cashpayment;
    private javax.swing.JButton exit;
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
    public javax.swing.JPanel jPanelInventory;
    private javax.swing.JPanel jPanelMonitoring;
    private javax.swing.JPanel jPanelPOS;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private SidebarPanel sidebar;
    private JPanel orderingPanel;
    // TODO: legacy — used only in old pay/reset flow, refactor to use orderEntries instead
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTableMonitoring;
    private javax.swing.JTable jTableSales;
    // TODO: legacy — used only in old pay/reset flow, refactor to use orderEntries instead
    private javax.swing.JTextField jTextFieldChange;
    private javax.swing.JTextField jTextFieldSubTotal;
    private javax.swing.JTextField jTextFieldTax;
    private javax.swing.JTextField jTextFieldTotal;
    private javax.swing.JButton pay;
    private javax.swing.JButton removeitem;
    private javax.swing.JButton resetorder;
}
