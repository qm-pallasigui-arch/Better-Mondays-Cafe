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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Cursor;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import loginregister.Login;
import loginregister.UserDataManager;
import loginregister.UserDataManager.Role;
import javax.swing.JPasswordField;
import javax.swing.JDialog;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BasicStroke;
import javax.swing.border.EmptyBorder;
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
import ui.InventoryPanel;
import persistence.sqlite.SQLiteInventoryRepository;
import persistence.sqlite.SQLiteStaffShiftRepository;
import persistence.sqlite.SQLiteSalesRepository;
import persistence.sqlite.SQLiteUserRepository;
import persistence.sqlite.SQLiteProfilePictureRepository;
import controller.InventoryController;
import controller.InventoryRowView;
import controller.OrderController;
import inventory.InventoryItem;

public class POSSystem extends javax.swing.JFrame {

    private MonitoringPanel monitoringPanel;
    private InventoryRegistrationPanel inventoryRegistrationPanel;
    private InventoryPanel inventoryPanel;
    private UserDataManager.Role currentUserRole;
    private String currentUsername;
    private InventoryController inventoryController;
    private OrderController orderController;

    private JLabel topNavUserIcon;
    private JLabel topNavUserName;
    private JLabel topNavUserRole;

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

        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        orderController = new OrderController(new SQLiteSalesRepository());

        monitoringPanel = new MonitoringPanel();

        AppTheme.applyToFrame(this);

        if (orderingPanel != null)
            orderingPanel.refreshCategoryPills();
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

        try {
            persistence.StaffShiftRepository shiftRepo = new persistence.sqlite.SQLiteStaffShiftRepository();
            shiftRepo.endShift(currentUsername, "Auto-ended on logout");
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(POSSystem.class.getName())
                    .log(java.util.logging.Level.WARNING,
                            "Could not auto-end shift for " + currentUsername, e);
            JOptionPane.showMessageDialog(this,
                    "Logged out, but shift could not be ended automatically.\n" + e.getMessage(),
                    "Shift Warning", JOptionPane.WARNING_MESSAGE);
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

        if (SPECIALTY_DRINKS.contains(itemName))
            return "Specialty Drinks";
        if (TEA_LATTE_ITEMS.contains(itemName))
            return "Tea Latte";
        if (NON_COFFEE_ITEMS.contains(itemName))
            return "Non-Coffee";
        if (FRUIT_TEA_ITEMS.contains(itemName))
            return "Fruit Tea";
        if (HERBAL_TEA_ITEMS.contains(itemName))
            return "Herbal Tea";
        if (SANDWICH_ITEMS.contains(itemName))
            return "Sandwiches";
        if (PANDESAL_PAIR_ITEMS.contains(itemName))
            return "Pandesal Pairs";
        if (PASTRY_ITEMS.contains(itemName))
            return "Pastries";
        if (HOUSE_FAVORITES_PRICES.containsKey(itemName))
            return "House Favorites";
        if ("Coffee".equals(item.getCategory()))
            return "Espresso & Coffee";

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
            orderController.persistCompletedTransaction(transactionRef, salesList, subTotal, cash, change, "Walk-in");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save sales to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        if (monitoringPanel != null)
            monitoringPanel.refreshData();
        if (inventoryPanel != null)
            inventoryPanel.refresh();

        orderCount++;
        orderEntries.clear();
        refreshOrderDisplay();
    }

