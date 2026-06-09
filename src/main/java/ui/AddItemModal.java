package ui;

import controller.InventoryController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * AddItemModal — modern single-dialog form for adding a new inventory item.
 *
 * Usage:
 *   AddItemModal modal = new AddItemModal(parentWindow, inventoryController, onSuccess);
 *   modal.setVisible(true);
 *
 * Replaces the sequential JOptionPane chain in InventoryPanel.openAddDialog().
 */
public class AddItemModal extends JDialog {

    // ── Palette (mirrors InventoryPanel) ─────────────────────────────────────
    private static final Color BG            = Color.WHITE;
    private static final Color BG_SURFACE    = new Color(0xF9FAFB);
    private static final Color BORDER        = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY  = new Color(0x111827);
    private static final Color TEXT_MUTED    = new Color(0x6B7280);
    private static final Color ACCENT        = new Color(0x1D4ED8);
    private static final Color ACCENT_HOVER  = new Color(0x1E40AF);
    private static final Color WARN_BG       = new Color(0xF3F4F6);
    private static final Color WARN_BORDER   = new Color(0xD1D5DB);
    private static final Color WARN_FG       = new Color(0x374151);
    private static final Color DANGER_FG     = new Color(0xDC2626);
    private static final Color PILL_BORDER   = new Color(0xD1D5DB);
    private static final Color PILL_SEL_BG   = new Color(0xEFF6FF);
    private static final Color PILL_SEL_BDR  = new Color(0x93C5FD);
    private static final Color PILL_SEL_FG   = new Color(0x1D4ED8);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_TITLE   = new Font("Segoe UI", Font.BOLD,   15);
    private static final Font F_SUB     = new Font("Segoe UI", Font.PLAIN,  12);
    private static final Font F_LABEL   = new Font("Segoe UI", Font.BOLD,   11);
    private static final Font F_BODY    = new Font("Segoe UI", Font.PLAIN,  13);
    private static final Font F_HINT    = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font F_BTN     = new Font("Segoe UI", Font.BOLD,   13);

    private static final String[] UNITS       = {"kg", "g", "L", "mL", "pcs", "packs", "bottles", "cups", "tbsp", "tsp"};
    private static final String[] CATEGORIES  = {"Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"};

    // ── Fields ────────────────────────────────────────────────────────────────
    private JTextField  nameField;
    private JTextField  qtyField;
    private JComboBox<String> unitCombo;
    private JTextField  alertField;
    private JLabel      alertUnitLabel;
    private final List<JToggleButton> categoryPills = new ArrayList<>();

    private final InventoryController controller;
    private final Runnable onSuccess;

