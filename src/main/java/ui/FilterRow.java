package ui;

import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Small helper panel to arrange filter controls consistently.
 */
public class FilterRow extends JPanel {

    public FilterRow() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    public void addLabeled(String label, JComponent comp) {
        add(new JLabel(label));
        add(comp);
        if (comp instanceof JTextField tf) {
            AppTheme.styleSearchField(tf);
        }
    }
}
