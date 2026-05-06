package engine;

import assets.Utility.FontManager;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class CreditsPanel extends JPanel {

    private Image backgroundImage;
    private GameWindow window;

    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    private final SoundManager sound = new SoundManager();

    public CreditsPanel(GameWindow window) {
        this.window = window;

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(null);

        loadBackground("/assets/maps/credits.png");
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // No back button anymore.
    }

    private void loadBackground(String path) {
        URL resource = getClass().getResource(path);
        if (resource != null) {
            backgroundImage = new ImageIcon(resource).getImage();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}