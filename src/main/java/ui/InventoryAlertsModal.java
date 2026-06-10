package ui;

import notifications.Notification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryAlertsModal extends JDialog {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = Color.WHITE;
    private static final Color BG_SURFACE = new Color(0xF9FAFB);
    private static final Color BORDER = new Color(0xE5E7EB);
    private static final Color TEXT_PRI = new Color(0x111827);
    private static final Color TEXT_SEC = new Color(0x374151);
    private static final Color TEXT_MUTED = new Color(0x9CA3AF);
    private static final Color ACCENT = new Color(0x1D4ED8);
    private static final Color ACCENT_HOV = new Color(0x1E40AF);
    private static final Color ROW_BASE = Color.WHITE;
    private static final Color ROW_ALT = new Color(0xF9FAFB);

    private static final Color CRIT_BG = new Color(0xFEE2E2);
    private static final Color CRIT_FG = new Color(0xDC2626);
    private static final Color WARN_BG = new Color(0xFEF3C7);
    private static final Color WARN_FG = new Color(0xD97706);
    private static final Color INFO_BG = new Color(0xE0F2FE);
    private static final Color INFO_FG = new Color(0x0284C7);
    private static final Color GOOD_BG = new Color(0xDCFCE7);
    private static final Color GOOD_FG = new Color(0x16A34A);

    // Group header backgrounds
    private static final Color CRIT_HDR_BG = new Color(0xFEF2F2);
    private static final Color WARN_HDR_BG = new Color(0xFFFBEB);
    private static final Color INFO_HDR_BG = new Color(0xF0F9FF);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_TITLE = new Font("Segoe UI", Font.PLAIN, 17);
    private static final Font F_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font F_COUNT = new Font("Segoe UI", Font.BOLD, 19);
    private static final Font F_HDR = new Font("Segoe UI", Font.BOLD, 12);

    // ── Constructor ───────────────────────────────────────────────────────────
    public InventoryAlertsModal(Window owner, List<Notification> alerts) {
        super(owner, "Inventory alerts", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true); // allow user to widen if messages are long

        boolean hasCrit = alerts.stream()
                .anyMatch(n -> n.getSeverity() == Notification.Severity.CRITICAL);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.add(buildTopBar(alerts, hasCrit), BorderLayout.NORTH);
        root.add(buildCenter(alerts), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(800, 280));
        // Start wider so long messages fit without scrolling sideways
        setSize(Math.max(620, getWidth()), getHeight());
        setLocationRelativeTo(owner);
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar(List<Notification> alerts, boolean hasCrit) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PAGE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(16, 20, 14, 16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        Color bellColor = alerts.isEmpty() ? GOOD_FG : hasCrit ? CRIT_FG : WARN_FG;
        left.add(makeBellIcon(20, bellColor));

        JLabel title = new JLabel("Inventory alerts");
        title.setFont(F_TITLE);
        title.setForeground(TEXT_PRI);
        left.add(title);

        if (!alerts.isEmpty()) {
            left.add(makeCountBadge(alerts.size(), hasCrit ? CRIT_FG : WARN_FG));
        }

        bar.add(left, BorderLayout.WEST);
        bar.add(buildXBtn(), BorderLayout.EAST);
        return bar;
    }

    private JLabel makeCountBadge(int count, Color bg) {
        JLabel lbl = new JLabel(String.valueOf(count)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setOpaque(false);
        int w = count >= 100 ? 32 : count > 9 ? 26 : 20;
        lbl.setPreferredSize(new Dimension(w, 18));
        return lbl;
    }

    // ── Center: summary strip + grouped collapsible sections ──────────────────
    private JPanel buildCenter(List<Notification> alerts) {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_PAGE);

        List<Notification> crits = alerts.stream()
                .filter(n -> n.getSeverity() == Notification.Severity.CRITICAL)
                .collect(Collectors.toList());
        List<Notification> warns = alerts.stream()
                .filter(n -> n.getSeverity() == Notification.Severity.WARNING)
                .collect(Collectors.toList());
        List<Notification> infos = alerts.stream()
                .filter(n -> n.getSeverity() == Notification.Severity.INFO)
                .collect(Collectors.toList());

        if (!alerts.isEmpty()) {
            JPanel strip = new JPanel(new GridLayout(1, 3, 8, 0));
            strip.setBackground(BG_SURFACE);
            strip.setBorder(new EmptyBorder(10, 20, 10, 20));
            strip.add(buildChip("Critical", crits.size(), Notification.Severity.CRITICAL));
            strip.add(buildChip("Warning", warns.size(), Notification.Severity.WARNING));
            strip.add(buildChip("Info", infos.size(), Notification.Severity.INFO));

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(BG_SURFACE);
            wrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            wrap.add(strip);
            center.add(wrap, BorderLayout.NORTH);
        }

        center.add(buildGroupedScrollList(crits, warns, infos), BorderLayout.CENTER);
        return center;
    }

    // ── Grouped, collapsible scroll list ──────────────────────────────────────
    private JScrollPane buildGroupedScrollList(
            List<Notification> crits,
            List<Notification> warns,
            List<Notification> infos) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PAGE);

        boolean anyAlerts = !crits.isEmpty() || !warns.isEmpty() || !infos.isEmpty();

        if (!anyAlerts) {
            panel.add(buildEmptyState());
        } else {
            if (!crits.isEmpty()) {
                panel.add(buildGroup("Critical", crits,
                        Notification.Severity.CRITICAL, CRIT_FG, CRIT_HDR_BG, true));
            }
            if (!warns.isEmpty()) {
                panel.add(buildGroup("Warning", warns,
                        Notification.Severity.WARNING, WARN_FG, WARN_HDR_BG, crits.isEmpty()));
            }
            if (!infos.isEmpty()) {
                panel.add(buildGroup("Info", infos,
                        Notification.Severity.INFO, INFO_FG, INFO_HDR_BG,
                        crits.isEmpty() && warns.isEmpty()));
            }
        }

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(0, Math.min(420, Math.max(160,
                (crits.size() + warns.size() + infos.size()) * 58 + 60))));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_PAGE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    /**
     * Builds a collapsible group section with a clickable header row.
     * The body panel contains all alert rows and is shown/hidden on click.
     *
     * @param label    e.g. "Critical"
     * @param items    alerts in this group
     * @param sev      severity enum value
     * @param accentFg foreground colour for accent elements
     * @param headerBg tinted background for the header row
     * @param expanded whether the section starts open
     */
    private JPanel buildGroup(String label, List<Notification> items,
            Notification.Severity sev, Color accentFg,
            Color headerBg, boolean expanded) {

        // ── Body panel (the rows, hidden when collapsed) ──────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PAGE);

        for (int i = 0; i < items.size(); i++) {
            body.add(buildRow(items.get(i), i, sev));
            if (i < items.size() - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(BORDER);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                sep.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(sep);
            }
        }
        body.setVisible(expanded);

        // ── Chevron arrow (painted inline) ────────────────────────────────────
        // We store the expanded state in a single-element boolean array so the
        // lambda can mutate it.
        final boolean[] open = { expanded };

        JComponent chevron = new JComponent() {
            {
                setPreferredSize(new Dimension(16, 16));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(accentFg);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                if (open[0]) {
                    // Pointing down ∨
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 3);
                    g2.drawLine(cx, cy + 3, cx + 4, cy - 2);
                } else {
                    // Pointing right ›
                    g2.drawLine(cx - 2, cy - 4, cx + 3, cy);
                    g2.drawLine(cx + 3, cy, cx - 2, cy + 4);
                }
                g2.dispose();
            }
        };

        // ── Header row ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(headerBg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Left accent stripe
                g2.setColor(accentFg);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(9, 20, 9, 16));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Icon + label
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(makeSeverityIcon(sev, 18));

        JLabel lbl = new JLabel(label + "  ·  " + items.size()
                + (items.size() == 1 ? " alert" : " alerts"));
        lbl.setFont(F_HDR);
        lbl.setForeground(accentFg);
        left.add(lbl);

        header.add(left, BorderLayout.CENTER);
        header.add(chevron, BorderLayout.EAST);

        // Toggle on click
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                open[0] = !open[0];
                body.setVisible(open[0]);
                chevron.repaint();
                // Revalidate the top-level scroll viewport
                SwingUtilities.invokeLater(() -> {
                    Container c = header.getParent();
                    while (c != null && !(c instanceof JScrollPane))
                        c = c.getParent();
                    if (c != null) {
                        c.revalidate();
                        c.repaint();
                    }
                });
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                header.setBackground(headerBg.darker());
                header.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                header.setBackground(headerBg);
                header.repaint();
            }
        });

        // ── Section wrapper ───────────────────────────────────────────────────
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Bottom border under the section
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        headerWrap.add(header);
        headerWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        headerWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(headerWrap);
        section.add(body);

        // Bottom separator between sections
        JSeparator secSep = new JSeparator();
        secSep.setForeground(BORDER);
        secSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        secSep.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(secSep);

        return section;
    }

    // ── Empty state ───────────────────────────────────────────────────────────
    private JPanel buildEmptyState() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_PAGE);
        outer.setPreferredSize(new Dimension(0, 180));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JComponent ico = makeCheckCircleIcon(44);
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel("All clear");
        t.setFont(F_BOLD);
        t.setForeground(TEXT_PRI);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel s = new JLabel("Inventory looks healthy — no active alerts.");
        s.setFont(F_BODY);
        s.setForeground(TEXT_MUTED);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(Box.createVerticalStrut(12));
        inner.add(ico);
        inner.add(Box.createVerticalStrut(10));
        inner.add(t);
        inner.add(Box.createVerticalStrut(4));
        inner.add(s);
        outer.add(inner);
        return outer;
    }

    // ── Single alert row ──────────────────────────────────────────────────────
    /**
     * Each row uses a multi-line HTML label for the message so long strings
     * wrap instead of getting clipped. The row height is therefore variable;
     * we remove the fixed maximum so the BoxLayout can size it correctly.
     */
    private JPanel buildRow(Notification n, int index, Notification.Severity groupSev) {
        Color fg = sevFg(groupSev);
        Color bg = sevBg(groupSev);

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(index % 2 == 0 ? ROW_BASE : ROW_ALT);
        row.setBorder(new EmptyBorder(11, 20, 11, 16));
        // No fixed maximum height — allow wrapping rows to be taller
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left accent bar
        final Color barColor = fg;
        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 8, 3, 3);
                g2.dispose();
            }
        };
        accentBar.setOpaque(false);
        accentBar.setPreferredSize(new Dimension(3, 0));
        row.add(accentBar, BorderLayout.WEST);

        // Icon + text body
        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);

        JPanel icoWrap = new JPanel(new GridBagLayout());
        icoWrap.setOpaque(false);
        // Align icon to top of the cell so it stays put when text wraps
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(2, 0, 0, 0);
        icoWrap.add(makeSeverityIcon(groupSev, 28), gbc);
        body.add(icoWrap, BorderLayout.WEST);

        // Text column: message only (severity is shown in the group header)
        // Use an HTML label so the text wraps naturally instead of being clipped.
        // The wrapping width is driven by the label's preferred width, which
        // BorderLayout.CENTER gives as "all remaining space".
        JLabel msg = new JLabel("<html><body style='width:100%'>"
                + escapeHtml(n.getMessage()) + "</body></html>");
        msg.setFont(F_BODY);
        msg.setForeground(TEXT_PRI);
        // Ensure the label can shrink below its HTML preferred width
        msg.setMinimumSize(new Dimension(0, 0));

        body.add(msg, BorderLayout.CENTER);
        row.add(body, BorderLayout.CENTER);
        return row;
    }

    /** Minimal HTML escaping so message text is safe inside an HTML label. */
    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ── Summary chip ──────────────────────────────────────────────────────────
    private JPanel buildChip(String name, long count, Notification.Severity sev) {
        boolean active = count > 0;
        Color fg = active ? sevFg(sev) : TEXT_MUTED;
        Color bg = active ? sevBg(sev) : BG_SURFACE;

        JPanel chip = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel icoWrap = new JPanel(new GridBagLayout());
        icoWrap.setOpaque(false);
        icoWrap.add(active ? makeSeverityIcon(sev, 24) : makeCheckCircleIcon(24));
        chip.add(icoWrap, BorderLayout.WEST);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);

        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(F_COUNT);
        countLbl.setForeground(fg);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(F_SMALL);
        nameLbl.setForeground(fg);

        stack.add(countLbl);
        stack.add(nameLbl);
        chip.add(stack, BorderLayout.CENTER);
        return chip;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setBackground(BG_PAGE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        footer.add(buildCloseBtn());
        return footer;
    }

    // ── X button ─────────────────────────────────────────────────────────────
    private JButton buildXBtn() {
        JButton btn = new JButton() {
            private boolean hov;
            {
                init();
            }

            private void init() {
                setPreferredSize(new Dimension(28, 28));
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
                addActionListener(e -> dispose());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                if (hov) {
                    g2.setColor(new Color(0xF3F4F6));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.setColor(TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int p = 8;
                g2.drawLine(p, p, getWidth() - p, getHeight() - p);
                g2.drawLine(getWidth() - p, p, p, getHeight() - p);
                g2.dispose();
            }
        };
        return btn;
    }

    // ── Close button ──────────────────────────────────────────────────────────
    private JButton buildCloseBtn() {
        JButton btn = new JButton("Close") {
            private boolean hov;
            {
                init();
            }

            private void init() {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
                addActionListener(e -> dispose());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(hov ? ACCENT_HOV : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(F_BODY);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(84, 32));
        return btn;
    }

    // ── Painted icons (no emoji, no external resources) ───────────────────────

    /**
     * Triangle + exclamation for CRITICAL/WARNING; circle + "i" for INFO.
     * All drawn with Java2D — zero emoji, zero font glyphs.
     */
    static JComponent makeSeverityIcon(Notification.Severity sev, int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();

                Color circleBg = switch (sev) {
                    case CRITICAL -> new Color(0xFECACA);
                    case WARNING -> new Color(0xFDE68A);
                    default -> new Color(0xBAE6FD);
                };
                Color stroke = sevFg(sev);

                g2.setColor(circleBg);
                g2.fillOval(1, 1, W - 2, H - 2);

                g2.setColor(stroke);
                float sw = Math.max(1.3f, size / 20f);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = W / 2, cy = H / 2;

                if (sev == Notification.Severity.INFO) {
                    // "i" — dot above, bar below
                    int dotR = Math.max(1, size / 12);
                    g2.fillOval(cx - dotR, cy - H / 4 - dotR, dotR * 2, dotR * 2);
                    g2.drawLine(cx, cy - H / 10, cx, cy + H / 4);
                } else {
                    // Triangle + exclamation
                    int halfW = W * 9 / 24;
                    int top = cy - H * 5 / 16;
                    int bot = cy + H * 5 / 16;
                    Path2D.Float tri = new Path2D.Float();
                    tri.moveTo(cx, top);
                    tri.lineTo(cx + halfW, bot);
                    tri.lineTo(cx - halfW, bot);
                    tri.closePath();
                    g2.draw(tri);

                    int lineTop = top + (bot - top) / 4;
                    int lineMid = top + (bot - top) * 6 / 10;
                    int dotY = top + (bot - top) * 76 / 100;
                    int dotR = Math.max(1, size / 18);
                    g2.drawLine(cx, lineTop, cx, lineMid);
                    g2.fillOval(cx - dotR, dotY, dotR * 2, dotR * 2);
                }
                g2.dispose();
            }
        };
    }

    /**
     * Green circle with a checkmark. Used in the empty state and zero-count chips.
     */
    static JComponent makeCheckCircleIcon(int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();
                g2.setColor(GOOD_BG);
                g2.fillOval(1, 1, W - 2, H - 2);
                g2.setColor(GOOD_FG);
                float sw = Math.max(1.6f, size / 14f);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = Math.max(4, size / 5);
                g2.drawLine(pad, H / 2 + 1, W / 2 - 1, H - pad - 1);
                g2.drawLine(W / 2 - 1, H - pad - 1, W - pad, pad + 2);
                g2.dispose();
            }
        };
    }

    /**
     * Bell icon — cubic Bézier body, flat cap bar, clapper dot.
     * No emoji, no font glyph, pure Java2D.
     */
    static JComponent makeBellIcon(int size, Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();
                g2.setColor(color);

                int cx = W / 2;
                int top = H / 7;
                int bot = H * 13 / 16;
                int halfW = W * 2 / 5;

                Path2D.Float bell = new Path2D.Float();
                bell.moveTo(cx - halfW, bot);
                bell.curveTo(cx - halfW, top + (bot - top) * 0.55,
                        cx - halfW * 0.6, top,
                        cx, top);
                bell.curveTo(cx + halfW * 0.6, top,
                        cx + halfW, top + (bot - top) * 0.55,
                        cx + halfW, bot);
                bell.closePath();
                g2.fill(bell);

                g2.setStroke(new BasicStroke(Math.max(1.5f, size / 10f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - halfW - 1, bot, cx + halfW + 1, bot);

                int cr = Math.max(2, size / 8);
                g2.fillOval(cx - cr, bot, cr * 2, cr + 1);
                g2.dispose();
            }
        };
    }

    // ── Severity colour helpers ───────────────────────────────────────────────
    private static Color sevFg(Notification.Severity s) {
        return switch (s) {
            case CRITICAL -> CRIT_FG;
            case WARNING -> WARN_FG;
            default -> INFO_FG;
        };
    }

    private static Color sevBg(Notification.Severity s) {
        return switch (s) {
            case CRITICAL -> CRIT_BG;
            case WARNING -> WARN_BG;
            default -> INFO_BG;
        };
    }

    // ── Anti-aliased graphics helper ──────────────────────────────────────────
    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }
}