package assets.Utility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Full-screen overlay for battle announcements:
 * - "Round X... Begins" at round start
 * - "Player 1/2 Wins" when a round ends
 * - "Victory - You Win" / "KO - You Lose" at match end
 * 
 * Darkens the background slightly while displaying text.
 */
public class BattleMessageOverlay extends JPanel {

    private static final int FADE_IN_MS     = 300;
    private static final int HOLD_MS        = 2000;
    private static final int FADE_OUT_MS    = 300;
    private static final int BG_DARK_ALPHA  = 140;   // 0-255, how much to darken the background

    public enum MessageType {
        ROUND_START,   // "Round X... Begins"
        ROUND_WIN,     // "Player 1/2 Wins Round X"
        VICTORY,       // "Victory - You Win"
        KO_DEFEAT      // "KO - You Lose"
    }

    private enum State {
        HIDDEN, FADE_IN, HOLD, FADE_OUT
    }

    private State state = State.HIDDEN;

    private String displayText = "";
    private long animationStart = 0L;
    private int  animationDuration = 0;

    private final Font messageFont;
    private final Color textColor       = new Color(245, 224, 175);
    private final Color textShadowColor = new Color(30, 20, 10, 200);
    private final Color bgDarkColor     = new Color(0, 0, 0, BG_DARK_ALPHA);

    private Timer timer;
    private boolean isPlayerOneMatchWin = true;

    public BattleMessageOverlay(Font messageFont) {
        this.messageFont = messageFont;
        setOpaque(false);
        setLayout(null);
    }

    /** Show a "Round X... Begins" message. */
    public void showRoundStart(int roundNumber) {
        displayText = "Round " + roundNumber + "... Begins";
        startAnimation(MessageType.ROUND_START);
    }

    /** Show a "Player N Wins" (round win) message. */
    public void showRoundWin(boolean playerOneWon, int roundNumber) {
        displayText = (playerOneWon ? "Player 1" : "Player 2") + " Wins";
        startAnimation(MessageType.ROUND_WIN);
    }

    /** Show match-end message: Victory or KO. */
    public void showMatchResult(boolean playerOneWon, boolean isKO) {
        isPlayerOneMatchWin = playerOneWon;
        if (isKO) {
            displayText = playerOneWon ? "KO Victory" : "KO Victory";
        } else {
            displayText = playerOneWon ? "Victory" : "Victory";
        }
        startAnimation(isKO ? MessageType.KO_DEFEAT : MessageType.VICTORY);
    }

    /** Show custom KO message for Survival mode. */
    public void showSurvivalKO(boolean isKO) {
        isPlayerOneMatchWin = false;
        displayText = isKO ? "YOU LOSE" : "Victory";
        startAnimation(isKO ? MessageType.KO_DEFEAT : MessageType.VICTORY);
    }

    /** Start the full fade-in/hold/fade-out cycle. */
    private void startAnimation(MessageType type) {
        state = State.FADE_IN;
        animationStart = System.currentTimeMillis();

        switch (type) {
            case ROUND_START, ROUND_WIN -> {
                animationDuration = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
            }
            case VICTORY, KO_DEFEAT -> {
                // Match-end messages stay longer
                animationDuration = FADE_IN_MS + (HOLD_MS * 2) + FADE_OUT_MS;
            }
        }

        // If timer not running, create one that checks progress
        if (timer == null) {
            timer = new Timer(16, this::onTimerTick);
            timer.setRepeats(true);
        }
        timer.start();
        repaint();
    }

