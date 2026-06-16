package pos;

import inventory.Inventory;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import ui.MonitoringPanel;
import java.util.List;
import java.util.ArrayList;
import monitoring.SalesRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.text.SimpleDateFormat;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.concurrent.atomic.AtomicReference;
import loginregister.Login;
import loginregister.UserDataManager;
import loginregister.UserDataManager.Role;
import ui.AppTheme;
import ui.SidebarPanel;
import ui.InventoryPanel;
import ui.NotificationsDialog;
import ui.UserSettingsDialog;
import ui.OrderQueuePanel; // ← NEW
import persistence.sqlite.SQLiteInventoryRepository;
import persistence.sqlite.SQLiteStaffShiftRepository;
import persistence.sqlite.SQLiteSalesRepository;
import persistence.sqlite.SQLiteUserRepository;
import persistence.sqlite.SQLiteProfilePictureRepository;
import persistence.OrderSyncClient;
import controller.InventoryController;
import controller.OrderController;
import ui.MenuMaintenancePanel;
import ui.SearchModule;
import ui.AboutModule;
import ui.HelpModule;
import ui.InventoryRegistrationPanel;
import ui.StaffPanel;
import ui.InventoryGuidePanel;

public class POSSystem extends javax.swing.JFrame {

    private MonitoringPanel monitoringPanel;
    private InventoryRegistrationPanel inventoryRegistrationPanel;
    private InventoryPanel inventoryPanel;
    private UserDataManager.Role currentUserRole;
    private String currentUsername;
    private InventoryController inventoryController;
    private OrderController orderController;

    // ── NEW: Order Queue panel reference ────────────────────────
    private OrderQueuePanel orderQueuePanel;

    // ── Top-nav label references (updated on username change) ────
    private JLabel topNavUserIcon;
    private JLabel topNavUserName;
    private JLabel topNavUserRole;

    // ── Modernized nav bar palette ───────────────────────────────
    private static final Color NAV_BG = new Color(0x12, 0x1A, 0x2B);
    private static final Color NAV_BORDER_COLOR = new Color(0x27, 0x38, 0x50);
    private static final Color CLOCK_TIME_FG = new Color(0xF0, 0xF4, 0xFF);
    private static final Color CLOCK_DATE_FG = new Color(0x70, 0x8A, 0xA8);
    private static final Color ONLINE_DOT_COLOR = new Color(0x34, 0xD3, 0x99);
    private static final Color ADMIN_PILL_BG = new Color(0x1E, 0x3A, 0x8A, 100);
    private static final Color ADMIN_PILL_FG = new Color(0x93, 0xC5, 0xFD);
    private static final Color ADMIN_PILL_BD = new Color(0x3B, 0x82, 0xF6, 140);
    private static final Color STAFF_PILL_BG = new Color(0x06, 0x4E, 0x3B, 100);
    private static final Color STAFF_PILL_FG = new Color(0x6E, 0xE7, 0xB7);
    private static final Color STAFF_PILL_BD = new Color(0x34, 0xD3, 0x99, 140);
    private static final Color AVATAR_TOP_ADMIN = new Color(0x3B, 0x82, 0xF6);
    private static final Color AVATAR_BOT_ADMIN = new Color(0x1D, 0x4E, 0xD8);
    private static final Color AVATAR_TOP_STAFF = new Color(0x34, 0xD3, 0x99);
    private static final Color AVATAR_BOT_STAFF = new Color(0x05, 0x96, 0x69);

    private static class OrderEntry {
        String name;
        String variant;
        int quantity;
        double unitPrice;

        OrderEntry(String name, String variant, int quantity, double unitPrice) {
            this.name = name;
            this.variant = variant == null ? "" : variant;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        String displayName() {
            return variant == null || variant.isEmpty() ? name : name + " (" + variant + ")";
        }

        double lineTotal() {
            return quantity * unitPrice;
        }
    }

    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private int orderCount = 0;
    private JPanel orderPanel;
    private JPanel receiptPanel;

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────
    public POSSystem(String username, UserDataManager.Role role) {
        this.currentUserRole = role;
        this.currentUsername = username;
        try {
            persistence.Phase2Bootstrap.seedCatalogIfEmpty();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database initialization warning: " + e.getMessage(),
                    "Database",
                    JOptionPane.WARNING_MESSAGE);
        }

        initComponents();
        setTitle("Better Mondays Coffee Cafe Management System - " + username);
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

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────
    private void logoutAndReturnToLogin() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out of the current session?",
                "Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

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

