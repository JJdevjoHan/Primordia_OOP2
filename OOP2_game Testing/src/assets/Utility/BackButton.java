package assets.Utility;

import javax.swing.*;
import java.awt.*;

import engine.core.GameWindow;
import engine.audio.SoundManager;
import engine.interfaces.SkillExecutor;

/**
 * OOP Principle: Inheritance + Single Responsibility
 *
 * BEFORE: BackButton was a factory class (createBackButton()) with its own
 *         copy of all button styling AND an instanceof chain to call stopMusic()
 *         on whichever panel type happened to be passed in.
 *
 * AFTER:  Styling is inherited from BaseGameButton.
 *         stopMusic() is called via the SkillExecutor interface — no instanceof,
 *         no imports of concrete panel classes.
 */
public class BackButton extends BaseGameButton {

    private static final Color NORMAL = new Color(40, 40, 60);
    private static final Color HOVER  = new Color(70, 70, 100);

    private final GameWindow   window;
    private final JPanel       panel;
    private final SkillExecutor executor;   // used only for stopMusic()

    /**
     * @param window   GameWindow to call showIntro() on
     * @param panel    Parent panel (used as dialog owner)
     * @param executor The active game panel (implements SkillExecutor + stopMusic)
     */
    public BackButton(GameWindow window, JPanel panel, SkillExecutor executor) {
        super();
        this.window   = window;
        this.panel    = panel;
        this.executor = executor;
    }

    @Override protected String getLabel()       { return "Back"; }
    @Override protected Color  getNormalColor() { return NORMAL; }
    @Override protected Color  getHoverColor()  { return HOVER;  }
    @Override protected float  getFontSize()    { return 24f; }

    @Override
    protected javax.swing.border.Border getButtonBorder() {
        return BorderFactory.createLineBorder(new Color(200, 160, 40), 2);
    }

    @Override
    protected void onClick() {
        int choice = JOptionPane.showConfirmDialog(
                panel, "Return to intro?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            executor.stopMusic();
            SwingUtilities.invokeLater(window::showIntro);
        }
    }
}