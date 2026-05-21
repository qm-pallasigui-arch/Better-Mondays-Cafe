package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

public class RoundedPanel extends JPanel {

    private int arcWidth = 20;
    private int arcHeight = 20;
    private Color fillColor = new Color(36, 55, 83);
    private Color borderColor = new Color(36, 55, 83);

    public RoundedPanel() {
        setOpaque(false);
    }

    public RoundedPanel(int arc) {
        this.arcWidth = arc;
        this.arcHeight = arc;
        setOpaque(false);
    }

    public void setArc(int arc) {
        this.arcWidth = arc;
        this.arcHeight = arc;
        repaint();
    }

    public void setFillColor(Color color) {
        this.fillColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Shape round = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arcWidth, arcHeight);
        g2.setColor(fillColor);
        g2.fill(round);
        g2.setColor(borderColor);
        g2.draw(round);
        g2.dispose();
        super.paintComponent(g);
    }
}
