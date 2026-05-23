package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;

/**
 * Lightweight card wrapper using RoundedPanel for consistent cards.
 */
public class CardPanel extends RoundedPanel {

    public CardPanel() {
        super(12);
        setLayout(new BorderLayout());
        setFillColor(AppTheme.BG_SURFACE);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    public CardPanel(int arc, Color bg) {
        super(arc);
        setLayout(new BorderLayout());
        setFillColor(bg == null ? AppTheme.BG_SURFACE : bg);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }
}
