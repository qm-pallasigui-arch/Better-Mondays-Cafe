package ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class AboutModule extends JPanel {

    // ── Font system (mirrors OrderingPanel) ───────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BOLD_SM = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_XSMALL = new Font("Segoe UI", Font.PLAIN, 9);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.PLAIN, 9);
    // App-name display size — one step above FONT_TITLE, kept as a named constant
    private static final Font FONT_DISPLAY = new Font("Segoe UI", Font.PLAIN, 24);

    public AboutModule() {
        super(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.BG_PRIMARY);
        content.setBorder(new EmptyBorder(28, 32, 32, 32));

        content.add(aligned(buildHero()));
        content.add(Box.createVerticalStrut(20));
        content.add(aligned(buildDivider()));
        content.add(Box.createVerticalStrut(20));
        content.add(aligned(buildTechBadgeRow()));
        content.add(Box.createVerticalStrut(28));
        content.add(aligned(buildSectionHeader("Development Team", "The people who built this")));
        content.add(Box.createVerticalStrut(12));
        content.add(aligned(buildTeamGrid()));
        content.add(Box.createVerticalStrut(28));
        content.add(aligned(buildSectionHeader("Contact & Support", "Reach us anytime")));
        content.add(Box.createVerticalStrut(12));
        content.add(aligned(buildContactCard()));
        content.add(Box.createVerticalStrut(28));
        content.add(aligned(buildSectionHeader("Changelog", "What's new in this release")));
        content.add(Box.createVerticalStrut(12));
        content.add(aligned(buildChangelogCard()));
        content.add(Box.createVerticalStrut(28));
        content.add(aligned(buildSysInfoRow()));
        content.add(Box.createVerticalStrut(24));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(AppTheme.BG_PRIMARY);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static JComponent aligned(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Thin 1px separator */
    private static JPanel buildDivider() {
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(AppTheme.FG_MUTED.getRed(),
                        AppTheme.FG_MUTED.getGreen(), AppTheme.FG_MUTED.getBlue(), 40));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        div.setOpaque(false);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(100, 1));
        return div;
    }

    /**
     * Section header — title uses FONT_SUBTITLE (Bold 13), subtitle uses
     * FONT_SMALL (Plain 11), matching the hierarchy in OrderingPanel's
     * buildCustomerSection / buildSummarySection headers.
     */
    private static JPanel buildSectionHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(FONT_SUBTITLE);
        t.setForeground(AppTheme.FG_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(FONT_SMALL);
        s.setForeground(AppTheme.FG_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(2));
        p.add(s);
        return p;
    }

    /**
     * Rounded pill badge — label uses FONT_BADGE (Bold 9) to match the
     * status badges in OrderingPanel's createOrderCard.
     */
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
        lbl.setFont(FONT_BADGE);
        lbl.setForeground(fg);
        lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
        pill.add(lbl, BorderLayout.CENTER);
        return pill;
    }

    // ── sections ──────────────────────────────────────────────────────────────

    private static JPanel buildHero() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(false);

        // Coffee mug icon inside a pill-circle
        JPanel mugPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(16, 26, 26, 20, 4, 4);
                g2.drawArc(39, 30, 10, 12, -30, -120);
                g2.drawLine(13, 48, 51, 48);
                g2.drawArc(13, 45, 38, 8, 0, -180);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int x : new int[] { 22, 29, 36 }) {
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

        // App name — FONT_DISPLAY (Bold 24), the only place this large size is used
        JLabel appName = new JLabel("Better Mondays Coffee Cafe");
        appName.setFont(FONT_DISPLAY);
        appName.setForeground(AppTheme.FG_PRIMARY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tagline — FONT_BODY (Plain 12), matching secondary body copy in OrderingPanel
        JLabel tagline = new JLabel("Cafe Management System — made with care in the Philippines");
        tagline.setFont(FONT_BODY);
        tagline.setForeground(AppTheme.FG_MUTED);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel versionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        versionRow.setOpaque(false);
        versionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionRow.add(buildPillBadge("v1.0", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));
        versionRow.add(buildPillBadge("Stable", AppTheme.BG_BADGE_GREEN, AppTheme.SUCCESS));

        hero.add(mugPanel);
        hero.add(Box.createVerticalStrut(12));
        hero.add(appName);
        hero.add(Box.createVerticalStrut(4));
        hero.add(tagline);
        hero.add(Box.createVerticalStrut(10));
        hero.add(versionRow);
        return hero;
    }

    private static JPanel buildTechBadgeRow() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);

        // "Built with" label — FONT_SMALL (Plain 11) matches muted helper labels
        JLabel label = new JLabel("Built with");
        label.setFont(FONT_SMALL);
        label.setForeground(AppTheme.FG_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(buildPillBadge("Java 21", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));
        row.add(buildPillBadge("Swing", AppTheme.BG_BADGE_GREEN, AppTheme.SUCCESS));
        row.add(buildPillBadge("SQLite", AppTheme.BG_BADGE_YELLOW, AppTheme.WARNING));
        row.add(buildPillBadge("Firebase-Ready", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT_DARK));

        wrapper.add(label);
        wrapper.add(Box.createVerticalStrut(6));
        wrapper.add(row);
        return wrapper;
    }

    private static JPanel buildTeamGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        Object[][] members = {
                {
                        "Aleck Constantinopla", "Main Programmer",
                        "/images/Profiles/aleck.jpg",
                        "Born: November 12, 2003", "Technicalogical Institute of the Philippines",
                        "Aleck leads backend architecture and core logic. He designed the POS engine, "
                                + "inventory analytics, and the FEFO batch tracking system."
                },
                {
                        "Rafael Bisnar", "UI Designer",
                        "/images/Profiles/rafael.jpg",
                        "Born: July 5, 2003", "Technicalogical Institute of the Philippines",
                        "Rafael crafted the visual language of Better Mondays — from the color palette "
                                + "to every pixel of the dashboard layout."
                },
                {
                        "Miguel Pallasigui", "Database Administrator",
                        "/images/Profiles/miguel.jpg",
                        "Born: November 20, 2002", "Technicalogical Institute of the Philippines",
                        "Miguel architected the SQLite schema, query optimization strategy, "
                                + "and Firebase integration roadmap for the system."
                }
        };

        for (Object[] m : members) {
            grid.add(buildMemberCard(
                    (String) m[0], (String) m[1], (String) m[2],
                    (String) m[3], (String) m[4], (String) m[5]));
        }
        return grid;
    }

    private static JPanel buildMemberCard(
            String name, String role, String imagePath,
            String born, String school, String summary) {

        JPanel card = new JPanel() {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                        setCursor(Cursor.getDefaultCursor());
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        showProfileModal(name, role, imagePath, born, school, summary,
                                SwingUtilities.getWindowAncestor((Component) e.getSource()));
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = hovered
                        ? new Color(AppTheme.ACCENT.getRed(), AppTheme.ACCENT.getGreen(),
                                AppTheme.ACCENT.getBlue(), 18)
                        : getBackground();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                if (hovered) {
                    g2.setColor(new Color(AppTheme.ACCENT.getRed(), AppTheme.ACCENT.getGreen(),
                            AppTheme.ACCENT.getBlue(), 80));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        CircularAvatarPanel avatar = new CircularAvatarPanel(imagePath, 56);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Member name — FONT_SMALL (Plain 11), matching item name labels in order rows
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(FONT_SMALL);
        nameLbl.setForeground(AppTheme.FG_PRIMARY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Role — FONT_XSMALL (Plain 9), matching metadata/caption text
        JLabel roleLbl = new JLabel(role);
        roleLbl.setFont(FONT_XSMALL);
        roleLbl.setForeground(AppTheme.FG_MUTED);
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // "View profile" hint — FONT_XSMALL (Plain 9) in accent color
        JLabel hint = new JLabel("View profile \u2192");
        hint.setFont(FONT_XSMALL);
        hint.setForeground(AppTheme.ACCENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(avatar);
        card.add(Box.createVerticalStrut(10));
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(3));
        card.add(roleLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(hint);
        return card;
    }

    /** Pop-up profile modal */
    private static void showProfileModal(
            String name, String role, String imagePath,
            String born, String school, String summary, Window owner) {

        JDialog dialog = new JDialog(owner, name, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.setColor(AppTheme.BG_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(28, 28, 28, 28));

        // ── header row ──
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        CircularAvatarPanel av = new CircularAvatarPanel(imagePath, 72);
        header.add(av, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        // Name in modal — FONT_TITLE (Bold 16), the top-level heading size
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(FONT_TITLE);
        nameLbl.setForeground(AppTheme.FG_PRIMARY);

        JPanel roleBadge = buildPillBadge(role, AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT);

        // Born / school metadata — FONT_SMALL (Plain 11)
        JLabel bornLbl = new JLabel(born);
        bornLbl.setFont(FONT_SMALL);
        bornLbl.setForeground(AppTheme.FG_MUTED);

        JLabel schoolLbl = new JLabel("\uD83C\uDF93  " + school);
        schoolLbl.setFont(FONT_SMALL);
        schoolLbl.setForeground(AppTheme.FG_MUTED);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(6));
        info.add(roleBadge);
        info.add(Box.createVerticalStrut(8));
        info.add(bornLbl);
        info.add(Box.createVerticalStrut(3));
        info.add(schoolLbl);

        header.add(info, BorderLayout.CENTER);

        // ── summary ──
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(AppTheme.FG_MUTED.getRed(),
                        AppTheme.FG_MUTED.getGreen(), AppTheme.FG_MUTED.getBlue(), 40));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(100, 1));

        // Bio text — FONT_BODY (Plain 12) for comfortable reading
        JTextArea bio = new JTextArea(summary);
        bio.setFont(FONT_BODY);
        bio.setForeground(AppTheme.FG_PRIMARY);
        bio.setEditable(false);
        bio.setLineWrap(true);
        bio.setWrapStyleWord(true);
        bio.setBorder(new EmptyBorder(14, 0, 0, 0));
        bio.setOpaque(false);

        summaryPanel.add(divider, BorderLayout.NORTH);
        summaryPanel.add(bio, BorderLayout.CENTER);

        // ── close button ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(18, 0, 0, 0));

        JButton close = styledButton("Close", AppTheme.ACCENT);
        close.addActionListener(e -> dialog.dispose());
        btnRow.add(close);

        root.add(header, BorderLayout.NORTH);
        root.add(summaryPanel, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // ── contact ──────────────────────────────────────────────────────────────

    private static CardPanel buildContactCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(buildContactRow("email", "support@bettermondays.com"));
        card.add(Box.createVerticalStrut(10));
        card.add(buildContactRow("fb", "facebook.com/bettermondays"));
        return card;
    }

    private static JPanel buildContactRow(String iconType, String labelText) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillOval(0, 0, 30, 30);
                g2.setColor(AppTheme.ACCENT);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if ("email".equals(iconType)) {
                    g2.drawRoundRect(4, 8, 22, 15, 2, 2);
                    g2.drawLine(4, 8, 15, 17);
                    g2.drawLine(15, 17, 26, 8);
                } else {
                    g2.drawOval(5, 5, 20, 20);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("f", 15 - fm.stringWidth("f") / 2, 19);
                }
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(32, 32));

        // Contact label — FONT_BODY (Plain 12) in accent color, matching item labels
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(AppTheme.ACCENT);

        row.add(icon, BorderLayout.WEST);
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    // ── changelog ────────────────────────────────────────────────────────────

    private static CardPanel buildChangelogCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel versionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        versionRow.setOpaque(false);
        versionRow.add(buildPillBadge("v1.0", AppTheme.BG_BADGE_BLUE, AppTheme.ACCENT));

        // Date label — FONT_SMALL (Plain 11), muted metadata
        JLabel date = new JLabel("June 2026  \u00B7  Initial release");
        date.setFont(FONT_SMALL);
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
            bullets.add(Box.createVerticalStrut(5));
        }

        card.add(versionRow, BorderLayout.NORTH);
        card.add(bullets, BorderLayout.CENTER);
        return card;
    }

    /**
     * Each bullet row — dot uses FONT_BOLD_SM (Bold 11) for visual weight,
     * label uses FONT_BODY (Plain 12) matching body copy in OrderingPanel.
     */
    private static JPanel buildBullet(String text) {
        JPanel container = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(
                        AppTheme.BG_BADGE_BLUE.getRed(),
                        AppTheme.BG_BADGE_BLUE.getGreen(),
                        AppTheme.BG_BADGE_BLUE.getBlue(), 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(7, 12, 7, 12));
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel dot = new JLabel("\u2022");
        dot.setFont(FONT_BOLD_SM);
        dot.setForeground(AppTheme.ACCENT);

        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(AppTheme.FG_PRIMARY);

        container.add(dot, BorderLayout.WEST);
        container.add(lbl, BorderLayout.CENTER);
        return container;
    }

    // ── sys info ─────────────────────────────────────────────────────────────

    private JPanel buildSysInfoRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        JButton btn = styledButton("View System Info", AppTheme.ACCENT);
        btn.addActionListener(e -> showSysInfoModal(SwingUtilities.getWindowAncestor(this)));
        row.add(btn);
        return row;
    }

    /**
     * Modernized System Info modal — styled undecorated dialog that matches
     * the rest of the About panel.
     */
    private static void showSysInfoModal(Window owner) {
        JDialog dialog = new JDialog(owner, "System Info", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        // ── root card ──
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.setColor(AppTheme.BG_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(28, 28, 24, 28));

        // ── title row ──
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0, 0, 18, 0));

        // Modal title — FONT_TITLE (Bold 16), same as OrderingPanel dialog titles
        JLabel title = new JLabel("System Information");
        title.setFont(FONT_TITLE);
        title.setForeground(AppTheme.FG_PRIMARY);

        // Modal subtitle — FONT_SMALL (Plain 11)
        JLabel subtitle = new JLabel("Runtime environment details");
        subtitle.setFont(FONT_SMALL);
        subtitle.setForeground(AppTheme.FG_MUTED);

        JPanel titleText = new JPanel();
        titleText.setLayout(new BoxLayout(titleText, BoxLayout.Y_AXIS));
        titleText.setOpaque(false);
        titleText.add(title);
        titleText.add(Box.createVerticalStrut(2));
        titleText.add(subtitle);

        titleRow.add(titleText, BorderLayout.CENTER);

        // ── info rows ──
        String[][] sysData = {
                { "Operating System", System.getProperty("os.name") + " " + System.getProperty("os.version") },
                { "Architecture", System.getProperty("os.arch") },
                { "Java Version", System.getProperty("java.version") },
                { "Java Vendor", System.getProperty("java.vendor") },
                { "JVM Name", System.getProperty("java.vm.name") },
                { "User", System.getProperty("user.name") },
                { "Home Directory", System.getProperty("user.home") },
        };

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);

        for (String[] entry : sysData) {
            rows.add(buildSysInfoRow(entry[0], entry[1]));
            rows.add(Box.createVerticalStrut(6));
        }

        // ── divider ──
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(AppTheme.FG_MUTED.getRed(),
                        AppTheme.FG_MUTED.getGreen(), AppTheme.FG_MUTED.getBlue(), 40));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        div.setOpaque(false);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(100, 1));

        // ── close button ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));

        JButton close = styledButton("Close", AppTheme.ACCENT);
        close.addActionListener(e -> dialog.dispose());
        btnRow.add(close);

        // ── assemble ──
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(rows);
        body.add(Box.createVerticalStrut(14));
        body.add(div);

        root.add(titleRow, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Single info line — key uses FONT_SMALL (Plain 11), value uses inline HTML
     * rendered at the same effective size to avoid the old bold override.
     */
    private static JPanel buildSysInfoRow(String label, String value) {
        JPanel container = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(
                        AppTheme.BG_BADGE_BLUE.getRed(),
                        AppTheme.BG_BADGE_BLUE.getGreen(),
                        AppTheme.BG_BADGE_BLUE.getBlue(), 70));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(9, 14, 9, 14));

        // Key — FONT_SMALL (Plain 11) muted
        JLabel keyLbl = new JLabel(label);
        keyLbl.setFont(FONT_SMALL);
        keyLbl.setForeground(AppTheme.FG_MUTED);
        keyLbl.setPreferredSize(new Dimension(140, 20));

        // Value — FONT_BODY (Plain 12) via HTML, keeps weight consistent with body copy
        String safe = (value != null ? value : "\u2014")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String fgHex = String.format("#%02x%02x%02x",
                AppTheme.FG_PRIMARY.getRed(),
                AppTheme.FG_PRIMARY.getGreen(),
                AppTheme.FG_PRIMARY.getBlue());
        JLabel valLbl = new JLabel(
                "<html><body style='font-family:Segoe UI;font-size:10pt;"
                        + "font-weight:normal;color:" + fgHex + ";'>"
                        + safe
                        + "</body></html>");
        valLbl.setFont(FONT_BODY);
        valLbl.setForeground(AppTheme.FG_PRIMARY);

        container.add(keyLbl, BorderLayout.WEST);
        container.add(valLbl, BorderLayout.CENTER);
        return container;
    }

    // ── shared button factory ────────────────────────────────────────────────

    /**
     * Styled action button — FONT_BODY (Plain 12) matching the Close/Cancel
     * buttons used across OrderingPanel dialogs.
     */
    private static JButton styledButton(String label, Color accent) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(9, 22, 9, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── CircularAvatarPanel ───────────────────────────────────────────────────

    /**
     * Circular avatar that loads images via four strategies in order:
     *
     * 1. getResourceAsStream(path) — classpath resource (JAR / IDE)
     * 2. getClass().getResource(path) — URL-based classpath lookup
     * 3. ClassLoader.getSystemResource(path) — system classloader fallback
     * 4. File-system probes — relative & absolute disk paths
     *
     * Falls back to an initials placeholder when all strategies fail.
     */
    static class CircularAvatarPanel extends JPanel {
        private final int size;
        private BufferedImage image;
        private final String imagePath;

        CircularAvatarPanel(String imagePath, int size) {
            this.size = size;
            this.imagePath = imagePath;
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            loadImage(imagePath);
        }

        private void loadImage(String path) {
            if (path == null || path.isEmpty())
                return;

            // ── Strategy 1: getResourceAsStream (works in JAR and IDE) ──
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    try {
                        image = ImageIO.read(is);
                    } finally {
                        is.close();
                    }
                    if (image != null)
                        return;
                }
            } catch (Exception ignored) {
            }

            // ── Strategy 2: getResource URL (some loaders prefer this) ──
            try {
                URL url = getClass().getResource(path);
                if (url != null) {
                    image = ImageIO.read(url);
                    if (image != null)
                        return;
                }
            } catch (Exception ignored) {
            }

            // ── Strategy 3: system classloader (useful when running from IDE) ──
            try {
                String relative = path.startsWith("/") ? path.substring(1) : path;
                URL url = ClassLoader.getSystemResource(relative);
                if (url != null) {
                    image = ImageIO.read(url);
                    if (image != null)
                        return;
                }
            } catch (Exception ignored) {
            }

            // ── Strategy 4: file-system probes ──
            try {
                String stripped = path.startsWith("/") ? path.substring(1) : path;
                File f = new File(stripped);
                if (f.exists() && f.isFile()) {
                    image = ImageIO.read(f);
                    if (image != null)
                        return;
                }
            } catch (Exception ignored) {
            }

            try {
                File f = new File("." + path);
                if (f.exists() && f.isFile()) {
                    image = ImageIO.read(f);
                    if (image != null)
                        return;
                }
            } catch (Exception ignored) {
            }

            try {
                File f = new File(path);
                if (f.exists() && f.isFile()) {
                    image = ImageIO.read(f);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Shape clip = new Ellipse2D.Float(0, 0, size, size);
            g2.setClip(clip);

            if (image != null) {
                int iw = image.getWidth(), ih = image.getHeight();
                double scale = Math.max((double) size / iw, (double) size / ih);
                int sw = (int) (iw * scale), sh = (int) (ih * scale);
                int ox = (size - sw) / 2, oy = (size - sh) / 2;
                g2.drawImage(image, ox, oy, sw, sh, null);
            } else {
                // Initials placeholder — font sized proportionally to the avatar circle
                g2.setColor(AppTheme.BG_BADGE_BLUE);
                g2.fillOval(0, 0, size, size);
                g2.setColor(AppTheme.ACCENT);
                String initials = deriveInitials(imagePath);
                int fontSize = Math.max(10, size / 3);
                g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (size - fm.stringWidth(initials)) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
            }

            g2.setClip(null);
            g2.setColor(new Color(AppTheme.ACCENT.getRed(), AppTheme.ACCENT.getGreen(),
                    AppTheme.ACCENT.getBlue(), 60));
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(1, 1, size - 2, size - 2);
            g2.dispose();
        }

        private static String deriveInitials(String path) {
            if (path == null || path.isEmpty())
                return "?";
            String file = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            file = file.replaceAll("\\.[^.]+$", "");
            return file.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(file.charAt(0)));
        }
    }
}