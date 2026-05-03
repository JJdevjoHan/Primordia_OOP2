package assets.Utility;

import javax.swing.*;
import java.awt.*;

/**
 * Displays up to 3 round indicators.
 * - Shaded (filled) when a round was won
 * - Outlined only when a round was not won
 */
public class RoundsWonIndicator extends JPanel {

    private int wins;
    private int totalRounds;
    private Color shadeColor;
    private boolean fillFromRight;

    private static final int CIRCLE_DIAMETER = 18;
    private static final int SPACING = 6;
    private static final Color OUTLINE_COLOR = new Color(212, 175, 55);
    private static final int OUTLINE_WIDTH = 2;

    public RoundsWonIndicator(Color shadeColor) {
        this(shadeColor, false);
    }

    public RoundsWonIndicator(Color shadeColor, boolean fillFromRight) {
        this.wins = 0;
        this.totalRounds = 2;
        this.shadeColor = shadeColor;
        this.fillFromRight = fillFromRight;
        
        setOpaque(false);
        setPreferredSize(new Dimension(
            (CIRCLE_DIAMETER + SPACING) * totalRounds + SPACING,
            CIRCLE_DIAMETER + 8
        ));
    }

    public void setWins(int wins) {
        this.wins = Math.min(wins, totalRounds);
        repaint();
    }

    public int getWins() {
        return wins;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int startX = SPACING;
        int startY = (getHeight() - CIRCLE_DIAMETER) / 2;

        for (int i = 0; i < totalRounds; i++) {
            int circleIndex = fillFromRight ? totalRounds - 1 - i : i;
            int x = startX + circleIndex * (CIRCLE_DIAMETER + SPACING);
            
            if (i < wins) {
                // Shaded (filled) circle for won round
                g2.setColor(shadeColor);
                g2.fillOval(x, startY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
            }
            
            // Outline for all circles
            g2.setColor(OUTLINE_COLOR);
            g2.setStroke(new BasicStroke(OUTLINE_WIDTH));
            g2.drawOval(x, startY, CIRCLE_DIAMETER, CIRCLE_DIAMETER);
        }
    }
}
