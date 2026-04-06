package engine;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {

    private CardLayout layout;
    private JPanel container;

    public GameWindow() {

        layout = new CardLayout();
        container = new JPanel(layout);

        IntroPanel intro = new IntroPanel(this);
        GameModeSelector menu = new GameModeSelector(this);

        container.add(intro, "INTRO");
        container.add(menu, "MENU");

        container.add(new JPanel(), "CHAR_SELECT");
        container.add(new JPanel(), "GAME");
        container.add(new JPanel(), "SURVIVAL");

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

    public void showSurivivalGamePanel() {
        layout.show(container,"SURVIVAL");
    }

    public void showCharacterSelection(GameMode mode) {

        CharacterSelectionPanel panel = new CharacterSelectionPanel(this, mode);

        container.add(panel, "CHAR_SELECT");
        layout.show(container, "CHAR_SELECT");

        container.revalidate();
        container.repaint();
        panel.requestFocusInWindow();
    }

    public void startPvPMatch(int p1, int p2) {

        GamePanel panel = new GamePanel(p1, p2);

        container.add(panel, "GAME");
        layout.show(container, "GAME");

        container.revalidate();
        container.repaint();
        panel.requestFocusInWindow();
    }

    public void startSurvivalMatch(int playerIndex) {

        int botIndex = (int)(Math.random() * GamePanel.ALL_CHARACTERS.size());

        SurivivalGamePanel surivivalGamePanel = new SurivivalGamePanel(
                playerIndex,
                botIndex,
                GameMode.SURVIVAL,
                BotAI.Difficulty.NORMAL
        );

        container.add(surivivalGamePanel, "SURVIVAL");
        layout.show(container, "SURVIVAL");

        container.revalidate();
        container.repaint();
        surivivalGamePanel.requestFocusInWindow();
    }

    public void startArcadeMatch(int focusedIndex) {
    }
}