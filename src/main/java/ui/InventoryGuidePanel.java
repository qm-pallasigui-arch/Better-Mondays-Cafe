package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class InventoryGuidePanel extends JPanel {

    @FunctionalInterface
    private interface IconPainter {
        void paint(Graphics2D g2, int cx, int cy);
    }

    public InventoryGuidePanel() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BG_PRIMARY);
        content.setBorder(new EmptyBorder(20, 28, 20, 28));

        content.add(buildHeader("Inventory Status Guide",
                "Color cues, ABC classification, EOQ, ROP, and FEFO reference"));
        content.add(Box.createVerticalStrut(20));

        content.add(aligned(buildSectionTitle("Color Status Indicators")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildColorStatusRow()));
        content.add(Box.createVerticalStrut(24));

        content.add(aligned(buildSectionTitle("ABC Classification")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildAbcCard()));
        content.add(Box.createVerticalStrut(24));

        content.add(aligned(buildSectionTitle("EOQ & ROP")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildEoqRopRow()));
        content.add(Box.createVerticalStrut(24));

        content.add(aligned(buildSectionTitle("FEFO")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildFefoCard()));
        content.add(Box.createVerticalStrut(20));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(AppTheme.BG_PRIMARY);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static JComponent aligned(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel buildHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t.setForeground(AppTheme.FG_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        s.setForeground(AppTheme.FG_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(s);
        return p;
    }

    private static JLabel buildSectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        l.setForeground(AppTheme.FG_PRIMARY);
        return l;
    }

    private static JPanel buildPillBadge(String text, Color bg, Color fg) {
        JPanel pill = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(3, 10, 3, 10));
        pill.add(lbl, BorderLayout.CENTER);
        return pill;
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

        JLabel bodyLbl = new JLabel("<html><body style='width:300px'>" + body + "</body></html>");
        bodyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bodyLbl.setForeground(AppTheme.FG_MUTED);

        text.add(titleLbl, BorderLayout.NORTH);
        text.add(bodyLbl, BorderLayout.CENTER);

        card.add(iconBox, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ── sections ─────────────────────────────────────────────────────────────

    private static JPanel buildColorStatusRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);

        row.add(buildStatusCard("EXPIRED",
                AppTheme.BG_BADGE_RED, AppTheme.DANGER,
                "At least one batch has passed its expiry date. Dispose immediately."));
        row.add(buildStatusCard("LOW / EXPIRING",
                AppTheme.BG_BADGE_YELLOW, AppTheme.WARNING,
                "Stock is at or below the alert level, or a batch expires within 7 days."));
        row.add(buildStatusCard("GOOD",
                AppTheme.BG_BADGE_GREEN, AppTheme.SUCCESS,
                "Stock levels are adequate and no batches are near expiry."));
        return row;
    }

    private static CardPanel buildStatusCard(String label, Color bg, Color fg, String desc) {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillRow.setOpaque(false);
        pillRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pillRow.add(buildPillBadge(label, bg, fg));

        JLabel descLbl = new JLabel("<html><body style='width:160px'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(AppTheme.FG_MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(pillRow);
        card.add(Box.createVerticalStrut(8));
        card.add(descLbl);
        return card;
    }

    private static CardPanel buildAbcCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JLabel intro = new JLabel("Items are ranked by cumulative usage value (annual demand × unit cost).");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        intro.setForeground(AppTheme.FG_MUTED);

        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 8));
        rows.setOpaque(false);
        rows.add(buildAbcRow("A", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT,
                "High-priority items — top ~80% of cumulative usage value. Monitor closely, order frequently."));
        rows.add(buildAbcRow("B", AppTheme.BG_BADGE_YELLOW, AppTheme.WARNING,
                "Medium-priority items — next ~15%. Review periodically."));
        rows.add(buildAbcRow("C", AppTheme.BG_BADGE_GREEN, AppTheme.FG_MUTED,
                "Lower-priority items — remaining ~5%. Order in bulk when needed."));

        card.add(intro, BorderLayout.NORTH);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private static JPanel buildAbcRow(String tier, Color bg, Color fg, String desc) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillWrap.setOpaque(false);
        pillWrap.setPreferredSize(new Dimension(60, 26));
        pillWrap.add(buildPillBadge(tier, bg, fg));

        JLabel lbl = new JLabel("<html><body style='width:300px'>" + desc + "</body></html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(AppTheme.FG_MUTED);

        row.add(pillWrap, BorderLayout.WEST);
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private static JPanel buildEoqRopRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);

        row.add(buildCard(
                (g2, cx, cy) -> {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    String sym = "≈";
                    g2.drawString(sym, cx - fm.stringWidth(sym) / 2, cy + 2);
                    g2.drawRoundRect(cx - 8, cy + 7, 16, 10, 2, 2);
                    g2.drawLine(cx - 4, cy + 7, cx - 4, cy + 3);
                    g2.drawLine(cx + 4, cy + 7, cx + 4, cy + 3);
                },
                "EOQ — Economic Order Quantity",
                "The optimal reorder quantity minimizing combined ordering and holding costs. Shown as EOQ~N in the status column."));

        row.add(buildCard(
                (g2, cx, cy) -> {
                    g2.drawLine(cx, cy - 14, cx, cy + 14);
                    g2.setColor(AppTheme.DANGER);
                    g2.drawLine(cx - 10, cy + 4, cx + 10, cy + 4);
                    g2.drawLine(cx + 6, cy, cx + 10, cy + 4);
                    g2.drawLine(cx + 6, cy + 8, cx + 10, cy + 4);
                },
                "ROP — Reorder Point",
                "The stock level at which a reorder must be placed. The row turns red when quantity falls at or below ROP~N."));

        return row;
    }

    private static CardPanel buildFefoCard() {
        CardPanel card = buildCard(
                (g2, cx, cy) -> {
                    Color[] colors = { AppTheme.DANGER, AppTheme.WARNING, AppTheme.SUCCESS };
                    for (int i = 0; i < 3; i++) {
                        g2.setColor(colors[i]);
                        g2.fillRoundRect(cx - 14, cy - 12 + i * 10, 28, 8, 2, 2);
                        g2.setColor(AppTheme.BORDER);
                        g2.drawRoundRect(cx - 14, cy - 12 + i * 10, 28, 8, 2, 2);
                    }
                    g2.setColor(AppTheme.ACCENT);
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx + 18, cy - 4, cx + 18, cy + 6);
                    g2.drawLine(cx + 14, cy + 2, cx + 18, cy + 6);
                    g2.drawLine(cx + 22, cy + 2, cx + 18, cy + 6);
                },
                "FEFO — First-Expire-First-Out",
                "Inventory deductions always consume the batch with the nearest expiry date first, minimizing waste.");

        // example pill row
        JPanel exRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        exRow.setOpaque(false);
        exRow.add(buildPillBadge("Batch A · exp Jan 1", AppTheme.BG_BADGE_RED, AppTheme.DANGER));
        exRow.add(buildPillBadge("Batch B · exp Feb 5", AppTheme.BG_BADGE_YELLOW, AppTheme.WARNING));
        exRow.add(buildPillBadge("Batch C · exp Mar 10", AppTheme.BG_BADGE_GREEN, AppTheme.SUCCESS));

        // The card CENTER is the body label; wrap both in a column panel
        Component bodyLbl = card.getComponent(1); // text panel is CENTER
        card.remove(bodyLbl);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel titleLbl = new JLabel("FEFO — First-Expire-First-Out");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(AppTheme.FG_PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel bodyLblNew = new JLabel(
                "<html><body style='width:300px'>Inventory deductions always consume the batch with the nearest expiry date first, minimizing waste.</body></html>");
        bodyLblNew.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bodyLblNew.setForeground(AppTheme.FG_MUTED);
        bodyLblNew.setAlignmentX(Component.LEFT_ALIGNMENT);

        exRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        textCol.add(titleLbl);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(bodyLblNew);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(exRow);

        card.add(textCol, BorderLayout.CENTER);
        return card;
    }
}
