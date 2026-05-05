package assets.Utility;

import assets.Utility.ButtonTextRenderer;
import assets.Utility.FontManager;
import engine.GameWindow;
import engine.SoundManager;

import javax.swing.*;
import java.awt.*;

public class CreditsButton {

    public JButton createCreditsButton(GameWindow window, SoundManager sound) {
        final String label = "CREDITS";
        JButton creditsButton = new JButton("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonTextRenderer.drawCenteredText(g2, this, label, 3);
                g2.dispose();
            }
        };

        creditsButton.setFont(FontManager.getFont(24f));
        creditsButton.setForeground(Color.WHITE);
        creditsButton.setBackground(new Color(30, 30, 50));
        creditsButton.setFocusPainted(false);
        creditsButton.setOpaque(true);
        creditsButton.setContentAreaFilled(true);
        creditsButton.setBorder(BorderFactory.createLineBorder(new Color(200, 160, 40), 2));

        creditsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                creditsButton.setBackground(new Color(70, 70, 100));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                creditsButton.setBackground(new Color(30, 30, 50));
            }
        });

        creditsButton.addActionListener(e -> {
            sound.setFile(8);
            sound.play();
            window.showCredits();
        });

        return creditsButton;
    }
}