package ui;

import notifications.Notification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;

public class InventoryAlertsModal extends JDialog {

    // ── Palette (mirrors InventoryPanel exactly) ──────────────────────────────
    private static final Color BG_PAGE = Color.WHITE;
    private static final Color BG_SURFACE = new Color(0xF9FAFB);
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY = new Color(0x111827);
    private static final Color TEXT_SECONDARY = new Color(0x374151);
    private static final Color TEXT_MUTED = new Color(0x9CA3AF);
    private static final Color ACCENT = new Color(0x1D4ED8);
    private static final Color ACCENT_HOVER = new Color(0x1E40AF);
    private static final Color ROW_BASE = Color.WHITE;
    private static final Color ROW_ALT = new Color(0xF9FAFB);

    private static final Color ALERT_CRITICAL_BG = new Color(0xFEE2E2);
    private static final Color ALERT_CRITICAL_FG = new Color(0xDC2626);
    private static final Color ALERT_WARN_BG = new Color(0xFEF3C7);
    private static final Color ALERT_WARN_FG = new Color(0xD97706);
    private static final Color ALERT_INFO_BG = new Color(0xE0F2FE);
    private static final Color ALERT_INFO_FG = new Color(0x0284C7);
    private static final Color STATUS_GOOD_BG = new Color(0xDCFCE7);
    private static final Color STATUS_GOOD_FG = new Color(0x16A34A);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 11);

    // ── Constructor ───────────────────────────────────────────────────────────
    public InventoryAlertsModal(Window owner, List<Notification> alerts) {
        super(owner, "Inventory Alerts", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        boolean hasCrit = alerts.stream()
                .anyMatch(n -> n.getSeverity() == Notification.Severity.CRITICAL);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.add(buildTopBar(alerts, hasCrit), BorderLayout.NORTH);
        root.add(buildCenter(alerts), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(500, 260));
        setLocationRelativeTo(owner);
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar(List<Notification> alerts, boolean hasCrit) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_PAGE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(18, 24, 16, 20)));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);

        // Bell icon (painted, no emoji)
        titleRow.add(makeBellIcon(20, alerts.isEmpty() ? STATUS_GOOD_FG
                : hasCrit ? ALERT_CRITICAL_FG : ALERT_WARN_FG));

        JLabel titleLbl = new JLabel("Inventory Alerts");
        titleLbl.setFont(FONT_TITLE);
        titleLbl.setForeground(TEXT_PRIMARY);
        titleRow.add(titleLbl);

        if (!alerts.isEmpty()) {
            JLabel countBadge = new JLabel(String.valueOf(alerts.size())) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = aa(g);
                    g2.setColor(hasCrit ? ALERT_CRITICAL_FG : ALERT_WARN_FG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    super.paintComponent(g);
                    g2.dispose();
                }
            };
            countBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            countBadge.setForeground(Color.WHITE);
            countBadge.setHorizontalAlignment(SwingConstants.CENTER);
            countBadge.setOpaque(false);
            countBadge.setPreferredSize(new Dimension(alerts.size() > 9 ? 28 : 22, 18));
            titleRow.add(countBadge);
        }

        topBar.add(titleRow, BorderLayout.WEST);
        topBar.add(buildXButton(), BorderLayout.EAST);
        return topBar;
    }

    // ── Center: summary strip + alert list ───────────────────────────────────
    private JPanel buildCenter(List<Notification> alerts) {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG_PAGE);

        if (!alerts.isEmpty()) {
            long critCount = alerts.stream().filter(n -> n.getSeverity() == Notification.Severity.CRITICAL).count();
            long warnCount = alerts.stream().filter(n -> n.getSeverity() == Notification.Severity.WARNING).count();
            long infoCount = alerts.size() - critCount - warnCount;

            JPanel strip = new JPanel(new GridLayout(1, 3, 8, 0));
            strip.setBackground(BG_SURFACE);
            strip.setBorder(new EmptyBorder(12, 24, 12, 24));
            strip.add(buildSummaryChip("Critical", critCount, Notification.Severity.CRITICAL));
            strip.add(buildSummaryChip("Warning", warnCount, Notification.Severity.WARNING));
            strip.add(buildSummaryChip("Info", infoCount, Notification.Severity.INFO));

            JPanel stripWrap = new JPanel(new BorderLayout());
            stripWrap.setBackground(BG_SURFACE);
            stripWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            stripWrap.add(strip);
            center.add(stripWrap, BorderLayout.NORTH);
        }

        center.add(buildScrollList(alerts), BorderLayout.CENTER);
        return center;
    }

    // ── Scrollable alert list ─────────────────────────────────────────────────
    private JScrollPane buildScrollList(List<Notification> alerts) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_PAGE);

        if (alerts.isEmpty()) {
            listPanel.add(buildEmptyState());
        } else {
            for (int i = 0; i < alerts.size(); i++) {
                listPanel.add(buildAlertRow(alerts.get(i), i));
                if (i < alerts.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(BORDER_COLOR);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(sep);
                }
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(500, Math.min(360, Math.max(140, alerts.size() * 66 + 16))));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_PAGE);
        return scroll;
    }

    // ── Empty state ───────────────────────────────────────────────────────────
    private JPanel buildEmptyState() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_PAGE);
        outer.setPreferredSize(new Dimension(500, 180));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JComponent checkIco = makeCheckCircleIcon(44);
        checkIco.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("All Clear");
        title.setFont(FONT_BOLD);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Inventory looks healthy — no active alerts.");
        sub.setFont(FONT_BODY);
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(Box.createVerticalStrut(16));
        inner.add(checkIco);
        inner.add(Box.createVerticalStrut(10));
        inner.add(title);
        inner.add(Box.createVerticalStrut(4));
        inner.add(sub);
        outer.add(inner);
        return outer;
    }

    // ── Single alert row ──────────────────────────────────────────────────────
    private JPanel buildAlertRow(Notification n, int index) {
        Color accentBg, accentFg;
        String sevText;
        switch (n.getSeverity()) {
            case CRITICAL -> {
                accentBg = ALERT_CRITICAL_BG;
                accentFg = ALERT_CRITICAL_FG;
                sevText = "CRITICAL";
            }
            case WARNING -> {
                accentBg = ALERT_WARN_BG;
                accentFg = ALERT_WARN_FG;
                sevText = "WARNING";
            }
            default -> {
                accentBg = ALERT_INFO_BG;
                accentFg = ALERT_INFO_FG;
                sevText = "INFO";
            }
        }

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(index % 2 == 0 ? ROW_BASE : ROW_ALT);
        row.setBorder(new EmptyBorder(13, 24, 13, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left severity bar
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(accentFg);
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 8, 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(4, 0));
        row.add(bar, BorderLayout.WEST);

        // Icon + text block
        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);

        JPanel icoWrap = new JPanel(new GridBagLayout());
        icoWrap.setOpaque(false);
        icoWrap.add(makeSeverityIcon(n.getSeverity(), 32));
        content.add(icoWrap, BorderLayout.WEST);

        JPanel textCol = new JPanel(new BorderLayout(0, 4));
        textCol.setOpaque(false);

        // Severity badge pill
        JLabel badge = new JLabel(sevText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(accentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setForeground(accentFg);
        badge.setOpaque(false);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        FontMetrics bfm = badge.getFontMetrics(FONT_BADGE);
        badge.setPreferredSize(new Dimension(bfm.stringWidth(sevText) + 16, 16));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(badge);
        textCol.add(badgeRow, BorderLayout.NORTH);

        JLabel msgLbl = new JLabel(n.getMessage());
        msgLbl.setFont(FONT_BODY);
        msgLbl.setForeground(TEXT_PRIMARY);
        textCol.add(msgLbl, BorderLayout.CENTER);

        content.add(textCol, BorderLayout.CENTER);
        row.add(content, BorderLayout.CENTER);
        return row;
    }

    // ── Summary chip ──────────────────────────────────────────────────────────
    private JPanel buildSummaryChip(String label, long count, Notification.Severity severity) {
        boolean hasCount = count > 0;
        Color fg = hasCount ? severityFg(severity) : TEXT_MUTED;
        Color bg = hasCount ? severityBg(severity) : BG_SURFACE;

        JPanel chip = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel icoWrap = new JPanel(new GridBagLayout());
        icoWrap.setOpaque(false);
        icoWrap.add(hasCount ? makeSeverityIcon(severity, 26) : makeCheckCircleIcon(26));
        chip.add(icoWrap, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        textStack.setOpaque(false);

        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(new Font("Segoe UI", Font.BOLD, 19));
        countLbl.setForeground(fg);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(FONT_SMALL);
        nameLbl.setForeground(fg);

        textStack.add(countLbl);
        textStack.add(nameLbl);
        chip.add(textStack, BorderLayout.CENTER);
        return chip;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(BG_PAGE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        footer.add(buildCloseBtn("Close"));
        return footer;
    }

    // ── X button (top-right) ──────────────────────────────────────────────────
    private JButton buildXButton() {
        JButton btn = new JButton() {
            private boolean hov;
            {
                setPreferredSize(new Dimension(30, 30));
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
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int p = 9;
                g2.drawLine(p, p, getWidth() - p, getHeight() - p);
                g2.drawLine(getWidth() - p, p, p, getHeight() - p);
                g2.dispose();
            }
        };
        return btn;
    }

    // ── Primary close button (footer) ─────────────────────────────────────────
    private JButton buildCloseBtn(String text) {
        JButton btn = new JButton(text) {
            private boolean hov;
            {
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
                g2.setColor(hov ? ACCENT_HOVER : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(FONT_BODY);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(88, 34));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Painted icon helpers ──────────────────────────────────────────────────

    /** Triangle-with-exclamation severity icon, no emoji. */
    static JComponent makeSeverityIcon(Notification.Severity severity, int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();
                Color bg = switch (severity) {
                    case CRITICAL -> new Color(0xFECACA);
                    case WARNING -> new Color(0xFDE68A);
                    default -> new Color(0xBAE6FD);
                };
                Color fg = severityFg(severity);

                // Circle background
                g2.setColor(bg);
                g2.fillOval(0, 0, W - 1, H - 1);

                // Triangle + exclamation mark (requested alert icon)
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = W / 2;
                int cy = H / 2;
                int[] tx = { cx, cx - 10, cx + 10 };
                int[] ty = { cy - 10, cy + 8, cy + 8 };
                g2.drawPolygon(tx, ty, 3); // triangle
                g2.drawLine(cx, cy - 4, cx, cy + 1); // exclamation line
                g2.drawLine(cx, cy + 4, cx, cy + 4); // exclamation dot
                g2.dispose();
            }
        };
    }

    /** Green circle with a checkmark. */
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
                g2.setColor(STATUS_GOOD_BG);
                g2.fillOval(0, 0, W - 1, H - 1);
                g2.setColor(STATUS_GOOD_FG);
                float stroke = Math.max(1.6f, size / 14f);
                g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = Math.max(4, size / 5);
                // Checkmark: short left leg + long right leg
                g2.drawLine(pad, H / 2 + 1, W / 2 - 2, H - pad - 1);
                g2.drawLine(W / 2 - 2, H - pad - 1, W - pad, pad + 2);
                g2.dispose();
            }
        };
    }

    /** Bell icon (painted path, no emoji). */
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

                // Bell body (simplified arc-based path)
                int bx = W / 2;
                int top = H / 6;
                int bot = H * 4 / 5;
                int halfW = W * 2 / 5;

                Path2D.Float bell = new Path2D.Float();
                bell.moveTo(bx - halfW, bot);
                bell.curveTo(bx - halfW, top + (bot - top) / 2,
                        bx - halfW, top,
                        bx, top);
                bell.curveTo(bx + halfW, top,
                        bx + halfW, top + (bot - top) / 2,
                        bx + halfW, bot);
                bell.closePath();
                g2.fill(bell);

                // Handle bar at top
                g2.setStroke(
                        new BasicStroke(Math.max(1.5f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(bx - halfW - 1, bot, bx + halfW + 1, bot);

                // Clapper dot
                int clapR = Math.max(2, size / 8);
                g2.fillOval(bx - clapR, bot, clapR * 2, clapR + 1);

                g2.dispose();
            }
        };
    }

    // ── Severity color helpers ────────────────────────────────────────────────
    private static Color severityFg(Notification.Severity s) {
        return switch (s) {
            case CRITICAL -> ALERT_CRITICAL_FG;
            case WARNING -> ALERT_WARN_FG;
            default -> ALERT_INFO_FG;
        };
    }

    private static Color severityBg(Notification.Severity s) {
        return switch (s) {
            case CRITICAL -> ALERT_CRITICAL_BG;
            case WARNING -> ALERT_WARN_BG;
            default -> ALERT_INFO_BG;
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