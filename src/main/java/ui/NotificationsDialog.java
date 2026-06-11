package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NotificationsDialog — light-mode notification centre for Better Mondays CMS.
 *
 * Fixes over previous version:
 * • Tab switching actually rebuilds the tab bar so the underline + colour
 * update.
 * • Per-row Archive / Delete buttons appear on hover (right side).
 * • Footer "Archive all" moves unread → Archive tab; "Delete all" removes
 * permanently.
 * • All icons are drawn into a strict square viewport (no stretching).
 * • Header and footer use HEADER_BG; list body uses pure white.
 */
public class NotificationsDialog extends JDialog {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BG_ROW_HOVER = new Color(0xF0, 0xF6, 0xFF);
    private static final Color BG_ROW_UNREAD = new Color(0xF7, 0xFB, 0xFF);
    private static final Color ROW_DIVIDER = new Color(0xE5, 0xEA, 0xF0);
    private static final Color BORDER_COLOR = new Color(0xD1, 0xD9, 0xE6);
    private static final Color TAB_ACTIVE_UL = new Color(0x3B, 0x82, 0xF6);
    private static final Color ACCENT_BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color ACCENT_AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color ACCENT_RED = new Color(0xEF, 0x44, 0x44);
    private static final Color ACCENT_GREEN = new Color(0x10, 0xB9, 0x81);
    private static final Color FG_TITLE = new Color(0x1E, 0x29, 0x3B);
    private static final Color FG_BODY = new Color(0x47, 0x5A, 0x6E);
    private static final Color FG_TIME = new Color(0x94, 0xA3, 0xB8);
    private static final Color FG_TAB_ACTIVE = new Color(0x1E, 0x29, 0x3B);
    private static final Color FG_TAB_IDLE = new Color(0x94, 0xA3, 0xB8);
    private static final Color FG_ICON_IDLE = new Color(0xB0, 0xC0, 0xD4);
    private static final Color FG_ICON_HOVER = new Color(0x3B, 0x82, 0xF6);
    private static final Color FG_DEL_HOVER = new Color(0xEF, 0x44, 0x44);
    private static final Color BADGE_BG = new Color(0x3B, 0x82, 0xF6);
    private static final Color UNREAD_DOT = new Color(0x3B, 0x82, 0xF6);
    private static final Color CLOSE_BTN_BG = new Color(0xF1, 0xF5, 0xF9);
    private static final Color CLOSE_BTN_BD = new Color(0xD1, 0xD9, 0xE6);
    private static final Color MARK_READ_FG = new Color(0x3B, 0x82, 0xF6);
    private static final Color HEADER_BG = new Color(0xF8, 0xFA, 0xFC);

    // ── Data model ───────────────────────────────────────────────────────────
    public enum NotifType {
        INFO, WARNING, ALERT, SUCCESS
    }

    public static class Notification {
        public final String title;
        public final String body;
        public final String timestamp;
        public final NotifType type;
        public boolean read;
        public boolean archived;

        public Notification(String title, String body, String timestamp,
                NotifType type, boolean read) {
            this.title = title;
            this.body = body;
            this.timestamp = timestamp;
            this.type = type;
            this.read = read;
        }
    }

    // ── Provider ─────────────────────────────────────────────────────────────
    public interface NotificationProvider {
        List<Notification> fetch();
    }

    private static NotificationProvider provider = null;
    private static final List<Notification> FALLBACK = new ArrayList<>();

    static {
        FALLBACK.add(new Notification(
                "Low Inventory: Espresso Beans",
                "Stock has dropped below the minimum threshold — 2 kg remaining.",
                "2 hours ago", NotifType.ALERT, false));
        FALLBACK.add(new Notification(
                "Daily Sales Report Ready",
                "Yesterday's report is ready. Total sales: ₱18,450.",
                "3 hours ago", NotifType.INFO, false));
        FALLBACK.add(new Notification(
                "Staff Shift Started",
                "Maria Santos started her shift at 07:55 AM.",
                "6 hours ago", NotifType.SUCCESS, false));
        FALLBACK.add(new Notification(
                "Menu Price Updated",
                "Caramel Macchiato changed from ₱150 to ₱165.",
                "8 hours ago", NotifType.INFO, true));
        FALLBACK.add(new Notification(
                "System Backup Complete",
                "Automatic backup completed successfully.",
                "1 day ago", NotifType.SUCCESS, true));
    }