    // ─── Top Navigation Bar ──────────────────────────────────────
    private JPanel buildTopNavBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(AppTheme.BG_SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                new EmptyBorder(0, 24, 0, 20)));
        bar.setPreferredSize(new Dimension(0, 56));

        // Left — live clock
        JLabel clockLabel = new JLabel();
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clockLabel.setForeground(AppTheme.FG_MUTED);
        Timer clockTimer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            clockLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MM/dd/yyyy, hh:mm:ss a")));
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();
        JPanel clockPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clockPanel.setOpaque(false);
        clockPanel.add(clockLabel);
        bar.add(clockPanel, BorderLayout.WEST);

        // Right side — status indicators + user profile
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        profilePanel.setOpaque(false);
        profilePanel.setBorder(new EmptyBorder(0, 0, 0, 4));

        // Online indicator
        JPanel onlinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        onlinePanel.setOpaque(false);
        JLabel dot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.SUCCESS);
                int d = 8;
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setOpaque(false);
        JLabel onlineText = new JLabel("Online");
        onlineText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        onlineText.setForeground(AppTheme.SUCCESS);
        onlinePanel.add(dot);
        onlinePanel.add(onlineText);

        // Notification bell
        int notifCount = ui.MonitoringPanel.getPendingReportCount();
        JPanel bellPanel = new JPanel(new GridBagLayout());
        bellPanel.setOpaque(false);
        JLabel bellIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int cx = w / 2, by = h - 6;
                g2.setColor(new Color(0x2563EB));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawArc(cx - 6, by - 12, 12, 10, 0, 180);
                g2.drawLine(cx - 4, by, cx + 4, by);
                g2.fillOval(cx - 1, by - 14, 3, 3);
                g2.dispose();
            }
        };
        bellIcon.setPreferredSize(new Dimension(22, 22));
        bellIcon.setOpaque(false);
        bellIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ui.MonitoringPanel.showNotificationsDialog(SwingUtilities.windowForComponent(bellPanel));
            }
        });
        bellPanel.add(bellIcon);

        if (notifCount > 0) {
            JLabel badge = new JLabel(String.valueOf(notifCount), SwingConstants.CENTER);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 9));
            badge.setForeground(Color.WHITE);
            badge.setBackground(AppTheme.DANGER);
            badge.setOpaque(true);
            badge.setPreferredSize(new Dimension(16, 16));
            badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            GridBagConstraints bc = new GridBagConstraints();
            bc.gridx = 1;
            bc.gridy = 0;
            bc.anchor = GridBagConstraints.NORTHWEST;
            bellPanel.add(badge, bc);
        }

        // User avatar
        topNavUserIcon = new JLabel(String.valueOf(Character.toUpperCase(currentUsername.charAt(0)))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentUserRole == Role.ADMIN ? AppTheme.ACCENT_DARK : AppTheme.SUCCESS);
                int size = Math.min(getWidth(), getHeight());
                g2.fillOval(0, 0, size - 1, size - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        topNavUserIcon.setPreferredSize(new Dimension(36, 36));
        topNavUserIcon.setMinimumSize(new Dimension(36, 36));
        topNavUserIcon.setHorizontalAlignment(SwingConstants.CENTER);
        topNavUserIcon.setOpaque(false);

        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.setPreferredSize(new Dimension(40, 40));
        avatarWrap.add(topNavUserIcon);

        // Name + role pill
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        topNavUserName = new JLabel(currentUsername);
        topNavUserName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topNavUserName.setForeground(AppTheme.FG_PRIMARY);

        boolean isAdmin = currentUserRole == Role.ADMIN;
        topNavUserRole = new JLabel(isAdmin ? "Admin" : "Staff");
        topNavUserRole.setFont(new Font("Segoe UI", Font.BOLD, 10));
        topNavUserRole.setForeground(isAdmin ? AppTheme.ACCENT_DARK : AppTheme.SUCCESS);
        topNavUserRole.setBackground(isAdmin ? AppTheme.BG_BADGE_BLUE : AppTheme.BG_BADGE_GREEN);
        topNavUserRole.setOpaque(true);
        topNavUserRole.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 6, 1, 6),
                BorderFactory.createLineBorder(isAdmin ? AppTheme.ACCENT_DARK : AppTheme.SUCCESS, 1)));
        topNavUserRole.setAlignmentX(Component.LEFT_ALIGNMENT);

        namePanel.add(topNavUserName);
        namePanel.add(Box.createVerticalStrut(2));
        namePanel.add(topNavUserRole);

        JButton settingsBtn = new JButton();
        settingsBtn.putClientProperty("appTheme.variant", "transparent");
        settingsBtn.setIcon(createOverflowIcon());
        settingsBtn.setPreferredSize(new Dimension(24, 24));
        settingsBtn.setForeground(AppTheme.FG_MUTED);
        settingsBtn.setBackground(new Color(0, 0, 0, 0));
        settingsBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setOpaque(false);
        settingsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsBtn.addActionListener(e -> showProfileSettingsDialog());

        profilePanel.add(onlinePanel);
        if (isAdmin) {
            profilePanel.add(bellPanel);
        }
        profilePanel.add(Box.createHorizontalStrut(4));
        profilePanel.add(avatarWrap);
        profilePanel.add(Box.createHorizontalStrut(2));
        profilePanel.add(namePanel);
        profilePanel.add(Box.createHorizontalStrut(4));
        profilePanel.add(settingsBtn);

        bar.add(profilePanel, BorderLayout.EAST);
        return bar;
    }

    private Icon createOverflowIcon() {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }

            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.FG_MUTED);
                g2.fillOval(x + 3, y + 1, 4, 4);
                g2.fillOval(x + 3, y + 5, 4, 4);
                g2.fillOval(x + 3, y + 9, 4, 4);
                g2.dispose();
            }
        };
    }

    private void showProfileSettingsDialog() {
        JDialog dialog = new JDialog(this, "User Settings", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(0, 14));
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        content.setBackground(AppTheme.BG_SURFACE);

        JPanel summary = new JPanel(new GridLayout(0, 1, 0, 4));
        summary.setOpaque(false);
        JLabel title = new JLabel("User Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel current = new JLabel("Current username: " + currentUsername);
        current.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        current.setForeground(AppTheme.FG_MUTED);
        summary.add(title);
        summary.add(current);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 10));
        actions.setOpaque(false);

        JButton checkUsername = new JButton("Check Username");
        JButton editUsername = new JButton("Edit Username");
        JButton changePassword = new JButton("Change Password");
        JButton close = new JButton("Close");

        checkUsername.addActionListener(ae -> JOptionPane.showMessageDialog(
                dialog, "Current username: " + currentUsername, "Username",
                JOptionPane.INFORMATION_MESSAGE));
        editUsername.addActionListener(ae -> showEditUsernameDialog());
        changePassword.addActionListener(ae -> showChangePasswordDialog());
        close.addActionListener(ae -> dialog.dispose());

        styleSettingsButton(checkUsername);
        styleSettingsButton(editUsername);
        styleSettingsButton(changePassword);
        styleSettingsButton(close);

        actions.add(checkUsername);
        actions.add(editUsername);
        actions.add(changePassword);
        actions.add(close);

        content.add(summary, BorderLayout.NORTH);
        content.add(actions, BorderLayout.CENTER);
        AppTheme.applyToComponent(content);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void styleSettingsButton(JButton button) {
        if (button == null)
            return;
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(220, 36));
        button.setMinimumSize(new Dimension(220, 36));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void showEditUsernameDialog() {
        JTextField usernameField = new JTextField(currentUsername, 18);
        JPasswordField currentPasswordField = new JPasswordField(18);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("New username"));
        panel.add(usernameField);
        panel.add(new JLabel("Current password"));
        panel.add(currentPasswordField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Edit Username",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String newUsername = usernameField.getText().trim();
        String currentPassword = new String(currentPasswordField.getPassword()).trim();
        if (newUsername.isEmpty() || currentPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.",
                    "Edit Username", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newUsername.equals(currentUsername)) {
            JOptionPane.showMessageDialog(this, "New username must be different.",
                    "Edit Username", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (UserDataManager.updateUsername(currentUsername, newUsername, currentPassword)) {
            currentUsername = newUsername;
            topNavUserName.setText(newUsername);
            topNavUserIcon.setText(String.valueOf(Character.toUpperCase(newUsername.charAt(0))));
            setTitle("Better Mondays Coffeee Cafe Management System - " + newUsername);
            JOptionPane.showMessageDialog(this, "Username updated.",
                    "Edit Username", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Unable to update username. Check your password or whether the username is already in use.",
                    "Edit Username", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showChangePasswordDialog() {
        JPasswordField currentPasswordField = new JPasswordField(18);
        JPasswordField newPasswordField = new JPasswordField(18);
        JPasswordField confirmPasswordField = new JPasswordField(18);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Current password"));
        panel.add(currentPasswordField);
        panel.add(new JLabel("New password"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Confirm new password"));
        panel.add(confirmPasswordField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String currentPassword = new String(currentPasswordField.getPassword()).trim();
        String newPassword = new String(newPasswordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All password fields are required.",
                    "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.",
                    "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isStrongPassword(newPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (UserDataManager.updatePassword(currentUsername, currentPassword, newPassword)) {
            JOptionPane.showMessageDialog(this, "Password updated.",
                    "Change Password", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Unable to update password. Check your current password.",
                    "Change Password", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 8
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    // ─── initComponents ─────────────────────────────────────────
    private void initComponents() {
        jPanelPOS = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        sidebar = new SidebarPanel(currentUsername, currentUserRole, page -> {
            cardLayout.show(contentPanel, page);
        });
        sidebar.setLogoutListener(this::logoutAndReturnToLogin);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_PRIMARY);

        boolean isAdmin = currentUserRole == Role.ADMIN;

        // ── Ordering tab ────────────────────────────────────────
        orderingPanel = new OrderingPanel(() -> {
            if (inventoryPanel != null)
                inventoryPanel.refresh();
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

        // ── Inventory tab — delegated to InventoryPanel ─────────
        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        inventoryPanel = new InventoryPanel(isAdmin, inventoryController, () -> {
            if (monitoringPanel != null)
                monitoringPanel.refreshData();
        });
        contentPanel.add(inventoryPanel, "Inventory");

        // ── Monitoring tab ──────────────────────────────────────
        monitoringPanel = new MonitoringPanel(isAdmin);
        monitoringPanel.setBackground(AppTheme.BG_PRIMARY);
        contentPanel.add(monitoringPanel, "Monitoring");

        // ── Other tabs ──────────────────────────────────────────
        try {
            contentPanel.add(new MenuMaintenancePanel(isAdmin), "Menu Maintenance");
        } catch (Exception e) {
            System.err.println("MenuMaintenancePanel init failed: " + e.getMessage());
        }
        try {
            inventoryRegistrationPanel = new InventoryRegistrationPanel(
                    new SQLiteInventoryRepository(),
                    () -> {
                        if (inventoryPanel != null)
                            inventoryPanel.refresh();
                    },
                    () -> {
                        if (monitoringPanel != null)
                            monitoringPanel.refreshData();
                    });
            contentPanel.add(inventoryRegistrationPanel, "Register Product");
        } catch (Exception e) {
            System.err.println("InventoryRegistrationPanel init failed: " + e.getMessage());
        }
        try {
            contentPanel.add(new StaffPanel(
                    new SQLiteStaffShiftRepository(),
                    new SQLiteUserRepository(),
                    new SQLiteProfilePictureRepository(),
                    currentUsername, currentUserRole), "Staff");
        } catch (Exception e) {
            System.err.println("StaffPanel init failed: " + e.getMessage());
        }
        contentPanel.add(new InventoryGuidePanel(), "Inventory Guide");
        contentPanel.add(new AboutModule(), "About");
        contentPanel.add(new HelpModule(), "Help");

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.WEST);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(buildTopNavBar(), BorderLayout.NORTH);
        centerWrapper.add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(centerWrapper, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    // ─── Legacy handler stubs (old POS table) ───────────────────
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
                    "Payment Successful!\nSubtotal: " + jTextFieldSubTotal.getText()
                            + "\nVAT (12%): " + jTextFieldTax.getText()
                            + "\nTotal: " + jTextFieldTotal.getText()
                            + "\nCash: \u20B1" + String.format("%.2f", cash)
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

    private void chooseHotOrIced(String rawProductName) {
        Object[] options = { "Hot", "Regular Iced", "Large Iced" };
        int choice = JOptionPane.showOptionDialog(this,
                "Choose variant for " + rawProductName + ":", "Choose Variant",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice == JOptionPane.CLOSED_OPTION || choice < 0)
            return;
        String variant = options[choice].toString();
        String displayName = rawProductName + " (" + variant + ")";
        double price = Menu.getInstance().getPrice(rawProductName, variant);
        if (price > 0)
            addItem(displayName, price);
    }

    // ─── Event handlers (legacy POS table) ──────────────────────
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
                "Better Mondays Coffeee Cafe Management System",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
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
            orderController.persistCompletedTransaction(transactionRef, salesList, subTotal, cash, change, "Walk-in");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save sales to database: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        String lineSep = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
        String receiptStr = " ☕ Better Mondays Cafe ☕\n 123 Main St., Manila\n VAT REG TIN: 123-456-789\n"
                + lineSep
                + "Date: " + new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date()) + "\n"
                + "Txn #: " + transactionRef + "\n" + lineSep
                + String.format("%-15s %3s %8s\n", "ITEM", "QTY", "AMOUNT")
                + "───────────────────────────────\n";
        for (SalesRecord s : salesList) {
            receiptStr += String.format("%-15s %3d %8.2f\n",
                    truncate(s.getProductName(), 15), s.getQuantity(), s.getTotal());
        }
        receiptStr += "───────────────────────────────\n"
                + String.format("%-23s %8.2f\n", "Subtotal (excl VAT):", subTotal / 1.12)
                + String.format("%-23s %8.2f\n", "VAT (12%):", subTotal * 0.12 / 1.12)
                + String.format("%-23s %8.2f\n", "TOTAL (incl VAT):", subTotal)
                + String.format("%-23s %8.2f\n", "Cash:", cash)
                + String.format("%-23s %8.2f\n", "Change:", change)
                + lineSep
                + " Thank you! Come again!\n *** Have a nice day ***\n";

        JOptionPane.showMessageDialog(this, receiptStr,
                "✅ RECEIPT - " + transactionRef, JOptionPane.INFORMATION_MESSAGE);

        orderModel.setRowCount(0);
        jTextFieldChange.setText("");
        jTextFieldTax.setText("");
        jTextFieldTotal.setText("");
        jTextFieldSubTotal.setText("");
        cashpayment.setText("");

        if (inventoryPanel != null)
            inventoryPanel.refresh();
        if (monitoringPanel != null)
            monitoringPanel.refreshData();
    }

    private static int loadTransactionCounter() {
        try (Connection conn = persistence.AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT transaction_ref FROM sales_transactions ORDER BY id DESC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return parseTransactionNumber(rs.getString(1));
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelSubTotal;
    private javax.swing.JLabel jLabelTotal;
    private javax.swing.JLabel jLabelTotal1;
    private javax.swing.JLabel jLabelTotal2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanelMonitoring;
    private javax.swing.JPanel jPanelPOS;
    private javax.swing.JPanel jPanelSummary;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
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