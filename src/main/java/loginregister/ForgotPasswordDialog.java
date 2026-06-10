package loginregister;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.stream.Collectors;

/**
 * Modern Forgot Password dialog for Better Mondays Coffee Cafe.
 *
 * Replaces the old JOptionPane-based showForgotPasswordDialog() with a
 * fully custom JDialog that matches the login screen's visual language:
 *   - Icon header + warm-brown accent palette
 *   - Rounded input field with focus ring
 *   - Two-step progress indicator (dots)
 *   - Inline validation error banner
 *   - Animated success state (no second dialog)
 *
 * Usage (inside Login.java — replaces the old method):
 *
 *   private void showForgotPasswordDialog() {
 *       new ForgotPasswordDialog(this).setVisible(true);
 *   }
 */
public class ForgotPasswordDialog extends JDialog {

    // ── Palette (matches Login.java) ──────────────────────────────────────────
    private static final Color BG           = Color.WHITE;
    private static final Color BRAND        = new Color(0x1A, 0x0F, 0x05);
    private static final Color ACCENT       = new Color(0x8B, 0x5E, 0x3C);
    private static final Color SUBTLE       = new Color(0x6B, 0x5C, 0x4A);
    private static final Color FIELD_BG     = new Color(0xFF, 0xFD, 0xF9);
    private static final Color FIELD_BORDER = new Color(0xD1, 0xC4, 0xB0);
    private static final Color FIELD_FOCUS  = new Color(0x8B, 0x5E, 0x3C);
    private static final Color ERR_BG       = new Color(0xFF, 0xF3, 0xE8);
    private static final Color ERR_BORDER   = new Color(0xD4, 0x95, 0x6B);
    private static final Color ERR_TEXT     = new Color(0x6B, 0x3A, 0x1F);
    private static final Color SUCCESS_ICON = new Color(0x2E, 0x7D, 0x32);  // green check

    // ── Step dots ─────────────────────────────────────────────────────────────
    private static final Color DOT_INACTIVE = new Color(0xD1, 0xC4, 0xB0);
    private static final Color DOT_ACTIVE   = ACCENT;
    private static final Color DOT_DONE     = BRAND;

    // ── State ─────────────────────────────────────────────────────────────────
    private JPanel cardPanel;           // CardLayout host
    private CardLayout cards;

    // Form-view references
    private JTextField usernameField;
    private JPanel errorBanner;
    private JLabel errorLabel;

    // ── Entry point ───────────────────────────────────────────────────────────
    public ForgotPasswordDialog(Frame owner) {
        super(owner, "Forgot Password", true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD
    // ─────────────────────────────────────────────────────────────────────────
    private void buildUI() {
        cards     = new CardLayout();
        cardPanel = new JPanel(cards);
        cardPanel.setBackground(BG);
        cardPanel.add(buildFormCard(),    "FORM");
        cardPanel.add(buildSuccessCard(), "SUCCESS");
        setContentPane(cardPanel);
    }

    // ── FORM CARD ─────────────────────────────────────────────────────────────
    private JPanel buildFormCard() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(28, 28, 24, 28));
        root.setPreferredSize(new Dimension(400, 340));

        // ── Header row: icon + title/subtitle ──
        root.add(buildHeaderRow(
                "\uD83D\uDD13",  // 🔓
                "Reset your password",
                "Enter your username and we'll submit a reset request to an admin."));

        root.add(Box.createVerticalStrut(22));

        // ── Step dots ──
        root.add(buildDots(1));
        root.add(Box.createVerticalStrut(18));

        // ── Username field ──
        JLabel lbl = fieldLabel("USERNAME");
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(lbl);
        root.add(Box.createVerticalStrut(6));

        usernameField = new JTextField();
        usernameField.putClientProperty("JTextField.placeholderText", "Enter your username");
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        RoundedField fieldWrapper = new RoundedField(usernameField);
        fieldWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        root.add(fieldWrapper);

        root.add(Box.createVerticalStrut(6));

        JLabel hint = new JLabel("Use the same username you log in with.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(new Color(0xAA, 0x99, 0x88));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(hint);

        // ── Error banner (hidden initially) ──
        root.add(Box.createVerticalStrut(10));
        errorBanner = buildErrorBanner();
        errorBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorBanner.setVisible(false);
        root.add(errorBanner);

        root.add(Box.createVerticalGlue());
        root.add(Box.createVerticalStrut(24));

        // ── Submit button ──
        JButton submit = buildPrimaryButton("Submit reset request");
        submit.setAlignmentX(Component.LEFT_ALIGNMENT);
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        submit.addActionListener(e -> handleSubmit());
        root.add(submit);
        root.add(Box.createVerticalStrut(10));

        // ── Cancel button ──
        JButton cancel = buildGhostButton("Cancel");
        cancel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        cancel.addActionListener(e -> dispose());
        root.add(cancel);

        // Enter key triggers submit
        usernameField.addActionListener(e -> handleSubmit());

        return root;
    }

    // ── SUCCESS CARD ──────────────────────────────────────────────────────────
    private JPanel buildSuccessCard() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(28, 28, 24, 28));
        root.setPreferredSize(new Dimension(400, 340));

        // Step dots (both active)
        JPanel dots = buildDots(2);
        dots.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(dots);
        root.add(Box.createVerticalStrut(20));

