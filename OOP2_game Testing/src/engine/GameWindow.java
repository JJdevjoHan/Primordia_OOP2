package engine;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;
    private CharacterSelectionPanel characterSelectionPanel;
    private GamePanel gamePanel;

    public GameWindow() {

        layout = new CardLayout();
        container = new JPanel(layout);

        IntroPanel intro = new IntroPanel(this);
        GameModeSelector menu = new GameModeSelector(this);
        characterSelectionPanel = new CharacterSelectionPanel(this);
        gamePanel = new GamePanel();


        container.add(intro, "INTRO");
        container.add(menu, "MENU");
        container.add(characterSelectionPanel, "CHAR_SELECT");
        container.add(gamePanel, "GAME");

        add(container);

        setTitle("Primordia");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        layout.show(container, "INTRO");
    }

    public void showMenu(){

        layout.show(container,"MENU");
    }

    public void showGamePanel(){
        layout.show(container,"GAME");
    }

    public void showCharacterSelection() {
        characterSelectionPanel.resetSelectionState();
        layout.show(container, "CHAR_SELECT");
    }

    public void startPvPMatch(int playerOneCharacterIndex, int playerTwoCharacterIndex) {
        container.remove(gamePanel);
        gamePanel = new GamePanel(playerOneCharacterIndex, playerTwoCharacterIndex);
        container.add(gamePanel, "GAME");
        container.revalidate();
        container.repaint();
        layout.show(container, "GAME");
        gamePanel.requestFocusInWindow();
    }

}