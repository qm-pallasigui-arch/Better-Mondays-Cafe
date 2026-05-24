package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.Border;

/**
 * A simple rounded rectangle border with configurable arc and thickness.
 */
public class RoundedLineBorder implements Border {

    private final Color color;
    private final int thickness;
    private final int arc;

    public RoundedLineBorder(Color color, int thickness, int arc) {
        this.color = color == null ? Color.BLACK : color;
        this.thickness = Math.max(1, thickness);
        this.arc = Math.max(2, arc);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            // draw inside the component bounds so stroke is fully visible
            int half = thickness / 2;
            Shape r = new RoundRectangle2D.Float(x + half, y + half, width - thickness, height - thickness, arc, arc);
            g2.draw(r);
        } finally {
            g2.dispose();
        }
    }
}
