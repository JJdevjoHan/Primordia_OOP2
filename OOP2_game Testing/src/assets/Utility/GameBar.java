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
        setPreferredSize(new Dimension(200, 20));
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

        double percent = (double) currentVal / maxVal;
        if (percent < 0) percent = 0;

        int fillWidth = (int) (w * percent);

        // Background
        g2.setColor(new Color(50, 50, 50));
        int[] xBg = {0, w, w - 15, -15};
        int[] yBg = {0, 0, h, h};
        g2.fillPolygon(xBg, yBg, 4);

        // Fill
        g2.setColor(barColor);
        if (fillWidth > 0) {
            int[] xFill = {0, fillWidth, fillWidth - 15, -15};
            int[] yFill = {0, 0, h, h};
            g2.fillPolygon(xFill, yFill, 4);
        }

        // Border
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(xBg, yBg, 4);
    }
}