    // ─────────────────────────────────────────────────────────────
    // Order display helpers
    // ─────────────────────────────────────────────────────────────
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
            if (entry.quantity > 1)
                entry.quantity--;
            else
                orderEntries.remove(entry);
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
        for (OrderEntry entry : orderEntries)
            totalInclusive += entry.lineTotal();
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

        receiptPanel.add(new JScrollPane(orderPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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

    // ─────────────────────────────────────────────────────────────
    // showReceipt – now also pushes the order to the Order Queue
    // ─────────────────────────────────────────────────────────────
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

        // Deduct inventory
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

        // Persist sales
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

        // ── NEW: Push order to Order Queue panel ─────────────────
        if (orderQueuePanel != null) {
            List<String> queueItems = new ArrayList<>();
            for (OrderEntry entry : orderEntries) {
                queueItems.add(entry.displayName() + " x" + entry.quantity);
            }
            orderQueuePanel.addOrder(transactionRef, queueItems);
        }

        if (monitoringPanel != null)
            monitoringPanel.refreshData();
        if (inventoryPanel != null)
            inventoryPanel.refresh();

        orderCount++;
        orderEntries.clear();
        refreshOrderDisplay();
    }

    // ═══════════════════════════════════════════════════════════════
    // ─── MODERNIZED Top Navigation Bar ─────────────────────────────
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildTopNavBar() {
        boolean isAdmin = currentUserRole == Role.ADMIN;

        JPanel bar = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(NAV_BORDER_COLOR);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        bar.setBackground(NAV_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 20));
        bar.setPreferredSize(new Dimension(0, 60));

        // ── LEFT: live clock ──────────────────────────────────────
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        timeLabel.setForeground(CLOCK_TIME_FG);

        JLabel dateLabel = new JLabel();
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateLabel.setForeground(CLOCK_DATE_FG);
        dateLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

        Timer clockTimer = new Timer(500, e -> {
            LocalDateTime now = LocalDateTime.now();
            timeLabel.setText(now.format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
            dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();

        JPanel clockPanel = new JPanel();
        clockPanel.setLayout(new BoxLayout(clockPanel, BoxLayout.Y_AXIS));
        clockPanel.setOpaque(false);
        clockPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        clockPanel.add(timeLabel);
        clockPanel.add(dateLabel);
        bar.add(clockPanel, BorderLayout.WEST);

        // ── RIGHT ─────────────────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);

        // Animated online pill
        int[] dotAlpha = { 255 };
        int[] dotDir = { -4 };
        JPanel dotIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(
                        ONLINE_DOT_COLOR.getRed(),
                        ONLINE_DOT_COLOR.getGreen(),
                        ONLINE_DOT_COLOR.getBlue(),
                        dotAlpha[0] / 4));
                g2.fillOval(0, 0, 12, 12);
                g2.setColor(ONLINE_DOT_COLOR);
                g2.fillOval(3, 3, 6, 6);
                g2.dispose();
            }
        };
        dotIcon.setOpaque(false);
        dotIcon.setPreferredSize(new Dimension(12, 12));

        Timer pulseTimer = new Timer(40, e -> {
            dotAlpha[0] += dotDir[0];
            if (dotAlpha[0] <= 80)
                dotDir[0] = 4;
            if (dotAlpha[0] >= 255)
                dotDir[0] = -4;
            dotIcon.repaint();
        });
        pulseTimer.start();

        JLabel onlineText = new JLabel("Online");
        onlineText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        onlineText.setForeground(ONLINE_DOT_COLOR);

        JPanel onlinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        onlinePanel.setOpaque(false);
        onlinePanel.add(dotIcon);
        onlinePanel.add(onlineText);

        right.add(onlinePanel);
        right.add(Box.createRigidArea(new Dimension(12, 0)));

        // Bell icon (admin only)
        if (isAdmin) {
            int notifCount = NotificationsDialog.getPendingCount();
            int wrapW = notifCount > 0 ? 34 : 28;

            JPanel bellWrap = new JPanel(null);
            bellWrap.setOpaque(false);
            bellWrap.setPreferredSize(new Dimension(wrapW, 34));

            Color bellDefaultColor = ADMIN_PILL_FG;
            Color bellHoverColor = new Color(0xC0, 0xCC, 0xD6);
            boolean[] bellHovered = { false };

            JLabel bellIcon = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color c = bellHovered[0] ? bellHoverColor : bellDefaultColor;
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    g2.drawArc(cx - 7, 3, 14, 12, 0, 180);
                    g2.drawLine(cx - 7, 9, cx - 7, 16);
                    g2.drawLine(cx + 7, 9, cx + 7, 16);
                    g2.drawLine(cx - 8, 16, cx + 8, 16);
                    g2.fillOval(cx - 2, 17, 4, 3);
                    g2.fillOval(cx - 2, 1, 4, 3);
                    g2.dispose();
                }
            };
            bellIcon.setBounds(0, 6, 22, 22);
            bellIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bellIcon.setToolTipText("Notifications");
            bellIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    bellHovered[0] = true;
                    bellIcon.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    bellHovered[0] = false;
                    bellIcon.repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    NotificationsDialog.show(SwingUtilities.windowForComponent(bellWrap));
                }
            });
            bellWrap.add(bellIcon);

            if (notifCount > 0) {
                JLabel badge = new JLabel(notifCount > 9 ? "9+" : String.valueOf(notifCount),
                        SwingConstants.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0xEF, 0x44, 0x44));
                        g2.fillOval(0, 0, getWidth(), getHeight());
                        super.paintComponent(g);
                        g2.dispose();
                    }
                };
                badge.setFont(new Font("Segoe UI", Font.BOLD, 8));
                badge.setForeground(Color.WHITE);
                badge.setOpaque(false);
                badge.setBounds(wrapW - 15, 0, 15, 15);
                badge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                badge.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        NotificationsDialog.show(SwingUtilities.windowForComponent(bellWrap));
                    }
                });
                bellWrap.add(badge);
            }

            bellWrap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            bellWrap.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    NotificationsDialog.show(SwingUtilities.windowForComponent(bellWrap));
                }
            });

            right.add(bellWrap);
            right.add(Box.createRigidArea(new Dimension(12, 0)));
        }

        // Thin divider
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(NAV_BORDER_COLOR);
                g.fillRect(0, 8, 1, getHeight() - 16);
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(1, 44));
        right.add(divider);
        right.add(Box.createRigidArea(new Dimension(14, 0)));

        // Gradient avatar
        Color avatarTop = isAdmin ? AVATAR_TOP_ADMIN : AVATAR_TOP_STAFF;
        Color avatarBot = isAdmin ? AVATAR_BOT_ADMIN : AVATAR_BOT_STAFF;
        String initLetter = currentUsername.isEmpty() ? "?"
                : String.valueOf(Character.toUpperCase(currentUsername.charAt(0)));

        topNavUserIcon = new JLabel(initLetter) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 10;
                GradientPaint gp = new GradientPaint(0, 0, avatarTop, 0, getHeight(), avatarBot);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, arc, arc));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                g2.drawString(txt,
                        (getWidth() - fm.stringWidth(txt)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        topNavUserIcon.setPreferredSize(new Dimension(36, 36));
        topNavUserIcon.setMinimumSize(new Dimension(36, 36));
        topNavUserIcon.setOpaque(false);
        topNavUserIcon.setHorizontalAlignment(SwingConstants.CENTER);

        right.add(topNavUserIcon);
        right.add(Box.createRigidArea(new Dimension(10, 0)));

        // Name + role pill
        Color pillBg = isAdmin ? ADMIN_PILL_BG : STAFF_PILL_BG;
        Color pillFg = isAdmin ? ADMIN_PILL_FG : STAFF_PILL_FG;
        Color pillBd = isAdmin ? ADMIN_PILL_BD : STAFF_PILL_BD;

        topNavUserName = new JLabel(currentUsername);
        topNavUserName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topNavUserName.setForeground(CLOCK_TIME_FG);

        topNavUserRole = new JLabel(isAdmin ? "Admin" : "Staff", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = getHeight();
                g2.setColor(pillBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(pillBd);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        topNavUserRole.setFont(new Font("Segoe UI", Font.BOLD, 10));
        topNavUserRole.setForeground(pillFg);
        topNavUserRole.setOpaque(false);
        topNavUserRole.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        topNavUserRole.setPreferredSize(new Dimension(68, 18));

        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);
        nameStack.add(topNavUserName);
        nameStack.add(Box.createVerticalStrut(3));
        nameStack.add(topNavUserRole);

        right.add(nameStack);
        right.add(Box.createRigidArea(new Dimension(10, 0)));

        // Settings ⋮ button
        boolean[] btnHovered = { false };
        JButton settingsBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (btnHovered[0]) {
                    g2.setColor(new Color(255, 255, 255, 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.setColor(new Color(0x8A, 0xA8, 0xC4));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.fillOval(cx - 2, cy - 8, 4, 4);
                g2.fillOval(cx - 2, cy - 2, 4, 4);
                g2.fillOval(cx - 2, cy + 4, 4, 4);
                g2.dispose();
            }
        };
        settingsBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnHovered[0] = true;
                settingsBtn.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnHovered[0] = false;
                settingsBtn.repaint();
            }
        });
        settingsBtn.setPreferredSize(new Dimension(28, 36));
        settingsBtn.setOpaque(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setFocusPainted(false);
        settingsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsBtn.setToolTipText("User Settings");
        settingsBtn.addActionListener(e -> showProfileSettingsDialog());

        right.add(settingsBtn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────
    // Profile settings dialog
    // ─────────────────────────────────────────────────────────────
    private void showProfileSettingsDialog() {
        UserSettingsDialog dialog = new UserSettingsDialog(
                this,
                currentUsername,
                currentUserRole,
                newUsername -> {
                    currentUsername = newUsername;
                    topNavUserName.setText(newUsername);
                    topNavUserIcon.setText(
                            String.valueOf(Character.toUpperCase(newUsername.charAt(0))));
                    topNavUserIcon.repaint();
                    setTitle("Better Mondays Coffee Cafe Management System - " + newUsername);
                });

        dialog.addPropertyChangeListener("logout", evt -> {
            if (Boolean.TRUE.equals(evt.getNewValue())) {
                dialog.dispose();
                logoutAndReturnToLogin();
            }
        });

        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // initComponents
    // ─────────────────────────────────────────────────────────────
    private void initComponents() {
        sidebar = new SidebarPanel(currentUsername, currentUserRole, page -> {
            cardLayout.show(contentPanel, page);
        });
        sidebar.setLogoutListener(this::logoutAndReturnToLogin);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_PRIMARY);

        boolean isAdmin = currentUserRole == Role.ADMIN;

        // ── Ordering ─────────────────────────────────────────────
        orderingPanel = new OrderingPanel(() -> {
            if (inventoryPanel != null)
                inventoryPanel.refresh();
            if (monitoringPanel != null)
                monitoringPanel.refreshData();
        });
        orderingPanel.setBackground(AppTheme.BG_PRIMARY);
        contentPanel.add(orderingPanel, "Ordering");
        contentPanel.add(new SearchModule(), "Search");

        Menu.getInstance().addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            if (orderingPanel != null) {
                orderingPanel.refreshCategoryPills();
                orderingPanel.rebuildProducts();
            }
        }));

        // ── NEW: Order Queue ─────────────────────────────────────
        orderQueuePanel = new OrderQueuePanel();
        contentPanel.add(orderQueuePanel, "Order Queue");

        // ── Wire OrderSyncClient (shared) ─────────────────────────
        OrderSyncClient orderSyncClient = new OrderSyncClient();
        orderSyncClient.setOnNewOrder(receipt -> SwingUtilities.invokeLater(() -> orderQueuePanel.addOrder(receipt)));
        orderSyncClient.setOnStatusChange(payload -> SwingUtilities
                .invokeLater(() -> orderQueuePanel.applyRemoteStatus(payload[0], payload[1])));

        // When other instances update menu/inventory, reload repository and refresh UI
        orderSyncClient.setOnMenuUpdate(ent -> SwingUtilities.invokeLater(() -> {
            try {
                Menu.getInstance().reloadFromRepository();
                if (orderingPanel != null) {
                    orderingPanel.rebuildProducts();
                }
            } catch (Exception ignored) {
            }
        }));

        // Suppression flag to avoid re-broadcasting changes we load from remote
        final AtomicBoolean suppressPublish = new AtomicBoolean(false);

        orderSyncClient.setOnInventoryUpdate(ent -> SwingUtilities.invokeLater(() -> {
            try {
                suppressPublish.set(true);
                inventory.Inventory.getInstance().reloadFromRepository();
                if (inventoryPanel != null)
                    inventoryPanel.refresh();
                // Skip expensive UI rebuilds on every inventory update
                // They'll refresh via monitoring and order UI separately
            } finally {
                suppressPublish.set(false);
            }
        }));

        // ── Wire OrderingPanel → OrderQueuePanel ──────────────────
        orderingPanel.setOrderQueuePanel(orderQueuePanel);
        orderQueuePanel.setOnKitchenCompleted(posOrderId -> SwingUtilities
                .invokeLater(() -> orderingPanel.markOrderCompletedFromKitchen(posOrderId)));

        // Publish local kitchen status changes to server
        orderQueuePanel.setOnKitchenStatusChanged(payload -> {
            orderSyncClient.publishStatusChange(payload[0], payload[1]);
        });

        // Debounce menu changes (batch rapid edits, don't send every keystroke)
        final AtomicReference<Timer> menuBroadcastTimer = new AtomicReference<>();
        Menu.getInstance().addChangeListener(() -> {
            if (suppressPublish.get())
                return; // avoid loops

            Timer existingTimer = menuBroadcastTimer.getAndSet(null);
            if (existingTimer != null)
                existingTimer.stop();

            // Wait 300ms before sending, to batch rapid changes
            Timer newTimer = new Timer(300, e -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    for (var item : Menu.getInstance().getAllItems().values()) {
                        if (!first)
                            sb.append(',');
                        first = false;
                        sb.append('{');
                        sb.append("\"name\":").append(q(item.getName())).append(',');
                        sb.append("\"hotPrice\":").append(item.getHotPrice()).append(',');
                        sb.append("\"icedRegularPrice\":").append(item.getIcedRegularPrice()).append(',');
                        sb.append("\"icedLargePrice\":").append(item.getIcedLargePrice());
                        sb.append('}');
                    }
                    sb.append("]");
                    orderSyncClient.publishMenuUpdate(sb.toString());
                } catch (Exception ignored) {
                }
            });
            newTimer.setRepeats(false);
            menuBroadcastTimer.set(newTimer);
            newTimer.start();
        });

        // Debounce inventory changes (batch rapid edits, don't send every keystroke)
        final AtomicReference<Timer> inventoryBroadcastTimer = new AtomicReference<>();
        inventory.Inventory.getInstance().addChangeListener(() -> {
            if (suppressPublish.get())
                return;

            Timer existingTimer = inventoryBroadcastTimer.getAndSet(null);
            if (existingTimer != null)
                existingTimer.stop();

            // Wait 300ms before sending, to batch rapid changes
            Timer newTimer = new Timer(300, e -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    for (var it : inventory.Inventory.getInstance().getAllItems().values()) {
                        if (!first)
                            sb.append(',');
                        first = false;
                        sb.append('{');
                        sb.append("\"name\":").append(q(it.getName())).append(',');
                        sb.append("\"quantity\":").append(it.getQuantity()).append(',');
                        sb.append("\"unit\":").append(q(it.getUnit())).append(',');
                        sb.append("\"alertLevel\":").append(it.getAlertLevel());
                        sb.append('}');
                    }
                    sb.append("]");
                    orderSyncClient.publishInventoryUpdate(sb.toString());
                } catch (Exception ignored) {
                }
            });
            newTimer.setRepeats(false);
            inventoryBroadcastTimer.set(newTimer);
            newTimer.start();
        });

        // Let OrderingPanel publish new orders via the shared client
        orderingPanel.setOrderSyncClient(orderSyncClient);
        orderSyncClient.connect();

        // ── Inventory ────────────────────────────────────────────
        inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        inventoryPanel = new InventoryPanel(isAdmin, inventoryController, () -> {
            if (monitoringPanel != null)
                monitoringPanel.refreshData();
        });
        contentPanel.add(inventoryPanel, "Inventory");

        // ── Monitoring ───────────────────────────────────────────
        monitoringPanel = new MonitoringPanel(isAdmin);
        monitoringPanel.setBackground(AppTheme.BG_PRIMARY);
        contentPanel.add(monitoringPanel, "Monitoring");

        // ── Other tabs ───────────────────────────────────────────
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

        // ── Layout ───────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // Legacy handler stubs (old POS table)
    // ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────
    // Transaction counter
    // ─────────────────────────────────────────────────────────────
    private static int transactionCounter = loadTransactionCounter();

    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }

    private static int loadTransactionCounter() {
        try (Connection conn = persistence.AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT transaction_ref FROM sales_transactions ORDER BY id DESC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return parseTransactionNumber(rs.getString(1));
        } catch (Exception ignored) {
        }
        return 1000;
    }

    // Small helper to produce JSON string literals
    private static String q(String s) {
        if (s == null)
            return "\"\"";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
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

    // ─────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────
    private javax.swing.JTextField cashpayment;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private SidebarPanel sidebar;
    private OrderingPanel orderingPanel;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextFieldChange;
    private javax.swing.JTextField jTextFieldSubTotal;
    private javax.swing.JTextField jTextFieldTax;
    private javax.swing.JTextField jTextFieldTotal;
}