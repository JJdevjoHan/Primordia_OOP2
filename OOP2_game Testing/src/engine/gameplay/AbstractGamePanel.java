package engine.gameplay;

import engine.interfaces.SkillExecutor;

import javax.swing.*;
import java.awt.*;

//na tanan constrct na shared sa 3 godmajor panel, so dili na sila duplciated tho intact nila ilang same logic japon

public abstract class AbstractGamePanel extends JPanel implements SkillExecutor {

    protected static final int TILE_SIZE     = 128;
    protected static final int SCREEN_WIDTH  = TILE_SIZE * 12;   // 1536
    protected static final int SCREEN_HEIGHT = TILE_SIZE * 7;    // 896

    protected static final int MAX_MP            = 100;
    protected static final int MP_REGEN_PER_TURN = 10;
    protected static final int[] SKILL_MP_COST   = { 10, 20, 30 }; // index 0 = skill 1

    protected static final int TURN_TIME_SECONDS    = 10;
    protected static final int TIMER_WARN_THRESHOLD = 3;

    protected static final int    SKILL_PANEL_MIN_WIDTH     = 928;
    protected static final int    SKILL_PANEL_MAX_WIDTH     = 1216;
    protected static final int    SKILL_PANEL_HEIGHT        = 192;
    protected static final int    SKILL_PANEL_BOTTOM_MARGIN = 64;
    protected static final int    SKILL_PANEL_SIDE_MARGIN   = 32;
    protected static final String BATTLE_UI_BOX_PATH        = "/assets/BattleUI/BattleUI_Box.png";

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