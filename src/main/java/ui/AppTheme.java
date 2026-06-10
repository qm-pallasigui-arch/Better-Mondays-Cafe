package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.LineBorder;

/** Shared palette and typography for visual consistency across screens. */
public final class AppTheme {

    public static final Color BG_PRIMARY = new Color(0xF9FAFB);
    public static final Color BG_SURFACE = new Color(0xFFFFFF);
    public static final Color FG_PRIMARY = new Color(0x111827);
    public static final Color FG_MUTED = new Color(0x6B7280);
    public static final Color FG_SUBTLE = new Color(0x9CA3AF);
    public static final Color ACCENT = new Color(0x3B82F6);
    public static final Color ACCENT_DARK = new Color(0x2563EB);
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color SUCCESS = new Color(0x10B981);
    public static final Color WARNING = new Color(0xF59E0B);
    public static final Color DANGER = new Color(0xEF4444);
    public static final Color BG_BADGE_GREEN = new Color(0xD1FAE5);
    public static final Color BG_BADGE_YELLOW = new Color(0xFEF3C7);
    public static final Color BG_BADGE_RED = new Color(0xFEE2E2);
    public static final Color BG_BADGE_BLUE = new Color(0xDBEAFE);

    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font TITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final int BORDER_RADIUS = 12;
    public static final int BORDER_THICKNESS = 1;

    private AppTheme() {
    }

    public static void applyToFrame(JFrame frame) {
        if (frame == null) {
            return;
        }
        if (frame.getContentPane() instanceof JComponent root) {
            applyRecursive(root);
        }
    }

    public static void applyToComponent(JComponent component) {
        if (component == null) {
            return;
        }
        applyRecursive(component);
    }

