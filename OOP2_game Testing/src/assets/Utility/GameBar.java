package assets.Utility;

import javax.swing.*;
import java.awt.*;

public class GameBar extends JPanel {

    public enum BarType { HP, MP }

    private int     maxVal;
    private int     currentVal;
    private Color   barColor;
    private BarType barType;

   
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 11);

    public GameBar(int max, Color color) {
        this(max, color, BarType.HP);
    }

    public GameBar(int max, Color color, BarType type) {
        this.maxVal     = max;
        this.currentVal = max;
        this.barColor   = color;
        this.barType    = type;

        setOpaque(false);
        setPreferredSize(new Dimension(260, 18));
    }

    public void updateValue(int current) {
        this.currentVal = current;
        repaint();
    }

    public void setMax(int max) {
        this.maxVal = max;
    }

    public int getCurrentVal() { return currentVal; }
    public int getMaxVal()     { return maxVal;     }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int totalW = getWidth();
        int h      = getHeight();

        String prefix = barType == BarType.MP ? "MP" : "HP";
        g2.setFont(LABEL_FONT);
        FontMetrics fm    = g2.getFontMetrics();
        int barX          = 0;
        int barW          = totalW;
        int labelPad      = 10;
        int valuePad      = 8;

        g2.setColor(barType == BarType.MP ? new Color(160, 210, 255) : new Color(200, 255, 180));

        if (maxVal <= 0) maxVal = 1;
        float percent  = Math.max(0, (float) currentVal / maxVal);
        int fillWidth  = (int) (barW * percent);
        int arc        = 12;

        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRoundRect(barX, 0, barW, h, arc, arc);

        if (fillWidth > 0) {
            GradientPaint gradient = new GradientPaint(
                    barX, 0, barColor.brighter(),
                    barX + barW, h, barColor.darker());
            g2.setPaint(gradient);
            g2.fillRoundRect(barX, 0, fillWidth, h, arc, arc);

            g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 80));
            g2.fillRoundRect(barX, 0, fillWidth, h, arc, arc);
        }

        g2.setColor(new Color(212, 175, 55));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, 0, barW - 1, h - 1, arc, arc);

        String valueText = currentVal + "/" + maxVal;
        int prefixW = fm.stringWidth(prefix);
        int textW = fm.stringWidth(valueText);
        int combinedW = prefixW + valuePad + textW;
        int textX = barX + Math.max(labelPad, (barW - combinedW) / 2);
        int textY = h / 2 + fm.getAscent() / 2 - 1;

        g2.setFont(LABEL_FONT);

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(prefix, textX + 1, textY + 1);
        g2.drawString(valueText, textX + prefixW + valuePad + 1, textY + 1);

        g2.setColor(Color.WHITE);
        g2.drawString(prefix, textX, textY);
        g2.drawString(valueText, textX + prefixW + valuePad, textY);
    }
}