package assets.Utility;

import javax.swing.*;
import java.awt.*;

public final class ButtonTextRenderer {
    private ButtonTextRenderer() {
    }

    public static void drawCenteredText(Graphics2D g2, JComponent component, String text, int offsetY) {
        g2.setFont(component.getFont());
        g2.setColor(component.getForeground());
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = (component.getWidth() - textWidth) / 2;
        int textY = (component.getHeight() - fm.getHeight()) / 2 + fm.getAscent() + offsetY;
        g2.drawString(text, textX, textY);
    }
}
