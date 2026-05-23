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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.border.LineBorder;

/** Shared palette and typography for visual consistency across screens. */
public final class AppTheme {

    public static final Color BG_PRIMARY = new Color(23, 36, 54);
    public static final Color BG_SURFACE = new Color(36, 55, 83);
    public static final Color FG_PRIMARY = new Color(245, 248, 252);
    public static final Color FG_MUTED = new Color(197, 209, 224);
    public static final Color ACCENT = new Color(50, 157, 111);

    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font TITLE = new Font("Segoe UI", Font.BOLD, 14);

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
        tf.setBackground(new Color(49, 73, 105));
        tf.setCaretColor(FG_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 85, 120), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        tf.putClientProperty("JTextField.roundPlaceholder", true);
    }

    /** Standard input border used across the app. */
    public static javax.swing.border.Border inputBorderRegular() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 85, 120), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    /** Compact pill-style input border. */
    public static javax.swing.border.Border inputBorderPill() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 85, 120), 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    /** Focused input border with the given highlight color. */
    public static javax.swing.border.Border focusInputBorder(Color focusColor) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(focusColor, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    /** Apply standard table defaults for consistent look-and-feel. */
    public static void applyTableDefaults(JTable table) {
        if (table == null) return;
        table.setBackground(BG_SURFACE);
        table.setForeground(FG_PRIMARY);
        table.setSelectionBackground(new Color(63, 94, 138));
        table.setSelectionForeground(FG_PRIMARY);
        table.setGridColor(new Color(79, 102, 135));
        table.setFont(BODY);
        table.getTableHeader().setBackground(new Color(49, 73, 105));
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
            tf.setFont(BODY);
        }
        if (c instanceof JTextArea ta) {
            ta.setBackground(Color.WHITE);
            ta.setForeground(new Color(33, 33, 33));
            ta.setFont(BODY);
        }
        if (c instanceof JTable table) {
            table.setBackground(BG_SURFACE);
            table.setForeground(FG_PRIMARY);
            table.setSelectionBackground(new Color(63, 94, 138));
            table.setSelectionForeground(FG_PRIMARY);
            table.setGridColor(new Color(79, 102, 135));
            table.setFont(BODY);
            table.getTableHeader().setBackground(new Color(49, 73, 105));
            table.getTableHeader().setForeground(FG_PRIMARY);
            table.getTableHeader().setFont(TITLE);
            table.setRowHeight(Math.max(22, table.getRowHeight()));
        }
        if (c instanceof JScrollPane sp) {
            sp.getViewport().setBackground(BG_SURFACE);
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursive(child);
            }
        }
    }
}