        // Big checkmark icon (painted)
        JLabel checkIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                // Circle
                g2.setColor(new Color(0xE8, 0xF5, 0xE9));
                g2.fillOval(cx - 28, cy - 28, 56, 56);
                // Check stroke
                g2.setColor(SUCCESS_ICON);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 12, cy, cx - 3, cy + 10);
                g2.drawLine(cx - 3, cy + 10, cx + 14, cy - 9);
                g2.dispose();
            }
        };
        checkIcon.setPreferredSize(new Dimension(64, 64));
        checkIcon.setMaximumSize(new Dimension(64, 64));
        checkIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(checkIcon);

        root.add(Box.createVerticalStrut(16));

        // Title
        JLabel title = new JLabel("Request submitted");
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setForeground(BRAND);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(title);

        root.add(Box.createVerticalStrut(10));

        // Body text (multi-line HTML)
        JLabel body = new JLabel(
                "<html><div style='text-align:center;width:280px;'>"
                        + "Your password reset request has been sent to an admin.<br><br>"
                        + "Please wait for approval before logging in again."
                        + "</div></html>");
        body.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        body.setForeground(SUBTLE);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(body);

        root.add(Box.createVerticalGlue());
        root.add(Box.createVerticalStrut(24));

        // Back to login button
        JButton back = buildPrimaryButton("← Back to login");
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        back.addActionListener(e -> dispose());
        root.add(back);

        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSubmit() {
        String username = usernameField.getText().trim();

        // ── Blank check ──
        if (username.isEmpty()) {
            showError("Please enter your username.");
            return;
        }

        // ── Existence check ──
        boolean exists = UserDataManager.listUsers().stream()
                .anyMatch(a -> a.getUsername().equalsIgnoreCase(username));
        if (!exists) {
            showError("Username not found. Please check and try again.");
            return;
        }

        // ── Duplicate pending check ──
        if (UserDataManager.hasPendingResetRequest(username)) {
            showError("A reset request for \"" + username + "\" is already pending.");
            return;
        }

        // ── Submit ──
        try {
            UserDataManager.submitResetRequest(username);
            errorBanner.setVisible(false);
            cards.show(cardPanel, "SUCCESS");
        } catch (Exception ex) {
            showError("Unable to submit: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
        usernameField.requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPONENT BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Circular icon + title + subtitle row at the top of the dialog. */
    private JPanel buildHeaderRow(String emoji, String title, String subtitle) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Icon circle
        JLabel icon = new JLabel(emoji, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFF, 0xF3, 0xE8));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        icon.setPreferredSize(new Dimension(48, 48));

        // Text stack
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(BG);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(BRAND);

        JLabel subLbl = new JLabel(
                "<html><div style='width:270px;'>" + subtitle + "</div></html>");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(SUBTLE);

        text.add(titleLbl);
        text.add(Box.createVerticalStrut(3));
        text.add(subLbl);

        row.add(icon, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    /** Two progress dots. pass step=1 for first step active, step=2 for both done. */
    private JPanel buildDots(int step) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setBackground(BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));

        for (int i = 1; i <= 2; i++) {
            final int idx = i;
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    Color c = (idx < step) ? DOT_DONE
                            : (idx == step) ? DOT_ACTIVE
                            : DOT_INACTIVE;
                    g2.setColor(c);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(8, 8));
            dot.setOpaque(false);
            p.add(dot);
        }
        return p;
    }

    /** Inline error banner with warm-amber styling. */
    private JPanel buildErrorBanner() {
        JPanel banner = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ERR_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(ERR_BORDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(9, 12, 9, 12));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Warning icon
        JLabel warnIcon = new JLabel("⚠");
        warnIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        warnIcon.setForeground(ERR_TEXT);

        // Message
        errorLabel = new JLabel("Error.");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorLabel.setForeground(ERR_TEXT);

        banner.add(warnIcon, BorderLayout.WEST);
        banner.add(errorLabel, BorderLayout.CENTER);
        return banner;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(0x8B, 0x72, 0x5A));
        return lbl;
    }

    /** Full-width dark primary button. */
    private JButton buildPrimaryButton(String label) {
        JButton btn = new JButton(label) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? new Color(0x3D, 0x28, 0x14).darker()
                         : hovered               ? new Color(0x3D, 0x28, 0x14)
                         :                         new Color(0x1A, 0x0F, 0x05);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Full-width ghost (outline) button. */
    private JButton buildGhostButton(String label) {
        JButton btn = new JButton(label) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(0xF5, 0xF0, 0xE8) : BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(FIELD_BORDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(SUBTLE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =========================================================================
    // INNER — RoundedField wrapper
    // Wraps a JTextField in a custom-painted rounded border with focus ring.
    // =========================================================================
    private static class RoundedField extends JPanel {
        private boolean focused = false;

        RoundedField(JTextField field) {
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 12, 0, 12));

            // User icon painted on the left
            JLabel icon = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(focused ? ACCENT : new Color(0xAA, 0x99, 0x88));
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    // Head
                    g2.drawOval(cx - 4, cy - 8, 8, 8);
                    // Shoulders arc
                    g2.drawArc(cx - 9, cy + 1, 18, 12, 0, 180);
                    g2.dispose();
                }
            };
            icon.setPreferredSize(new Dimension(20, 44));

            field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            field.setForeground(new Color(0x1A, 0x0F, 0x05));
            field.setBackground(FIELD_BG);
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            field.setCaretColor(ACCENT);

            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true;  repaint(); }
                @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
            });

            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(FIELD_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            // Border
            g2.setColor(focused ? FIELD_FOCUS : FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 0.8f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
        }
    }
}
