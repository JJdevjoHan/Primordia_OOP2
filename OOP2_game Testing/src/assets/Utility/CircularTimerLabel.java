package assets.Utility;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public class CircularTimerLabel extends JComponent {

    private static final Color FILL_COLOR = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = Color.BLACK;
    private static final Color DEFAULT_TEXT_COLOR = new Color(20, 20, 20);
    private String text;

    public CircularTimerLabel(String text) {
        this.text = text;
        setOpaque(false);
        setForeground(DEFAULT_TEXT_COLOR);
    }

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public String getText() {
        return text;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int diameter = Math.min(getWidth(), getHeight()) - 2;
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        Shape circle = new Ellipse2D.Float(x, y, diameter, diameter);
        g2.setClip(circle);
        g2.setColor(FILL_COLOR);
        g2.fill(circle);

        g2.setClip(null);
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(3f));
        g2.draw(circle);

        g2.setColor(getForeground());
        g2.setFont(getFont());
        String value = text != null ? text : "";
        FontRenderContext frc = g2.getFontRenderContext();
        GlyphVector glyphVector = getFont().createGlyphVector(frc, value);
        Rectangle2D bounds = glyphVector.getVisualBounds();
        double textX = x + (diameter - bounds.getWidth()) / 2.0 - bounds.getX();
        double textY = y + (diameter - bounds.getHeight()) / 2.0 - bounds.getY();
        g2.drawGlyphVector(glyphVector, (float) textX, (float) textY);
        g2.dispose();
    }
}