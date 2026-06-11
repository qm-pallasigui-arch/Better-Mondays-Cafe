package ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import loginregister.UserDataManager;

/**
 * UserSettingsDialog – clean, modern profile/settings modal.
 * Color palette matches the app's white/blue Search + table UI.
 */
public class UserSettingsDialog extends JDialog {

    // ── Palette ──────────────────────────────────────────────────
    private static final Color BG_PAGE = new Color(0xF4, 0xF7, 0xFB);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BG_INPUT = new Color(0xF8, 0xFA, 0xFD);
    private static final Color BORDER_COLOR = new Color(0xDD, 0xE3, 0xED);
    private static final Color ACCENT = new Color(0x2D, 0x7D, 0xD2);
    private static final Color ACCENT_HOVER = new Color(0x1A, 0x63, 0xB5);
    private static final Color DANGER = new Color(0xE5, 0x39, 0x35);
    private static final Color DANGER_HOVER = new Color(0xC6, 0x28, 0x28);
    private static final Color TEXT_DARK = new Color(0x1A, 0x23, 0x40);
    private static final Color TEXT_MUTED = new Color(0x6B, 0x7A, 0x99);
    private static final Color AVATAR_TOP = new Color(0x2D, 0x7D, 0xD2);
    private static final Color AVATAR_BOT = new Color(0x1A, 0x4E, 0x8C);
    private static final Color SUCCESS_FG = new Color(0x2E, 0x7D, 0x32);

    private final String[] usernameHolder;
    private final UserDataManager.Role role;

    public interface UsernameChangeCallback {
        void onUsernameChanged(String newUsername);
    }

    private final UsernameChangeCallback callback;

    // ── Constructor ──────────────────────────────────────────────
    public UserSettingsDialog(Window owner, String username,
            UserDataManager.Role role,
            UsernameChangeCallback callback) {
        super(owner, "User Settings", ModalityType.APPLICATION_MODAL);
        this.usernameHolder = new String[] { username };
        this.role = role;
        this.callback = callback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_PAGE);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        setContentPane(root);

