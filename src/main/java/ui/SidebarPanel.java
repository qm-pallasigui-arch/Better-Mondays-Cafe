package ui;

import loginregister.UserDataManager.Role;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class SidebarPanel extends JPanel {

    public interface NavigationListener {
        void onNavigate(String pageName);
    }

    private static final int EXPANDED_WIDTH = 228;
    private static final int COLLAPSED_WIDTH = 60;
    private static final int NAV_HEIGHT = 40;

    private final NavigationListener listener;
    private final boolean isAdmin;
    private final String username;
    private boolean collapsed = false;
    private String activePage = "Ordering";

    private final List<NavItem> navItems = new ArrayList<>();
    private JLabel brandingText;
    private JButton collapseBtn;
    private final JPanel navPanel;
    private final JPanel headerPanel;

    public SidebarPanel(String username, Role role, NavigationListener listener) {
        this.username = username;
        this.isAdmin = role == Role.ADMIN;
        this.listener = listener;

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PRIMARY);

        headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);

        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(AppTheme.BG_PRIMARY);
        buildNavContent();

        JScrollPane scroll = new JScrollPane(navPanel);
        scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        add(scroll, BorderLayout.CENTER);

        add(createUserProfile(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));
    }

    public void setActivePage(String page) {
        activePage = page;
        for (NavItem ni : navItems) {
            ni.setActive(ni.pageName.equals(page));
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    private void toggleCollapse() {
        collapsed = !collapsed;
        int w = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        setPreferredSize(new Dimension(w, getPreferredSize().height));
        headerPanel.setPreferredSize(new Dimension(w, 56));
        for (NavItem ni : navItems) {
            ni.setCollapsed(collapsed);
        }
        brandingText.setVisible(!collapsed);
        collapseBtn.setText(collapsed ? "\u25B6" : "\u25C0");
        revalidate();
        repaint();
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(AppTheme.BG_PRIMARY);
        p.setPreferredSize(new Dimension(EXPANDED_WIDTH, 56));
        p.setBorder(new EmptyBorder(0, 10, 0, 6));

        JPanel brand = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        brand.setOpaque(false);

        JLabel logo = new JLabel("\u2615");
        logo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        logo.setForeground(AppTheme.FG_PRIMARY);

        brandingText = new JLabel("Better Mondays");
        brandingText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        brandingText.setForeground(AppTheme.FG_PRIMARY);

        brand.add(logo);
        brand.add(brandingText);

        collapseBtn = new JButton("\u25C0");
        collapseBtn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        collapseBtn.setForeground(AppTheme.FG_MUTED);
        collapseBtn.setBackground(AppTheme.BG_PRIMARY);
        collapseBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        collapseBtn.setFocusPainted(false);
        collapseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapseBtn.addActionListener(e -> toggleCollapse());

        p.add(brand, BorderLayout.CENTER);
        p.add(collapseBtn, BorderLayout.EAST);
        return p;
    }

    private void buildNavContent() {
        addNavItem("Ordering", "Ordering", this::paintCoffeeIcon);
        addNavItem("Search", "Search", this::paintSearchIcon);
        if (isAdmin) {
            addNavItem("Inventory", "Inventory", this::paintBoxIcon);
            addNavItem("Monitoring", "Monitoring", this::paintChartIcon);
            addNavItem("Menu Maintenance", "Menu Maintenance", this::paintListIcon);
            addNavItem("Register Product", "Register Product", this::paintTagIcon);
        }
        addNavItem("Staff", "Staff", this::paintPeopleIcon);

        navPanel.add(Box.createVerticalGlue());

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(60, 85, 120));
        sep.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        navPanel.add(sep);
        navPanel.add(Box.createVerticalStrut(4));

        addNavItem("Inventory Guide", "Inventory Guide", this::paintBookIcon);
        addNavItem("About", "About", this::paintInfoIcon);
        addNavItem("Help", "Help", this::paintHelpIcon);
    }

    private void addNavItem(String pageName, String label, IconPainter painter) {
        NavItem item = new NavItem(pageName, label, painter);
        navItems.add(item);
        navPanel.add(item);
    }

    private JPanel createUserProfile() {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setBackground(new Color(28, 43, 63));
        p.setBorder(new EmptyBorder(8, 10, 8, 8));
        p.setPreferredSize(new Dimension(EXPANDED_WIDTH, 50));

        JLabel userIcon = new JLabel(String.valueOf(Character.toUpperCase(username.charAt(0)))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        userIcon.setPreferredSize(new Dimension(30, 30));
        userIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel userNameLabel = new JLabel(username);
        userNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userNameLabel.setForeground(AppTheme.FG_PRIMARY);

        JLabel threeDots = new JLabel("\u22EE");
        threeDots.setFont(new Font("Segoe UI", Font.BOLD, 16));
        threeDots.setForeground(AppTheme.FG_MUTED);
        threeDots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        p.add(userIcon, BorderLayout.WEST);
        p.add(userNameLabel, BorderLayout.CENTER);
        p.add(threeDots, BorderLayout.EAST);
        return p;
    }

    // ─── Icon painters (minimalist line-art) ─────────────────

    @FunctionalInterface
    private interface IconPainter {
        void paint(Graphics2D g, int x, int y, int size);
    }

    private void paintCoffeeIcon(Graphics2D g, int x, int y, int s) {
        g.drawRoundRect(x + 1, y + 1, s - 5, s - 3, 3, 3);
        g.drawArc(x + s - 5, y + 4, 5, s / 3, 0, -180);
    }

    private void paintSearchIcon(Graphics2D g, int x, int y, int s) {
        g.drawOval(x + 1, y + 1, s - 7, s - 7);
        g.drawLine(x + s - 8, y + s - 8, x + s - 2, y + s - 2);
    }

    private void paintBoxIcon(Graphics2D g, int x, int y, int s) {
        g.drawRect(x + 1, y + 4, s - 2, s - 7);
        g.drawLine(x + 1, y + s / 2, x + s - 1, y + s / 2);
    }

    private void paintChartIcon(Graphics2D g, int x, int y, int s) {
        int bw = 3;
        int[] hs = {s - 8, s - 4, s - 12};
        for (int i = 0; i < 3; i++) {
            g.fillRect(x + i * (bw + 3) + 1, y + s - hs[i] - 2, bw, hs[i]);
        }
    }

    private void paintListIcon(Graphics2D g, int x, int y, int s) {
        for (int i = 0; i < 3; i++) {
            int yy = y + 2 + i * 6;
            g.fillOval(x + 1, yy + 1, 3, 3);
            g.drawLine(x + 7, yy + 2, x + s - 2, yy + 2);
        }
    }

    private void paintTagIcon(Graphics2D g, int x, int y, int s) {
        int[] xs = {x + 1, x + s - 1, x + s - 1, x + 1};
        int[] ys = {y + s / 2, y + 1, y + s - 1, y + s / 2};
        g.drawPolygon(xs, ys, 4);
    }

    private void paintPeopleIcon(Graphics2D g, int x, int y, int s) {
        g.drawOval(x + 1, y + 1, 5, 5);
        g.drawLine(x + 4, y + 7, x + 4, y + s - 2);
        g.drawLine(x + 1, y + 10, x + 7, y + 10);
        g.drawOval(x + s - 7, y + 3, 5, 5);
        g.drawLine(x + s - 5, y + 9, x + s - 5, y + s - 2);
        g.drawLine(x + s - 8, y + 12, x + s - 2, y + 12);
    }

    private void paintBookIcon(Graphics2D g, int x, int y, int s) {
        g.drawRect(x + 1, y + 1, s / 2 - 1, s - 2);
        g.drawRect(x + s / 2, y + 1, s / 2 - 1, s - 2);
        g.drawLine(x + s / 2, y + 1, x + s / 2, y + s - 1);
    }

    private void paintInfoIcon(Graphics2D g, int x, int y, int s) {
        g.drawOval(x + 1, y + 1, s - 2, s - 2);
        g.setFont(new Font("Segoe UI", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        String text = "i";
        g.drawString(text, x + (s - fm.stringWidth(text)) / 2,
                y + (s - fm.getHeight()) / 2 + fm.getAscent() - 1);
    }

    private void paintHelpIcon(Graphics2D g, int x, int y, int s) {
        g.drawOval(x + 1, y + 1, s - 2, s - 2);
        g.setFont(new Font("Segoe UI", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        String text = "?";
        g.drawString(text, x + (s - fm.stringWidth(text)) / 2 + 1,
                y + (s - fm.getHeight()) / 2 + fm.getAscent() - 1);
    }

    // ─── NavItem inner class ────────────────────────────────

    private class NavItem extends JPanel {
        private final String pageName;
        private final String label;
        private final IconPainter painter;
        private boolean active = false;
        private boolean hovered = false;
        private boolean collapsed = false;

        NavItem(String pageName, String label, IconPainter painter) {
            this.pageName = pageName;
            this.label = label;
            this.painter = painter;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setActivePage(pageName);
                    if (listener != null) {
                        listener.onNavigate(pageName);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean a) {
            active = a;
            repaint();
        }

        void setCollapsed(boolean c) {
            collapsed = c;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
            return new Dimension(w, NAV_HEIGHT);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(COLLAPSED_WIDTH, NAV_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Short.MAX_VALUE, NAV_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(new Color(50, 157, 111, 25));
                g2.fillRect(0, 0, w, h);
                g2.setColor(AppTheme.ACCENT);
                g2.fillRect(0, 0, 3, h);
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRect(0, 0, w, h);
            }

            int iconSize = 18;
            int ix = collapsed ? (w - iconSize) / 2 : 14;
            int iy = (h - iconSize) / 2;
            Color iconColor = active ? AppTheme.ACCENT
                    : (hovered ? AppTheme.FG_PRIMARY : AppTheme.FG_MUTED);
            g2.setColor(iconColor);
            g2.setStroke(new java.awt.BasicStroke(1.6f,
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            painter.paint(g2, ix, iy, iconSize);

            if (!collapsed) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(iconColor);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, 44, (h - fm.getHeight()) / 2 + fm.getAscent());
            }

            g2.dispose();
        }
    }
}
