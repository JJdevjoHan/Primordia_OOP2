package engine.gameplay;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractGamePanel extends JPanel {
    // Shared constants for all panels
    protected final int TILE_SIZE = 128;
    protected final int SCREEN_WIDTH = TILE_SIZE * 12;
    protected final int SCREEN_HEIGHT = TILE_SIZE * 7;

    public AbstractGamePanel() {
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setFocusable(true);
    }

    // Abstract methods: Every panel MUST implement these,
    // but the logic will differ (e.g., Menu vs. Combat)
    public abstract void updateLogic();
    public abstract void resetPanel();

    // Every panel needs to paint, but we can provide a default background clear
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
