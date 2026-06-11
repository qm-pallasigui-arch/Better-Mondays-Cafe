package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * HelpModule — tabbed help panel.
 *
 * Tabs: Quick Start | Ordering | Inventory | Reports | FAQ
 *
 * Quick Start renders 5 accordion cards, each with its OWN gif
 * (step1.gif … step5.gif under /images/Help/). All other tabs
 * are rendered as simple info cards or FAQ cards.
 */
public class HelpModule extends JPanel {

    // ── GIF paths — one per Quick Start step (1-indexed) ──────────
    private static final String[] STEP_GIFS = {
            "/images/Help/login.gif",
            "/images/Help/navigate.gif",
            "/images/Help/order.gif",
            "/images/Help/monitor.gif",
            "/images/Help/sales.gif",
    };

    // ── Tab names ──────────────────────────────────────────────────
    private static final String[] TABS = {
            "Quick Start", "Ordering", "Inventory", "Reports", "FAQ"
    };

    // ── State ──────────────────────────────────────────────────────
    private String activeTab = TABS[0];
    private final java.util.List<JButton> pillButtons = new java.util.ArrayList<>();
    private JPanel contentArea;

    // ── Constructor ────────────────────────────────────────────────
    public HelpModule() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);
        build();
    }

    // ══════════════════════════════════════════════════════════════
    // Top-level layout
    // ══════════════════════════════════════════════════════════════

    private void build() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 14, 10, 14));

        body.add(buildTabBar(), BorderLayout.NORTH);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setOpaque(false);
        showTab(activeTab);
        body.add(contentArea, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    // ── Pill tab bar ───────────────────────────────────────────────

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 2, 0));
        pillButtons.clear();

        for (String tab : TABS) {
            JButton btn = new JButton(tab);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            stylePill(btn, tab.equals(activeTab));
            btn.addActionListener(e -> showTab(tab));
            pillButtons.add(btn);
            bar.add(btn);
        }
        return bar;
    }

    private void stylePill(JButton btn, boolean active) {
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
        pillButtons.forEach(b -> stylePill(b, b.getText().equals(tab)));
        contentArea.removeAll();
        contentArea.add(buildContent(tab), BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    // ══════════════════════════════════════════════════════════════
    // Tab content routers
    // ══════════════════════════════════════════════════════════════

    private JPanel buildContent(String tab) {
        return switch (tab) {
            case "Quick Start" -> buildQuickStart();
            case "Ordering" -> buildOrdering();
            case "Inventory" -> buildInventory();
            case "Reports" -> buildReports();
            case "FAQ" -> buildFaq();
            default -> buildQuickStart();
        };
    }

    // ══════════════════════════════════════════════════════════════
    // Quick Start — 5 accordion cards, each with its own GIF
    // ══════════════════════════════════════════════════════════════

    private JPanel buildQuickStart() {
        JPanel list = columnPanel();

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
            list.add(accordionCard(i + 1, steps[i][0], steps[i][1], STEP_GIFS[i]));
            list.add(Box.createVerticalStrut(8));
        }

        return scrollWrap(list);
    }

    /**
     * One accordion card. Clicking the header reveals/hides the body which
     * contains a GIF specific to this step plus a description label.
     *
     * @param step    1-based step number shown in the circle badge
     * @param title   short step title
     * @param desc    one-sentence description shown when expanded
     * @param gifPath resource path of the GIF for this step
     */
    private static JPanel accordionCard(int step, String title, String desc, String gifPath) {
        boolean[] expanded = { false };

        // ── Outer card (rounded, bordered) ────────────────────────
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
        card.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1, true));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // ── Step number circle ─────────────────────────────────────
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

        // ── Title label ────────────────────────────────────────────
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);

        // ── Chevron (› / ▾) ────────────────────────────────────────
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

        // ── Header row ─────────────────────────────────────────────
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

        // ── Body (hidden until expanded) ───────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(new Color(0xF7F9FD));
        body.setBorder(new EmptyBorder(12, 16, 14, 16));
        body.setVisible(false);

        // GIF — unique per step
        // Fixed 800×450 GIF viewer — never stretches, never shrinks.
        java.net.URL gifUrl = HelpModule.class.getResource(gifPath);
        final Image gifImage = (gifUrl != null) ? new ImageIcon(gifUrl).getImage() : null;

        JPanel gifBox = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 450);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(800, 450);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(800, 450);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setColor(new Color(0xEBF0FA));
                g2.fillRect(0, 0, 800, 450);
                if (gifImage != null) {
                    g2.drawImage(gifImage, 0, 0, 800, 450, this);
                } else {
                    g2.setColor(new Color(0x90A8CC));
                    g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    String msg = "[ GIF placeholder — " + gifPath + " ]";
                    g2.drawString(msg, (800 - fm.stringWidth(msg)) / 2, 225 + fm.getAscent() / 2);
                }
                g2.dispose();
            }
        };
        gifBox.setBorder(BorderFactory.createLineBorder(new Color(0xCDD8F0), 1, true));
        gifBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        gifBox.setOpaque(false);

        // Description
        JLabel descLbl = new JLabel(
                "<html><body style='width:440px; padding:10px 0 4px 0'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLbl.setForeground(AppTheme.FG_MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(gifBox);
        body.add(descLbl);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // ── Toggle listener ────────────────────────────────────────
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

        // Wrapper keeps BoxLayout sizing sane
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════
    // Ordering tab — 2×2 info-card grid
    // ══════════════════════════════════════════════════════════════

    private static JPanel buildOrdering() {
        JPanel list = columnPanel();
        JPanel grid = infoGrid(2, 2);

        grid.add(infoCard(
                "Category Selection",
                "Browse Espresso & Coffee, Tea Latte, Non-Coffee, Fruit Tea, Herbal Tea, and Food categories."));
        grid.add(infoCard(
                "Product Variants",
                "Choose size or add-on variants for applicable items before adding to cart."));
        grid.add(infoCard(
                "Cart Review",
                "Review all items and quantities in the right panel before proceeding to checkout."));
        grid.add(infoCard(
                "Payment & Receipt",
                "Confirm total, process payment, and optionally print or view the receipt."));

        list.add(grid);
        return scrollWrap(list);
    }

    // ══════════════════════════════════════════════════════════════
    // Inventory tab — warning banner + 2×2 info-card grid
    // ══════════════════════════════════════════════════════════════

    private static JPanel buildInventory() {
        JPanel list = columnPanel();

        // Admin-only warning banner
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
        list.add(banner);
        list.add(Box.createVerticalStrut(14));

        JPanel grid = infoGrid(2, 2);
        grid.add(infoCard("Stock Management",
                "Monitor current quantities, alert thresholds, and batch SKUs across all inventory items."));
        grid.add(infoCard("Batch Tracking",
                "Each item can have multiple batches with separate SKUs, quantities, and expiry dates."));
        grid.add(infoCard("FEFO Deduction",
                "The system always deducts from the batch with the earliest expiry date first, minimizing waste."));
        grid.add(infoCard("ABC / EOQ Analysis",
                "Items are classified A/B/C by usage value. EOQ and ROP thresholds are calculated automatically."));

        list.add(grid);
        return scrollWrap(list);
    }

    // ══════════════════════════════════════════════════════════════
    // Reports tab — 1×3 info-card grid
    // ══════════════════════════════════════════════════════════════

    private static JPanel buildReports() {
        JPanel list = columnPanel();
        JPanel grid = infoGrid(1, 3);

        grid.add(infoCard("Search",
                "Filter by item name or date range using the search bar at the top of Inventory and Monitoring screens."));
        grid.add(infoCard("Export CSV",
                "Click the Export button in Sales Reports to download a CSV file of all transactions."));
        grid.add(infoCard("Date Filters",
                "Use the date range pickers in Monitoring to scope reports to a specific time period."));

        list.add(grid);
        return scrollWrap(list);
    }

    // ══════════════════════════════════════════════════════════════
    // FAQ tab — Q&A cards
    // ══════════════════════════════════════════════════════════════

    private static JPanel buildFaq() {
        JPanel list = columnPanel();

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
                        "Items are ranked by cumulative usage value (demand × unit cost). Top ~80 % = A, next ~15 % = B, remaining ~5 % = C." },
                { "What is EOQ?",
                        "Economic Order Quantity — the optimal replenishment size to minimize combined ordering and holding costs, shown as EOQ~N in the status column." }
        };

        for (int i = 0; i < faqs.length; i++) {
            list.add(faqCard(faqs[i][0], faqs[i][1]));
            if (i < faqs.length - 1)
                list.add(Box.createVerticalStrut(10));
        }

        return scrollWrap(list);
    }

    // ══════════════════════════════════════════════════════════════
    // Shared factory helpers
    // ══════════════════════════════════════════════════════════════

    /** Vertical BoxLayout column with standard padding. */
    private static JPanel columnPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(AppTheme.BG_PRIMARY);
        p.setBorder(new EmptyBorder(20, 4, 20, 4));
        return p;
    }

    /** GridLayout panel for info cards. */
    private static JPanel infoGrid(int rows, int cols) {
        JPanel g = new JPanel(new GridLayout(rows, cols, 12, 12));
        g.setOpaque(false);
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        return g;
    }

    /** Wraps a column panel in a scroll pane. */
    private static JPanel scrollWrap(JPanel content) {
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

    // ── Info card (title + body text, no icon) ─────────────────────

    private static JPanel infoCard(String title, String body) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
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
                new EmptyBorder(14, 16, 14, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);

        JLabel bodyLbl = new JLabel("<html><body style='width:200px'>" + body + "</body></html>");
        bodyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bodyLbl.setForeground(AppTheme.FG_MUTED);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(bodyLbl, BorderLayout.CENTER);
        return card;
    }

    // ── FAQ card ───────────────────────────────────────────────────

    private static JPanel faqCard(String question, String answer) {
        JPanel card = new JPanel(new BorderLayout(0, 7)) {
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
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        // Question row with blue "Q" badge
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