    // ── Constructor ───────────────────────────────────────────────────────────
    public AddItemModal(Window owner, InventoryController controller, Runnable onSuccess) {
        super(owner, "Add Inventory Item", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.onSuccess  = onSuccess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(BG);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildBody(),    BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);
        setContentPane(root);

        pack();
        setMinimumSize(new Dimension(480, getHeight()));
        setLocationRelativeTo(owner);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(18, 24, 16, 24)));

        JLabel title = new JLabel("Add inventory item");
        title.setFont(F_TITLE);
        title.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel("Register a new ingredient and set its stock details.");
        sub.setFont(F_SUB);
        sub.setForeground(TEXT_MUTED);

        JPanel textStack = new JPanel(new BorderLayout(0, 3));
        textStack.setOpaque(false);
        textStack.add(title, BorderLayout.NORTH);
        textStack.add(sub,   BorderLayout.SOUTH);
        p.add(textStack, BorderLayout.CENTER);

        // X close button
        JButton closeBtn = new JButton("✕") {
            private boolean hov;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0xF3F4F6) : BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.setColor(TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        p.add(closeBtn, BorderLayout.EAST);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Body
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20, 24, 8, 24));

        // ── Name ─────────────────────────────────────────────────────────────
        JPanel nameSection = new JPanel(new BorderLayout(0, 6));
        nameSection.setOpaque(false);
        nameSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        nameSection.add(fieldLabel("INGREDIENT NAME", false), BorderLayout.NORTH);
        nameField = styledField("e.g. Espresso Beans");
        nameSection.add(nameField, BorderLayout.CENTER);
        nameSection.add(hintLabel("A clear, recognisable name for this ingredient."), BorderLayout.SOUTH);
        body.add(nameSection);
        body.add(Box.createVerticalStrut(14));

        // ── Quantity + Unit (side by side) ────────────────────────────────────
        JPanel qtyRow = new JPanel(new GridLayout(1, 2, 12, 0));
        qtyRow.setOpaque(false);
        qtyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel qtyPanel = new JPanel(new BorderLayout(0, 6));
        qtyPanel.setOpaque(false);
        qtyPanel.add(fieldLabel("QUANTITY", false), BorderLayout.NORTH);
        qtyField = styledField("0");
        qtyPanel.add(qtyField, BorderLayout.CENTER);
        qtyPanel.add(hintLabel("Current stock on hand."), BorderLayout.SOUTH);

        JPanel unitPanel = new JPanel(new BorderLayout(0, 6));
        unitPanel.setOpaque(false);
        unitPanel.add(fieldLabel("UNIT", false), BorderLayout.NORTH);
        unitCombo = new JComboBox<>(UNITS);
        unitCombo.setFont(F_BODY);
        unitCombo.setBackground(BG);
        unitCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(4, 8, 4, 8)));
        unitCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        unitCombo.addActionListener(e -> syncAlertUnitLabel());
        unitPanel.add(unitCombo, BorderLayout.CENTER);
        unitPanel.add(hintLabel("Measurement unit for this item."), BorderLayout.SOUTH);

        qtyRow.add(qtyPanel);
        qtyRow.add(unitPanel);
        qtyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(qtyRow);
        body.add(Box.createVerticalStrut(16));

        // ── Alert level callout ───────────────────────────────────────────────
        JPanel alertCallout = buildAlertCallout();
        alertCallout.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(alertCallout);
        body.add(Box.createVerticalStrut(16));

        // ── Categories ────────────────────────────────────────────────────────
        JPanel catSection = new JPanel(new BorderLayout(0, 8));
        catSection.setOpaque(false);
        catSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        catSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        catSection.add(fieldLabel("CATEGORY", true), BorderLayout.NORTH);
        catSection.add(buildCategoryPills(), BorderLayout.CENTER);
        body.add(catSection);
        body.add(Box.createVerticalStrut(20));

        return body;
    }

    private JPanel buildAlertCallout() {
        // Outer callout card
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WARN_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(WARN_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Bell icon
        JLabel bell = new JLabel("🔔");
        bell.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        bell.setForeground(new Color(0x6B7280));
        bell.setVerticalAlignment(SwingConstants.TOP);
        card.add(bell, BorderLayout.WEST);

        // Right side
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel alertTitle = new JLabel("Low stock alert level");
        alertTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        alertTitle.setForeground(WARN_FG);
        alertTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel alertDesc = new JLabel("<html>You'll be notified when stock falls at or below this amount.</html>");
        alertDesc.setFont(F_HINT);
        alertDesc.setForeground(TEXT_MUTED);
        alertDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        inputRow.setOpaque(false);
        alertField = new JTextField("5", 6);
        alertField.setFont(F_BODY);
        alertField.setBackground(BG);
        alertField.setForeground(TEXT_PRIMARY);
        alertField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WARN_BORDER, 1),
                new EmptyBorder(4, 8, 4, 8)));
        alertField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                alertField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1), new EmptyBorder(4, 8, 4, 8)));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                alertField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(WARN_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
            }
        });

        alertUnitLabel = new JLabel(" " + UNITS[0]);
        alertUnitLabel.setFont(F_BODY);
        alertUnitLabel.setForeground(TEXT_MUTED);

        inputRow.add(alertField);
        inputRow.add(alertUnitLabel);
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(alertTitle);
        right.add(Box.createVerticalStrut(3));
        right.add(alertDesc);
        right.add(Box.createVerticalStrut(8));
        right.add(inputRow);
        card.add(right, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCategoryPills() {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        for (String cat : CATEGORIES) {
            JToggleButton pill = new JToggleButton(cat) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean sel = isSelected();
                    g2.setColor(sel ? PILL_SEL_BG : BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(sel ? PILL_SEL_BDR : PILL_BORDER);
                    g2.setStroke(new BasicStroke(sel ? 1.5f : 1f));
                    g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                    g2.setFont(F_BODY);
                    g2.setColor(sel ? PILL_SEL_FG : TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    String t = getText();
                    g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            pill.setFont(F_BODY);
            pill.setPreferredSize(new Dimension(
                    pill.getFontMetrics(F_BODY).stringWidth(cat) + 28, 30));
            pill.setBorderPainted(false);
            pill.setContentAreaFilled(false);
            pill.setFocusPainted(false);
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addChangeListener(e -> pill.repaint());
            categoryPills.add(pill);
            wrap.add(pill);
        }
        return wrap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Footer
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(14, 24, 16, 24)));

        // Required notice
        JLabel req = new JLabel("* Name, Quantity, Unit and Alert Level are required.");
        req.setFont(F_HINT);
        req.setForeground(TEXT_MUTED);
        footer.add(req, BorderLayout.WEST);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);

        JButton cancelBtn = buildOutlineBtn("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton addBtn = buildPrimaryBtn("+ Add item");
        addBtn.addActionListener(e -> handleSubmit());

        btns.add(cancelBtn);
        btns.add(addBtn);
        footer.add(btns, BorderLayout.EAST);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Submit logic
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSubmit() {
        String name  = nameField.getText().trim();
        String qty   = qtyField.getText().trim();
        String unit  = (String) unitCombo.getSelectedItem();
        String alert = alertField.getText().trim();

        // Validate
        if (name.isEmpty()) {
            shake(nameField);
            showError("Ingredient name is required.");
            return;
        }
        double qtyVal, alertVal;
        try {
            qtyVal = Double.parseDouble(qty);
            if (qtyVal < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            shake(qtyField);
            showError("Quantity must be a non-negative number (e.g. 10 or 2.5).");
            return;
        }
        try {
            alertVal = Double.parseDouble(alert);
            if (alertVal < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            shake(alertField);
            showError("Alert level must be a non-negative number.");
            return;
        }
        if (alertVal >= qtyVal && qtyVal > 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "<html>The alert level (<b>" + alert + " " + unit +
                    "</b>) is ≥ current quantity.<br>This will immediately trigger a low-stock alert.<br><br>Add item anyway?</html>",
                    "Check alert level",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        // Collect categories
        List<String> selected = new ArrayList<>();
        for (JToggleButton pill : categoryPills)
            if (pill.isSelected()) selected.add(pill.getText());

        try {
            controller.addItem(name, qty, unit, alert);
            // If your controller/model supports categories, wire it here:
            // controller.setCategories(name, String.join(",", selected));
            if (onSuccess != null) onSuccess.run();
            dispose();
        } catch (Exception ex) {
            showError("Could not save item: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void syncAlertUnitLabel() {
        if (alertUnitLabel != null)
            alertUnitLabel.setText(" " + unitCombo.getSelectedItem());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    /** Rapid horizontal shake animation on a component to signal invalid input. */
    private void shake(Component c) {
        Point orig = c.getLocation();
        int[] dx = {8, -8, 6, -6, 4, -4, 2, -2, 0};
        Timer t = new Timer(30, null);
        final int[] step = {0};
        t.addActionListener(e -> {
            if (step[0] < dx.length) {
                c.setLocation(orig.x + dx[step[0]++], orig.y);
            } else {
                c.setLocation(orig);
                ((Timer) e.getSource()).stop();
            }
        });
        t.start();
    }

    // ── Field factory helpers ─────────────────────────────────────────────────
    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(F_BODY);
        f.setForeground(TEXT_MUTED);
        f.setBackground(BG);
        f.setCaretColor(ACCENT);
        f.setText(placeholder);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(5, 10, 5, 10)));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(TEXT_PRIMARY);
                }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1),
                        new EmptyBorder(5, 10, 5, 10)));
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isBlank()) {
                    f.setText(placeholder);
                    f.setForeground(TEXT_MUTED);
                }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        new EmptyBorder(5, 10, 5, 10)));
            }
        });
        return f;
    }

    private JLabel fieldLabel(String text, boolean optional) {
        String html = "<html><span style='font-size:10px; letter-spacing:1px;'>"
                + text + "</span>"
                + (optional ? " <span style='font-size:10px; font-weight:normal; color:#9CA3AF;'>(optional)</span>" : "")
                + "</html>";
        JLabel l = new JLabel(html);
        l.setFont(F_LABEL);
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel hintLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_HINT);
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(4, 0, 0, 0));
        return l;
    }

    private JButton buildPrimaryBtn(String text) {
        JButton btn = new JButton(text) {
            private boolean hov;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? ACCENT_HOVER : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(F_BTN);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildOutlineBtn(String text) {
        JButton btn = new JButton(text) {
            private boolean hov;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0xF9FAFB) : BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setFont(F_BTN);
                g2.setColor(TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
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
}