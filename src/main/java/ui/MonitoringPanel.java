package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import persistence.AppDatabase;

public class MonitoringPanel extends JPanel {

    // ─── Global Refresh Listener ─────────────────────────────────────────────
    private static final CopyOnWriteArrayList<Runnable> REFRESH_LISTENERS = new CopyOnWriteArrayList<>();

    public static void notifyRefresh() {
        for (Runnable r : REFRESH_LISTENERS)
            SwingUtilities.invokeLater(r);
    }

    // ─── Fonts ───────────────────────────────────────────────────────────────
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font SUB_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font CARD_NUM_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font CARD_LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 10);

    // ─── Card Config ─────────────────────────────────────────────────────────
    private static final Color[] CARD_TINTS = {
            AppTheme.ACCENT, AppTheme.WARNING, AppTheme.DANGER, new Color(0x4B5563)
    };
    private static final String[] CARD_TITLES = {
            "Total Items", "Low Stock", "Expired", "Out of Stock"
    };
    private static final String[] CARD_ICONS = {
            "\uD83D\uDCE6", "\u26A0\uFE0F", "\uD83D\uDD25", "\u274C"
    };

    // ─── State ───────────────────────────────────────────────────────────────
    private JLabel[] cardCountLabels = new JLabel[4];
    private JLabel[] cardSubtextLabels = new JLabel[4];
    private JTable salesTable;
    private DefaultTableModel salesTableModel;
    private BarChartPanel barChart;
    private LineChartPanel lineChart;
    private javax.swing.Timer autoRefreshTimer;
    private JComboBox<String> barCategoryCombo;
    private String barCategory = "All Categories";

    private static final String[] BAR_CATEGORIES = {
        "All Categories", "Espresso & Coffee", "Specialty Drinks", "Tea Latte",
        "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"
    };

    private static final java.util.Map<String, java.util.List<String>> CATEGORY_PRODUCTS =
        Map.ofEntries(
            Map.entry("Espresso & Coffee", List.of(
                "Americano", "Latte", "Cappuccino", "Salted Cream Latte", "Spanish Latte",
                "Dark Mocha", "White Mocha", "Caramel Macchiato", "Brewed Coffee")),
            Map.entry("Specialty Drinks", List.of(
                "Vietnamese Coffee", "Ube Espresso", "Manila Latte",
                "Pumpkin Spice Latte", "Spiced Cookie Latte")),
            Map.entry("Tea Latte", List.of(
                "Matcha Latte", "Chocolate Matcha", "Matcha Espresso",
                "Hojicha Latte", "Chai Latte")),
            Map.entry("Non-Coffee", List.of(
                "Chocolate Latte", "Strawberry Latte", "Mango Latte", "Ube Latte",
                "Dragon Fruit Coconut Latte")),
            Map.entry("Fruit Tea", List.of(
                "Strawberry Green Tea", "Mango Green Tea", "Peach Green Tea", "Passion Fruit Green Tea")),
            Map.entry("Herbal Tea", List.of(
                "Peppermint", "Chamomile", "Earl Grey", "Cinnamon")),
            Map.entry("Food", List.of(
                "Signature Ham & Cheese", "Classic Grilled Cheese", "Homestyle Pesto & Cheese",
                "Ham & Cheese", "Cheesy Pesto", "Spam & Cheese",
                "Chocolate Crinkles", "Chocolate Cookies", "S'mores Cookie",
                "Red Velvet Cream Cheese Cookie", "Brownies", "Banana Bread",
                "Chocolate Tiramisu", "Matcha Tiramisu", "Creamy Spinach", "Blueberry Cheesecake"))
        );

    // ─── Constructor ─────────────────────────────────────────────────────────
    public MonitoringPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        buildUI();
        refreshData();

        autoRefreshTimer = new javax.swing.Timer(1_000, e -> refreshData());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();

        Runnable myRefresh = this::refreshData;
        REFRESH_LISTENERS.add(myRefresh);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                    && !isDisplayable()) {
                autoRefreshTimer.stop();
                REFRESH_LISTENERS.remove(myRefresh);
            }
        });
    }

    // ─── Public API ──────────────────────────────────────────────────────────
    public void refreshData() {
        loadSummaryCards();
        loadSalesTable();
        barChart.refreshData();
        lineChart.refreshData();
    }

    // ─── UI Construction ─────────────────────────────────────────────────────
    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JPanel contentBody = new JPanel(new BorderLayout(0, 20));
        contentBody.setOpaque(false);
        contentBody.add(buildSummaryRow(), BorderLayout.NORTH);
        contentBody.add(buildSalesCard(), BorderLayout.CENTER);

        JPanel chartsRow = buildChartsRow();
        chartsRow.setPreferredSize(new Dimension(0, 280));
        contentBody.add(chartsRow, BorderLayout.SOUTH);

        add(contentBody, BorderLayout.CENTER);
    }

    // ─── Header ──────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Monitoring");
        title.setFont(TITLE_FONT);
        title.setForeground(AppTheme.FG_PRIMARY);

        JLabel subtitle = new JLabel();
        subtitle.setFont(SUB_FONT);
        subtitle.setForeground(AppTheme.FG_MUTED);
        new javax.swing.Timer(1000, e -> subtitle.setText(
                "As of " + new SimpleDateFormat("EEEE, MM/dd/yyyy hh:mm:ss a").format(new Date()))).start();

        JPanel stack = new JPanel(new BorderLayout(0, 2));
        stack.setOpaque(false);
        stack.add(title, BorderLayout.NORTH);
        stack.add(subtitle, BorderLayout.SOUTH);
        header.add(stack, BorderLayout.WEST);

        JButton reportBtn = new JButton("\uD83D\uDCCA Sales Report");
        reportBtn.setFont(BOLD_FONT);
        reportBtn.setForeground(Color.WHITE);
        reportBtn.setBackground(AppTheme.ACCENT);
        reportBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        reportBtn.setFocusPainted(false);
        reportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Opens the chooser modal (Weekly / Monthly)
        reportBtn.addActionListener(e -> showReportChooserModal());
        header.add(reportBtn, BorderLayout.EAST);

        return header;
    }

    // ─── Report Chooser Modal ────────────────────────────────────────────────
    /**
     * Shows a small dialog with two card-style buttons:
     * • Weekly Report → SalesReportGenerator.generateWeekly()
     * • Monthly Report → SalesReportGenerator.generateMonthly()
     */
    private void showReportChooserModal() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Generate Sales Report",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_SURFACE);

        // ── Banner ────────────────────────────────────────────────
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(AppTheme.ACCENT);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel bannerIcon = new JLabel("\uD83D\uDCCA");
        bannerIcon.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        bannerIcon.setForeground(Color.WHITE);
        bannerIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));

        JPanel bannerText = new JPanel(new BorderLayout(0, 3));
        bannerText.setOpaque(false);

        JLabel bannerTitle = new JLabel("Sales Report");
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        bannerTitle.setForeground(Color.WHITE);

        JLabel bannerSub = new JLabel("Choose the report period you want to generate");
        bannerSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bannerSub.setForeground(new Color(255, 255, 255, 200));

        bannerText.add(bannerTitle, BorderLayout.NORTH);
        bannerText.add(bannerSub, BorderLayout.SOUTH);
        banner.add(bannerIcon, BorderLayout.WEST);
        banner.add(bannerText, BorderLayout.CENTER);

        // ── Two card buttons ──────────────────────────────────────
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 16, 0));
        btnRow.setBackground(AppTheme.BG_SURFACE);
        btnRow.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JButton weeklyBtn = makeReportTypeButton(
                "\uD83D\uDCC5", "Weekly Report",
                "Current week breakdown\nby day & category",
                new Color(0x2563EB));

        JButton monthlyBtn = makeReportTypeButton(
                "\uD83D\uDCC6", "Monthly Report",
                "4-week summary for\nthe current month",
                new Color(0x059669));

        weeklyBtn.addActionListener(e -> {
            dialog.dispose();
            SalesReportGenerator.generateWeekly(MonitoringPanel.this);
        });
        monthlyBtn.addActionListener(e -> {
            dialog.dispose();
            SalesReportGenerator.generateMonthly(MonitoringPanel.this);
        });

        btnRow.add(weeklyBtn);
        btnRow.add(monthlyBtn);

        // ── Cancel link ───────────────────────────────────────────
        JPanel footer = new JPanel();
        footer.setBackground(AppTheme.BG_SURFACE);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel cancelLbl = new JLabel("Cancel");
        cancelLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelLbl.setForeground(AppTheme.FG_MUTED);
        cancelLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dialog.dispose();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                cancelLbl.setForeground(AppTheme.ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelLbl.setForeground(AppTheme.FG_MUTED);
            }
        });
        footer.add(cancelLbl);

        root.add(banner, BorderLayout.NORTH);
        root.add(btnRow, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(440, dialog.getHeight()));
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    /** Card-style button used in the report chooser. */
    private JButton makeReportTypeButton(String icon, String label, String desc, Color accent) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? accent.brighter()
                        : getModel().isPressed() ? accent.darker()
                                : AppTheme.BG_PRIMARY;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setLayout(new BoxLayout(btn, BoxLayout.Y_AXIS));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        btn.setPreferredSize(new Dimension(170, 130));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI", Font.PLAIN, 30));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLbl = new JLabel(label, SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(accent);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLbl = new JLabel(
                "<html><div style='text-align:center;color:#6B7280;font-size:10px;'>"
                        + desc.replace("\n", "<br>") + "</div></html>",
                SwingConstants.CENTER);
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.add(Box.createVerticalGlue());
        btn.add(iconLbl);
        btn.add(Box.createVerticalStrut(6));
        btn.add(nameLbl);
        btn.add(Box.createVerticalStrut(4));
        btn.add(descLbl);
        btn.add(Box.createVerticalGlue());
        return btn;
    }

    // ─── Summary Cards ────────────────────────────────────────────────────────
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            CardPanel card = new CardPanel(16, AppTheme.BG_SURFACE);
            card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            card.setLayout(new BorderLayout(14, 0));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showSummaryModal(idx);
                }
            });

            JPanel iconBox = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CARD_TINTS[idx]);
                    g2.fillRoundRect(0, 0, 48, 48, 12, 12);
                    g2.dispose();
                }
            };
            iconBox.setPreferredSize(new Dimension(48, 48));
            iconBox.setOpaque(false);
            iconBox.setLayout(new BorderLayout());
            JLabel iconLbl = new JLabel(CARD_ICONS[idx], SwingConstants.CENTER);
            iconLbl.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            iconBox.add(iconLbl, BorderLayout.CENTER);

            JPanel textStack = new JPanel(new BorderLayout(0, 2));
            textStack.setOpaque(false);

            JLabel countLabel = new JLabel("0");
            countLabel.setFont(CARD_NUM_FONT);
            countLabel.setForeground(AppTheme.FG_PRIMARY);
            cardCountLabels[idx] = countLabel;

            JLabel titleLbl = new JLabel(CARD_TITLES[idx]);
            titleLbl.setFont(CARD_LABEL_FONT);
            titleLbl.setForeground(AppTheme.FG_MUTED);

            JLabel subtextLbl = new JLabel("");
            subtextLbl.setFont(SMALL_FONT);
            cardSubtextLabels[idx] = subtextLbl;

            textStack.add(countLabel, BorderLayout.NORTH);
            textStack.add(titleLbl, BorderLayout.CENTER);
            textStack.add(subtextLbl, BorderLayout.SOUTH);

            card.add(iconBox, BorderLayout.WEST);
            card.add(textStack, BorderLayout.CENTER);
            row.add(card);
        }
        return row;
    }

    private void loadSummaryCards() {
        int totalItems = 0, lowStock = 0, expired = 0, outOfStock = 0;
        try (Connection conn = AppDatabase.openConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM inventory_items")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    totalItems = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM inventory_items WHERE quantity > 0 AND quantity <= alert_level")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    lowStock = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT ib.inventory_item_id) FROM inventory_batches ib "
                            + "JOIN inventory_items ii ON ii.id = ib.inventory_item_id "
                            + "WHERE ib.expiry_date < date('now') AND ib.quantity > 0")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    expired = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM inventory_items WHERE quantity <= 0")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    outOfStock = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cardCountLabels[0].setText(String.valueOf(totalItems));
        cardCountLabels[1].setText(String.valueOf(lowStock));
        cardCountLabels[2].setText(String.valueOf(expired));
        cardCountLabels[3].setText(String.valueOf(outOfStock));

        cardSubtextLabels[0].setText("In inventory");
        cardSubtextLabels[0].setForeground(AppTheme.SUCCESS);
        cardSubtextLabels[1].setText(lowStock > 0 ? "Needs restocking" : "All stocked");
        cardSubtextLabels[1].setForeground(lowStock > 0 ? AppTheme.WARNING : AppTheme.SUCCESS);
        cardSubtextLabels[2].setText(expired > 0 ? "Dispose immediately" : "All fresh");
        cardSubtextLabels[2].setForeground(expired > 0 ? AppTheme.DANGER : AppTheme.SUCCESS);
        cardSubtextLabels[3].setText(outOfStock > 0 ? "Unavailable" : "All available");
        cardSubtextLabels[3].setForeground(outOfStock > 0 ? new Color(0x4B5563) : AppTheme.SUCCESS);
    }

    private void showSummaryModal(int idx) {
        String title = CARD_TITLES[idx];
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = AppDatabase.openConnection()) {
            String sql = switch (idx) {
                case 0 -> "SELECT name, quantity, unit FROM inventory_items ORDER BY name";
                case 1 -> "SELECT name, quantity, unit, alert_level FROM inventory_items "
                        + "WHERE quantity > 0 AND quantity <= alert_level ORDER BY name";
                case 2 -> "SELECT ii.name, ib.sku, ib.quantity, ib.expiry_date "
                        + "FROM inventory_batches ib "
                        + "JOIN inventory_items ii ON ii.id = ib.inventory_item_id "
                        + "WHERE ib.expiry_date < date('now') AND ib.quantity > 0 ORDER BY ib.expiry_date";
                case 3 -> "SELECT name, unit FROM inventory_items WHERE quantity <= 0 ORDER BY name";
                default -> "";
            };
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<String> cols = new ArrayList<>();
                    for (int c = 1; c <= rs.getMetaData().getColumnCount(); c++)
                        cols.add(rs.getString(c) == null ? "" : rs.getString(c));
                    rows.add(cols.toArray(new String[0]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(AppTheme.BG_SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel headerLbl = new JLabel(title + " (" + rows.size() + ")");
        headerLbl.setFont(BOLD_FONT);
        headerLbl.setForeground(AppTheme.FG_PRIMARY);
        content.add(headerLbl, BorderLayout.NORTH);

        String[] columns = switch (idx) {
            case 0 -> new String[] { "Item Name", "Qty", "Unit" };
            case 1 -> new String[] { "Item Name", "Qty", "Unit", "Alert Level" };
            case 2 -> new String[] { "Item", "Batch SKU", "Qty", "Expiry" };
            case 3 -> new String[] { "Item Name", "Unit" };
            default -> new String[] { "" };
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (String[] row : rows)
            model.addRow(row);

        JTable table = new JTable(model);
        AppTheme.applyTableDefaults(table);
        table.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(520, Math.min(400, rows.size() * 26 + 30)));
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        content.add(scroll, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this),
                content, title, JOptionPane.PLAIN_MESSAGE);
    }

    // ─── Recent Sales Table ───────────────────────────────────────────────────
    private JPanel buildSalesCard() {
        CardPanel card = new CardPanel(16, AppTheme.BG_SURFACE);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);

        JLabel titleLbl = new JLabel("Recent Sales");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);
        topBar.add(titleLbl, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new BorderLayout(8, 0));
        filterPanel.setOpaque(false);

        JComboBox<String> timeCombo = new JComboBox<>(TIME_FILTERS);
        timeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeCombo.setBackground(AppTheme.BG_SURFACE);
        timeCombo.setForeground(AppTheme.FG_PRIMARY);
        timeCombo.addActionListener(e -> {
            timeFilter = (String) timeCombo.getSelectedItem();
            loadSalesTable();
        });
        filterPanel.add(timeCombo, BorderLayout.WEST);

        JTextField searchField = new JTextField(18);
        AppTheme.styleSearchField(searchField);
        searchField.putClientProperty("JTextField.roundPlaceholder", true);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applySalesFilter(searchField);
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applySalesFilter(searchField);
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applySalesFilter(searchField);
            }
        });
        filterPanel.add(searchField, BorderLayout.EAST);
        topBar.add(filterPanel, BorderLayout.EAST);
        card.add(topBar, BorderLayout.NORTH);

        salesTableModel = new DefaultTableModel(
                new String[] { "Order ID", "Customer", "Transaction Details", "Total", "Receipt" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 2 || c == 4;
            }
        };

        salesTable = new JTable(salesTableModel);
        AppTheme.applyTableDefaults(salesTable);
        salesTable.setRowHeight(32);
        salesTable.getTableHeader().setReorderingAllowed(false);
        salesTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        salesTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        salesTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        salesTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        salesTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        salesTable.getColumnModel().getColumn(2).setCellRenderer(new DropdownRenderer());
        salesTable.getColumnModel().getColumn(2).setCellEditor(new DropdownEditor());
        salesTable.getColumnModel().getColumn(4).setCellRenderer(new ReceiptIconRenderer());
        salesTable.getColumnModel().getColumn(4).setCellEditor(new ReceiptIconEditor());

        JScrollPane scroll = new JScrollPane(salesTable);
        scroll.setBorder(null);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private List<TransactionRow> cachedTransactions = new ArrayList<>();
    private String timeFilter = "Today";
    private static final String[] TIME_FILTERS = { "Today", "Yesterday", "This Week" };

    private String buildTimeFilter(String filter) {
        if ("Yesterday".equals(filter))
            return "WHERE DATE(created_at) = DATE('now', '-1 day') ";
        if ("This Week".equals(filter)) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String monday = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            return "WHERE created_at >= '" + monday + "' ";
        }
        return "WHERE DATE(created_at) = DATE('now') ";
    }

    private void loadSalesTable() {
        cachedTransactions.clear();
        salesTableModel.setRowCount(0);

        String whereClause = buildTimeFilter(timeFilter);

        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT st.id, st.transaction_ref, "
                                + "COALESCE(st.customer_name,'') AS customer_name, "
                                + "COALESCE(st.subtotal,0)      AS subtotal, "
                                + "COALESCE(st.tax,0)           AS tax, "
                                + "COALESCE(st.total,0)         AS total, "
                                + "COALESCE(st.cash,0)          AS cash, "
                                + "COALESCE(st.change_amount,0) AS change_amount, "
                                + "COALESCE(st.created_at,'')   AS created_at "
                                + "FROM sales_transactions st "
                                + whereClause
                                + " ORDER BY st.id DESC LIMIT 50");
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long txId = rs.getLong("id");
                String ref = rs.getString("transaction_ref");
                String customerName = rs.getString("customer_name");
                if (customerName.isEmpty()) customerName = "Walk-in";
                double subtotal = rs.getDouble("subtotal");
                double tax = rs.getDouble("tax");
                double total = rs.getDouble("total");
                double cash = rs.getDouble("cash");
                double change = rs.getDouble("change_amount");
                String createdAt = rs.getString("created_at");

                List<SalesItem> items = new ArrayList<>();
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "SELECT product_name, quantity, price, total "
                                + "FROM sales_transaction_items WHERE transaction_id = ?")) {
                    ps2.setLong(1, txId);
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next())
                        items.add(new SalesItem(
                                rs2.getString("product_name"),
                                rs2.getInt("quantity"),
                                rs2.getDouble("price"),
                                rs2.getDouble("total")));
                }

                cachedTransactions.add(
                        new TransactionRow(ref, customerName, subtotal, tax, total, cash, change, createdAt, items));
                salesTableModel.addRow(new Object[] {
                        "#" + ref.replace("TXN", ""),
                        customerName,
                        "\u25BC Details",
                        String.format("\u20B1%.2f", total),
                        "\uD83D\uDCC4"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cachedTransactions.isEmpty())
            salesTableModel.addRow(new Object[] { "No sales yet", "-", "-", "-", "-" });
    }

    private void applySalesFilter(JTextField searchField) {
        String q = searchField.getText().trim().toLowerCase();
        salesTableModel.setRowCount(0);
        for (TransactionRow tr : cachedTransactions) {
            if (!q.isEmpty()
                    && !tr.ref.toLowerCase().contains(q)
                    && !String.format("%.2f", tr.total).contains(q))
                continue;
            salesTableModel.addRow(new Object[] {
                    "#" + tr.ref.replace("TXN", ""),
                    tr.customer,
                    "\u25BC Details",
                    String.format("\u20B1%.2f", tr.total),
                    "\uD83D\uDCC4"
            });
        }
        if (salesTableModel.getRowCount() == 0)
            salesTableModel.addRow(new Object[] { "No matching sales", "-", "-", "-", "-" });
    }

    private void showTransactionDetails(int modelRow) {
        if (modelRow < 0 || modelRow >= cachedTransactions.size())
            return;
        TransactionRow tr = cachedTransactions.get(modelRow);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(AppTheme.BG_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel header = new JLabel("Items for " + tr.ref);
        header.setFont(BOLD_FONT);
        panel.add(header, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder(
                "<html><table style='width:320px;font-family:Segoe UI;font-size:12px;'>");
        sb.append("<tr style='font-weight:bold;'><td align='left'>Item</td>"
                + "<td align='center'>Qty</td><td align='right'>Price</td>"
                + "<td align='right'>Subtotal</td></tr>");
        sb.append("<tr><td colspan='4'><hr></td></tr>");
        for (SalesItem it : tr.items)
            sb.append(String.format(
                    "<tr><td align='left'>%s</td><td align='center'>x%d</td>"
                            + "<td align='right'>\u20B1%.2f</td><td align='right'>\u20B1%.2f</td></tr>",
                    it.productName, it.quantity, it.price, it.quantity * it.price));
        sb.append("<tr><td colspan='4'><hr></td></tr>");
        sb.append(String.format(
                "<tr style='font-weight:bold;'><td colspan='3' align='right'>Total:</td>"
                        + "<td align='right'>\u20B1%.2f</td></tr>",
                tr.total));
        sb.append("</table></html>");

        panel.add(new JLabel(sb.toString()), BorderLayout.CENTER);
        JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this),
                panel, tr.ref + " - Items", JOptionPane.PLAIN_MESSAGE);
    }

    private void showReceiptModal(int modelRow) {
        if (modelRow < 0 || modelRow >= cachedTransactions.size())
            return;
        TransactionRow tr = cachedTransactions.get(modelRow);
        String line = "\u2500".repeat(40);
        String dbl = "\u2550".repeat(40);

        StringBuilder receipt = new StringBuilder();
        receipt.append("            \u2615 Better Mondays Cafe \u2615\n");
        receipt.append("            123 Main St., Manila\n");
        receipt.append("            VAT REG TIN: 123-456-789\n");
        receipt.append(dbl).append("\n");
        receipt.append(" Date: ").append(tr.createdAt).append("\n");
        receipt.append(" Customer: ").append(tr.customer).append("\n");
        receipt.append(" ").append(tr.ref).append("\n");
        receipt.append(dbl).append("\n");
        receipt.append(String.format(" %-16s %2s %8s\n", "ITEM", "QTY", "AMOUNT"));
        receipt.append(line).append("\n");
        for (SalesItem it : tr.items)
            receipt.append(String.format(" %-16s %2d %8.2f\n",
                    trunc(it.productName, 16), it.quantity, it.total));
        receipt.append(line).append("\n");
        receipt.append(String.format(" %-22s %8.2f\n", "Subtotal (excl VAT):", tr.subtotal));
        receipt.append(String.format(" %-22s %8.2f\n", "VAT (12%):", tr.tax));
        receipt.append(String.format(" %-22s %8.2f\n", "TOTAL (incl VAT):", tr.total));
        receipt.append(line).append("\n");
        receipt.append(String.format(" %-22s %8.2f\n", "Cash:", tr.cash));
        receipt.append(String.format(" %-22s %8.2f\n", "Change:", tr.change));
        receipt.append(dbl).append("\n");
        receipt.append("         Thank you! Come again!\n");
        receipt.append("         *** Have a nice day ***\n");

        JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this),
                new JTextAreaWithFont(receipt.toString()),
                "\u2705 RECEIPT - " + tr.ref, JOptionPane.PLAIN_MESSAGE);
    }

    // ─── Charts ───────────────────────────────────────────────────────────────
    private JPanel buildChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);

        CardPanel barCard = new CardPanel(16, AppTheme.BG_SURFACE);
        barCard.setLayout(new BorderLayout(0, 8));
        barCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel barTop = new JPanel(new BorderLayout(8, 0));
        barTop.setOpaque(false);
        JLabel barTitle = new JLabel("Top-Selling Products");
        barTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        barTitle.setForeground(AppTheme.FG_PRIMARY);
        barTop.add(barTitle, BorderLayout.WEST);

        barCategoryCombo = new JComboBox<>(BAR_CATEGORIES);
        barCategoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        barCategoryCombo.setBackground(AppTheme.BG_SURFACE);
        barCategoryCombo.setForeground(AppTheme.FG_PRIMARY);

        barCategoryCombo.addActionListener(e -> {
            barCategory = (String) barCategoryCombo.getSelectedItem();
            barChart.refreshData();
        });
        barTop.add(barCategoryCombo, BorderLayout.EAST);
        barCard.add(barTop, BorderLayout.NORTH);
        barChart = new BarChartPanel();
        barCard.add(barChart, BorderLayout.CENTER);

        CardPanel lineCard = new CardPanel(16, AppTheme.BG_SURFACE);
        lineCard.setLayout(new BorderLayout(0, 8));
        lineCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel lineTitle = new JLabel("Sales per Day");
        lineTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lineTitle.setForeground(AppTheme.FG_PRIMARY);
        lineCard.add(lineTitle, BorderLayout.NORTH);
        lineChart = new LineChartPanel();
        lineCard.add(lineChart, BorderLayout.CENTER);

        row.add(barCard);
        row.add(lineCard);
        return row;
    }

    // ─── Horizontal Bar Chart ─────────────────────────────────────────────────
    private class BarChartPanel extends JPanel {
        private List<BarData> data = new ArrayList<>();
        private static final Color[] RAINBOW = {
            new Color(0xFFADAD),
            new Color(0xFFD6A5),
            new Color(0xFDFFB6),
            new Color(0xCAFFBF),
            new Color(0xA0C4FF),
        };

        BarChartPanel() {
            setOpaque(false);
            setFont(BODY_FONT);
        }

        void refreshData() {
            data.clear();
            String stripSuffix = "TRIM(SUBSTR(product_name, 1, "
                    + "CASE WHEN INSTR(product_name, ' (') > 0 "
                    + "THEN INSTR(product_name, ' (') - 1 ELSE LENGTH(product_name) END))";
            String baseExpr = "TRIM(CASE "
                    + "WHEN " + stripSuffix + " LIKE 'Hot %' THEN SUBSTR(" + stripSuffix + ", 5) "
                    + "WHEN " + stripSuffix + " LIKE 'Iced %' THEN SUBSTR(" + stripSuffix + ", 6) "
                    + "ELSE " + stripSuffix + " END)";
            StringBuilder sql = new StringBuilder(
                    "SELECT " + baseExpr + " AS base_name, SUM(quantity) AS total_qty, SUM(total) AS total_revenue "
                    + "FROM sales_transaction_items");
            java.util.List<String> products = barCategory != null ? CATEGORY_PRODUCTS.get(barCategory) : null;
            if (products != null && !products.isEmpty()) {
                sql.append(" WHERE ").append(baseExpr).append(" IN (");
                for (int i = 0; i < products.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append("'").append(products.get(i).replace("'", "''")).append("'");
                }
                sql.append(")");
            }
            sql.append(" GROUP BY ").append(baseExpr).append(" ORDER BY total_qty DESC LIMIT 5");
            try (Connection conn = AppDatabase.openConnection();
                    PreparedStatement ps = conn.prepareStatement(sql.toString());
                    ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        data.add(new BarData(rs.getString("base_name"),
                                rs.getInt("total_qty"), rs.getDouble("total_revenue")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padR = 50, padT = 10, padB = 10;

            if (data.isEmpty()) {
                g2.setFont(BODY_FONT);
                g2.setColor(AppTheme.FG_MUTED);
                String msg = "No sales data available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int barCount = data.size();
            int barGap = 8;
            int barH = Math.min(28, (h - padT - padB - barGap * (barCount - 1)) / barCount);
            int totalBarH = barCount * barH + (barCount - 1) * barGap;
            int startY = padT + ((h - padT - padB) - totalBarH) / 2;

            int maxQty = data.stream().mapToInt(d -> d.qty).max().orElse(1);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            FontMetrics nameFm = g2.getFontMetrics();
            int nameLabelWidth = data.stream().mapToInt(d -> nameFm.stringWidth(d.name)).max().orElse(0);
            int padL = Math.max(85, nameLabelWidth + 14);

            int chartW = w - padL - padR;

            for (int i = 0; i < barCount; i++) {
                BarData bd = data.get(i);
                int barW = Math.max(4, (int) ((double) bd.qty / maxQty * chartW));
                int y = startY + i * (barH + barGap);

                g2.setColor(RAINBOW[i]);
                g2.fillRoundRect(padL, y, barW, barH, 4, 4);

                g2.setColor(AppTheme.FG_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString(bd.name, padL - 8 - nameFm.stringWidth(bd.name),
                        y + barH / 2 + nameFm.getAscent() / 2 - 2);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics qtyFm = g2.getFontMetrics();
                String qtyLabel = String.valueOf(bd.qty);
                g2.setColor(AppTheme.FG_PRIMARY);
                g2.drawString(qtyLabel, padL + barW + 6,
                        y + barH / 2 + qtyFm.getAscent() / 2 - 2);
            }
            g2.dispose();
        }
    }

    // ─── Line / Area Chart ────────────────────────────────────────────────────
    private class LineChartPanel extends JPanel {
        private List<DailyPoint> thisWeek = new ArrayList<>();
        private List<DailyPoint> lastWeek = new ArrayList<>();

        LineChartPanel() {
            setOpaque(false);
        }

        void refreshData() {
            thisWeek.clear();
            lastWeek.clear();

            Map<String, Double> dailyMap = new LinkedHashMap<>();
            try (Connection conn = AppDatabase.openConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT DATE(created_at) AS d, SUM(total) AS rev "
                                    + "FROM sales_transactions "
                                    + "WHERE created_at >= date('now','-14 days') "
                                    + "GROUP BY DATE(created_at) ORDER BY d");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    dailyMap.put(rs.getString("d"), rs.getDouble("rev"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();

            for (int offset = -14; offset < -7; offset++) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, offset);
                String key = sdf.format(cal.getTime());
                lastWeek.add(new DailyPoint(cal.getTime(), dailyMap.getOrDefault(key, 0.0)));
            }
            for (int offset = -7; offset < 0; offset++) {
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, offset);
                String key = sdf.format(cal.getTime());
                thisWeek.add(new DailyPoint(cal.getTime(), dailyMap.getOrDefault(key, 0.0)));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 55, padR = 20, padT = 20, padB = 40;

            boolean empty = thisWeek.stream().allMatch(p -> p.revenue == 0)
                    && lastWeek.stream().allMatch(p -> p.revenue == 0);
            if (empty) {
                g2.setFont(BODY_FONT);
                g2.setColor(AppTheme.FG_MUTED);
                String msg = "No sales data available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int chartW = w - padL - padR;
            int chartH = h - padT - padB;
            double maxRev = 0;
            for (DailyPoint p : thisWeek)
                if (p.revenue > maxRev)
                    maxRev = p.revenue;
            for (DailyPoint p : lastWeek)
                if (p.revenue > maxRev)
                    maxRev = p.revenue;
            if (maxRev == 0)
                maxRev = 100;
            maxRev = Math.ceil(maxRev / 100) * 100;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(AppTheme.FG_MUTED);
            for (int i = 0; i <= 4; i++) {
                int y = padT + chartH - (int) (chartH * i / 4.0);
                String lbl = "\u20B1" + String.format("%.0f", maxRev * i / 4.0);
                g2.drawString(lbl, 2, y + 3);
                g2.setColor(AppTheme.BORDER);
                g2.drawLine(padL, y, w - padR, y);
                g2.setColor(AppTheme.FG_MUTED);
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(0x3B82F6));
            g2.fillRect(w - 120, padT - 8, 10, 10);
            g2.setColor(AppTheme.FG_PRIMARY);
            g2.drawString("This Week", w - 106, padT);
            g2.setColor(new Color(0x94A3B8));
            g2.fillRect(w - 60, padT - 8, 10, 10);
            g2.setColor(AppTheme.FG_PRIMARY);
            g2.drawString("Last Week", w - 46, padT);

            drawLineSeries(g2, lastWeek, maxRev, padL, padT, chartW, chartH,
                    new Color(0x94A3B8), new Color(0xE2E8F0), false);
            drawLineSeries(g2, thisWeek, maxRev, padL, padT, chartW, chartH,
                    new Color(0x3B82F6), new Color(0xDBEAFE), true);

            g2.dispose();
        }

        private void drawLineSeries(Graphics2D g2, List<DailyPoint> points, double maxRev,
                int padL, int padT, int chartW, int chartH,
                Color lineColor, Color fillColor, boolean showLabels) {
            if (points.isEmpty())
                return;
            int n = points.size();
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = padL + (int) ((i + 0.5) * chartW / n);
                ys[i] = padT + chartH - (int) ((points.get(i).revenue / maxRev) * chartH);
            }

            int[] xFill = new int[n + 2], yFill = new int[n + 2];
            System.arraycopy(xs, 0, xFill, 0, n);
            System.arraycopy(ys, 0, yFill, 0, n);
            xFill[n] = xs[n - 1];
            yFill[n] = padT + chartH;
            xFill[n + 1] = xs[0];
            yFill[n + 1] = padT + chartH;

            g2.setColor(fillColor);
            g2.fillPolygon(xFill, yFill, n + 2);

            g2.setColor(lineColor);
            g2.setStroke(new java.awt.BasicStroke(2.5f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n - 1; i++)
                g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);

            for (int i = 0; i < n; i++) {
                g2.setColor(lineColor);
                g2.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
                g2.setColor(Color.WHITE);
                g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);
            }

            if (showLabels) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(AppTheme.FG_MUTED);
                SimpleDateFormat labelFmt = new SimpleDateFormat("MMM d");
                int step = Math.max(1, n / 7);
                for (int i = 0; i < n; i += step) {
                    String lbl = labelFmt.format(points.get(i).date);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(lbl, xs[i] - fm.stringWidth(lbl) / 2, padT + chartH + 14);
                }
            }
        }
    }

    // ─── Renderers & Editors ──────────────────────────────────────────────────
    private class DropdownRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = new JLabel("\u25BC Details", SwingConstants.CENTER);
            lbl.setFont(BODY_FONT);
            lbl.setForeground(AppTheme.ACCENT);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (isSelected)
                lbl.setOpaque(true);
            return lbl;
        }
    }

    private class DropdownEditor extends AbstractCellEditor implements TableCellEditor {
        @Override
        public Object getCellEditorValue() {
            return "\u25BC Details";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            showTransactionDetails(table.convertRowIndexToModel(row));
            return new JLabel();
        }
    }

    private class ReceiptIconRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = new JLabel("\uD83D\uDCC4", SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return lbl;
        }
    }

    private class ReceiptIconEditor extends AbstractCellEditor implements TableCellEditor {
        @Override
        public Object getCellEditorValue() {
            return "\uD83D\uDCC4";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            showReceiptModal(table.convertRowIndexToModel(row));
            return new JLabel();
        }
    }

    // ─── Data Classes ─────────────────────────────────────────────────────────
    private static class TransactionRow {
        String ref, customer, createdAt;
        double subtotal, tax, total, cash, change;
        List<SalesItem> items;

        TransactionRow(String ref, String customer, double subtotal, double tax, double total,
                double cash, double change, String createdAt, List<SalesItem> items) {
            this.ref = ref;
            this.customer = customer;
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
            this.cash = cash;
            this.change = change;
            this.createdAt = createdAt;
            this.items = items;
        }
    }

    private static class SalesItem {
        String productName;
        int quantity;
        double price, total;

        SalesItem(String name, int qty, double price, double total) {
            this.productName = name;
            this.quantity = qty;
            this.price = price;
            this.total = total;
        }
    }

    private static class BarData {
        String name;
        int qty;
        double revenue;

        BarData(String n, int q, double r) {
            name = n;
            qty = q;
            revenue = r;
        }
    }

    private static class DailyPoint {
        Date date;
        double revenue;

        DailyPoint(Date d, double r) {
            date = d;
            revenue = r;
        }
    }

    private static class JTextAreaWithFont extends JScrollPane {
        JTextAreaWithFont(String text) {
            javax.swing.JTextArea ta = new javax.swing.JTextArea(text);
            ta.setFont(new Font("Consolas", Font.PLAIN, 12));
            ta.setEditable(false);
            ta.setBackground(Color.WHITE);
            ta.setForeground(new Color(0x111827));
            ta.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setViewportView(ta);
            setPreferredSize(new Dimension(380, 400));
            setBorder(null);
        }
    }

    private String trunc(String s, int len) {
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }
}