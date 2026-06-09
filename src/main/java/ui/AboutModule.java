package ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AboutModule extends JPanel {

    public AboutModule() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BG_PRIMARY);
        content.setBorder(new EmptyBorder(20, 28, 20, 28));

        content.add(aligned(buildHero()));
        content.add(Box.createVerticalStrut(16));
        content.add(aligned(buildTechBadgeRow()));
        content.add(Box.createVerticalStrut(24));
        content.add(aligned(buildSectionTitle("Development Team")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildTeamGrid()));
        content.add(Box.createVerticalStrut(24));
        content.add(aligned(buildSectionTitle("Contact & Support")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildContactCard()));
        content.add(Box.createVerticalStrut(24));
        content.add(aligned(buildSectionTitle("Changelog")));
        content.add(Box.createVerticalStrut(8));
        content.add(aligned(buildChangelogCard()));
        content.add(Box.createVerticalStrut(20));
        content.add(aligned(buildSysInfoRow()));
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

    // ── sections ─────────────────────────────────────────────────────────────

    private static JPanel buildHero() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(false);

        // Coffee mug icon
        JPanel mugPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // blue circle background
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillOval(0, 0, 63, 63);
                // mug line art
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(16, 26, 26, 20, 4, 4);  // mug body
                g2.drawArc(39, 30, 10, 12, -30, -120);   // handle
                g2.drawLine(13, 48, 51, 48);              // saucer top
                g2.drawArc(13, 45, 38, 8, 0, -180);      // saucer curve
                // steam
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] sx = { 22, 29, 36 };
                for (int x : sx) {
                    g2.drawLine(x, 24, x - 1, 20);
                    g2.drawLine(x - 1, 20, x + 1, 16);
                }
                g2.dispose();
            }
        };
        mugPanel.setOpaque(false);
        mugPanel.setPreferredSize(new Dimension(64, 64));
        mugPanel.setMaximumSize(new Dimension(64, 64));
        mugPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appName = new JLabel("Better Mondays Coffee Cafe");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appName.setForeground(AppTheme.FG_PRIMARY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel versionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        versionRow.setOpaque(false);
        versionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionRow.add(buildPillBadge("v1.0", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));
        JLabel sub = new JLabel("Cafe Management System");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(AppTheme.FG_MUTED);
        versionRow.add(sub);

        hero.add(mugPanel);
        hero.add(Box.createVerticalStrut(10));
        hero.add(appName);
        hero.add(Box.createVerticalStrut(6));
        hero.add(versionRow);
        return hero;
    }

    private static JPanel buildTechBadgeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(buildPillBadge("Java 21", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));
        row.add(buildPillBadge("Swing", AppTheme.BG_BADGE_GREEN, AppTheme.SUCCESS));
        row.add(buildPillBadge("SQLite", AppTheme.BG_BADGE_YELLOW, AppTheme.WARNING));
        row.add(buildPillBadge("Firebase-Ready", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT_DARK));
        return row;
    }

    private static JPanel buildTeamGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        String[][] members = {
            { "Aleck Constantinopla", "Main Programmer" },
            { "Rafael Bisnar", "UI Designer" },
            { "Miguel Pallasigui", "Database Administrator" }
        };
        for (String[] m : members) {
            grid.add(buildMemberCard(m[0], m[1]));
        }
        return grid;
    }

    private static CardPanel buildMemberCard(String name, String role) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 0));

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillRoundRect(0, 0, 39, 39, 10, 10);
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(13, 6, 14, 14);              // head
                g2.drawArc(6, 24, 28, 16, 0, 180);      // shoulders
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(40, 40));

        JPanel text = new JPanel(new BorderLayout(0, 3));
        text.setOpaque(false);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(AppTheme.FG_PRIMARY);

        JLabel roleLbl = new JLabel(role);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLbl.setForeground(AppTheme.FG_MUTED);

        text.add(nameLbl, BorderLayout.NORTH);
        text.add(roleLbl, BorderLayout.CENTER);

        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static CardPanel buildContactCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(buildContactRow("email", "[support@bettermondays.com]"));
        card.add(Box.createVerticalStrut(8));
        card.add(buildContactRow("fb", "[facebook.com/bettermondays]"));
        return card;
    }

    private static JPanel buildContactRow(String iconType, String labelText) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if ("email".equals(iconType)) {
                    g2.drawRoundRect(1, 3, 18, 12, 2, 2);
                    g2.drawLine(1, 3, 10, 10);
                    g2.drawLine(10, 10, 19, 3);
                } else {
                    g2.drawOval(1, 1, 18, 18);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("f", 10 - fm.stringWidth("f") / 2, 14);
                }
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(22, 22));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(AppTheme.ACCENT);

        row.add(icon, BorderLayout.WEST);
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private static CardPanel buildChangelogCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel versionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        versionRow.setOpaque(false);
        versionRow.add(buildPillBadge("v1.0", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));
        JLabel date = new JLabel("June 2026");
        date.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        date.setForeground(AppTheme.FG_MUTED);
        versionRow.add(date);

        JPanel bullets = new JPanel();
        bullets.setLayout(new BoxLayout(bullets, BoxLayout.Y_AXIS));
        bullets.setOpaque(false);
        String[] items = {
            "Point-of-sale ordering with variant support",
            "ABC / EOQ inventory analytics",
            "FEFO batch tracking and expiry management",
            "Sales monitoring and CSV export",
            "Admin staff management and role control"
        };
        for (String item : items) {
            bullets.add(buildBullet(item));
            bullets.add(Box.createVerticalStrut(4));
        }

        card.add(versionRow, BorderLayout.NORTH);
        card.add(bullets, BorderLayout.CENTER);
        return card;
    }

    private static JPanel buildBullet(String text) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dot = new JLabel("•");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dot.setForeground(AppTheme.ACCENT);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(AppTheme.FG_PRIMARY);

        row.add(dot, BorderLayout.WEST);
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildSysInfoRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        JButton btn = new JButton("View System Info");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(AppTheme.ACCENT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        AppTheme.applyToComponent(btn);

        btn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "OS: " + System.getProperty("os.name") +
                "\nJava: " + System.getProperty("java.version") +
                "\nUser: " + System.getProperty("user.name"),
                "System Info", JOptionPane.INFORMATION_MESSAGE));

        row.add(btn);
        return row;
    }
}
