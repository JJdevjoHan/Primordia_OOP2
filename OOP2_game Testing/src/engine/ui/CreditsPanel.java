package engine.ui;

import assets.Utility.BackButton;
import engine.core.GameWindow;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class CreditsPanel extends JPanel {

    private Image backgroundImage;
    private JButton backButton;

    private final int tileSize = 128;
    private final int screenWidth = tileSize * 12;
    private final int screenHeight = tileSize * 7;

    public CreditsPanel(GameWindow window) {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setLayout(null);

        loadBackground("/assets/maps/credits.png");

        backButton = new JButton("Back");
        backButton.addActionListener(e -> window.showIntro());
        add(backButton);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (backButton != null) {
            int margin = 20;
            int buttonSize = 72;
            backButton.setBounds(getWidth() - buttonSize - margin, margin, buttonSize, 36);
        }
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