    public static void setProvider(NotificationProvider p) {
        provider = p;
    }

    public static int getPendingCount() {
        List<Notification> list = provider != null ? provider.fetch() : FALLBACK;
        return (int) list.stream().filter(n -> !n.read && !n.archived).count();
    }

    public static void show(Window owner) {
        List<Notification> items = provider != null
                ? provider.fetch()
                : new ArrayList<>(FALLBACK);
        new NotificationsDialog(owner, items).setVisible(true);
    }

    // ── Instance state ───────────────────────────────────────────────────────
    private final List<Notification> notifications;
    private JPanel listPanel;
    private JPanel tabBarPanel; // stored so we can rebuild tabs on switch
    private String activeTab = "All";

    private NotificationsDialog(Window owner, List<Notification> notifications) {
        super(owner, "Notifications", ModalityType.APPLICATION_MODAL);
        this.notifications = notifications;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        getRootPane().setOpaque(false);

        setContentPane(buildRoot());
        setSize(460, 600);
        setLocationRelativeTo(owner);

        // ESC closes
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════
    // Root card
    // ═══════════════════════════════════════════════════════
    private JPanel buildRoot() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 8 * i));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2,
                            getWidth() - i * 2, getHeight() - i * 2, 14, 14));
                }
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f,
                        getWidth() - 1.5f, getHeight() - 1.5f, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(true);
        listPanel.setBackground(BG_CARD);

        JScrollPane scroll = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(buildFooter(), BorderLayout.SOUTH);

        renderList();
        return card;
    }

    // ═══════════════════════════════════════════════════════
    // Header — title row + tab bar (stored as tabBarPanel)
    // ═══════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(HEADER_BG);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(18, 20, 12, 16));

        JLabel titleLbl = new JLabel("Your Notifications");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(FG_TITLE);
        titleRow.add(titleLbl, BorderLayout.WEST);

        JButton closeBtn = buildCloseButton();
        titleRow.add(closeBtn, BorderLayout.EAST);

        // Tab bar — stored so refreshTabs() can replace its contents
        tabBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBarPanel.setOpaque(false);
        tabBarPanel.setBorder(new EmptyBorder(0, 16, 0, 16));
        populateTabBar();

        wrapper.add(titleRow, BorderLayout.NORTH);
        wrapper.add(tabBarPanel, BorderLayout.CENTER);
        wrapper.add(buildHairline(), BorderLayout.SOUTH);
        return wrapper;
    }

    /** Replace tab bar contents and repaint without rebuilding the whole header. */
    private void refreshTabs() {
        tabBarPanel.removeAll();
        populateTabBar();
        tabBarPanel.revalidate();
        tabBarPanel.repaint();
    }

    private void populateTabBar() {
        tabBarPanel.add(buildTab("All"));
        tabBarPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        tabBarPanel.add(buildTab("Archive"));
    }

    private JPanel buildTab(String label) {
        boolean active = label.equals(activeTab);
        long unread = notifications.stream().filter(n -> !n.read && !n.archived).count();

        JPanel tab = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (active) {
                    g.setColor(TAB_ACTIVE_UL);
                    g.fillRect(0, getHeight() - 2, getWidth(), 2);
                }
            }
        };
        tab.setOpaque(false);
        tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(active ? FG_TAB_ACTIVE : FG_TAB_IDLE);

        int tabW = 62;
        int extraW = 0;

        if (label.equals("All") && unread > 0) {
            String num = unread > 9 ? "9+" : String.valueOf(unread);
            JLabel badge = new JLabel(num, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BADGE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    super.paintComponent(g);
                    g2.dispose();
                }
            };
            badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            badge.setForeground(Color.WHITE);
            badge.setOpaque(false);
            extraW = 24;
            badge.setBounds(tabW - 2, 8, extraW, 16);
            tab.add(badge);
        }

        lbl.setBounds(4, 8, tabW - 8, 18);
        tab.add(lbl);
        tab.setPreferredSize(new Dimension(tabW + extraW + 4, 36));

        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!label.equals(activeTab)) {
                    activeTab = label;
                    refreshTabs();
                    renderList();
                }
            }
        });
        return tab;
    }

    // ═══════════════════════════════════════════════════════
    // Notification list
    // ═══════════════════════════════════════════════════════
    private void renderList() {
        listPanel.removeAll();

        List<Notification> visible = notifications.stream()
                .filter(n -> activeTab.equals("Archive") ? n.archived : !n.archived)
                .collect(Collectors.toList());

        if (visible.isEmpty()) {
            listPanel.add(buildEmptyState());
        } else {
            for (int i = 0; i < visible.size(); i++) {
                listPanel.add(buildRow(visible.get(i)));
                if (i < visible.size() - 1)
                    listPanel.add(buildHairline());
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
        // Also refresh tab badge count
        refreshTabs();
    }

    // ── Single row ────────────────────────────────────────
    private JPanel buildRow(Notification n) {
        boolean[] hov = { false };

        // Outer row — paints background, holds avatar + centre + action buttons
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(hov[0] ? BG_ROW_HOVER : n.read ? BG_CARD : BG_ROW_UNREAD);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(12, 18, 12, 12));

        // Action buttons (archive + delete) — only visible on hover
        JPanel actions = buildRowActions(n, row);
        actions.setVisible(false);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hov[0] = true;
                actions.setVisible(true);
                row.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Only hide if cursor truly left the row (not entering a child)
                Point p = e.getPoint();
                if (!row.contains(p)) {
                    hov[0] = false;
                    actions.setVisible(false);
                    row.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!n.read) {
                    n.read = true;
                    renderList();
                }
            }
        });

        // Propagate enter/exit from child components back to the row
        MouseAdapter childFwd = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hov[0] = true;
                actions.setVisible(true);
                row.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point abs = e.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(abs, row);
                if (!row.contains(abs)) {
                    hov[0] = false;
                    actions.setVisible(false);
                    row.repaint();
                }
            }
        };

        // ── Avatar ─────────────────────────────────────────
        Color ac = typeColor(n.type);
        JPanel avatar = new JPanel() {
            final int SZ = 38, ICON = 18;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // soft glow ring
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 22));
                g2.fillOval(0, 0, SZ, SZ);
                // filled circle
                int p = 3;
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 210));
                g2.fillOval(p, p, SZ - p * 2, SZ - p * 2);
                // icon — always square ICON×ICON in the centre
                int ix = (SZ - ICON) / 2, iy = (SZ - ICON) / 2;
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawIcon(g2, typeIcon(n.type), ix, iy, ICON, ICON);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(SZ, SZ);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        avatar.setOpaque(false);
        avatar.addMouseListener(childFwd);

        // ── Text block ─────────────────────────────────────
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel tl = new JLabel("<html><b>" + esc(n.title) + "</b></html>");
        tl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tl.setForeground(n.read ? FG_BODY : FG_TITLE);

        JLabel bl = new JLabel("<html><body style='width:240px'>" + esc(n.body) + "</body></html>");
        bl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bl.setForeground(FG_BODY);

        JLabel ts = new JLabel(n.timestamp);
        ts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ts.setForeground(FG_TIME);

        text.add(tl);
        text.add(Box.createVerticalStrut(2));
        text.add(bl);
        text.add(Box.createVerticalStrut(4));
        text.add(ts);
        text.addMouseListener(childFwd);

        // ── Unread dot + action area (EAST) ────────────────
        JPanel east = new JPanel(new BorderLayout(6, 0));
        east.setOpaque(false);
        east.setPreferredSize(new Dimension(70, 38));

        // Unread dot (top-right corner)
        JPanel dotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        dotPanel.setOpaque(false);
        dotPanel.setPreferredSize(new Dimension(12, 20));
        if (!n.read) {
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UNREAD_DOT);
                    g2.fillOval(0, 0, 8, 8);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(8, 8);
                }

                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            dot.setOpaque(false);
            dotPanel.add(dot);
        }

        east.add(dotPanel, BorderLayout.WEST);
        east.add(actions, BorderLayout.EAST);
        east.addMouseListener(childFwd);

        row.add(avatar, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(east, BorderLayout.EAST);
        return row;
    }

    /**
     * Builds the per-row Archive + Delete icon buttons shown on hover.
     * Archive moves to archive tab; Delete removes the notification entirely.
     */
    private JPanel buildRowActions(Notification n, JPanel row) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        p.setOpaque(false);

        // Archive button (or Unarchive when in Archive tab)
        boolean inArchive = activeTab.equals("Archive");
        JButton archiveBtn = buildMiniIconBtn(
                inArchive ? IconType.UNARCHIVE : IconType.ARCHIVE,
                inArchive ? "Move back to All" : "Archive",
                FG_ICON_HOVER,
                e -> {
                    n.archived = !n.archived;
                    if (n.archived)
                        n.read = true; // archived items count as read
                    renderList();
                });

        // Delete button — removes from list entirely
        JButton deleteBtn = buildMiniIconBtn(IconType.TRASH, "Delete", FG_DEL_HOVER, e -> {
            notifications.remove(n);
            renderList();
        });

        p.add(archiveBtn);
        p.add(deleteBtn);
        return p;
    }

    /** Small 28×28 icon button used in each row. */
    private JButton buildMiniIconBtn(IconType icon, String tip, Color hoverColor, ActionListener action) {
        final int BTN = 26, ICO = 14;
        JButton btn = new JButton() {
            boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) {
                    g2.setColor(new Color(hoverColor.getRed(), hoverColor.getGreen(),
                            hoverColor.getBlue(), 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                int ox = (getWidth() - ICO) / 2;
                int oy = (getHeight() - ICO) / 2;
                g2.setColor(hov ? hoverColor : FG_ICON_IDLE);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawIcon(g2, icon, ox, oy, ICO, ICO);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(BTN, BTN));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tip);
        btn.addActionListener(action);
        return btn;
    }

    // ═══════════════════════════════════════════════════════
    // Empty state
    // ═══════════════════════════════════════════════════════
    private JPanel buildEmptyState() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(60, 0, 60, 0));

        final int ICO = 24, WRAP = 56;
        JPanel iconWrap = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 22));
                g2.fillOval(0, 0, WRAP, WRAP);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 170));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int off = (WRAP - ICO) / 2;
                drawIcon(g2, IconType.INBOX, off, off, ICO, ICO);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(WRAP, WRAP);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        iconWrap.setOpaque(false);
        iconWrap.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("All caught up");
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        msg.setForeground(FG_BODY);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(activeTab.equals("Archive")
                ? "No archived notifications"
                : "No new notifications");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(FG_TIME);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(iconWrap);
        p.add(Box.createVerticalStrut(14));
        p.add(msg);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        return p;
    }

    // ═══════════════════════════════════════════════════════
    // Footer
    // ═══════════════════════════════════════════════════════
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(true);
        footer.setBackground(HEADER_BG);
        footer.add(buildHairline(), BorderLayout.NORTH);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(10, 16, 14, 16));

        // Left: Trash (delete all visible) + Archive (archive all unread)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(buildFooterIconBtn(IconType.TRASH, "Delete all notifications", FG_DEL_HOVER, e -> {
            // Remove all notifications in the current tab view
            if (activeTab.equals("Archive")) {
                notifications.removeIf(n -> n.archived);
            } else {
                notifications.removeIf(n -> !n.archived);
            }
            renderList();
        }));
        left.add(buildFooterIconBtn(IconType.ARCHIVE, "Archive all unread", FG_ICON_HOVER, e -> {
            // Archive all unread (only applies to All tab)
            notifications.stream()
                    .filter(n -> !n.archived && !n.read)
                    .forEach(n -> {
                        n.archived = true;
                        n.read = true;
                    });
            renderList();
        }));

        // Right: envelope + "Mark all as read"
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        final int ENV = 15;
        JPanel envIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MARK_READ_FG);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int ox = (getWidth() - ENV) / 2, oy = (getHeight() - ENV) / 2;
                drawIcon(g2, IconType.ENVELOPE, ox, oy, ENV, ENV);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ENV + 4, ENV + 4);
            }
        };
        envIcon.setOpaque(false);

        JButton markAll = new JButton("Mark all as read");
        markAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        markAll.setForeground(MARK_READ_FG);
        markAll.setOpaque(false);
        markAll.setContentAreaFilled(false);
        markAll.setBorderPainted(false);
        markAll.setFocusPainted(false);
        markAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markAll.addActionListener(e -> {
            notifications.stream().filter(n -> !n.archived).forEach(n -> n.read = true);
            renderList();
        });

        right.add(envIcon);
        right.add(markAll);

        inner.add(left, BorderLayout.WEST);
        inner.add(right, BorderLayout.EAST);
        footer.add(inner, BorderLayout.CENTER);
        return footer;
    }

    /** 32×32 footer icon button with configurable hover colour. */
    private JButton buildFooterIconBtn(IconType icon, String tip, Color hoverColor, ActionListener action) {
        final int BTN = 32, ICO = 16;
        JButton btn = new JButton() {
            boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) {
                    g2.setColor(new Color(hoverColor.getRed(), hoverColor.getGreen(),
                            hoverColor.getBlue(), 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                int ox = (getWidth() - ICO) / 2;
                int oy = (getHeight() - ICO) / 2;
                g2.setColor(hov ? hoverColor : FG_ICON_IDLE);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawIcon(g2, icon, ox, oy, ICO, ICO);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(BTN, BTN));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tip);
        btn.addActionListener(action);
        return btn;
    }

    // ═══════════════════════════════════════════════════════
    // Hairline divider
    // ═══════════════════════════════════════════════════════
    private static JPanel buildHairline() {
        JPanel d = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(ROW_DIVIDER);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        d.setOpaque(false);
        d.setPreferredSize(new Dimension(0, 1));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return d;
    }

    // ═══════════════════════════════════════════════════════
    // Close button
    // ═══════════════════════════════════════════════════════
    private JButton buildCloseButton() {
        JButton btn = new JButton() {
            boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0xE2, 0xE8, 0xF0) : CLOSE_BTN_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(CLOSE_BTN_BD);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                int p = 9;
                g2.setColor(hov ? FG_TITLE : FG_BODY);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(p, p, getWidth() - p, getHeight() - p);
                g2.drawLine(getWidth() - p, p, p, getHeight() - p);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> dispose());
        return btn;
    }

    // ═══════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color typeColor(NotifType t) {
        return switch (t) {
            case ALERT -> ACCENT_RED;
            case WARNING -> ACCENT_AMBER;
            case SUCCESS -> ACCENT_GREEN;
            default -> ACCENT_BLUE;
        };
    }

    private static IconType typeIcon(NotifType t) {
        return switch (t) {
            case ALERT -> IconType.ALERT;
            case WARNING -> IconType.WARNING;
            case SUCCESS -> IconType.CHECK;
            default -> IconType.INFO;
        };
    }

    // ── Icon catalogue ────────────────────────────────────
    enum IconType {
        INFO, WARNING, ALERT, CHECK, INBOX, TRASH, ARCHIVE, UNARCHIVE, ENVELOPE
    }

    /**
     * Draws icon into a strict square bounding box.
     * sz = min(w,h) is computed first so icons never stretch.
     * Caller must pre-set color and stroke.
     */
    private static void drawIcon(Graphics2D g, IconType type, int x, int y, int w, int h) {
        int sz = Math.min(w, h);
        int ox = x + (w - sz) / 2;
        int oy = y + (h - sz) / 2;
        float cx = ox + sz * 0.5f;
        float cy = oy + sz * 0.5f;

        switch (type) {

            case INFO -> {
                g.drawOval(ox, oy, sz, sz);
                int dr = Math.max(1, sz / 10);
                // dot
                g.fillOval((int) (cx - dr), (int) (oy + sz * 0.18f), dr * 2, dr * 2);
                // stem
                g.drawLine((int) cx, (int) (oy + sz * 0.36f), (int) cx, (int) (oy + sz * 0.76f));
            }

            case WARNING -> {
                // equilateral triangle
                int[] xs = { (int) cx, ox, ox + sz };
                int[] ys = { oy, oy + sz, oy + sz };
                g.drawPolygon(xs, ys, 3);
                g.drawLine((int) cx, (int) (oy + sz * 0.38f), (int) cx, (int) (oy + sz * 0.62f));
                int dr = Math.max(1, sz / 10);
                g.fillOval((int) (cx - dr), (int) (oy + sz * 0.70f), dr * 2, dr * 2);
            }

            case ALERT -> {
                // circle with exclamation
                g.drawOval(ox, oy, sz, sz);
                g.drawLine((int) cx, (int) (oy + sz * 0.22f), (int) cx, (int) (oy + sz * 0.54f));
                int dr = Math.max(1, sz / 10);
                g.fillOval((int) (cx - dr), (int) (oy + sz * 0.63f), dr * 2, dr * 2);
            }

            case CHECK -> {
                g.drawOval(ox, oy, sz, sz);
                g.drawPolyline(
                        new int[] { (int) (ox + sz * 0.28f), (int) (ox + sz * 0.45f), (int) (ox + sz * 0.72f) },
                        new int[] { (int) (oy + sz * 0.52f), (int) (oy + sz * 0.68f), (int) (oy + sz * 0.32f) },
                        3);
            }

            case INBOX -> {
                // tray body
                g.drawRoundRect(ox, (int) (oy + sz * 0.36f), sz, (int) (sz * 0.57f), 3, 3);
                // roof / V shape
                g.drawLine(ox, (int) (oy + sz * 0.36f), (int) cx, (int) (oy + sz * 0.16f));
                g.drawLine((int) cx, (int) (oy + sz * 0.16f), ox + sz, (int) (oy + sz * 0.36f));
            }

            case TRASH -> {
                // handle on top
                g.drawRoundRect((int) (ox + sz * 0.30f), oy, (int) (sz * 0.40f), (int) (sz * 0.24f), 3, 3);
                // lid line
                g.drawLine((int) (ox + sz * 0.08f), (int) (oy + sz * 0.24f),
                        (int) (ox + sz * 0.92f), (int) (oy + sz * 0.24f));
                // body
                g.drawRoundRect((int) (ox + sz * 0.10f), (int) (oy + sz * 0.24f),
                        (int) (sz * 0.80f), (int) (sz * 0.72f), 3, 3);
                // three vertical lines inside
                float lTop = oy + sz * 0.38f, lBot = oy + sz * 0.82f;
                g.drawLine((int) cx, (int) lTop, (int) cx, (int) lBot);
                g.drawLine((int) (cx - sz * 0.18f), (int) lTop, (int) (cx - sz * 0.18f), (int) lBot);
                g.drawLine((int) (cx + sz * 0.18f), (int) lTop, (int) (cx + sz * 0.18f), (int) lBot);
            }

            case ARCHIVE -> {
                // lid (closed box top)
                g.drawRoundRect((int) (ox + sz * 0.04f), oy, (int) (sz * 0.92f), (int) (sz * 0.38f), 3, 3);
                // body
                g.drawRoundRect(ox, (int) (oy + sz * 0.38f), sz, (int) (sz * 0.56f), 3, 3);
                // down arrow
                g.drawLine((int) cx, (int) (oy + sz * 0.52f), (int) cx, (int) (oy + sz * 0.76f));
                g.drawLine((int) (cx - sz * 0.14f), (int) (oy + sz * 0.63f), (int) cx, (int) (oy + sz * 0.76f));
                g.drawLine((int) (cx + sz * 0.14f), (int) (oy + sz * 0.63f), (int) cx, (int) (oy + sz * 0.76f));
            }

            case UNARCHIVE -> {
                // same box but arrow points UP (restore from archive)
                g.drawRoundRect((int) (ox + sz * 0.04f), oy, (int) (sz * 0.92f), (int) (sz * 0.38f), 3, 3);
                g.drawRoundRect(ox, (int) (oy + sz * 0.38f), sz, (int) (sz * 0.56f), 3, 3);
                // up arrow
                g.drawLine((int) cx, (int) (oy + sz * 0.76f), (int) cx, (int) (oy + sz * 0.52f));
                g.drawLine((int) (cx - sz * 0.14f), (int) (oy + sz * 0.64f), (int) cx, (int) (oy + sz * 0.52f));
                g.drawLine((int) (cx + sz * 0.14f), (int) (oy + sz * 0.64f), (int) cx, (int) (oy + sz * 0.52f));
            }

            case ENVELOPE -> {
                g.drawRoundRect(ox, (int) (oy + sz * 0.18f), sz, (int) (sz * 0.64f), 3, 3);
                g.drawLine(ox, (int) (oy + sz * 0.18f), (int) cx, (int) (oy + sz * 0.52f));
                g.drawLine((int) cx, (int) (oy + sz * 0.52f), ox + sz, (int) (oy + sz * 0.18f));
            }
        }
    }

    // ── Slim scrollbar ────────────────────────────────────
    private static class SlimScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(0xC8, 0xD6, 0xE8);
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zeroBtn();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zeroBtn();
        }

        private JButton zeroBtn() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty())
                return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }
    }
}