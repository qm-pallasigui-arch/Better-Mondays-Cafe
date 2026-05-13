package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
