package assets.Utility;

import javax.swing.*;
import java.awt.*;

import engine.core.GameWindow;
import engine.audio.SoundManager;

/**
 * OOP Principle: Inheritance + Single Responsibility
 *
 * BEFORE: ExitButton was a factory class with its own copy of all button styling,
 *         its own hover listener, and a private getGameWindow() helper that walked
 *         the component tree to find the GameWindow.
 *
 * AFTER:  Styling and hover are inherited from BaseGameButton.
 *         GameWindow is injected directly — no tree-walking needed.
 */
public class ExitButton extends BaseGameButton {

    private static final Color NORMAL = new Color(180, 40,  40);
    private static final Color HOVER  = new Color(220, 60,  60);

    private final GameWindow window;

    public ExitButton(GameWindow window) {
        super();
        this.window = window;
    }

    @Override protected String getLabel()       { return "X"; }
    @Override protected Color  getNormalColor() { return NORMAL; }
    @Override protected Color  getHoverColor()  { return HOVER;  }
    @Override protected float  getFontSize()    { return 30f; }

    @Override
    protected void onClick() {
        if (window != null) window.handleExitButtonClick();
        else System.exit(0);
    }
}