    public static void applyResponsiveFrameSize(JFrame frame, double widthRatio, double heightRatio, Dimension minimumSize) {
        if (frame == null) {
            return;
        }
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) Math.round(screen.width * widthRatio);
        int height = (int) Math.round(screen.height * heightRatio);
        if (minimumSize != null) {
            width = Math.max(width, minimumSize.width);
            height = Math.max(height, minimumSize.height);
            frame.setMinimumSize(minimumSize);
        }
        frame.setSize(width, height);
    }

    /** Apply consistent styling to a search text field (fonts, colors, border). */
    public static void styleSearchField(JTextField tf) {
        if (tf == null) return;
        tf.setFont(BODY);
        tf.setForeground(FG_PRIMARY);
        tf.setBackground(BG_SURFACE);
        tf.setCaretColor(FG_PRIMARY);
        if (tf.getCaret() != null) {
            tf.getCaret().setBlinkRate(500);
        }
        tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER, BORDER_THICKNESS, BORDER_RADIUS),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        tf.putClientProperty("JTextField.roundPlaceholder", true);
    }

    /** Standard input border used across the app. */
    public static javax.swing.border.Border inputBorderRegular() {
        return BorderFactory.createCompoundBorder(
            new RoundedLineBorder(BORDER, BORDER_THICKNESS, BORDER_RADIUS),
            BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    /** Compact pill-style input border. */
    public static javax.swing.border.Border inputBorderPill() {
        return BorderFactory.createCompoundBorder(
            new RoundedLineBorder(BORDER, BORDER_THICKNESS, BORDER_RADIUS),
            BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    /** Focused input border with the given highlight color. */
    public static javax.swing.border.Border focusInputBorder(Color focusColor) {
        return BorderFactory.createCompoundBorder(
            new RoundedLineBorder(focusColor, BORDER_THICKNESS, BORDER_RADIUS),
            BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    /** Apply standard table defaults for consistent look-and-feel. */
    public static void applyTableDefaults(JTable table) {
        if (table == null) return;
        table.setBackground(BG_SURFACE);
        table.setForeground(FG_PRIMARY);
        table.setSelectionBackground(BG_BADGE_BLUE);
        table.setSelectionForeground(FG_PRIMARY);
        table.setGridColor(BORDER);
        table.setFont(BODY);
        configureTableHeader(table);
        table.setRowHeight(Math.max(22, table.getRowHeight()));
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private static void applyRecursive(Component c) {
        if (c instanceof JPanel panel) {
            panel.setBackground(BG_PRIMARY);
            panel.setForeground(FG_PRIMARY);
        }
        if (c instanceof JLabel label) {
            label.setForeground(FG_PRIMARY);
            java.awt.Font laf = javax.swing.UIManager.getFont("Label.font");
            if (label.getFont() == laf || label.getFont() == null) {
                label.setFont(TITLE);
            }
        }
        if (c instanceof JButton button) {
            Object variant = button.getClientProperty("appTheme.variant");
            if ("transparent".equals(variant)) {
                button.setBackground(new Color(0, 0, 0, 0));
                button.setForeground(FG_MUTED);
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setUI(new BasicButtonUI());
            } else if ("danger".equals(variant)) {
                button.setBackground(DANGER);
                button.setForeground(Color.WHITE);
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setFont(BODY);
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                return;
            } else {
                button.setBackground(ACCENT);
            }
            if (!"transparent".equals(variant)) {
                button.setForeground(Color.WHITE);
            }
            button.setFont(BODY);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            if (!"transparent".equals(variant)) {
                button.setUI(new RoundedButtonUI());
            }
        }
        if (c instanceof JTextField tf) {
            tf.setBackground(Color.WHITE);
            tf.setForeground(new Color(33, 33, 33));
            tf.setCaretColor(new Color(33, 33, 33));
            if (tf.getCaret() != null) {
                tf.getCaret().setBlinkRate(500);
            }
            tf.setFont(BODY);
        }
        if (c instanceof JPasswordField pf) {
            pf.setBackground(Color.WHITE);
            pf.setForeground(new Color(33, 33, 33));
            pf.setCaretColor(new Color(33, 33, 33));
            if (pf.getCaret() != null) {
                pf.getCaret().setBlinkRate(500);
            }
            pf.setFont(BODY);
        }
        if (c instanceof JTextArea ta) {
            ta.setBackground(Color.WHITE);
            ta.setForeground(new Color(33, 33, 33));
            ta.setCaretColor(new Color(33, 33, 33));
            if (ta.getCaret() != null) {
                ta.getCaret().setBlinkRate(500);
            }
            ta.setFont(BODY);
        }
        if (c instanceof JTable table) {
            table.setBackground(BG_SURFACE);
            table.setForeground(FG_PRIMARY);
            table.setSelectionBackground(BG_BADGE_BLUE);
            table.setSelectionForeground(FG_PRIMARY);
            table.setGridColor(BORDER);
            table.setFont(BODY);
            configureTableHeader(table);
            table.setRowHeight(Math.max(22, table.getRowHeight()));
        }
        if (c instanceof JScrollPane sp) {
            sp.getViewport().setBackground(BG_SURFACE);
            sp.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(BORDER, BORDER_THICKNESS, BORDER_RADIUS),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        }
        if (c instanceof JTable table) {
            table.setBorder(new RoundedLineBorder(BORDER, BORDER_THICKNESS, BORDER_RADIUS));
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursive(child);
            }
        }
    }

    private static void configureTableHeader(JTable table) {
        if (table == null || table.getTableHeader() == null) {
            return;
        }
        table.getTableHeader().setDefaultRenderer(new TableHeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(true);
    }

    private static final class RoundedButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            if (!(c instanceof JButton button)) {
                super.paint(g, c);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill = button.isEnabled() ? button.getBackground() : FG_SUBTLE;
                Color border = button.isEnabled()
                        ? ("danger".equals(button.getClientProperty("appTheme.variant"))
                                ? DANGER.darker()
                                : ACCENT_DARK)
                        : BORDER;

                int width = c.getWidth();
                int height = c.getHeight();
                int arc = BORDER_RADIUS;

                g2.setColor(fill);
                g2.fillRoundRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 3), arc, arc);

                g2.setColor(border);
                g2.drawRoundRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 3), arc, arc);
            } finally {
                g2.dispose();
            }

            super.paint(g, c);
        }
    }

    private static final class TableHeaderRenderer extends DefaultTableCellRenderer {
        private TableHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setFont(TITLE);
            setForeground(Color.WHITE);
            setBackground(ACCENT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString());
            setForeground(Color.WHITE);
            setBackground(ACCENT);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, ACCENT_DARK));
            return this;
        }
    }
}
