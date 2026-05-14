package assets.Utility;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * OOP Principle: Inheritance + Don't Repeat Yourself (DRY)
 *
 * BEFORE: BackButton, ExitButton, and CreditsButton each independently set:
 *   - setOpaque(true), setContentAreaFilled(true), setFocusPainted(false)
 *   - Font (via FontManager), foreground color, background color, border
 *   - A hover MouseAdapter with mouseEntered / mouseExited
 *
 * AFTER:  All shared setup lives here once. Subclasses only provide:
 *   - getLabel()        — the button text
 *   - getNormalColor()  — default background
 *   - getHoverColor()   — hover background
 *   - getFontSize()     — float size passed to FontManager
 *   - getBorder()       — border (or null for none)
 *   - onClick()         — action logic
 *
 * Adding a new button type is one new file, zero changes here.
 */
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

    // ── Template-method hooks ──────────────────────────────────────────────

    /** Text rendered on the button face. */
    protected abstract String getLabel();

    /** Background colour when the mouse is not over the button. */
    protected abstract Color getNormalColor();

    /** Background colour when the mouse hovers. */
    protected abstract Color getHoverColor();

    /** Font size (float) forwarded to FontManager. */
    protected float getFontSize() { return 24f; }

    /** Border for the button, or null for an empty border. */
    protected javax.swing.border.Border getButtonBorder() { return null; }

    /** Called when the button is clicked. */
    protected abstract void onClick();

    // ── Shared rendering ──────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ButtonTextRenderer.drawCenteredText(g2, this, getLabel(), 0);
        g2.dispose();
    }
}