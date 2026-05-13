package engine.gameplay;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyInputs implements KeyListener {
    private GamePanel gp;
    private SurivivalGamePanel sp;
    private ArcadeGamePanel ap;

    public KeyInputs(ArcadeGamePanel ap) {

        this.ap = ap;
    }

    public KeyInputs(SurivivalGamePanel sp) {
        this.sp = sp;
    }

    public KeyInputs(GamePanel gp)
    {
        this.gp = gp;
    }


    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (ap != null) {
            boolean blocked = ap.isOverlayAnimatingOrPopupShown();
            System.out.println("[KeyInputs] ArcadeGamePanel key: " + code + ", blocked=" + blocked);
            if (blocked) {
                System.out.println("[KeyInputs] Key blocked by overlay/popup");
                e.consume();
                return;
            }
            if (code == KeyEvent.VK_1) ap.executeSkill(1);
            if (code == KeyEvent.VK_2) ap.executeSkill(2);
            if (code == KeyEvent.VK_3) ap.executeSkill(3);
            return;
        }
        if (sp != null) {
            if (code == KeyEvent.VK_1) sp.executeSkill(1);
            if (code == KeyEvent.VK_2) sp.executeSkill(2);
            if (code == KeyEvent.VK_3) sp.executeSkill(3);
            return;
        }
        if (gp != null) {
            if (code == KeyEvent.VK_1) gp.executeSkill(1);
            if (code == KeyEvent.VK_2) gp.executeSkill(2);
            if (code == KeyEvent.VK_3) gp.executeSkill(3);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
