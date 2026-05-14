package engine.gameplay;

import engine.interfaces.SkillExecutor;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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