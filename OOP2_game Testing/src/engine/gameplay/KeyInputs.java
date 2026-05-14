package engine.gameplay;

import engine.interfaces.SkillExecutor;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * OOP Principle: Dependency Inversion
 *
 * BEFORE: Three separate fields (GamePanel gp, SurivivalGamePanel sp, ArcadeGamePanel ap)
 *         plus a cascading if/else chain to decide which to call.
 *         Every new game mode required editing this class.
 *
 * AFTER:  One SkillExecutor field. This class never changes when new modes are added.
 */
public class KeyInputs implements KeyListener {

    private final SkillExecutor executor;

    public KeyInputs(SkillExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (executor.isInputBlocked()) {
            e.consume();
            return;
        }
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_1) executor.executeSkill(1);
        if (code == KeyEvent.VK_2) executor.executeSkill(2);
        if (code == KeyEvent.VK_3) executor.executeSkill(3);
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}