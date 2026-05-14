package assets.Utility;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//subclasses and shared naa dri
public abstract class BaseGameButton extends JButton {

    protected BaseGameButton() {
        super("");                       // text is drawn via paintComponent

        setOpaque(true);
        setContentAreaFilled(true);
        setFocusPainted(false);
        setFont(FontManager.getFont(getFontSize()));
        setForeground(Color.WHITE);
        setBackground(getNormalColor());

        Border border = getButtonBorder();
        if (border != null) setBorder(border);
        else setBorder(BorderFactory.createEmptyBorder());

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setBackground(getHoverColor()); }
            @Override public void mouseExited (MouseEvent e) { setBackground(getNormalColor()); }
        });

        addActionListener(e -> onClick());
    }

    protected abstract String getButtonLabel();
    protected abstract Color getNormalColor();
    protected abstract Color getHoverColor();
    protected float getFontSize() { return 24f; }
    protected javax.swing.border.Border getButtonBorder() { return null; }
    protected abstract void onClick();


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ButtonTextRenderer.drawCenteredText(g2, this, getLabel(), 0);
        g2.dispose();
    }
}