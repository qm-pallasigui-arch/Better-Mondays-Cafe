package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class HelpModule extends JPanel {

    @FunctionalInterface
    private interface IconPainter {
        void paint(Graphics2D g2, int cx, int cy);
    }

    public HelpModule() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);

        UIManager.put("TabbedPane.selected", AppTheme.BG_SURFACE);
        UIManager.put("TabbedPane.background", AppTheme.BG_PRIMARY);
        UIManager.put("TabbedPane.foreground", AppTheme.FG_PRIMARY);
        UIManager.put("TabbedPane.tabAreaBackground", AppTheme.BG_PRIMARY);
        UIManager.put("TabbedPane.contentAreaColor", AppTheme.BG_SURFACE);
        UIManager.put("TabbedPane.focus", AppTheme.ACCENT);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppTheme.BG_PRIMARY);
        tabs.setForeground(AppTheme.FG_PRIMARY);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tabs.addTab("Quick Start", buildQuickStartTab());
        tabs.addTab("Ordering", buildOrderingTab());
        tabs.addTab("Inventory", buildInventoryTab());
        tabs.addTab("Reports", buildReportsTab());
        tabs.addTab("FAQ", buildFaqTab());

        add(tabs, BorderLayout.CENTER);
    }

    // ── tab builders ─────────────────────────────────────────────────────────

    private static JScrollPane buildQuickStartTab() {
        JPanel content = tabContent();

        content.add(buildNumberedCard(1, "Launch the Application",
                "Open the POS system and log in with your assigned credentials."));
        content.add(Box.createVerticalStrut(10));
        content.add(buildNumberedCard(2, "Navigate Using the Sidebar",
                "Use the left navigation panel to switch between Ordering, Inventory, Monitoring, and other modules."));
        content.add(Box.createVerticalStrut(10));
        content.add(buildNumberedCard(3, "Place an Order",
                "Go to Ordering, select a category, choose items and variants, review the cart, and confirm payment."));
        content.add(Box.createVerticalStrut(10));
        content.add(buildNumberedCard(4, "Monitor Inventory",
                "Check the Inventory tab to view stock levels, batches, and ABC/EOQ analytics (admin only)."));
        content.add(Box.createVerticalStrut(10));
        content.add(buildNumberedCard(5, "Generate Reports",
                "Admins can access Monitoring to generate sales reports and export CSV files."));

        return wrapScroll(content);
    }

    private static JScrollPane buildOrderingTab() {
        JPanel content = tabContent();

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // 4-tile grid icon
                    g2.drawRoundRect(cx - 14, cy - 14, 11, 11, 3, 3);
                    g2.drawRoundRect(cx + 3, cy - 14, 11, 11, 3, 3);
                    g2.drawRoundRect(cx - 14, cy + 3, 11, 11, 3, 3);
                    g2.setColor(AppTheme.BG_BADGE_BLUE);
                    g2.fillRoundRect(cx + 3, cy + 3, 11, 11, 3, 3);
                    g2.setColor(AppTheme.ACCENT);
                    g2.drawRoundRect(cx + 3, cy + 3, 11, 11, 3, 3);
                },
                "Category Selection",
                "Browse Espresso & Coffee, Tea Latte, Non-Coffee, Fruit Tea, Herbal Tea, and Food categories."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // radio list icon
                    for (int i = 0; i < 3; i++) {
                        int ry = cy - 7 + i * 8;
                        g2.drawOval(cx - 13, ry - 3, 6, 6);
                        if (i == 1) {
                            g2.fillOval(cx - 11, ry - 1, 2, 2);
                        }
                        g2.drawLine(cx - 4, ry, cx + 13, ry);
                    }
                },
                "Product Variants",
                "Choose size or add-on variants for applicable items before adding to cart."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // cart icon
                    g2.drawLine(cx - 14, cy - 7, cx - 9, cy - 7);
                    g2.drawLine(cx - 9, cy - 7, cx - 5, cy + 7);
                    g2.drawLine(cx - 5, cy + 7, cx + 10, cy + 7);
                    g2.drawLine(cx - 7, cy - 3, cx + 12, cy - 3);
                    g2.drawLine(cx + 12, cy - 3, cx + 10, cy + 7);
                    g2.fillOval(cx - 3, cy + 9, 4, 4);
                    g2.fillOval(cx + 7, cy + 9, 4, 4);
                },
                "Cart Review",
                "Review all items and quantities in the right panel before proceeding to checkout."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // receipt icon
                    g2.drawRoundRect(cx - 10, cy - 13, 20, 24, 2, 2);
                    g2.drawLine(cx - 6, cy - 7, cx + 6, cy - 7);
                    g2.drawLine(cx - 6, cy - 2, cx + 6, cy - 2);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    FontMetrics fm = g2.getFontMetrics();
                    String sym = "₱";
                    g2.drawString(sym, cx - fm.stringWidth(sym) / 2, cy + 6);
                },
                "Payment & Receipt",
                "Confirm total, process payment, and optionally print or view the receipt."));

        content.add(grid);
        return wrapScroll(content);
    }

    private static JScrollPane buildInventoryTab() {
        JPanel content = tabContent();

        // admin banner
        RoundedPanel banner = new RoundedPanel(8);
        banner.setFillColor(AppTheme.BG_BADGE_YELLOW);
        banner.setBorderColor(AppTheme.WARNING);
        banner.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel warn = new JLabel("⚠  Some inventory features are available to Admin users only.");
        warn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        warn.setForeground(AppTheme.WARNING);
        banner.add(warn);

        content.add(banner);
        content.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // warehouse/box stack
                    g2.drawRoundRect(cx - 12, cy - 4, 24, 12, 2, 2);
                    g2.drawRoundRect(cx - 10, cy - 14, 20, 10, 2, 2);
                    g2.drawLine(cx - 12, cy, cx + 12, cy);
                },
                "Stock Management",
                "Monitor current quantities, alert thresholds, and batch SKUs across all inventory items."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // stacked layers
                    for (int i = 0; i < 3; i++) {
                        g2.drawRoundRect(cx - 12 + i * 2, cy - 10 + i * 7, 20, 8, 2, 2);
                    }
                },
                "Batch Tracking",
                "Each item can have multiple batches with separate SKUs, quantities, and expiry dates."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // clock icon
                    g2.drawOval(cx - 12, cy - 12, 24, 24);
                    g2.drawLine(cx, cy, cx, cy - 8);
                    g2.drawLine(cx, cy, cx + 6, cy);
                    // directional arrow
                    g2.drawLine(cx + 9, cy + 9, cx + 13, cy + 5);
                    g2.drawLine(cx + 9, cy + 9, cx + 13, cy + 13);
                },
                "FEFO Deduction",
                "The system always deducts from the batch with the earliest expiry date first, minimizing waste."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // bar chart A/B/C
                    int[] h = { 18, 13, 8 };
                    Color[] clr = { AppTheme.ACCENT, AppTheme.WARNING, AppTheme.FG_MUTED };
                    for (int i = 0; i < 3; i++) {
                        g2.setColor(clr[i]);
                        g2.fillRoundRect(cx - 14 + i * 10, cy + 10 - h[i], 8, h[i], 2, 2);
                    }
                    g2.setColor(AppTheme.ACCENT);
                },
                "ABC / EOQ Analysis",
                "Items are classified A/B/C by usage value. EOQ and ROP thresholds are calculated automatically."));

        content.add(grid);
        return wrapScroll(content);
    }

    private static JScrollPane buildReportsTab() {
        JPanel content = tabContent();

        JPanel grid = new JPanel(new GridLayout(1, 3, 12, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // magnifying glass
                    g2.drawOval(cx - 10, cy - 12, 16, 16);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx + 4, cy + 2, cx + 12, cy + 12);
                },
                "Search",
                "Filter by item name or date range using the search bar at the top of Inventory and Monitoring screens."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // download arrow + table
                    g2.drawLine(cx, cy - 12, cx, cy + 2);
                    g2.drawLine(cx - 5, cy - 2, cx, cy + 4);
                    g2.drawLine(cx + 5, cy - 2, cx, cy + 4);
                    g2.drawRoundRect(cx - 12, cy + 6, 24, 8, 2, 2);
                    g2.drawLine(cx - 4, cy + 6, cx - 4, cy + 14);
                    g2.drawLine(cx + 4, cy + 6, cx + 4, cy + 14);
                },
                "Export CSV",
                "Click the Export button in Sales Reports to download a CSV file of all transactions."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    // calendar grid
                    g2.drawRoundRect(cx - 12, cy - 12, 24, 22, 2, 2);
                    g2.drawLine(cx - 12, cy - 5, cx + 12, cy - 5);
                    g2.drawLine(cx - 4, cy - 12, cx - 4, cy - 7);
                    g2.drawLine(cx + 4, cy - 12, cx + 4, cy - 7);
                    // calendar dots
                    for (int row = 0; row < 2; row++) {
                        for (int col = 0; col < 3; col++) {
                            g2.fillOval(cx - 9 + col * 9, cy - 1 + row * 8, 3, 3);
                        }
                    }
                },
                "Date Filters",
                "Use the date range pickers in Monitoring to scope reports to a specific time period."));

        content.add(grid);
        return wrapScroll(content);
    }

    private static JScrollPane buildFaqTab() {
        JPanel content = tabContent();

        String[][] faqs = {
            { "Can I use the system offline?",
              "Yes. The app uses a local SQLite database and works fully offline. Firebase sync is planned for a future release." },
            { "How do I reset a user's password?",
              "Admins can manage user accounts from the Staff panel. Password reset and role changes can be performed there." },
            { "What does FEFO mean?",
              "First-Expire-First-Out. The system automatically deducts inventory from the batch with the nearest expiry date to minimize waste." },
            { "Why is a row highlighted red in Inventory?",
              "Red indicates at least one batch for that item has passed its expiry date. Dispose of expired batches immediately." },
            { "How is ABC classification determined?",
              "Items are ranked by cumulative usage value (demand × unit cost). Top ~80% = A, next ~15% = B, remaining ~5% = C." },
            { "What is EOQ?",
              "Economic Order Quantity — the optimal replenishment size to minimize combined ordering and holding costs, shown as EOQ~N in the status column." }
        };

        for (int i = 0; i < faqs.length; i++) {
            content.add(buildFaqCard(faqs[i][0], faqs[i][1]));
            if (i < faqs.length - 1) content.add(Box.createVerticalStrut(10));
        }

        return wrapScroll(content);
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    private static JPanel tabContent() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(AppTheme.BG_PRIMARY);
        p.setBorder(new EmptyBorder(20, 28, 20, 28));
        return p;
    }

    private static JScrollPane wrapScroll(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(AppTheme.BG_PRIMARY);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private static CardPanel buildNumberedCard(int step, String title, String desc) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel numCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillOval(4, 4, 40, 40);
                g2.setColor(AppTheme.ACCENT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String n = String.valueOf(step);
                g2.drawString(n, 4 + (40 - fm.stringWidth(n)) / 2,
                        4 + (40 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        numCircle.setOpaque(false);
        numCircle.setPreferredSize(new Dimension(48, 48));

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);

        JLabel descLbl = new JLabel("<html><body style='width:340px'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLbl.setForeground(AppTheme.FG_MUTED);

        text.add(titleLbl, BorderLayout.NORTH);
        text.add(descLbl, BorderLayout.CENTER);

        card.add(numCircle, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static CardPanel buildCard(IconPainter painter, String title, String body) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(12, 0));

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillRoundRect(4, 4, 40, 40, 10, 10);
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                painter.paint(g2, 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(48, 48));

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);

        JLabel bodyLbl = new JLabel("<html><body style='width:240px'>" + body + "</body></html>");
        bodyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bodyLbl.setForeground(AppTheme.FG_MUTED);

        text.add(titleLbl, BorderLayout.NORTH);
        text.add(bodyLbl, BorderLayout.CENTER);

        card.add(iconBox, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static CardPanel buildFaqCard(String question, String answer) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 6));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel q = new JLabel(question);
        q.setFont(new Font("Segoe UI", Font.BOLD, 13));
        q.setForeground(AppTheme.FG_PRIMARY);

        JLabel a = new JLabel("<html><body style='width:440px'>" + answer + "</body></html>");
        a.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        a.setForeground(AppTheme.FG_MUTED);

        card.add(q, BorderLayout.NORTH);
        card.add(a, BorderLayout.CENTER);
        return card;
    }
}
