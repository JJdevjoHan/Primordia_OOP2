package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyInputs implements KeyListener {
    private GamePanel gp;

    public KeyInputs(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_1) gp.executeSkill(1);
        if (code == KeyEvent.VK_2) gp.executeSkill(2);
        if (code == KeyEvent.VK_3) gp.executeSkill(3);
        if (code == KeyEvent.VK_4) gp.executeSkill(4);
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
