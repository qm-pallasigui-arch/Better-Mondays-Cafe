package ui;

import loginregister.UserDataManager.Role;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.JPasswordField;
import javax.swing.JDialog;
import javax.swing.JLabel;
import util.FontHelper;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import loginregister.UserDataManager;

public class SidebarPanel extends JPanel {

    public interface NavigationListener {
        void onNavigate(String pageName);
    }

    public interface LogoutListener {
        void onLogout();
    }

    public interface ProfileUpdateListener {
        void onUsernameChanged(String newUsername);
    }

    private static final int EXPANDED_WIDTH = 228;
    private static final int COLLAPSED_WIDTH = 60;
    private static final int NAV_HEIGHT = 40;
    private static final Font SIDEBAR_BRAND_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font SIDEBAR_NAV_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font SIDEBAR_PROFILE_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Dimension PROFILE_AVATAR_SIZE = new Dimension(30, 30);

    private final NavigationListener listener;
    private final LogoutListener logoutListener;
    private final ProfileUpdateListener profileUpdateListener;
    private final boolean isAdmin;
    private final String username;
    private boolean collapsed = false;

    private final List<NavItem> navItems = new ArrayList<>();
    private JLabel brandingText;
    private JButton collapseBtn;
    private JButton overflowBtn;
    private JButton logoutBtn;
    private final JPanel navPanel;
    private final JPanel headerPanel;
    private JPanel bottomHolder;
    private JLabel userNameLabel;

    public SidebarPanel(String username, Role role, NavigationListener listener) {
        this(username, role, listener, null, null);
    }

    public SidebarPanel(String username, Role role, NavigationListener listener, LogoutListener logoutListener) {
        this(username, role, listener, logoutListener, null);
    }

    public SidebarPanel(String username, Role role, NavigationListener listener, LogoutListener logoutListener, ProfileUpdateListener profileUpdateListener) {
        this.username = username;
        this.isAdmin = role == Role.ADMIN;
        this.listener = listener;
        this.logoutListener = logoutListener;
        this.profileUpdateListener = profileUpdateListener;

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_PRIMARY);

        // create logout button early so toggleCollapse() can reference it
        logoutBtn = createLogoutButton();

        headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);

        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(AppTheme.BG_PRIMARY);
        buildNavContent();

        JScrollPane scroll = new JScrollPane(navPanel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(AppTheme.BG_PRIMARY);
        add(scroll, BorderLayout.CENTER);

        add(createUserProfile(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));
        refreshChromeStyles();
    }

    public void setActivePage(String page) {
        for (NavItem ni : navItems) {
            ni.setActive(ni.pageName.equals(page));
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void refreshChromeStyles() {
        applyTransparentButtonChrome(collapseBtn);
        applyTransparentButtonChrome(overflowBtn);
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
        collapseBtn.setIcon(createCollapseIcon(collapsed));
        if (logoutBtn != null) {
            logoutBtn.setText(collapsed ? "\u21AA" : "Logout");
            logoutBtn.setPreferredSize(collapsed ? new Dimension(42, 30) : new Dimension(84, 28));
            logoutBtn.setToolTipText(collapsed ? "Logout" : null);
            logoutBtn.setMargin(collapsed ? new java.awt.Insets(4, 4, 4, 4) : new java.awt.Insets(6, 12, 6, 12));
            logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, collapsed ? 12 : 11));
            if (bottomHolder != null && bottomHolder.getLayout() instanceof java.awt.FlowLayout) {
                ((java.awt.FlowLayout) bottomHolder.getLayout()).setAlignment(collapsed ? java.awt.FlowLayout.CENTER : java.awt.FlowLayout.RIGHT);
            }
            if (bottomHolder != null) {
                bottomHolder.setBorder(new EmptyBorder(collapsed ? 4 : 6, collapsed ? 4 : 8, collapsed ? 6 : 8, collapsed ? 4 : 8));
            }
        }
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

        JLabel logo = new JLabel();
        logo.setPreferredSize(new Dimension(22, 22));
        logo.setMinimumSize(new Dimension(22, 22));
        logo.setMaximumSize(new Dimension(22, 22));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setVerticalAlignment(SwingConstants.CENTER);
        ImageIcon logoIcon = loadSidebarLogoIcon(22, 22);
        if (logoIcon != null) {
            logo.setIcon(logoIcon);
        } else {
            logo.setForeground(AppTheme.FG_PRIMARY);
            logo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            logo.setText("\u2615");
            FontHelper.ensureGlyphs(logo, '\u2615');
        }

        brandingText = new JLabel("Better Mondays");
        brandingText.setFont(SIDEBAR_BRAND_FONT);
        brandingText.setForeground(AppTheme.FG_PRIMARY);

        brand.add(logo);
        brand.add(brandingText);

        collapseBtn = new JButton();
        collapseBtn.setIcon(createCollapseIcon(false));
        collapseBtn.putClientProperty("appTheme.variant", "transparent");
        collapseBtn.setPreferredSize(new Dimension(22, 22));
        collapseBtn.setForeground(AppTheme.FG_MUTED);
        collapseBtn.setBackground(new Color(0, 0, 0, 0));
        collapseBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        collapseBtn.setFocusPainted(false);
        collapseBtn.setContentAreaFilled(false);
        collapseBtn.setOpaque(false);
        collapseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapseBtn.addActionListener(e -> toggleCollapse());

        JPanel controls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        controls.add(collapseBtn);

        p.add(brand, BorderLayout.CENTER);
        p.add(controls, BorderLayout.EAST);
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
        addNavItem("Inventory Guide", "Inventory Guide", this::paintBookIcon);
        addNavItem("About", "About", this::paintInfoIcon);
        addNavItem("Help", "Help", this::paintHelpIcon);

        navPanel.add(Box.createVerticalGlue());

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(AppTheme.BORDER);
        sep.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        navPanel.add(sep);
        navPanel.add(Box.createVerticalStrut(4));
    }

    private void addNavItem(String pageName, String label, IconPainter painter) {
        NavItem item = new NavItem(pageName, label, painter);
        navItems.add(item);
        navPanel.add(item);
    }

    private JPanel createUserProfile() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(AppTheme.BG_SURFACE);
        p.setBorder(new EmptyBorder(8, 10, 8, 8));
        p.setPreferredSize(new Dimension(EXPANDED_WIDTH, 54));

        JLabel userIcon = new JLabel(String.valueOf(Character.toUpperCase(username.charAt(0)))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.ACCENT);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        userIcon.setPreferredSize(PROFILE_AVATAR_SIZE);
        userIcon.setMinimumSize(PROFILE_AVATAR_SIZE);
        userIcon.setMaximumSize(PROFILE_AVATAR_SIZE);
        userIcon.setHorizontalAlignment(SwingConstants.CENTER);
        userIcon.setOpaque(false);

        JPanel userIconWrap = new JPanel(new java.awt.GridBagLayout());
        userIconWrap.setOpaque(false);
        userIconWrap.setPreferredSize(new Dimension(34, 34));
        userIconWrap.setMinimumSize(new Dimension(34, 34));
        userIconWrap.setMaximumSize(new Dimension(34, 34));
        userIconWrap.add(userIcon);

        userNameLabel = new JLabel(username);
        userNameLabel.setFont(SIDEBAR_PROFILE_FONT);
        userNameLabel.setForeground(AppTheme.FG_PRIMARY);

        overflowBtn = createOverflowPreviewButton();
        overflowBtn.addActionListener(e -> showProfileSettingsDialog());

        p.add(userIconWrap, BorderLayout.WEST);
        p.add(userNameLabel, BorderLayout.CENTER);
        p.add(overflowBtn, BorderLayout.EAST);

        // wrapper to position profile at top and logout at bottom
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AppTheme.BG_PRIMARY);
        wrapper.add(p, BorderLayout.NORTH);

        bottomHolder = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 6));
        bottomHolder.setOpaque(false);
        bottomHolder.setBorder(new EmptyBorder(6, 8, 8, 8));
        bottomHolder.add(logoutBtn);
        wrapper.add(bottomHolder, BorderLayout.SOUTH);

        wrapper.setPreferredSize(new Dimension(EXPANDED_WIDTH, 116));
        return wrapper;
    }

    private JButton createLogoutButton() {
        JButton button = new JButton("Logout");
        button.putClientProperty("appTheme.variant", "danger");
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setBackground(AppTheme.DANGER);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                new ui.RoundedLineBorder(AppTheme.DANGER.darker(), 1, AppTheme.BORDER_RADIUS),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(84, 28));
        button.addActionListener(e -> {
            if (logoutListener != null) {
                logoutListener.onLogout();
            }
        });
        return button;
    }

    private JButton createOverflowPreviewButton() {
        JButton button = new JButton();
        button.putClientProperty("appTheme.variant", "transparent");
        button.setIcon(createOverflowIcon());
        button.setPreferredSize(new Dimension(22, 22));
        button.setForeground(AppTheme.FG_MUTED);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void applyTransparentButtonChrome(JButton button) {
        if (button == null) {
            return;
        }
        button.setBackground(new Color(0, 0, 0, 0));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setUI(new BasicButtonUI());
    }

    private void showProfileSettingsDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "User Settings", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new java.awt.BorderLayout(0, 14));
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        content.setBackground(AppTheme.BG_SURFACE);

        JPanel summary = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        summary.setOpaque(false);
        JLabel title = new JLabel("User Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel current = new JLabel("Current username: " + userNameLabel.getText());
        current.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        current.setForeground(AppTheme.FG_MUTED);
        summary.add(title);
        summary.add(current);

        JPanel actions = new JPanel(new java.awt.GridLayout(0, 1, 0, 10));
        actions.setOpaque(false);

        JButton checkUsername = new JButton("Check Username");
        checkUsername.addActionListener(ae -> javax.swing.JOptionPane.showMessageDialog(
                dialog,
                "Current username: " + userNameLabel.getText(),
                "Username",
                javax.swing.JOptionPane.INFORMATION_MESSAGE));

        JButton editUsername = new JButton("Edit Username");
        editUsername.addActionListener(ae -> showEditUsernameDialog());

        JButton changePassword = new JButton("Change Password");
        changePassword.addActionListener(ae -> showChangePasswordDialog());

        JButton close = new JButton("Close");
        close.addActionListener(ae -> dialog.dispose());

        styleSettingsButton(checkUsername);
        styleSettingsButton(editUsername);
        styleSettingsButton(changePassword);
        styleSettingsButton(close);

        actions.add(checkUsername);
        actions.add(editUsername);
        actions.add(changePassword);
        actions.add(close);

        content.add(summary, BorderLayout.NORTH);
        content.add(actions, BorderLayout.CENTER);
        AppTheme.applyToComponent(content);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    private void styleSettingsButton(JButton button) {
        if (button == null) {
            return;
        }
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(220, 36));
        button.setMinimumSize(new Dimension(220, 36));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void showEditUsernameDialog() {
        JTextField usernameField = new JTextField(userNameLabel.getText(), 18);
        JPasswordField currentPasswordField = new JPasswordField(18);

        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("New username"));
        panel.add(usernameField);
        panel.add(new JLabel("Current password"));
        panel.add(currentPasswordField);

        int result = javax.swing.JOptionPane.showConfirmDialog(
                this,
                panel,
                "Edit Username",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (result != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        String newUsername = usernameField.getText().trim();
        String currentPassword = new String(currentPasswordField.getPassword()).trim();
        if (newUsername.isEmpty() || currentPassword.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Username and password are required.", "Edit Username", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (newUsername.equals(userNameLabel.getText())) {
            javax.swing.JOptionPane.showMessageDialog(this, "New username must be different.", "Edit Username", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (UserDataManager.updateUsername(userNameLabel.getText(), newUsername, currentPassword)) {
            userNameLabel.setText(newUsername);
            javax.swing.JOptionPane.showMessageDialog(this, "Username updated.", "Edit Username", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (profileUpdateListener != null) {
                profileUpdateListener.onUsernameChanged(newUsername);
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to update username. Check your password or whether the username is already in use.", "Edit Username", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showChangePasswordDialog() {
        JPasswordField currentPasswordField = new JPasswordField(18);
        JPasswordField newPasswordField = new JPasswordField(18);
        JPasswordField confirmPasswordField = new JPasswordField(18);

        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Current password"));
        panel.add(currentPasswordField);
        panel.add(new JLabel("New password"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Confirm new password"));
        panel.add(confirmPasswordField);

        int result = javax.swing.JOptionPane.showConfirmDialog(
                this,
                panel,
                "Change Password",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (result != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        String currentPassword = new String(currentPasswordField.getPassword()).trim();
        String newPassword = new String(newPasswordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "All password fields are required.", "Change Password", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this, "New passwords do not match.", "Change Password", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isStrongPassword(newPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Change Password",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (UserDataManager.updatePassword(userNameLabel.getText(), currentPassword, newPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this, "Password updated.", "Change Password", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to update password. Check your current password.", "Change Password", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 8 && password.matches(".*\\d.*") && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    public void setUsername(String username) {
        if (userNameLabel != null) {
            userNameLabel.setText(username);
        }
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

    private Icon createCollapseIcon(boolean collapsedState) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }

            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.FG_MUTED);
                g2.setStroke(new java.awt.BasicStroke(1.8f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                if (collapsedState) {
                    g2.drawLine(x + 4, y + 2, x + 8, y + 6);
                    g2.drawLine(x + 4, y + 10, x + 8, y + 6);
                } else {
                    g2.drawLine(x + 8, y + 2, x + 4, y + 6);
                    g2.drawLine(x + 8, y + 10, x + 4, y + 6);
                }
                g2.dispose();
            }
        };
    }

    private Icon createOverflowIcon() {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }

            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.FG_MUTED);
                g2.fillOval(x + 3, y + 1, 4, 4);
                g2.fillOval(x + 3, y + 5, 4, 4);
                g2.fillOval(x + 3, y + 9, 4, 4);
                g2.dispose();
            }
        };
    }

    private ImageIcon loadSidebarLogoIcon(int width, int height) {
        java.net.URL location = getClass().getResource("/images/logo.png");
        if (location == null) {
            return null;
        }
        ImageIcon original = new ImageIcon(location);
        Image scaled = original.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
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
                bottomHolder.setBorder(new EmptyBorder(4, 4, 6, 4));
                g2.setColor(iconColor);
                g2.setFont(SIDEBAR_NAV_FONT);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, 44, (h - fm.getHeight()) / 2 + fm.getAscent());
            }

            g2.dispose();
        }
    }
}
