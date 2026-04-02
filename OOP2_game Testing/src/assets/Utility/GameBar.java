package assets.Utility;

import javax.swing.*;
import java.awt.*;

public class GameBar extends JPanel {

    private int maxVal;
    private int currentVal;
    private Color barColor;

    public GameBar(int max, Color color) {
        this.maxVal = max;
        this.currentVal = max;
        this.barColor = color;

        setOpaque(false);
        setPreferredSize(new Dimension(220, 18)); // thinner, cleaner
    }

    public void updateValue(int current) {
        this.currentVal = current;
        repaint();
    }

    public void setMax(int max) {
        this.maxVal = max;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (maxVal <= 0) maxVal = 1;

        float percent = Math.max(0, (float) currentVal / maxVal);
        int fillWidth = (int) (w * percent);

        int arc = 12;

        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        if (fillWidth > 0) {
            GradientPaint gradient = new GradientPaint(
                    0, 0, barColor.brighter(),
                    w, h, barColor.darker()
            );
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, fillWidth, h, arc, arc);

            g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 80));
            g2.fillRoundRect(0, 0, fillWidth, h, arc, arc);
        }

        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
    }
}