    private void onTimerTick(ActionEvent e) {
        if (state == State.HIDDEN) {
            timer.stop();
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStart;

        if (elapsed >= animationDuration) {
            state = State.HIDDEN;
            timer.stop();
            Runnable cb = onHide;
            onHide = null;
            if (cb != null) cb.run();
            repaint();
            return;
        }

        // State transitions
        int fadeInEnd = FADE_IN_MS;
        int holdEnd = FADE_IN_MS + HOLD_MS;
        if (animationDuration > (FADE_IN_MS + HOLD_MS + FADE_OUT_MS)) {
            holdEnd = FADE_IN_MS + (HOLD_MS * 2);
        }

        State newState = state;
        if (elapsed < fadeInEnd) {
            newState = State.FADE_IN;
        } else if (elapsed < holdEnd) {
            newState = State.HOLD;
        } else {
            newState = State.FADE_OUT;
        }

        if (newState != state) {
            state = newState;
        }
        repaint();
    }

    /** @return current alpha [0,1] based on state and elapsed time. */
    private float getCurrentAlpha() {
        long elapsed = System.currentTimeMillis() - animationStart;

        switch (state) {
            case FADE_IN:
                return Math.min(1.0f, (float) elapsed / FADE_IN_MS);
            case HOLD:
                return 1.0f;
            case FADE_OUT:
                int fadeOutStart = animationDuration - FADE_OUT_MS;
                long fadeOutElapsed = elapsed - fadeOutStart;
                return 1.0f - Math.min(1.0f, (float) fadeOutElapsed / FADE_OUT_MS);
            default:
                return 0.0f;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (state == State.HIDDEN) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float alpha = getCurrentAlpha();
        if (alpha <= 0.01f) {
            g2.dispose();
            return;
        }

        // -- 1) Darken background a little so text pops --
        g2.setColor(new Color(0, 0, 0, (int) (BG_DARK_ALPHA * alpha)));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // -- 2) Draw main message text --
        int fontSize = 64;
        // Slightly smaller on very narrow panels
        if (getWidth() < 900) fontSize = 48;
        if (getWidth() < 600) fontSize = 38;

        // Check if this is a survival KO message (contains "YOU LOSE")
        if (displayText.contains("YOU LOSE")) {
            // Render as two lines: "YOU LOSE" at full size, "AI WINS" smaller below
            Font mainLineFont = messageFont.deriveFont(Font.BOLD, fontSize * Math.min(getHeight() / 500f, 1.3f));
            Font smallLineFont = messageFont.deriveFont(Font.BOLD, fontSize * 0.6f * Math.min(getHeight() / 500f, 1.3f));
            
            g2.setFont(mainLineFont);
            FontMetrics fmMain = g2.getFontMetrics();
            int mainTextW = fmMain.stringWidth("YOU LOSE");
            int mainTextH = fmMain.getAscent();
            int mainX = (getWidth() - mainTextW) / 2;
            int mainY = (getHeight() - mainTextH) / 2 - 20;
            
            // Shadow for main line
            g2.setColor(new Color(0, 0, 0, (int) (180 * alpha)));
            g2.drawString("YOU LOSE", mainX + 4, mainY + 4);
            
            // Main text color for main line
            float textAlpha = Math.min(1.0f, alpha * 1.2f);
            Color mainColor = new Color(
                    textColor.getRed(),
                    textColor.getGreen(),
                    textColor.getBlue(),
                    (int) (textColor.getAlpha() * textAlpha));
            g2.setColor(mainColor);
            g2.drawString("YOU LOSE", mainX, mainY);
            
            // Now render "AI WINS" smaller below
            g2.setFont(smallLineFont);
            FontMetrics fmSmall = g2.getFontMetrics();
            int smallTextW = fmSmall.stringWidth("AI WINS");
            int smallTextH = fmSmall.getAscent();
            int smallX = (getWidth() - smallTextW) / 2;
            int smallY = mainY + mainTextH + 15;
            
            // Shadow for small line
            g2.setColor(new Color(0, 0, 0, (int) (180 * alpha)));
            g2.drawString("AI WINS", smallX + 4, smallY + 4);
            
            // Small text color
            g2.setColor(mainColor);
            g2.drawString("AI WINS", smallX, smallY);
        } else {
            // Original single-line rendering
            Font useFont = messageFont.deriveFont(Font.BOLD, fontSize * Math.min(getHeight() / 500f, 1.3f));
            g2.setFont(useFont);

            FontMetrics fm = g2.getFontMetrics();
            int textW = fm.stringWidth(displayText);
            int textH = fm.getAscent();
            int x = (getWidth()  - textW) / 2;
            int y = (getHeight() + textH) / 2;

            // Shadow
            g2.setColor(new Color(0, 0, 0, (int) (180 * alpha)));
            g2.drawString(displayText, x + 4, y + 4);

            // Main text color
            float textAlpha = Math.min(1.0f, alpha * 1.2f);
            Color mainColor = new Color(
                    textColor.getRed(),
                    textColor.getGreen(),
                    textColor.getBlue(),
                    (int) (textColor.getAlpha() * textAlpha));
            g2.setColor(mainColor);
            g2.drawString(displayText, x, y);
        }

        // -- 3) Optional extra line for match result --
        if (state == State.HOLD && (displayText.equals("Victory") || displayText.equals("Defeat")
                || displayText.equals("KO Victory") || displayText.equals("KO Defeat"))) {

            String extra = displayText.contains("KO")
                    ? (isPlayerOneMatchWin ? "Player 1 Wins Match" : "Player 2 Wins Match")
                    : (isPlayerOneMatchWin ? "Player 1 Wins Match" : "Player 2 Wins Match");

            Font smallFont = messageFont.deriveFont(Font.BOLD, fontSize * 0.45f);
            g2.setFont(smallFont);
            FontMetrics fm2 = g2.getFontMetrics();
            int extraW = fm2.stringWidth(extra);
            int extraX = (getWidth() - extraW) / 2;
            int extraY = (getHeight() + fontSize) / 2 + fm2.getAscent() + 12;

            g2.setColor(new Color(0, 0, 0, (int) (140 * alpha)));
            g2.drawString(extra, extraX + 2, extraY + 2);
            float textAlpha = Math.min(1.0f, alpha * 1.2f);
            g2.setColor(new Color(200, 180, 150, (int) (220 * textAlpha)));
            g2.drawString(extra, extraX, extraY);
        }

        g2.dispose();
    }

    /** Hide immediately and reset state. */
    public void hideNow() {
        state = State.HIDDEN;
        if (timer != null) {
            timer.stop();
        }
        onHide = null;
        repaint();
    }

    /** Set a callback to run when the overlay finishes hiding. */
    public void setOnHide(Runnable callback) {
        this.onHide = callback;
    }

    /** @return true if an animation is currently visible or fading. */
    public boolean isAnimating() {
        return state != State.HIDDEN;
    }

    private Runnable onHide = null;}
