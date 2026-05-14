package engine;

import engine.SkillExecutor;

import javax.swing.*;
import java.awt.*;

//na tanan constrct na shared sa 3 godmajor panel, so dili na sila duplciated tho intact nila ilang same logic japon

public abstract class AbstractGamePanel extends JPanel implements SkillExecutor {

    protected static final int TILE_SIZE     = 128;
    protected static final int SCREEN_WIDTH  = TILE_SIZE * 12;   // 1536
    protected static final int SCREEN_HEIGHT = TILE_SIZE * 7;    // 896


    protected AbstractGamePanel() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setFocusable(true);
        // KeyInputs now receives 'this' as a SkillExecutor — no concrete type needed
        this.addKeyListener(new KeyInputs(this));
    }

    @Override
    public abstract void executeSkill(int skillID);

    @Override
    public abstract void stopMusic();

    protected abstract void updateGameState();

    @Override
    public boolean isInputBlocked() {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
