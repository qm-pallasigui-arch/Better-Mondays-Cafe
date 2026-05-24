package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.BorderFactory;
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
            new RoundedLineBorder(new Color(60, 85, 120), BORDER_THICKNESS, BORDER_RADIUS),
            BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    /** Compact pill-style input border. */
    public static javax.swing.border.Border inputBorderPill() {
        return BorderFactory.createCompoundBorder(
            new RoundedLineBorder(new Color(60, 85, 120), BORDER_THICKNESS, BORDER_RADIUS),
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
        table.getTableHeader().setBackground(BG_PRIMARY);
        table.getTableHeader().setForeground(FG_MUTED);
        table.getTableHeader().setFont(TITLE);
        table.setRowHeight(Math.max(22, table.getRowHeight()));
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer hdr = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        hdr.setHorizontalAlignment(SwingConstants.CENTER);
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
            label.setFont(TITLE);
        }
        if (c instanceof JButton button) {
            button.setBackground(ACCENT);
            button.setForeground(FG_PRIMARY);
            button.setFont(BODY);
            button.setFocusPainted(false);
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
            table.getTableHeader().setBackground(BG_PRIMARY);
            table.getTableHeader().setForeground(FG_MUTED);
            table.getTableHeader().setFont(TITLE);
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
}
