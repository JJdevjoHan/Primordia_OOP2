package assets.Utility;

import javax.swing.*;
import java.awt.*;

import engine.core.GameWindow;
import engine.audio.SoundManager;

/**
 * OOP Principle: Inheritance + Single Responsibility
 *
 * BEFORE: CreditsButton was a factory class duplicating all button styling.
 *
 * AFTER:  Only the credits-specific behaviour lives here.
 *         All shared styling is inherited from BaseGameButton.
 */
public class CreditsButton extends BaseGameButton {

    private static final Color NORMAL = new Color(30,  30,  50);
    private static final Color HOVER  = new Color(70,  70, 100);

    private final GameWindow   window;
    private final SoundManager sound;

    public CreditsButton(GameWindow window, SoundManager sound) {
        super();
        this.window = window;
        this.sound  = sound;
    }

    @Override protected String getButtonLabel()       { return "CREDITS"; }
    @Override protected Color  getNormalColor() { return NORMAL; }
    @Override protected Color  getHoverColor()  { return HOVER;  }
    @Override protected float  getFontSize()    { return 24f; }

    @Override
    protected javax.swing.border.Border getButtonBorder() {
        return BorderFactory.createLineBorder(new Color(200, 160, 40), 2);
    }

    @Override
    protected void onClick() {
        sound.setFile(8);
        sound.play();
        window.showCredits();
    }
}