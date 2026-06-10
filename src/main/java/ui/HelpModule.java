package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class HelpModule extends JPanel {

    @FunctionalInterface
    private interface IconPainter {
        void paint(Graphics2D g2, int cx, int cy);
    }

    // ── Tab names ─────────────────────────────────────────────────────────────
    private static final String[] TAB_NAMES = {
            "Quick Start", "Ordering", "Inventory", "Reports", "FAQ"
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private String activeTab = TAB_NAMES[0];
    private final java.util.List<JButton> pillButtons = new java.util.ArrayList<>();
    private JPanel contentArea;

    // ── Constructor ───────────────────────────────────────────────────────────
    public HelpModule() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);
        buildUI();
    }

    // ═══════════════════════════════════════════════════════════════
    // UI Construction (mirrors OrderingPanel.buildUI structure)
    // ═══════════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Category pill bar — identical construction to
        // OrderingPanel.buildCategoryBar()
        body.add(buildTabBar(), BorderLayout.NORTH);

        // Content area that gets swapped on pill click
        contentArea = new JPanel(new BorderLayout());
        contentArea.setOpaque(false);
        showTab(activeTab);
        body.add(contentArea, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    // ── Tab bar (mirrors OrderingPanel.buildCategoryBar) ─────────────────────

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 2, 0));
        pillButtons.clear();

        for (String tab : TAB_NAMES) {
            JButton btn = new JButton(tab);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            applyPillStyle(btn, tab.equals(activeTab));
            btn.addActionListener(e -> showTab(tab));
            pillButtons.add(btn);
            bar.add(btn);
        }
        return bar;
    }

    // Mirrors OrderingPanel.updatePillStyle exactly
    private void applyPillStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(AppTheme.ACCENT);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.ACCENT, 1, true),
                    new EmptyBorder(6, 14, 6, 14)));
        } else {
            btn.setBackground(AppTheme.BG_SURFACE);
            btn.setForeground(AppTheme.FG_MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                    new EmptyBorder(6, 14, 6, 14)));
        }
    }

    private void showTab(String tab) {
        activeTab = tab;
        // Re-style all pills (mirrors OrderingPanel.showCategory)
        for (JButton btn : pillButtons) {
            applyPillStyle(btn, btn.getText().equals(tab));
        }
        contentArea.removeAll();
        contentArea.add(buildTabContent(tab), BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel buildTabContent(String tab) {
        return switch (tab) {
            case "Quick Start" -> buildQuickStartTab();
            case "Ordering" -> buildOrderingTab();
            case "Inventory" -> buildInventoryTab();
            case "Reports" -> buildReportsTab();
            case "FAQ" -> buildFaqTab();
            default -> buildQuickStartTab();
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // Tab content builders
    // ═══════════════════════════════════════════════════════════════

    // ── Quick Start: numbered accordion cards ─────────────────────

    private static JPanel buildQuickStartTab() {
        JPanel content = tabContent();

        String[][] steps = {
                { "Launch the Application",
                        "Open the POS system and log in with your assigned credentials." },
                { "Navigate Using the Sidebar",
                        "Use the left navigation panel to switch between Ordering, Inventory, Monitoring, and other modules." },
                { "Place an Order",
                        "Go to Ordering, select a category, choose items and variants, review the cart, and confirm payment." },
                { "Monitor Inventory",
                        "Check the Inventory tab to view stock levels, batches, and ABC/EOQ analytics (admin only)." },
                { "Generate Reports",
                        "Admins can access Monitoring to generate sales reports and export CSV files." }
        };

        for (int i = 0; i < steps.length; i++) {
            content.add(buildAccordionCard(i + 1, steps[i][0], steps[i][1]));
            content.add(Box.createVerticalStrut(8));
        }

        return wrapScroll(content);
    }

    private static JPanel buildAccordionCard(int step, String title, String desc) {
        // We use a single-element array so the inner anonymous classes can close over
        // a mutable reference without needing a field on a named class.
        boolean[] expanded = { false };

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Step circle (re-painted on toggle)
        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(expanded[0] ? AppTheme.ACCENT : AppTheme.BG_BADGE_BLUE);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(expanded[0] ? Color.WHITE : AppTheme.ACCENT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String n = String.valueOf(step);
                g2.drawString(n, (32 - fm.stringWidth(n)) / 2,
                        (32 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        circle.setPreferredSize(new Dimension(32, 32));
        circle.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);

        // ── Chevron
        JLabel chevron = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.FG_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                FontMetrics fm = g2.getFontMetrics();
                String sym = expanded[0] ? "▾" : "›";
                g2.drawString(sym, (getWidth() - fm.stringWidth(sym)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        chevron.setPreferredSize(new Dimension(22, 22));
        chevron.setOpaque(false);

        // ── Header row
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(circle);
        left.add(titleLbl);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(AppTheme.BG_SURFACE);
        header.setBorder(new EmptyBorder(13, 16, 13, 16));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(left, BorderLayout.CENTER);
        header.add(chevron, BorderLayout.EAST);

        // ── Body (collapsed by default)
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(0xF7F9FD));
        body.setBorder(new EmptyBorder(0, 16, 0, 16));
        body.setVisible(false);

        // GIF placeholder
        JPanel gifBox = new JPanel(new BorderLayout());
        gifBox.setBackground(new Color(0xEBF0FA));
        gifBox.setPreferredSize(new Dimension(0, 158));
        gifBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 158));
        gifBox.setBorder(BorderFactory.createLineBorder(new Color(0xCDD8F0), 1, true));
        gifBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel gifLbl = new JLabel(new ImageIcon(HelpModule.class.getResource("/images/Help/test.gif")),
                SwingConstants.CENTER);
        gifLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gifLbl.setForeground(new Color(0x90A8CC));
        gifBox.add(gifLbl, BorderLayout.CENTER);

        JLabel descLbl = new JLabel(
                "<html><body style='width:440px; padding:10px 0 14px 0'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLbl.setForeground(AppTheme.FG_MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLbl.setBorder(new EmptyBorder(10, 0, 14, 0));

        body.add(gifBox);
        body.add(descLbl);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // ── Toggle listener
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                expanded[0] = !expanded[0];
                body.setVisible(expanded[0]);
                circle.repaint();
                chevron.repaint();
                card.revalidate();
                card.repaint();
                SwingUtilities.invokeLater(() -> card.scrollRectToVisible(card.getBounds()));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                header.setBackground(new Color(0xF0F4FB));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                header.setBackground(AppTheme.BG_SURFACE);
            }
        });

        // Wrap in an outer panel so BoxLayout max-size works properly
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Ordering tab ──────────────────────────────────────────────

    private static JPanel buildOrderingTab() {
        JPanel content = tabContent();

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(buildCard(
                (g2, cx, cy) -> {
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
                    for (int i = 0; i < 3; i++) {
                        int ry = cy - 7 + i * 8;
                        g2.drawOval(cx - 13, ry - 3, 6, 6);
                        if (i == 1)
                            g2.fillOval(cx - 11, ry - 1, 2, 2);
                        g2.drawLine(cx - 4, ry, cx + 13, ry);
                    }
                },
                "Product Variants",
                "Choose size or add-on variants for applicable items before adding to cart."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
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

    // ── Inventory tab ─────────────────────────────────────────────

    private static JPanel buildInventoryTab() {
        JPanel content = tabContent();

        // Admin warning banner — same look as OrderingPanel unavailable notices
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        banner.setBackground(AppTheme.BG_BADGE_YELLOW);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.WARNING, 1, true),
                new EmptyBorder(0, 0, 0, 0)));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
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
                    g2.drawRoundRect(cx - 12, cy - 4, 24, 12, 2, 2);
                    g2.drawRoundRect(cx - 10, cy - 14, 20, 10, 2, 2);
                    g2.drawLine(cx - 12, cy, cx + 12, cy);
                },
                "Stock Management",
                "Monitor current quantities, alert thresholds, and batch SKUs across all inventory items."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    for (int i = 0; i < 3; i++) {
                        g2.drawRoundRect(cx - 12 + i * 2, cy - 10 + i * 7, 20, 8, 2, 2);
                    }
                },
                "Batch Tracking",
                "Each item can have multiple batches with separate SKUs, quantities, and expiry dates."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    g2.drawOval(cx - 12, cy - 12, 24, 24);
                    g2.drawLine(cx, cy, cx, cy - 8);
                    g2.drawLine(cx, cy, cx + 6, cy);
                    g2.drawLine(cx + 9, cy + 9, cx + 13, cy + 5);
                    g2.drawLine(cx + 9, cy + 9, cx + 13, cy + 13);
                },
                "FEFO Deduction",
                "The system always deducts from the batch with the earliest expiry date first, minimizing waste."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
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

    // ── Reports tab ───────────────────────────────────────────────

    private static JPanel buildReportsTab() {
        JPanel content = tabContent();

        JPanel grid = new JPanel(new GridLayout(1, 3, 12, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(buildCard(
                (g2, cx, cy) -> {
                    g2.drawOval(cx - 10, cy - 12, 16, 16);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx + 4, cy + 2, cx + 12, cy + 12);
                },
                "Search",
                "Filter by item name or date range using the search bar at the top of Inventory and Monitoring screens."));

        grid.add(buildCard(
                (g2, cx, cy) -> {
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
                    g2.drawRoundRect(cx - 12, cy - 12, 24, 22, 2, 2);
                    g2.drawLine(cx - 12, cy - 5, cx + 12, cy - 5);
                    g2.drawLine(cx - 4, cy - 12, cx - 4, cy - 7);
                    g2.drawLine(cx + 4, cy - 12, cx + 4, cy - 7);
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

    // ── FAQ tab ───────────────────────────────────────────────────

    private static JPanel buildFaqTab() {
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
            if (i < faqs.length - 1)
                content.add(Box.createVerticalStrut(10));
        }

        return wrapScroll(content);
    }

    // ═══════════════════════════════════════════════════════════════
    // Shared helpers
    // ═══════════════════════════════════════════════════════════════

    private static JPanel tabContent() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(AppTheme.BG_PRIMARY);
        p.setBorder(new EmptyBorder(20, 4, 20, 4));
        return p;
    }

    private static JPanel wrapScroll(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(AppTheme.BG_PRIMARY);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.BG_PRIMARY);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Info card (icon + title + body) ──────────────────────────

    private static CardPanel buildCard(IconPainter painter, String title, String body) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(14, 0));

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillRoundRect(4, 4, 40, 40, 12, 12);
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

        JLabel bodyLbl = new JLabel("<html><body style='width:220px'>" + body + "</body></html>");
        bodyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bodyLbl.setForeground(AppTheme.FG_MUTED);

        text.add(titleLbl, BorderLayout.NORTH);
        text.add(bodyLbl, BorderLayout.CENTER);

        card.add(iconBox, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ── FAQ card ─────────────────────────────────────────────────

    private static CardPanel buildFaqCard(String question, String answer) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 7));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel qRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        qRow.setOpaque(false);

        JLabel badge = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(AppTheme.ACCENT);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Q", (getWidth() - fm.stringWidth("Q")) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(22, 22));
        badge.setOpaque(false);

        JLabel q = new JLabel(question);
        q.setFont(new Font("Segoe UI", Font.BOLD, 13));
        q.setForeground(AppTheme.FG_PRIMARY);

        qRow.add(badge);
        qRow.add(q);

        JLabel a = new JLabel(
                "<html><body style='width:440px; padding-left:4px'>" + answer + "</body></html>");
        a.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        a.setForeground(AppTheme.FG_MUTED);

        card.add(qRow, BorderLayout.NORTH);
        card.add(a, BorderLayout.CENTER);
        return card;
    }
}