        // Fixed dialog size – enough to show all three action cards
        setSize(420, 500);
        setMinimumSize(new Dimension(400, 480));
        setLocationRelativeTo(owner);
    }

    // ═══════════════════════════════════════════════════════════════
    // HEADER – avatar + name + role badge
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_COLOR);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setBackground(BG_CARD);
        header.setLayout(new BorderLayout(0, 0));
        header.setBorder(new EmptyBorder(20, 24, 18, 24));

        // Left: avatar circle
        JPanel avatar = buildAvatar(52);
        header.add(avatar, BorderLayout.WEST);

        // Centre: name + role badge — vertically centred next to avatar
        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);

        JLabel nameLabel = new JLabel(usernameHolder[0]);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLabel.setForeground(TEXT_DARK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameStack.add(nameLabel);
        nameStack.add(Box.createVerticalStrut(6));
        nameStack.add(buildRoleBadge());

        // GridBagLayout with no constraints centres the child both axes
        JPanel centreWrap = new JPanel(new GridBagLayout());
        centreWrap.setOpaque(false);
        centreWrap.setBorder(new EmptyBorder(0, 16, 0, 0));
        centreWrap.add(nameStack);

        header.add(centreWrap, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildAvatar(int size) {
        String initial = usernameHolder[0].isEmpty() ? "?"
                : String.valueOf(Character.toUpperCase(usernameHolder[0].charAt(0)));
        JPanel av = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, AVATAR_TOP, 0, getHeight(), AVATAR_BOT);
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial,
                        (getWidth() - fm.stringWidth(initial)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        av.setPreferredSize(new Dimension(size, size));
        av.setMinimumSize(new Dimension(size, size));
        av.setMaximumSize(new Dimension(size, size));
        av.setOpaque(false);
        return av;
    }

    private JPanel buildRoleBadge() {
        boolean isAdmin = role == UserDataManager.Role.ADMIN;
        Color badgeBg = isAdmin ? new Color(0xE3, 0xF0, 0xFF) : new Color(0xE8, 0xF5, 0xE9);
        Color badgeFg = isAdmin ? ACCENT : SUCCESS_FG;
        Color badgeBd = isAdmin ? new Color(0xBB, 0xD6, 0xF5) : new Color(0xA5, 0xD6, 0xA7);
        String roleText = isAdmin ? "Administrator" : "Staff";

        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = getHeight();
                g2.setColor(badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(badgeBd);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
            }
        };
        badge.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(4, 10, 4, 12));

        // Icon
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeFg);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (isAdmin) {
                    int[] xp = { 5, 7, 9, 9, 7, 5 };
                    int[] yp = { 1, 0, 1, 5, 8, 5 };
                    g2.drawPolygon(xp, yp, 6);
                } else {
                    g2.drawOval(2, 0, 6, 6);
                    g2.drawArc(0, 7, 10, 4, 0, 180);
                }
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(12, 11));
        icon.setOpaque(false);

        JLabel lbl = new JLabel(roleText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(badgeFg);

        badge.add(icon);
        badge.add(lbl);

        // Wrap in a FlowLayout so it doesn't stretch full width
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(badge);
        return wrap;
    }

    // ═══════════════════════════════════════════════════════════════
    // BODY – section labels + action cards
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PAGE);
        body.setBorder(new EmptyBorder(20, 20, 24, 20));
        // DO NOT set a fixed preferred size here – let BoxLayout compute it

        // ── Account section ──────────────────────────────────────
        body.add(makeSectionLabel("ACCOUNT"));
        body.add(Box.createVerticalStrut(10));

        body.add(makeActionCard(
                IconType.USER, ACCENT, ACCENT_HOVER,
                "Edit Username", "Change your display name",
                e -> showEditUsernameDialog()));
        body.add(Box.createVerticalStrut(8));

        body.add(makeActionCard(
                IconType.LOCK, ACCENT, ACCENT_HOVER,
                "Change Password", "Update your account password",
                e -> showChangePasswordDialog()));

        // ── Session section ──────────────────────────────────────
        body.add(Box.createVerticalStrut(22));
        body.add(makeSectionLabel("SESSION"));
        body.add(Box.createVerticalStrut(10));

        body.add(makeActionCard(
                IconType.LOGOUT, DANGER, DANGER_HOVER,
                "Sign Out", "End your current session",
                e -> {
                    dispose();
                    firePropertyChange("logout", false, true);
                }));

        return body;
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    // ── Action card ──────────────────────────────────────────────
    private enum IconType {
        USER, LOCK, LOGOUT
    }

    private JPanel makeActionCard(IconType iconType, Color accent, Color hover,
            String title, String subtitle,
            ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(13, 14, 13, 14));
        card.setAlignmentX(LEFT_ALIGNMENT);
        // Fixed card height, full width
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        card.setPreferredSize(new Dimension(380, 68));

        // Icon bubble
        card.add(buildIconBubble(iconType, accent), BorderLayout.WEST);

        // Text
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(TEXT_DARK);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        s.setForeground(TEXT_MUTED);

        text.add(t);
        text.add(Box.createVerticalStrut(2));
        text.add(s);
        card.add(text, BorderLayout.CENTER);

        // Chevron button
        boolean[] hov = { false };
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov[0] ? hover : accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 3, cy - 4, cx + 3, cy);
                g2.drawLine(cx - 3, cy + 4, cx + 3, cy);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hov[0] = true;
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hov[0] = false;
                btn.repaint();
            }
        });
        btn.addActionListener(action);
        card.add(btn, BorderLayout.EAST);

        return card;
    }

    private JPanel buildIconBubble(IconType type, Color accent) {
        Color bg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20);
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                switch (type) {
                    case USER -> {
                        g2.drawOval(cx - 5, cy - 8, 10, 10);
                        g2.drawArc(cx - 8, cy + 3, 16, 8, 0, 180);
                    }
                    case LOCK -> {
                        g2.drawArc(cx - 5, cy - 10, 10, 10, 0, 180);
                        g2.drawRoundRect(cx - 6, cy - 1, 12, 10, 3, 3);
                        g2.fillOval(cx - 1, cy + 2, 3, 3);
                        g2.drawLine(cx, cy + 5, cx, cy + 7);
                    }
                    case LOGOUT -> {
                        g2.drawPolyline(
                                new int[] { cx - 1, cx - 6, cx - 6, cx - 1 },
                                new int[] { cy - 8, cy - 8, cy + 8, cy + 8 }, 4);
                        g2.drawLine(cx + 1, cy, cx + 7, cy);
                        g2.drawLine(cx + 4, cy - 3, cx + 7, cy);
                        g2.drawLine(cx + 4, cy + 3, cx + 7, cy);
                    }
                }
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(40, 40));
        p.setMinimumSize(new Dimension(40, 40));
        p.setMaximumSize(new Dimension(40, 40));
        p.setOpaque(false);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════
    // EDIT USERNAME sub-dialog
    // ═══════════════════════════════════════════════════════════════
    private void showEditUsernameDialog() {
        JPanel form = buildFormPanel(
                new String[] { "New Username", "Current Password" },
                new boolean[] { false, true });
        JTextField uField = findTextField(form, 0);
        JPasswordField pField = (JPasswordField) findTextField(form, 1);
        if (uField != null)
            uField.setText(usernameHolder[0]);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Edit Username", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String newUser = uField != null ? uField.getText().trim() : "";
        String curPwd = pField != null ? new String(pField.getPassword()).trim() : "";

        if (newUser.isEmpty() || curPwd.isEmpty()) {
            showError("Both fields are required.");
            return;
        }
        if (newUser.equals(usernameHolder[0])) {
            showError("New username must differ from the current one.");
            return;
        }

        if (UserDataManager.updateUsername(usernameHolder[0], newUser, curPwd)) {
            String old = usernameHolder[0];
            usernameHolder[0] = newUser;
            if (callback != null)
                callback.onUsernameChanged(newUser);
            showSuccess("Username changed from \"" + old + "\" to \"" + newUser + "\".");
            rebuildHeader();
        } else {
            showError("Could not update username. Check your password or if the name is already taken.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CHANGE PASSWORD sub-dialog
    // ═══════════════════════════════════════════════════════════════
    private void showChangePasswordDialog() {
        JPanel form = buildFormPanel(
                new String[] { "Current Password", "New Password", "Confirm New Password" },
                new boolean[] { true, true, true });

        int result = JOptionPane.showConfirmDialog(this, form,
                "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String cur = getPasswordAt(form, 0);
        String newPwd = getPasswordAt(form, 1);
        String confirm = getPasswordAt(form, 2);

        if (cur.isEmpty() || newPwd.isEmpty()) {
            showError("All password fields are required.");
            return;
        }
        if (!newPwd.equals(confirm)) {
            showError("New passwords do not match.");
            return;
        }
        if (!isStrongPassword(newPwd)) {
            showError("Password must be ≥ 8 characters and include a number and a special character.");
            return;
        }

        if (UserDataManager.updatePassword(usernameHolder[0], cur, newPwd)) {
            showSuccess("Password updated successfully.");
        } else {
            showError("Could not update password. Please check your current password.");
        }
    }

    // ── Shared form builder ──────────────────────────────────────
    private JPanel buildFormPanel(String[] labels, boolean[] isPassword) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PAGE);
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (int i = 0; i < labels.length; i++) {
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setForeground(TEXT_MUTED);
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(lbl);
            panel.add(Box.createVerticalStrut(4));

            JTextField field = isPassword[i] ? new JPasswordField(22) : new JTextField(22);
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            field.setBackground(BG_INPUT);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(6, 10, 6, 10)));
            field.setAlignmentX(LEFT_ALIGNMENT);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            panel.add(field);
            if (i < labels.length - 1)
                panel.add(Box.createVerticalStrut(10));
        }
        return panel;
    }

    private JTextField findTextField(JPanel panel, int index) {
        int found = 0;
        for (Component c : panel.getComponents()) {
            if (c instanceof JTextField) {
                if (found == index)
                    return (JTextField) c;
                found++;
            }
        }
        return null;
    }

    private String getPasswordAt(JPanel panel, int index) {
        JTextField f = findTextField(panel, index);
        if (f instanceof JPasswordField pf)
            return new String(pf.getPassword()).trim();
        return f != null ? f.getText().trim() : "";
    }

    private boolean isStrongPassword(String pwd) {
        return pwd.length() >= 8
                && pwd.matches(".*\\d.*")
                && pwd.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void rebuildHeader() {
        JPanel root = (JPanel) getContentPane();
        root.remove(((BorderLayout) root.getLayout()).getLayoutComponent(BorderLayout.NORTH));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.revalidate();
        root.repaint();
    }

    // ── Static convenience factory ───────────────────────────────
    public static UserSettingsDialog show(Window owner, String username,
            UserDataManager.Role role,
            UsernameChangeCallback callback) {
        UserSettingsDialog d = new UserSettingsDialog(owner, username, role, callback);
        d.setVisible(true);
        return d;
    }
}