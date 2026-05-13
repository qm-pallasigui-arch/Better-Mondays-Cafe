package ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InventoryGuidePanel extends JPanel {

    public InventoryGuidePanel() {
        super(new BorderLayout());
        JTextArea guide = new JTextArea();
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setText("""
Inventory Status Guide

ABC Classification
- A: High-priority inventory value items (top ~80% cumulative usage value)
- B: Medium-priority items (next ~15%)
- C: Lower-priority items (remaining ~5%)

EOQ (Economic Order Quantity)
- EOQ indicates suggested replenishment quantity to reduce ordering + holding cost.
- Status format EOQ~N means the recommended order size is approximately N units.

FEFO (First-Expire-First-Out)
- Inventory deductions use earliest expiry batches first.
- EXPIRED: at least one batch is already expired.
- EXPIRING<7D: at least one batch expires within 7 days.

Color Cues in Inventory Table
- Red: expired batch detected.
- Orange: low stock or expiring batch warning.
- Green: normal/healthy stock state.
""");
        add(new JScrollPane(guide), BorderLayout.CENTER);
    }
}
