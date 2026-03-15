package engine;

import assets.Utility.FontManager;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;

    public GameWindow() {
        layout = new CardLayout();
        container = new JPanel(layout);

        IntroPanel intro = new IntroPanel(this);
        GamePanel game = new GamePanel();

        container.add(intro, "INTRO");
        container.add(game, "GAME");

        add(container);

        setTitle("OOP2 Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        layout.show(container, "INTRO");
    }


    public void showGamePanel() {
        layout.show(container, "GAME");
    }
}