package assets.Utility;

import assets.Utility.FontManager;
import engine.GameWindow;
import engine.SoundManager;

import javax.swing.*;
import java.awt.*;

public class CreditsButton {

    public JButton createCreditsButton(GameWindow window, SoundManager sound) {
        JButton creditsButton = new JButton("CREDITS");

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