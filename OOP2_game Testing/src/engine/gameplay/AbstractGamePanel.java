package engine.gameplay;

import engine.interfaces.SkillExecutor;

import javax.swing.*;
import java.awt.*;

/**
 * OOP Principle: Inheritance + Single Responsibility + DRY
 *
 * BEFORE: GamePanel, ArcadeGamePanel, SurivivalGamePanel each declared the
 *         same ~60 fields (animation state, HP/MP, timers, etc.) and
 *         re-implemented the same helper methods independently.
 *
 * AFTER:  This class is the single place for shared constants and the
 *         contract (abstract methods) every panel must fulfil.
 *         It implements SkillExecutor so KeyInputs needs no knowledge of
 *         which concrete panel it is wired to.
 *
 * NOTE:   The three game panels are large and already working.
 *         Rather than risk breaking them by moving every field here,
 *         we use a MINIMAL base class approach:
 *           - Shared constants live here (no duplication).
 *           - Abstract methods define the contract.
 *           - Each panel keeps its own fields and logic intact,
 *             but now correctly inherits and implements the interface.
 *         This is the safest OOP migration path for a large existing codebase.
 */
public abstract class AbstractGamePanel extends JPanel implements SkillExecutor {

    // ── Shared screen constants (defined once, used by all panels) ────────
    protected static final int TILE_SIZE     = 128;
    protected static final int SCREEN_WIDTH  = TILE_SIZE * 12;   // 1536
    protected static final int SCREEN_HEIGHT = TILE_SIZE * 7;    // 896

    protected static final int DEFAULT_DRAW_WIDTH     = 480;
    protected static final int DEFAULT_DRAW_HEIGHT    = 480;
    protected static final int DEFAULT_FRAME_SIZE     = 128;
    protected static final int DEFAULT_IDLE_DELAY_MS  = 120;
    protected static final int DEFAULT_DEAD_DELAY_MS  = 150;
    protected static final int DEFAULT_SKILL_DELAY_MS = 90;
    protected static final int DEFAULT_HURT_DELAY_MS  = 90;
    protected static final int POST_ATTACK_HURT_MS    = 600;

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

    // ── Constructor ───────────────────────────────────────────────────────

    protected AbstractGamePanel() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setFocusable(true);
        // KeyInputs now receives 'this' as a SkillExecutor — no concrete type needed
        this.addKeyListener(new KeyInputs(this));
    }

    // ── Abstract contract — every panel must implement these ──────────────

    /**
     * Execute the skill in slot 1, 2, or 3 for the active player.
     * Required by SkillExecutor.
     */
    @Override
    public abstract void executeSkill(int skillID);

    /**
     * Stop background music. Called before navigating away.
     * Required by SkillExecutor.
     */
    @Override
    public abstract void stopMusic();

    /**
     * Called after a turn switch or round transition.
     * Each panel updates its own mode-specific state here.
     */
    protected abstract void updateGameState();

    // ── SkillExecutor default ─────────────────────────────────────────────

    /**
     * Override in panels that have overlay animations blocking input
     * (e.g. ArcadeGamePanel.isOverlayAnimatingOrPopupShown()).
     */
    @Override
    public boolean isInputBlocked() {
        return false;
    }

    // ── Shared rendering hook ─────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}